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
  private boolean END; // 序列结束标志位
  private boolean ACK; // ACK标志位
  private boolean FIN; // FIN标志位
  private byte[] data; // 数据包

  public ACKCallBack getCallBack() {
    return callBack;
  }

  public void setCallBack(ACKCallBack callBack) {
    this.callBack = callBack;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public InetSocketAddress getFrom() {
    return from;
  }

  public void setFrom(InetSocketAddress from) {
    this.from = from;
  }

  public boolean isEND() {
    return END;
  }

  public void setEND(boolean END) {
    this.END = END;
  }

  public interface ACKCallBack {
    void success(UDPPacket data);
  }

  public UDPPacket(int seq) {
    this.seq = seq;
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
