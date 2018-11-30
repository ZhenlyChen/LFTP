package cn.zhenly.lftp.net;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetSocket implements AutoCloseable {
  private int seq; // 包序号
  private DatagramSocket socket;
  private DatagramChannel channel;
  private LinkedList<UDPPacket> sendPackets; // 发送缓冲区
  private LinkedList<UDPPacket> receivePackets; // 接受缓冲区
  private AtomicBoolean running; // 是否在发送
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


  public NetSocket(int port, boolean isBlockMode) {
    blockMode = isBlockMode;
    initSocket(port);
    estimateRTT = 0;
  }

  public NetSocket(int port, InetSocketAddress target, boolean isBlockMode) {
    blockMode = isBlockMode;
    initSocket(port);
    this.targetAddress = target;
  }

  // 初始化自身socket
  private void initSocket(int port) {
    this.running = new AtomicBoolean(false);
    this.ackPacketNo = 0;
    this.timeoutInterval = 0;
    this.seq = 0;
    this.cwnd = 1;
    this.ssthresh = 64;
    this.sendPackets = new LinkedList<>();
    try {
      if (blockMode) {
        socket = new DatagramSocket(port);
      } else {
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(port));
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
      if (ackPacketNo == -1) {
        ackPacketNo = data.getSeq();
      } else if (ackPacketNo + 1 == data.getSeq()) {
        ackPacketNo++;
      } else if (ackPacketNo > data.getSeq()) {
        continue;
      }
      ackPacket = new UDPPacket(seq++);
      ackPacket.setACK(true);
      ackPacket.setAck(ackPacketNo);
      if (data.isFIN()) {
        ackPacket.setFIN(true);
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
    System.out.println("Finish!");
    System.out.println("Port " + socket.getLocalPort() + " Closed.");
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
    this.sendPackets.add(packet);
    if (running.compareAndSet(false, true)) {
      sendPacket();
    }
  }

  private void sendPacket() {
    while (true) {
      // 并行发送
      boolean goodbye = false;
      for (int i = 0; i < cwnd && i < sendPackets.size(); i++) {
        try {
          UDPPacket packet = sendPackets.get(i);
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
        if (blockMode) {
          System.out.println("Port " + socket.getLocalPort() + " Closed.");
        } else {
          System.out.println("Port " + channel.socket().getLocalPort() + " Closed.");
        }
        break;
      }
      // 接受
      UDPPacket packet;
      if (!blockMode) {
        packet = UDPReceive();
      } else {
        packet = UDPReceiveBlock(2000);
      }
      if (packet != null && packet.isACK()) {
        onACK(packet);
      } else { // 接收超时
        ssthresh = cwnd / 2;
        cwnd = ssthresh + 1;
        dupACKCount = 0;
      }
      if (timeoutInterval > (10 * 1000 * (long) (1000 * 1000))) { // 超时10秒
        System.out.println("[ERROR] Time out! Over!");
        break;
      }
      if (sendPackets.size() == 0 || packet != null && packet.isFIN()) { // 没有等待中的ACK了
        break;
      } else if (!blockMode) {
        long start = System.nanoTime();
        while (System.nanoTime() - start < timeoutInterval) ; // 等待预计RTT
      }

    }
    running.set(false);
  }

  // 接收到
  private void onACK(UDPPacket packet) {
    if (packet.isFIN()) {
      sendPackets.clear();
      return;
    }
    if (packet.getAck() != sendPackets.getFirst().getSeq()) { // 非 ACK 或 不正确的ACK 序号
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
    } else if (packet.getAck() == sendPackets.getFirst().getSeq()) { // 收到正确的ACK
      if (cwnd < ssthresh) { // 小于阈值
        cwnd *= 2; // TCP Tahoe
      } else { // 大于阈值
        cwnd++; // TCP Reno
      }
      lastACK = packet.getAck();
      dupACKCount = 0;
      if (sendPackets.size() == 0) {
        System.out.println("[ERROR] System Error! Null Buffer!");
        return;
      }
      UDPPacket sourcePacket = sendPackets.removeFirst();
      updateRTT((System.nanoTime()) - sourcePacket.getTime()); // 估计 RTT
      if (sourcePacket.getCallBack() != null) {
        sourcePacket.getCallBack().success(packet);
      }
    }
  }

  private void updateRTT(long rtt) {
    if (this.estimateRTT == 0) this.estimateRTT = rtt;
    double a = 0.125;
    this.estimateRTT = (long) ((1 - a) * this.estimateRTT + a * rtt);
    if (this.devRTT == 0) this.devRTT = rtt;
    double b = 0.25;
    this.devRTT = (long) ((1 - b) * this.devRTT + b * Math.abs(this.estimateRTT - rtt));
    this.timeoutInterval = this.estimateRTT + 6 * this.devRTT;
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
    packet.setFIN(true);
    packet.setCallBack(callBack);
    packet.setEND(true);
    addPackToQueue(packet);
    System.out.println("Finish!");
  }

}
