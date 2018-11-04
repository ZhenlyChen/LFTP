package cn.zhenly.LFTP.NetUDP;

import java.io.*;
import java.net.DatagramPacket;
import java.util.Objects;

public class UDPPacket implements Serializable {
  private static final transient String TEMP_ENCODING = "ISO-8859-1";
  private static final transient String DEFAULT_ENCODING = "UTF-8";
  private transient DatagramPacket packet;
  private byte[] content;
  private int id;
  private PacketType type;

  public UDPPacket(int id, byte[] content, PacketType type) {
    this.id = id;
    this.content = content;
    this.type = type;
  }

  public byte[] getContent() {
    return content;
  }

  public void setContent(byte[] content) {
    this.content = content;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public PacketType getType() {
    return type;
  }

  public void setType(PacketType type) {
    this.type = type;
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
}