package cn.zhenly.lftp.service;

import cn.zhenly.lftp.net.ByteConverter;
import cn.zhenly.lftp.net.FileChunk;
import cn.zhenly.lftp.net.FileData;
import cn.zhenly.lftp.net.NetSocket;

import java.io.File;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zhenly.lftp.net.ByteConverter.getByte;


public class FileNet {
  // 监听接受文件
  public static void listenReceiveFile(NetSocket netSocket, String dir, boolean showPercentage, int session) {
    Percentage percentage = new Percentage();
    // fileData = null;
    final FileData fileData = new FileData();
    netSocket.listen((data, ack) -> {
      // System.out.println("Get:" + data.getSeq());
      if (data.getSession() != session) {
        ack.setData("-1".getBytes());
        return ack;
      }
      FileChunk fileChunk = (FileChunk) ByteConverter.ReadByte(data.getData());
      if (fileChunk != null) {
        if (fileChunk.getId() == -1 && !fileData.isInitialized()) {
          fileData.init(fileChunk.getName(), fileChunk.getCount(), dir, fileChunk.getSize());
          System.out.println("[INFO] RecvFile " + fileChunk.getName() + " Size: " + getSize(fileChunk.getCount() * 1024));
          ack.setData("OK".getBytes());
          return ack;
        } else if (fileChunk.getId() != -1 && fileData.isInitialized()) {
          long totalCount = fileData.getTotalCount();
          if (showPercentage && (fileChunk.getId() % 300 == 0 || fileChunk.getId() + 100 > totalCount))
            percentage.show((float) (fileChunk.getId() + 1) / totalCount, totalCount * 1024);
          fileData.addChunk(fileChunk);
          if (fileChunk.getId() + 1 >= totalCount) {
            fileData.flush();
            ack.setCallBack(d -> System.out.println("\n[INFO] Finish!"));
          }
          return ack;
        }
      }
      ack.setData("-1".getBytes());
      return ack;
    }, 15000);
    netSocket.close();
  }

  // 发送文件
  public static void sendFile(NetSocket netSocket, String filePath, boolean showPercentage, int session) {
    File file = new File(filePath);
    long chunkCount = FileIO.getFileChunkCount(filePath);
    Percentage percentage = new Percentage();
    FileChunk initChunk = new FileChunk(file.getName(), -1, chunkCount, new byte[1024], file.length());
    Semaphore lock = new Semaphore(1);
    try {
      lock.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    AtomicInteger chunkIndex = new AtomicInteger(0);
    netSocket.send(getByte(initChunk), data -> {
      System.out.println("[INFO] Start to send file " + file.getName() + " (" + getSize(file.length()) + ")");
      if (new String(data.getData()).equals("OK")) {

        // 多线程分块加载文件发送
        (new Thread(() -> {
          final int readCount = 2048;
          for (long i = 0; i < chunkCount; i += readCount) { // 每次读入4M大小文件
            int loadCount = readCount;
            if (i + loadCount > chunkCount) loadCount = (int)(chunkCount - i);
            List<byte[]> fileChunks = FileIO.readFile(filePath, i, loadCount);
            for (int j = 0; j < fileChunks.size(); j++) {
              FileChunk fileChunk = new FileChunk(i+j,  fileChunks.get(j));
              netSocket.send(getByte(fileChunk), ignore -> {
                int id = chunkIndex.incrementAndGet();
                if (showPercentage && (id % 300 == 0 || id + 100 > chunkCount))
                  percentage.show((float) (id) / chunkCount, file.length());
                if (id >= chunkCount) {
                  System.out.println("\n[INFO] Finish!");
                  netSocket.disconnect();
                  lock.release();
                }
              }, false, session);
            }
          }
        })).start();

      } else {
        System.out.println("[ERROR] Can't connect to server");
      }
    }, true, session);
    // 阻止线程结束
    try {
      lock.acquire();
      lock.release();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


  public static String getSize(long s) {
    String[] unit = {"B", "KB", "MB", "GB", "TB"};
    int i = 0;
    double size = s;
    while (size > 1024 && i < 5) {
      size /= 1024;
      i++;
    }
    return (double) (Math.round(size * 100)) / 100 + unit[i];
  }

}
