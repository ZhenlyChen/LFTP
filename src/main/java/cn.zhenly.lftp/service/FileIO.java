package cn.zhenly.lftp.service;

import java.io.*;

public class FileIO {
  private static int CHUNK_SIZE = 1024; // 默认块大小
  // 生成占位文件
  public static void makeFile(String fileName, long size) {
    try {
      RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
      raf.setLength(size);
      raf.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // 写入文件指定块
  public static void writeFileChunk(String fileName, byte[] data, int index) {
    try {
      RandomAccessFile out = new RandomAccessFile(fileName,"rw");
      out.skipBytes(index * CHUNK_SIZE);
      out.write(data);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // 读取文件指定块
  public static byte[] readFileChunk(String fileName, int index) {
    byte[] buf = new byte[CHUNK_SIZE];
    try {
      File file = new File(fileName);
      int chunks = (int) (file.length() / CHUNK_SIZE + (file.length() % CHUNK_SIZE == 0 ? 0 : 1));
      if (index >= chunks) return buf;
      InputStream inStream = new FileInputStream(file);
      long skip = inStream.skip(index * CHUNK_SIZE);
      if (skip != index * CHUNK_SIZE) return buf;
      int res = inStream.read(buf);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return buf;
  }

  // 获取文件块数
  public static int getFileChunkCount(String fileName) {
    File file = new File(fileName);
    return (int) (file.length() / CHUNK_SIZE + (file.length() % CHUNK_SIZE == 0 ? 0 : 1));
  }

  // 检查文件夹是否存在
  public static File checkDir(String dir) {
    File file = new File(dir);
    if (!file.exists()) {
      if (!file.mkdirs()) {
        System.out.println("[ERROR] Can't make directory " + dir + ".");
        return null;
      }
    } else if (!file.isDirectory()) {
      System.out.println("[ERROR] File " + dir + " has exist, can't create directory here.");
      return null;
    }
    System.out.println("[INFO] Data directory: " + dir);
    return file;
  }
}
