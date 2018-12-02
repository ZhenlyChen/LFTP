package cn.zhenly.lftp.net;

import cn.zhenly.lftp.service.FileIO;

import java.util.ArrayList;
import java.util.List;

public class FileData {
  private boolean initialized;
  private long totalCount;
  private String savePath;
  private long fileSize;
  private List<byte[]> cacheChunks;

  public FileData() {
    initialized = false;
  }

  public void init(String name, long totalCount, String dir, long fileSize) {
    initialized = true;
    this.cacheChunks = new ArrayList<>();
    this.totalCount = totalCount;
    this.savePath = dir + "/" + name;
    this.fileSize = fileSize;
    FileIO.initFile(savePath);
  }

  public void addChunk(FileChunk chunk) {
    if (chunk.getId() >= totalCount || chunk.getId() < 0) {
      System.out.println("[ERROR] Invalid file chunk " + chunk.getId() + "! GG!");
      return;
    }
    byte[] data = chunk.getData();
    if (chunk.getId() == totalCount - 1 && fileSize % 1024 != 0) { // 处理结尾空白
      byte[] lastData = new byte[(int) fileSize % 1024];
      System.arraycopy(data, 0, lastData, 0, lastData.length);
      data = lastData;
    }
    cacheChunks.add(data);
    if (cacheChunks.size() > 2048) flush();
  }

  public void flush() {
    FileIO.writeFileAppend(savePath, cacheChunks);
    cacheChunks.clear();
  }

  public boolean isInitialized() {
    return initialized;
  }

  public long getTotalCount() {
    return totalCount;
  }
}
