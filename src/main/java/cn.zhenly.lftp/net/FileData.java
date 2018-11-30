package cn.zhenly.lftp.net;

import cn.zhenly.lftp.service.FileIO;

public class FileData {
  private int totalCount;
  private boolean[] validChunks;
  private String savePath;
  private long fileSize;

  public FileData(String name, int totalCount, String dir, long fileSize) {
    this.totalCount = totalCount;
    this.validChunks = new boolean[totalCount];
    this.savePath = dir + "./" +name;
    this.fileSize = fileSize;
    FileIO.getDir(dir);
    FileIO.makeFile(savePath, fileSize);
  }

  public boolean addChunk(FileChunk chunk) {
    if (chunk.getId() >= totalCount || chunk.getId() < 0 || validChunks[chunk.getId()]) return false;
    byte[] data = chunk.getData();
    if (chunk.getId() == totalCount - 1 && fileSize % 1024 != 0) { // 处理结尾空白
      byte[] lastData = new byte[(int) fileSize % 1024];
      System.arraycopy(data, 0, lastData, 0, lastData.length);
      data = lastData;
    }
    FileIO.writeFileChunk(savePath, data, chunk.getId());

    validChunks[chunk.getId()] = true;
    return true;
  }
}
