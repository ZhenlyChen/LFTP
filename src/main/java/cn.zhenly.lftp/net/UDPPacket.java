package cn.zhenly.lftp.net;

import java.io.*;
import java.net.InetSocketAddress;

// 基本数据包
public class UDPPacket implements Serializable {
  private transient ACKCallBack callBack;
  private transient InetSocketAddress from;
  private transient long time;
  private int winSize; // 窗口大小 (拥塞控制)
  private int seq; // 序列号
  private int ack; // 确认号
  private int session; // 会话ID
  private boolean END; // 序列结束标志位
  private boolean ACK; // ACK标志位
  private boolean FIN; // FIN标志位
  private byte[] data; // 数据包

  ACKCallBack getCallBack() {
    return callBack;
  }

  void setCallBack(ACKCallBack callBack) {
    this.callBack = callBack;
  }

  long getTime() {
    return time;
  }

  void setTime(long time) {
    this.time = time;
  }

  public InetSocketAddress getFrom() {
    return from;
  }

  void setFrom(InetSocketAddress from) {
    this.from = from;
  }

  boolean isEND() {
    return END;
  }

  void setEND(boolean END) {
    this.END = END;
  }

  public int getSession() {
    return session;
  }

  public void setSession(int session) {
    this.session = session;
  }

  public interface ACKCallBack {
    void success(UDPPacket data);
  }

  UDPPacket(int seq) {
    this.seq = seq;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  int getAck() {
    return ack;
  }

  void setAck(int ack) {
    this.ACK = true;
    this.ack = ack;
  }

  int getSeq() {
    return seq;
  }

  public void setSeq(int seq) {
    this.seq = seq;
  }

  public boolean isACK() {
    return ACK;
  }

  void setACK(boolean ACK) {
    this.ACK = ACK;
  }

  boolean isFIN() {
    return FIN;
  }

  void setFIN(boolean FIN) {
    this.FIN = FIN;
  }

  public int getWinSize() {
    return winSize;
  }

  public void setWinSize(int winSize) {
    this.winSize = winSize;
  }

}
