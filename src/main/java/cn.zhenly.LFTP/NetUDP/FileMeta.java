package cn.zhenly.LFTP.NetUDP;

import java.io.Serializable;

// FileMeta 文件元信息
public class FileMeta implements Serializable {
  public String name;
  public int size;
  public int chunkCount;
  public String[] hash;
}
