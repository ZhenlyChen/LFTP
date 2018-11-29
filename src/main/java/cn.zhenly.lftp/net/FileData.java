package cn.zhenly.lftp.net;

public class FileData {
  public String name;
  private int totalCount;
  private int validCount;
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
    if (chunk.getId() >= totalCount || chunk.getId() < 0 || chunks[chunk.getId()] != null) return false;
    chunks[chunk.getId()] = chunk;
    validCount++;
    return true;
  }
}
