package cn.zhenly.lftp.net;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NetSocket implements AutoCloseable {
  private final static int MAX_BUFFER_SIZE = 4096; // 最多可以读取4M数据
  private final Semaphore semaphoreSend = new Semaphore(MAX_BUFFER_SIZE); // 信号量(防止一次性读取太多数据到内存中)
  private int seq; // 包序号
  private int selfPort; // 自身端口号
  private DatagramSocket socket; // 阻塞Socket
  private DatagramChannel channel; // 非阻塞Socket
  private LinkedList<UDPPacket> sendingPacket; // 发送窗口缓冲区
  private LinkedList<UDPPacket> sendPackets; // 发送数据缓冲区
  private ReadWriteLock sendBuffRWLock = new ReentrantReadWriteLock(); // 发送缓冲区读写🔒
  private LinkedList<UDPPacket> receivePackets; // 接收缓冲区
  private ReadWriteLock recvBuffRWLock = new ReentrantReadWriteLock(); // 接收缓冲区读写🔒
  private int cwnd; // 窗口大小
  private int ssthresh; // 阈值
  private long estimateRTT; // 估计往返时间
  private long devRTT; // 网络波动
  private long timeoutInterval; // 超时时间
  private int ackPacketNo; // 已确认发送包序号
  private int dupACKCount; // 冗余次数
  private int lastACK; // 已确认包序号
  private InetSocketAddress targetAddress; // 目标地址;
  private boolean blockMode; // 阻塞模式
  private Thread sendThread; // 发送线程
  private Thread recvThread; // 处理线程

  public NetSocket(int port, boolean isBlockMode) throws IOException {
    blockMode = isBlockMode;
    this.selfPort = port;
    initSocket();
  }

  public NetSocket(int port, InetSocketAddress target, boolean isBlockMode) throws IOException {
    blockMode = isBlockMode;
    this.selfPort = port;
    this.targetAddress = target;
    initSocket();
  }

  // 切换非阻塞模式
  public void switchToNonBlock() {
    assert (this.blockMode);
    this.blockMode = false;
    socket.close();
    try {
      channel = DatagramChannel.open();
      channel.configureBlocking(false);
      channel.socket().bind(new InetSocketAddress(selfPort));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // 设置目标地址
  public void setTargetAddress(InetSocketAddress targetAddress) {
    this.targetAddress = targetAddress;
  }

  // 初始化自身socket
  private void initSocket() throws IOException {
    this.sendThread = null;
    this.recvThread = null;
    this.ackPacketNo = 0;
    this.timeoutInterval = 10 * 1000 * 1000; // 10ms
    this.seq = 0;
    this.cwnd = 1;
    this.ssthresh = 64;
    this.sendPackets = new LinkedList<>();
    this.receivePackets = new LinkedList<>();
    this.sendingPacket = new LinkedList<>();
    if (blockMode) {
      socket = new DatagramSocket(selfPort);
    } else {
      channel = DatagramChannel.open();
      channel.configureBlocking(false);
      channel.socket().bind(new InetSocketAddress(selfPort));
    }
    System.out.println("[INFO] Listening in port " + selfPort);
  }

  @Override
  public void close() {
    if (blockMode) {
      socket.close();
    } else {
      channel.socket().close();
    }
    System.out.println("[INFO] Port " + selfPort + " Closed.");
  }

  public interface listenCallBack {
    UDPPacket Receive(UDPPacket data, UDPPacket ack);
  }

  // 监听信息
  public void listen(listenCallBack callBack, int timeout) {
    assert (blockMode); // 必须阻塞模式才能使用
    this.ackPacketNo = -1;
    while (true) {
      UDPPacket data = UDPReceiveBlock(timeout);
      if (data == null) {
        System.out.println("[ERROR] Timeout! Listening over!");
        break;
      }
      if (data.isEND()) {
        ackPacketNo = -1;
      }
      UDPPacket ackPacket;
      boolean validPacket = false;
      if (ackPacketNo == -1) { // 新的数据
        ackPacketNo = data.getSeq();
        validPacket = true;
      } else if (ackPacketNo + 1 == data.getSeq()) { // 正确数据
        ackPacketNo++;
        validPacket = true;
      }
      // System.out.println("Get: " + data.getSeq() + ", Want: "+ ackPacketNo);
      // 发送确认ACK报文
      ackPacket = new UDPPacket(seq++);
      ackPacket.setAck(ackPacketNo);
      if (data.isFIN()) {
        ackPacket.setFIN();
      } else if (validPacket) {
        ackPacket = callBack.Receive(data, ackPacket);
        if (!blockMode) {
          // 切换非阻塞模式
          break;
        }
      }
      try {
        UDPSend(ackPacket, data.getFrom());
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (ackPacket.getCallBack() != null) {
        ackPacket.getCallBack().success(null);
        break;
      }
      if (data.isFIN()) {
        break;
      }
    }
  }

  // 将发送数据加入缓存
  public void send(byte[] content, UDPPacket.ACKCallBack callBack, boolean isEnd, int session) {
    UDPPacket packet = new UDPPacket(seq++);
    packet.setCallBack(callBack);
    packet.setData(content);
    packet.setEND(isEnd);
    packet.setSession(session);
    addPackToSendQueue(packet);
  }

  // 将发送数据加入缓存
  public void send(byte[] content, UDPPacket.ACKCallBack callBack) {
    UDPPacket packet = new UDPPacket(seq++);
    packet.setCallBack(callBack);
    packet.setData(content);
    packet.setEND(true); // 非序列化数据包
    packet.setSession(0); // 非会话数据包
    addPackToSendQueue(packet);
  }


  // 加入发送队列
  private void addPackToSendQueue(UDPPacket packet) {
    try {
      // 发送缓存信号量
      semaphoreSend.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    sendBuffRWLock.writeLock().lock();
    // 添加发送数据到缓存中
    this.sendPackets.add(packet);
    sendBuffRWLock.writeLock().unlock();
    // 启动发送线程
    if (sendThread == null || !sendThread.isAlive()) {
      this.sendThread = new Thread(this::sendPacket);
      sendThread.start();
    }
  }

  // 加入接收队列
  private void addPackToRecvQueue(UDPPacket packet) {
    recvBuffRWLock.writeLock().lock();
    receivePackets.add(packet); // 加入接收缓存
    recvBuffRWLock.writeLock().unlock();
    if (recvThread == null || !recvThread.isAlive()) {
      this.recvThread = new Thread(this::dealRecvPacket);
      recvThread.start(); // 开启接收处理线程
    }
  }

  private void sendPacket() {
    // System.out.println("Begin send");
    // 记录上一次ACK的时间
    long lastACKTime = System.nanoTime();
    while (true) {
      if (blockMode && socket.isClosed() || !blockMode && channel.socket().isClosed()) {
        System.out.println("[ERROR] Socket is closed");
        break;
      }
      boolean goodbye = false;
      sendBuffRWLock.writeLock().lock();
      while (sendingPacket.size() <= cwnd && sendPackets.size() > 0) {
        sendingPacket.add(sendPackets.removeFirst());
        semaphoreSend.release();
      }
      sendBuffRWLock.writeLock().unlock();
      // System.out.println("Sending: " + sendingPacket.size() + ", Ready:" + sendPackets.size());
      for (int i = 0; i < sendingPacket.size(); i++) {
        try {
          UDPPacket packet = sendingPacket.get(i);
          packet.setTime(System.nanoTime());
          UDPSend(packet, null);
          if (packet.isFIN()) {
            goodbye = true;
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      if (goodbye) {
        this.close();
        break;
      }
      // 一个RTT时间内循环接收ACK报文
      long start = System.nanoTime();
      boolean exit = false;
      do {
        UDPPacket packet;
        if (!blockMode) {
          packet = UDPReceive();
        } else {
          packet = UDPReceiveBlock(2000);
        }
        if (packet != null && packet.isACK()) { // 接收到ACK报文
          lastACKTime = System.nanoTime();
          onACK(packet);
        }
        if (packet != null && packet.isFIN()) { // 收到结束报文
          this.close();
          exit = true;
          break;
        }
        if (sendingPacket.size() == 0) { // 没有等待中的ACK了
          if (sendPackets.size() == 0) exit = true; // 没有需要发的包了
          break;
        }
      } while (System.nanoTime() - start < timeoutInterval && !blockMode); // 一个RTT估计时间
      if (exit) break;
      if (System.nanoTime() - lastACKTime > (10 * 1000 * (long) (1000 * 1000))) { // 超时10秒结束
        System.out.println("[ERROR] Time out! Over!");
        this.close();
        break;
      }
      // 是否已经超时
      if (sendingPacket.size() != 0) {
        ssthresh = cwnd / 2;
        cwnd = ssthresh + 1;
        dupACKCount = 0;
      } else { // 全部已经发送
        if (cwnd < ssthresh) { // 小于阈值
          cwnd *= 2; // TCP Tahoe
        } else { // 大于阈值
          cwnd++; // TCP Reno
        }
      }
    }
    // if (sendPackets.size() != 0) sendPacket();
    // System.out.println("End send");
  }

  // 接收到ACK
  private void onACK(UDPPacket packet) {
    // System.out.println("Want: " + sendingPacket.getFirst().getSeq() + ", Get: "+ packet.getAck());
    if (packet.isFIN()) {
      sendingPacket.clear();
      return;
    }
    if (packet.getAck() < sendingPacket.getFirst().getSeq()) { // 非 ACK 或 不正确的ACK 序号
      if (packet.getAck() == lastACK) {
        dupACKCount++;
        if (dupACKCount == 3) {
          ssthresh = (cwnd / 2) + 1;
          cwnd = 1;
        }
      }
      ssthresh = (cwnd / 2) + 1;
      cwnd = ssthresh + 1; // 快速恢复
      lastACK = packet.getAck();
    } else {
      while (sendingPacket.size() != 0 && packet.getAck() >= sendingPacket.getFirst().getSeq()) { // 收到正确的ACK
        lastACK = packet.getAck();
        dupACKCount = 0;
        if (sendingPacket.size() == 0) {
          System.out.println("[ERROR] System Error! Null Buffer!");
          return;
        }
//      if (packet.getWinSize() < 100) { // 接收缓冲区快满了
//        ssthresh = (cwnd / 2) + 1; // 减少发送窗口
//        cwnd = ssthresh + 1;
//      }
        UDPPacket sourcePacket = sendingPacket.removeFirst();
        updateRTT((System.nanoTime()) - sourcePacket.getTime()); // 估计 RTT
        if (sourcePacket.getCallBack() != null) {
          packet.setCallBack(sourcePacket.getCallBack());
          addPackToRecvQueue(packet);
        }
      }
    }
  }

  // 处理接收包
  private void dealRecvPacket() {
    while (receivePackets.size() > 0) {
      recvBuffRWLock.writeLock().lock();
      UDPPacket packet = receivePackets.removeFirst();
      recvBuffRWLock.writeLock().unlock();
      packet.getCallBack().success(packet);
    }
  }

  // 修改RTT估计值
  private void updateRTT(long rtt) {
    if (this.estimateRTT == 0) this.estimateRTT = rtt;
    double a = 0.125;
    this.estimateRTT = (long) ((1 - a) * this.estimateRTT + a * rtt);
    if (this.devRTT == 0) this.devRTT = rtt;
    double b = 0.25;
    this.devRTT = (long) ((1 - b) * this.devRTT + b * Math.abs(this.estimateRTT - rtt));
    this.timeoutInterval = this.estimateRTT + 4 * this.devRTT;
  }

  // 封包发送
  private void UDPSend(UDPPacket packetData, InetSocketAddress to) throws IOException {
    byte[] data = ByteConverter.getByte(packetData); // Max: 1321
    if (data == null) throw new IOException();
    if (blockMode) {
      if (to != null) {
        socket.send(new DatagramPacket(data, data.length, to.getAddress(), to.getPort()));
      } else {
        socket.send(new DatagramPacket(data, data.length, targetAddress.getAddress(), targetAddress.getPort()));
      }
    } else {
      ByteBuffer buffer = ByteBuffer.wrap(data);
      if (to == null) {
        // System.out.println("send" + targetAddress.getAddress().getHostAddress() + ":" + targetAddress.getPort());
        channel.send(buffer, targetAddress);
      } else {
        channel.send(buffer, to);
      }

    }
  }

  // 阻塞接收
  private UDPPacket UDPReceiveBlock(int timeout) {
    byte[] buf = new byte[1400];
    DatagramPacket p = new DatagramPacket(buf, 1400);
    try {
      socket.setSoTimeout(timeout);
      socket.receive(p);
      UDPPacket packet = (UDPPacket) ByteConverter.ReadByte(p.getData());
      if (packet != null) {
        packet.setFrom(new InetSocketAddress(p.getAddress(), p.getPort()));
      }
      return packet;
    } catch (IOException e) {
      // e.printStackTrace();
      return null;
    }
  }

  // 非阻塞接收
  private UDPPacket UDPReceive() {
    ByteBuffer byteBuffer = ByteBuffer.allocate(1400);
    try {
      InetSocketAddress socketAddress = (InetSocketAddress) channel.receive(byteBuffer);
      UDPPacket packet = (UDPPacket) ByteConverter.ReadByte(byteBuffer.array());
      if (packet != null) {
        packet.setFrom(socketAddress);
      }
      return packet;
    } catch (IOException e) {
      return null;
    }
  }

  // 断开连接
  public void disconnect() {
    UDPPacket packet = new UDPPacket(seq++);
    packet.setFIN();
    packet.setEND(true);
    addPackToSendQueue(packet);
  }
}
