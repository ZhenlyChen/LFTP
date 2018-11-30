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
  private final Semaphore semaphoreRecv = new Semaphore(MAX_BUFFER_SIZE); // 接收信号量
  private int seq; // 包序号
  private int selfPort;
  private DatagramSocket socket;
  private DatagramChannel channel;
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

  public NetSocket(int port, boolean isBlockMode) {
    blockMode = isBlockMode;
    this.selfPort = port;
    initSocket();
  }

  public NetSocket(int port, InetSocketAddress target, boolean isBlockMode) {
    blockMode = isBlockMode;
    this.selfPort = port;
    this.targetAddress = target;
    initSocket();
  }

  // 初始化自身socket
  private void initSocket() {
    this.sendThread = null;
    this.recvThread = null;
    this.ackPacketNo = 0;
    this.timeoutInterval = 0;
    this.seq = 0;
    this.cwnd = 1;
    this.ssthresh = 64;
    this.sendPackets = new LinkedList<>();
    this.receivePackets = new LinkedList<>();
    this.sendingPacket = new LinkedList<>();
    try {
      System.out.println("[INFO] Port " + selfPort + " is Listening.");
      if (blockMode) {
        socket = new DatagramSocket(selfPort);
      } else {
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(selfPort));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
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

  public void listen(listenCallBack callBack, int timeout) {
    this.ackPacketNo = -1;
    while (true) {
      UDPPacket data = UDPReceiveBlock(timeout);
      if (data == null) {
        System.out.println("Listen time out!");
        break;
      }
      if (data.isEND()) {
        ackPacketNo = -1;
      }
      UDPPacket ackPacket;
      if (ackPacketNo == -1) { // 新的数据
        ackPacketNo = data.getSeq();
      } else if (ackPacketNo + 1 == data.getSeq()) { // 正确数据
        ackPacketNo++;
      } else if (ackPacketNo > data.getSeq()) { // 失序数据
        continue;
      }
      ackPacket = new UDPPacket(seq++);
      ackPacket.setACK();
      ackPacket.setAck(ackPacketNo);
      ackPacket.setWinSize(semaphoreRecv.availablePermits());
      if (data.isFIN()) {
        ackPacket.setFIN();
      } else {
        ackPacket = callBack.Receive(data, ackPacket);
      }
      try {
        UDPSend(ackPacket, data.getFrom());
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (data.isFIN()) {
        break;
      }
    }
  }

  public void send(byte[] content, UDPPacket.ACKCallBack callBack, boolean isEnd, int session) {
    UDPPacket packet = new UDPPacket(seq++);
    packet.setCallBack(callBack);
    packet.setData(content);
    packet.setEND(isEnd);
    packet.setSession(session);
    addPackToQueue(packet);
  }

  private void addPackToQueue(UDPPacket packet) {
    try {
      semaphoreSend.acquire();
      sendBuffRWLock.writeLock().lock();
      this.sendPackets.add(packet);
      sendBuffRWLock.writeLock().unlock();
      if (sendThread == null || !sendThread.isAlive()) {
        this.sendThread = new Thread(this::sendPacket);
        sendThread.start();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void sendPacket() {
    long lastACKTime = System.nanoTime();
    while (true) {
      boolean goodbye = false;
      sendBuffRWLock.writeLock().lock();
      while (sendingPacket.size() <= cwnd && sendPackets.size() > 0) {
        sendingPacket.add(sendPackets.removeFirst());
        semaphoreSend.release();
      }
      sendBuffRWLock.writeLock().unlock();

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
      // 接受
      long start = System.nanoTime();
      boolean exit = false;
      do {
        UDPPacket packet;
        if (!blockMode) {
          packet = UDPReceive();
        } else {
          packet = UDPReceiveBlock(2000);
        }
        if (packet != null && packet.isACK()) {
          lastACKTime = System.nanoTime();
          onACK(packet);
        }
        if (packet != null && packet.isFIN()) {
          this.close();
          exit = true;
          break;
        }
        if (sendingPacket.size() == 0) { // 没有等待中的ACK了
          if (sendPackets.size() == 0) exit = true; // 没有需要发的包了
          break;
        }
      } while (System.nanoTime() - start < timeoutInterval && !blockMode);
      if (exit) break;
      if (System.nanoTime() - lastACKTime > (10 * 1000 * (long) (1000 * 1000))) { // 超时10秒
        System.out.println("[ERROR] Time out! Over!");
        this.close();
        break;
      }
      // 第一个包是否已经超时
      if (sendingPacket.size() != 0 && System.nanoTime() - sendingPacket.getFirst().getTime() > timeoutInterval) {
        ssthresh = cwnd / 2;
        cwnd = ssthresh + 1;
        dupACKCount = 0;
      }
    }
    if (sendPackets.size() != 0) sendPacket();
  }

  // 接收到
  private void onACK(UDPPacket packet) {
    if (packet.isFIN()) {
      sendingPacket.clear();
      return;
    }
    if (packet.getAck() != sendingPacket.getFirst().getSeq()) { // 非 ACK 或 不正确的ACK 序号
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
    } else if (packet.getAck() == sendingPacket.getFirst().getSeq()) { // 收到正确的ACK
      if (cwnd < ssthresh) { // 小于阈值
        cwnd *= 2; // TCP Tahoe
      } else { // 大于阈值
        cwnd++; // TCP Reno
      }
      lastACK = packet.getAck();
      dupACKCount = 0;
      if (sendingPacket.size() == 0) {
        System.out.println("[ERROR] System Error! Null Buffer!");
        return;
      }
      if (packet.getWinSize() < 100) { // 接收缓冲区快满了
        ssthresh = (cwnd / 2) + 1;
        cwnd = ssthresh + 1;
      }
      UDPPacket sourcePacket = sendingPacket.removeFirst();
      updateRTT((System.nanoTime()) - sourcePacket.getTime()); // 估计 RTT
      if (sourcePacket.getCallBack() != null) {
        packet.setCallBack(sourcePacket.getCallBack());
        try {
          semaphoreRecv.acquire();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        recvBuffRWLock.writeLock().lock();
        receivePackets.add(packet);
        recvBuffRWLock.writeLock().unlock();
        if (recvThread == null || !recvThread.isAlive()) {
          this.recvThread = new Thread(this::dealRecvPacket);
          recvThread.start();
        }
      }
    }
  }


  private void dealRecvPacket() {
    recvBuffRWLock.writeLock().lock();
    while (receivePackets.size() > 0) {
      UDPPacket packet = receivePackets.removeFirst();
      semaphoreRecv.release();
      packet.getCallBack().success(packet);
    }
    recvBuffRWLock.writeLock().unlock();
  }

  private void updateRTT(long rtt) {
    if (this.estimateRTT == 0) this.estimateRTT = rtt;
    double a = 0.125;
    this.estimateRTT = (long) ((1 - a) * this.estimateRTT + a * rtt);
    if (this.devRTT == 0) this.devRTT = rtt;
    double b = 0.25;
    this.devRTT = (long) ((1 - b) * this.devRTT + b * Math.abs(this.estimateRTT - rtt));
    this.timeoutInterval = this.estimateRTT + 5 * this.devRTT;
  }

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
        channel.send(buffer, targetAddress);
      } else {
        channel.send(buffer, to);
      }

    }
  }

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
      return null;
    }
  }

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
  public void disconnect(UDPPacket.ACKCallBack callBack) {
    UDPPacket packet = new UDPPacket(seq++);
    packet.setFIN();
    packet.setEND(true);
    packet.setCallBack(callBack);
    addPackToQueue(packet);
  }
}
