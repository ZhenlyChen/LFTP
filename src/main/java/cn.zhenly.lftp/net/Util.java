package cn.zhenly.lftp.net;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Util {

  public static <T> byte[] getByte(T obj) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(obj);
      out.flush();
      return bos.toByteArray();
    } catch (IOException e) {
      System.out.println("Error: Can't convert the packet to string.");
    }
    return null;
  }

  public static Object ReadByte(byte[] data) {
    try {
      return (new ObjectInputStream(new ByteArrayInputStream(data))).readObject();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }
}
