package cn.zhenly.LFTP.NetUDP;

import java.io.*;
import java.net.DatagramPacket;
import java.util.Objects;

public class UDPPacket implements Serializable {
  private static final transient String TEMP_ENCODING = "ISO-8859-1";
  private static final transient String DEFAULT_ENCODING = "UTF-8";
  private transient DatagramPacket packet;
  private int winSize; // 串口大小
  private int seq; // 序列号
  private int ack; // 确认号
  private boolean flagACK; // ACK标志位
  private boolean flagSYN; // SYN标志位
  private boolean flagFIN; // FIN标志位
  private int[] checkSum; // 校验和
  private byte[] data; // 数据包

  public UDPPacket(int seq) {
    this.seq = seq;
  }

  public boolean isValid() {
    // 检验校验和
    return true;
  }

  public byte[] getByte() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(this);
      out.flush();
      return bos.toByteArray();
    } catch (IOException e) {
      System.out.println("Error: Can't convert the packet to string.");
    }
    return null;
  }

  public static UDPPacket ReadByte(byte[] data) {
    try {
      return (UDPPacket) (new ObjectInputStream(new ByteArrayInputStream(data))).readObject();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null;
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
    this.flagACK = true;
    this.ack = ack;
  }

  public int getSeq() {
    return seq;
  }

  public void setSeq(int seq) {
    this.seq = seq;
  }

  public boolean isFlagACK() {
    return flagACK;
  }

  public void setFlagACK(boolean flagACK) {
    this.flagACK = flagACK;
  }

  public boolean isFlagSYN() {
    return flagSYN;
  }

  public void setFlagSYN(boolean flagSYN) {
    this.flagSYN = flagSYN;
  }

  public boolean isFlagFIN() {
    return flagFIN;
  }

  public void setFlagFIN(boolean flagFIN) {
    this.flagFIN = flagFIN;
  }

  public int getWinSize() {
    return winSize;
  }

  public void setWinSize(int winSize) {
    this.winSize = winSize;
  }

  public int[] getCheckSum() {
    return checkSum;
  }

  public void setCheckSum(int[] checkSum) {
    this.checkSum = checkSum;
  }
}