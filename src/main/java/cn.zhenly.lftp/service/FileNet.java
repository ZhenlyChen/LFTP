package cn.zhenly.lftp.service;

import cn.zhenly.lftp.net.ByteConverter;
import cn.zhenly.lftp.net.FileChunk;
import cn.zhenly.lftp.net.FileData;
import cn.zhenly.lftp.net.NetSocket;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static cn.zhenly.lftp.net.ByteConverter.getByte;


public class FileNet {
  private static FileData fileData;
  public static void listenReceiveFile(NetSocket netSocket, String dir, boolean showPercentage) {
    netSocket.setTimeOut(10000);
    Percentage percentage = new Percentage();
    netSocket.listen((data, ack) -> {
      FileChunk fileChunk = (FileChunk) ByteConverter.ReadByte(data.getData());
      if (fileChunk != null) {
        if (fileChunk.getId() == -1) {
          fileData = new FileData(fileChunk.getName(), fileChunk.getCount());
          System.out.println("[INFO] RecvFile " + fileChunk.getName() + " Chunks: " + fileChunk.getCount());
          ack.setData("OK".getBytes());
        } else if (fileData != null) {
          if (showPercentage) percentage.show((float) (fileChunk.getId() + 1) / fileChunk.getCount(), 100);
          ack.setData(String.valueOf(fileChunk.getId()).getBytes());
          fileData.addChunk(fileChunk);
          if (fileData.isComplete()) {
            FileIO.saveFile(fileData, dir);
          }
        } else {
          ack.setData(String.valueOf(-1).getBytes());
        }
      } else {
        ack.setData(String.valueOf(-1).getBytes());
      }
      return ack;
    });
    netSocket.close();
  }

  // 发送文件
  public static void sendFile(NetSocket netSocket, File file, boolean showPercentage) {
    try {
      List<byte[]> buffs = FileIO.readFile(file);
      if (buffs == null) throw new IOException();
      boolean[] finishChunk = new boolean[buffs.size()];
      Percentage percentage = new Percentage();
      FileChunk initChunk = new FileChunk(file.getName(), -1, buffs.size(), new byte[1024]);
      netSocket.send(getByte(initChunk), data-> {
        System.out.println("[INFO] Start to send file " + file.getName() + " (" + file.length() / 1024.0 + "KB)");
        if (new String(data.getData()).equals("OK")) {
          for (int i = 0; i < buffs.size(); i++) {
            FileChunk fileChunk = new FileChunk(file.getName(), i, buffs.size(), buffs.get(i));
            try {
              netSocket.send(getByte(fileChunk), d -> {
                int id = Integer.parseInt(new String(d.getData()));
                if (id >= 0 && id < buffs.size()) {
                  if (showPercentage) percentage.show((float)(id+1) / buffs.size(), file.length());
                  finishChunk[id] = true;
                  if (isFinishChunk(finishChunk, buffs.size())) {
                    try {
                      netSocket.disconnect(null);
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                  }
                }
              });
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        } else {
          System.out.println("[ERROR] Can't connect to server");
        }
      });
    } catch (IOException e) {
      System.out.printf("[ERROR] Can't read file: %s%n", file.getName());
    }
    netSocket.close();
  }

  private static boolean isFinishChunk(boolean[] finishChunk, int size) {
    for (int i = 0; i < size; i++) {
      if (!finishChunk[i]) return false;
    }
    return true;
  }
}
