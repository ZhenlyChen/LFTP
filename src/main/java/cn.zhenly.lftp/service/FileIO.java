package cn.zhenly.lftp.service;

import cn.zhenly.lftp.net.FileData;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileIO {
  public static void saveFile(FileData fileData, String dir) {
    try {
      DataOutputStream out = new DataOutputStream(new FileOutputStream(dir + "./" + fileData.name));
      for (int i = 0; i < fileData.chunks.length; i++) {
        out.write(fileData.chunks[i].getData());
      }
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  static List<byte[]> readFile(File file) {
    try {
      InputStream inStream = new FileInputStream(file);
      List<byte[]> buffs = new ArrayList<>();
      byte[] buf = new byte[1024];
      // 分割文件
      while (inStream.read(buf) != -1) {
        buffs.add(buf.clone());
      }
      return buffs;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
  public static File getDir(String dir) {
    File file = new File(dir);
    if (!file.exists()) {
      if (!file.mkdir()) {
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
