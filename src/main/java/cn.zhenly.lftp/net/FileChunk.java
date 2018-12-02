package cn.zhenly.lftp.net;

import java.io.Serializable;

// FileChunk 文件块数据
public class FileChunk implements Serializable {
  private String name; // 文件名
  private long id; // 分块ID
  private long count; // 分块数量
  private byte[] data; // 数据
  private long size; // 文件大小

  public FileChunk(String name, long id, long count, byte[] data, long size) {
    this.name = name;
    this.id = id;
    this.count = count;
    this.data = data;
    this.size = size;
  }

  public FileChunk(long id, byte[] data) {
    this.id = id;
    this.data = data;
  }

  public String getName() {
    return name;
  }

  public long getId() {
    return id;
  }

  public long getCount() {
    return count;
  }

  public byte[] getData() {
    return data;
  }

  public long getSize() {
    return size;
  }
}
