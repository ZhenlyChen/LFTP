package cn.zhenly.lftp.service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileIO {
  private static int CHUNK_SIZE = 1024; // 默认块大小

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

  //------------高性能文件IO---------------

  // 初始化文件
  public static void initFile(String fileName) {
    try {
      RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
      raf.setLength(0);
      raf.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //  连续追加写入
  public static void writeFileAppend(String fileName, List<byte[]> chunks) {
    // System.out.println("Write " + fileName + ",count:" + chunks.size());
    try {
      OutputStream outStream = new FileOutputStream(fileName, true);
      for (byte[] b: chunks) {
        outStream.write(b);
      }
      outStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  // 连续读取
  static List<byte[]> readFile(String fileName, long beginChunk, int chunkCount) {
    // System.out.println("Read " + fileName + ",begin:" + beginChunk + ", count:" + chunkCount);
    List<byte[]> fileChunks = new ArrayList<>();
    try {
      FileInputStream inStream = new FileInputStream(fileName);
      // System.out.println("Skip: " + beginChunk * CHUNK_SIZE);
      long skip = inStream.skip(beginChunk * CHUNK_SIZE);
      if (skip != beginChunk * CHUNK_SIZE) {
        System.out.println("[ERROR] Invalid Skip " + skip + " for " + beginChunk * CHUNK_SIZE);
      }
      while (chunkCount > 0) {
        byte[] buf = new byte[1024];
        int r = inStream.read(buf);
        fileChunks.add(buf);
        chunkCount--;
      }
      inStream.close();
      return fileChunks;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return fileChunks;
  }

  // 获取文件块数
  static long getFileChunkCount(String fileName) {
    File file = new File(fileName);
    return file.length() / CHUNK_SIZE + (file.length() % CHUNK_SIZE == 0 ? 0 : 1);
  }

}
