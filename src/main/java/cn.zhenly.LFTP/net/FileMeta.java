package cn.zhenly.lftp.net;

import java.io.Serializable;

// FileMeta 文件元信息
public class FileMeta implements Serializable {
  public String name; // 文件名
  public int size; // 文件大小
  public int chunkCount; // 文件分块
  public int port; // 传输端口
}
