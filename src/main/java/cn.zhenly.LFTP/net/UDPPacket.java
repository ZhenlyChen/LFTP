package cn.zhenly.lftp.net;

import java.io.*;
import java.net.DatagramPacket;

// 基本数据包
public class UDPPacket implements Serializable {
  private static final transient String TEMP_ENCODING = "ISO-8859-1";
  private static final transient String DEFAULT_ENCODING = "UTF-8";
  private transient ACKCallBack callBack;
  private transient DatagramPacket packet;
  private int winSize; // 窗口大小 (拥塞控制)
  private int seq; // 序列号
  private int ack; // 确认号
  private boolean ACK; // ACK标志位
  private boolean SYN; // SYN标志位
  private boolean FIN; // FIN标志位
  private byte[] data; // 数据包

  public ACKCallBack getCallBack() {
    return callBack;
  }

  public void setCallBack(ACKCallBack callBack) {
    this.callBack = callBack;
  }

  public interface ACKCallBack {
    void success(UDPPacket data);
  }

  public UDPPacket(int seq) {
    this.seq = seq;
  }

  public DatagramPacket getPacket() {
    return packet;
  }

  public void setPacket(DatagramPacket packet) {
    this.packet = packet;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public int getAck() {
    return ack;
  }

  public void setAck(int ack) {
    this.ACK = true;
    this.ack = ack;
  }

  public int getSeq() {
    return seq;
  }

  public void setSeq(int seq) {
    this.seq = seq;
  }

  public boolean isACK() {
    return ACK;
  }

  public void setACK(boolean ACK) {
    this.ACK = ACK;
  }

  public boolean isSYN() {
    return SYN;
  }

  public void setSYN(boolean SYN) {
    this.SYN = SYN;
  }

  public boolean isFIN() {
    return FIN;
  }

  public void setFIN(boolean FIN) {
    this.FIN = FIN;
  }

  public int getWinSize() {
    return winSize;
  }

  public void setWinSize(int winSize) {
    this.winSize = winSize;
  }

}