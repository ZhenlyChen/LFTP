package cn.zhenly.lftp.net;

import java.io.Serializable;

// FileChunk 文件块数据
public class FileChunk implements Serializable {
  private String name; // 文件名
  private int id; // 分块ID
  private int count; // 分块数量
  private byte[] data; // 数据

  public FileChunk(String name, int id, int count, byte[] data) {
    this.name = name;
    this.id = id;
    this.count = count;
    this.data = data;
  }

  public String getName() {
    return name;
  }

  public int getId() {
    return id;
  }

  public int getCount() {
    return count;
  }

  public byte[] getData() {
    return data;
  }

}
