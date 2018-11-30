package cn.zhenly.lftp.service;

import cn.zhenly.lftp.net.ByteConverter;
import cn.zhenly.lftp.net.FileChunk;
import cn.zhenly.lftp.net.FileData;
import cn.zhenly.lftp.net.NetSocket;

import java.io.File;
import java.util.concurrent.Semaphore;

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
          System.out.println("[INFO] RecvFile " + fileChunk.getName() + " Size: " + getSize(fileChunk.getCount() * 1024) );
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
    }, 20000);
    netSocket.close();
  }

  // 发送文件
  public static void sendFile(NetSocket netSocket, String filePath, boolean showPercentage, int session) {
    File file = new File(filePath);
    int chunkCount = FileIO.getFileChunkCount(filePath);
    Percentage percentage = new Percentage();
    FileChunk initChunk = new FileChunk(file.getName(), -1, chunkCount, new byte[1024], file.length());
    Semaphore lock = new Semaphore(1);
    try {
      lock.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    netSocket.send(getByte(initChunk), data -> {
      System.out.println("[INFO] Start to send file " + file.getName() + " (" + getSize(file.length()) + ")");
      if (new String(data.getData()).equals("OK")) {

        // 分块加载文件
        (new Thread(()->{
          for (int i = 0; i < chunkCount; i++) {
            FileChunk fileChunk = new FileChunk(file.getName(), i, chunkCount, FileIO.readFileChunk(filePath, i), file.length());
            netSocket.send(getByte(fileChunk), d -> {
              int id = Integer.parseInt(new String(d.getData()));
              if (id >= 0 && id < chunkCount) {
                if (showPercentage) percentage.show((float) (id + 1) / chunkCount, file.length());
                // if (id > chunkCount - 100) System.out.println("id: " +id + "count: " + chunkCount);
                if (id + 1 == chunkCount) {
                  netSocket.disconnect();
                  lock.release();
                }
              }
            }, false, session);
          }
        })).start();

      } else {
        System.out.println("[ERROR] Can't connect to server");
      }
    }, true, session);
    try {
      lock.acquire();
      lock.release();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


  private static String getSize(long s) {
    String[] unit = {"B", "KB", "MB", "GB", "TB"};
    int i = 0;
    double size = s;
    while (size > 1024 && i < 5) {
      size /= 1024;
      i++;
    }
    return (double)(Math.round(size*100))/100 + unit[i];
  }

}
