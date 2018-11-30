package cn.zhenly.lftp.service;

import cn.zhenly.lftp.net.ByteConverter;
import cn.zhenly.lftp.net.FileChunk;
import cn.zhenly.lftp.net.FileData;
import cn.zhenly.lftp.net.NetSocket;

import java.io.File;

import static cn.zhenly.lftp.net.ByteConverter.getByte;


public class FileNet {
  private static FileData fileData;
  // 监听接受文件
  public static void listenReceiveFile(NetSocket netSocket, String dir, boolean showPercentage, int session) {
    Percentage percentage = new Percentage();
    fileData = null;
    netSocket.listen((data, ack) -> {
      if (data.getSession() != session) {
        ack.setData("-1".getBytes());
        return ack;
      }
      FileChunk fileChunk = (FileChunk) ByteConverter.ReadByte(data.getData());
      if (fileChunk != null) {
        if (fileChunk.getId() == -1 && fileData == null) {
          fileData = new FileData(fileChunk.getName(), fileChunk.getCount(), dir, fileChunk.getSize());
          System.out.println("[INFO] RecvFile " + fileChunk.getName() + " Chunks: " + fileChunk.getCount());
          ack.setData("OK".getBytes());
          return ack;
        } else if (fileData != null) {
          if (showPercentage)
            percentage.show((float) (fileChunk.getId() + 1) / fileChunk.getCount(), fileChunk.getCount() * 1024);
          if (fileData.addChunk(fileChunk)) {
            ack.setData(String.valueOf(fileChunk.getId()).getBytes());
            return ack;
          }
        }
      }
      ack.setData("-1".getBytes());
      return ack;
    }, 10000);
    netSocket.close();
  }

  // 发送文件
  public static void sendFile(NetSocket netSocket, String filePath, boolean showPercentage, int session) {
    FileIO.checkDir("./cache");
    File file = new File(filePath);
    int chunkCount = FileIO.getFileChunkCount(filePath);
    boolean[] finishChunk = new boolean[chunkCount];
    Percentage percentage = new Percentage();
    FileChunk initChunk = new FileChunk(file.getName(), -1, chunkCount, new byte[1024], file.length());
    netSocket.send(getByte(initChunk), data -> {
      System.out.println("[INFO] Start to send file " + file.getName() + " (" + file.length() / 1024.0 + "KB)");
      if (new String(data.getData()).equals("OK")) {
        for (int i = 0; i < chunkCount; i++) {
          FileChunk fileChunk = new FileChunk(file.getName(), i, chunkCount, FileIO.readFileChunk(filePath, i), file.length());
          netSocket.send(getByte(fileChunk), d -> {
            int id = Integer.parseInt(new String(d.getData()));
            if (id >= 0 && id < chunkCount) {
              if (showPercentage) percentage.show((float) (id + 1) / chunkCount, file.length());
              finishChunk[id] = true;
              if (isFinishChunk(finishChunk, chunkCount)) {
                // 断开连接
                netSocket.disconnect(null);
              }
            }
          }, false, session);
        }
      } else {
        System.out.println("[ERROR] Can't connect to server");
      }
    }, true, session);
    netSocket.close();
  }

  private static boolean isFinishChunk(boolean[] finishChunk, int size) {
    for (int i = 0; i < size; i++) {
      if (!finishChunk[i]) return false;
    }
    return true;
  }
}
