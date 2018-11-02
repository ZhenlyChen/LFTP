package cn.zhenly.LFTP.NetUDP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class NetUDP {
  private InetAddress targetIP;
  private int targetPort;
  private DatagramSocket socket;

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
      e.printStackTrace();
    }
    return null;
  }

  public boolean UDPSend(UDPPacket packetData) {
    byte[] data = packetData.getByte();
    DatagramPacket packet = new DatagramPacket(data, data.length, this.targetIP, this.targetPort);
    try {
      socket.send(packet);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public void close() {
    socket.close();
  }
}
