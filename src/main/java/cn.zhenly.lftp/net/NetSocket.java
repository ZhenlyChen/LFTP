package cn.zhenly.lftp.net;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class NetSocket implements AutoCloseable {
  private int seq; // 包序号
  private int targetPort; // 目标端口
  private int selfPort;
  private InetAddress targetIP; // 目标地址
  private DatagramSocket socket;
  private Queue<UDPPacket> bufferPackets; // 发送缓冲区
  private boolean running; // 是否在发送
  private int cwnd; // 窗口大小
  private int ssthresh; // 阈值
  private long estimateRTT; // 往返时间
  private long devRTT;
  private long timeoutInterval;
  private boolean slowMode; // 慢启动模式
  private Selector selector;
  DatagramChannel socketChannel;
  private InetSocketAddress socketAddress;

  public NetSocket(int port) {
    initSocket(port);
    selfPort = port;
    estimateRTT = 0;
  }

  public NetSocket(int port, InetAddress targetIP, int targetPort) {
    initSocket(port);
    setTarget(targetIP, targetPort);
  }

  public AddressInfo getTarget() {
    AddressInfo addressInfo = new AddressInfo();
    addressInfo.ip = targetIP;
    addressInfo.port = targetPort;
    addressInfo.valid = true;
    return addressInfo;
  }

  // 设置目标
  private void setTarget(InetAddress targetIP, int targetPort) {
    this.targetPort = targetPort;
    this.targetIP = targetIP;
    this.socketAddress = new InetSocketAddress(targetIP, targetPort);
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
//      socketChannel = DatagramChannel.open();
//      socketChannel.configureBlocking(false); // 非阻塞
//      this.socket = socketChannel.socket();
//      socket.bind(new InetSocketAddress(port));
//      selector = Selector.open();
//      socketChannel.register(selector, SelectionKey.OP_READ);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private UDPPacket UDPReceive() {
    byte[] buf = new byte[1400];
    // ByteBuffer byteBuffer = ByteBuffer.allocate(1400);
    DatagramPacket p = new DatagramPacket(buf, 1400);
    try {
      // socketChannel.send(byteBuffer, socketAddress);
      socket.receive(p);
      UDPPacket packet = (UDPPacket) ByteConverter.ReadByte(p.getData());
      if (packet != null) {
        packet.setPacket(p);
      }
      return packet;
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public void close() {
    socket.close();
  }

  public interface listenCallBack {
    UDPPacket Receive(UDPPacket data, UDPPacket ack);
  }

  public void listen(listenCallBack callBack) {
    while (true) {
      UDPPacket data = UDPReceive();
      if (data != null) {
        // System.out.println("[DEBUG] Receive from " + data.getPacket().getAddress().getHostAddress() + ":" + data.getPacket().getPort());
        setTarget(data.getPacket().getAddress(), data.getPacket().getPort());
        UDPPacket ackPacket = new UDPPacket(seq++);
        ackPacket.setAck(data.getSeq());
        if (data.isFIN()) {
          ackPacket.setFIN(true);
          break;
        } else {
          ackPacket = callBack.Receive(data, ackPacket);
        }
        try {
          UDPSend(ackPacket);
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        System.out.println("TimeOut!");
        return;
      }
    }
    System.out.println("Finish!");
    System.out.println("Port " + socket.getLocalPort() + " Closed.");
    // callBack.Receive(null, null);
  }


  public void send(byte[] content, UDPPacket.ACKCallBack callBack) throws IOException {
    UDPPacket packet = new UDPPacket(seq++);
    packet.setCallBack(callBack);
    packet.setData(content);
    addPackToQueue(packet);
  }

  private void addPackToQueue(UDPPacket packet) throws IOException {
    this.bufferPackets.add(packet);
    if (!running) {
      run();
    }
  }

  // 发送数据
  private void run() throws IOException {
    this.running = true;
    socket.setSoTimeout(2000);
    // (new Thread(this::listenACK)).start();
    while (bufferPackets.size() > 0) {
      UDPPacket packet = bufferPackets.poll();
      // UDPSend(packet);
      // long nowTime = System.nanoTime();
      // while (nowTime + timeoutInterval > System.nanoTime()) {
      //   // Empty Loop
      // }
      int errorCount = 0;
      while (true) {
        packet.setTime(System.nanoTime());
        UDPSend(packet);
        if (packet.isFIN()) break;
        UDPPacket rec = UDPReceive();
        if (rec != null && ((rec.isACK() && rec.getAck() == packet.getSeq()) || rec.isFIN())) {
          long rtt = (System.nanoTime()) - packet.getTime();
          updateRRT(rtt);
          System.out.println("\tseq:" + packet.getSeq() + "\testimateRTT:" + rtt + "," + this.estimateRTT + "," + this.devRTT + "," + this.timeoutInterval);
          if (packet.getCallBack() != null) {
            packet.getCallBack().success(rec);
          }
          break;
        } else {
          errorCount++;
        }
        if (errorCount > 5) {
          System.out.println("[ERROR] System error.");
          System.exit(-1);
        }
      }
    }
    this.running = false;
  }

  private void updateRRT(long rtt) {
    if (this.estimateRTT == 0) this.estimateRTT = rtt;
    double a = 0.125;
    this.estimateRTT = (long) ((1 - a) * this.estimateRTT + a * rtt);
    if (this.devRTT == 0) this.devRTT = rtt;
    double b = 0.25;
    this.devRTT = (long) ((1 - b) * this.devRTT + b * Math.abs(this.estimateRTT - rtt));
    this.timeoutInterval = this.estimateRTT + 4 * this.devRTT;
  }

  private void UDPSend(UDPPacket packetData) throws IOException {
    byte[] data = ByteConverter.getByte(packetData);
    if (data == null) throw new IOException();
    DatagramPacket packet = new DatagramPacket(data, data.length, this.targetIP, this.targetPort);
    socket.send(packet);
    // ByteBuffer buffer = ByteBuffer.wrap(data);
    // socketChannel.send(buffer, new InetSocketAddress(targetIP, targetPort));
  }

  // 断开连接
  public void disconnect(UDPPacket.ACKCallBack callBack) throws IOException {
    UDPPacket packet = new UDPPacket(seq++);
    packet.setFIN(true);
    packet.setCallBack(callBack);
    addPackToQueue(packet);
  }

}
