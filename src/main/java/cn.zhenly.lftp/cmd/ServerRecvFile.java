package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.FileChunk;
import cn.zhenly.lftp.net.FileData;
import cn.zhenly.lftp.net.NetSocket;

import java.io.*;

import static cn.zhenly.lftp.cmd.Util.showPercentage;
import static cn.zhenly.lftp.net.Util.ReadByte;

public class ServerRecvFile implements Runnable {
  private int port;
  private String dir;
  private Thread t;
  private NetSocket netSocket;
  private FileData fileData;
  private CallbackEnd callbackEnd;

  public interface CallbackEnd {
    void finish();
  }

  ServerRecvFile(int port, String dir, CallbackEnd callbackEnd) {
    this.port = port;
    this.dir = dir;
    this.callbackEnd = callbackEnd;
  }

  @Override
  public void run() {
    netSocket = new NetSocket(port);
    netSocket.setTimeOut(10000);
    recvFile();
    callbackEnd.finish();
  }

  private void recvFile() {
    netSocket.listen((data, ack) -> {
      FileChunk fileChunk = (FileChunk) ReadByte(data.getData());
      if (fileChunk != null) {
        if (fileChunk.getId() == -1) {
          fileData = new FileData(fileChunk.getName(), fileChunk.getCount());
          System.out.println("[INFO] RecvFile " + fileChunk.getName() + " 0/" + fileChunk.getCount());
          ack.setData("OK".getBytes());
        } else if (fileData != null) {
          // System.out.println("[INFO] RecvFile "+fileChunk.getName() + " " + (fileChunk.getId() + 1) +"/" + fileChunk.getCount());
          showPercentage((float)(fileChunk.getId() + 1) / fileChunk.getCount(), 100);
          ack.setData(String.valueOf(fileChunk.getId()).getBytes());
          fileData.addChunk(fileChunk);
          if (fileData.isComplete()) {
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

  void start() {
    System.out.println("[INFO] Starting RecvFile");
    if (t == null) {
      t = new Thread(this);
      t.start();
    }
  }
}
