package cn.zhenly.lftp.net;

import java.util.List;

public class FileData {
  public String name;
  public int totalCount;
  public int validCount;
  public FileChunk[] chunks;

  public FileData(String name, int totalCount) {
    this.name = name;
    this.totalCount = totalCount;
    this.chunks = new FileChunk[totalCount];
  }

  public boolean isComplete() {
    return validCount == totalCount;
  }

  public boolean addChunk(FileChunk chunk) {
    if (chunk.getId() >= totalCount || chunk.getId() < 0 || chunks[chunk.getId()].isValid()) return false;
    chunk.setValid(true);
    chunks[chunk.getId()] = chunk;
    validCount++;
    return true;
  }
}
