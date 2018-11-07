package cn.zhenly.LFTP.NetUDP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class NetUDP {
  private int seq; // 包序号
  private int targetPort; // 目标端口
  private InetAddress targetIP; // 目标地址
  private DatagramSocket socket;
  private Queue<UDPPacket> bufferPackets; // 发送缓冲区
  private boolean running; // 是否在发送
  private int cwnd; // 窗口大小

  public NetUDP(int port) {
    initSocket(port);
  }

  public NetUDP(int port, InetAddress targetIP, int targetPort) {
    initSocket(port);
    setTarget(targetIP, targetPort);
  }

  // 设置目标
  public void setTarget(InetAddress targetIP, int targetPort) {
    this.targetPort = targetPort;
    this.targetIP = targetIP;
  }

  public void setTimeOut(int timeout) {
    try {
      this.socket.setSoTimeout(timeout);
    } catch (SocketException e) {
      e.printStackTrace();
    }
  }

  // 初始化自身socket
  private void initSocket(int port) {
    this.running = false;
    this.seq = 0;
    this.bufferPackets = new LinkedList<>();
    try {
      socket = new DatagramSocket(port);
    } catch (SocketException e) {
      e.printStackTrace();
    }
  }

  public UDPPacket UDPReceive() {
    byte[] buf = new byte[1024];
    DatagramPacket p = new DatagramPacket(buf, 1024);
    try {
      socket.receive(p);
      UDPPacket packet = UDPPacket.ReadByte(p.getData());
      if (packet != null) {
        packet.setPacket(p);
      }
      return packet;
    } catch (IOException e) {
      return null;
    }
  }

  public void listen() {
    while(true) {
      UDPPacket data = UDPReceive();
      System.out.println("server received data from client：");
      if (data != null) {
        System.out.println(data.getPacket().getAddress().getHostAddress() + ":" + data.getPacket().getPort());
        System.out.println(new String(data.getData()));
        System.out.println(data.getSeq());
        setTarget(data.getPacket().getAddress(), data.getPacket().getPort());
        UDPPacket ackPacket = new UDPPacket(seq++);
        ackPacket.setAck(data.getSeq());
        try {
          UDPSend(ackPacket);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }


  public void send(byte[] content) throws IOException {
    UDPPacket packet = new UDPPacket(seq++);
    packet.setData(content);
    this.bufferPackets.add(packet);
    if (!running) {
      run();
    }
  }

  // 发送数据
  private void run() throws IOException {
    this.running = true;
    socket.setSoTimeout(2000);
    while (bufferPackets.size() > 0) {
      UDPPacket packet = bufferPackets.poll();
      while (true) {
        UDPSend(packet);
        UDPPacket rec = UDPReceive();
        if (rec != null && rec.isValid() && rec.isFlagACK() && rec.getAck() == packet.getSeq()) {
          break;
        }
      }
    }
    this.running = false;
  }

  public void UDPSend(UDPPacket packetData) throws IOException {
    byte[] data = packetData.getByte();
    DatagramPacket packet = new DatagramPacket(data, data.length, this.targetIP, this.targetPort);
    socket.send(packet);
  }

  public void close() {
    socket.close();
  }
}
