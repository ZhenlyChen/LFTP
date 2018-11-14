package cn.zhenly.LFTP.NetUDP;

import java.io.Serializable;

// FileChunk 文件块数据
public class FileChunk implements Serializable {
  public String name;
  public int id;
  public byte[] data;
  public String hash;
}
