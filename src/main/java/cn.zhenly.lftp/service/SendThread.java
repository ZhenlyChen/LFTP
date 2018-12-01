package cn.zhenly.lftp.service;

import cn.zhenly.lftp.net.NetSocket;

import java.io.IOException;
import java.net.InetSocketAddress;

public class SendThread implements Runnable {
  private int port;
  private int session;
  private Thread t;
  private String filePath;
  private CallbackEnd callbackEnd;

  public interface CallbackEnd {
    void finish();
  }

  public SendThread(int session, int port, String filePath, CallbackEnd callbackEnd) {
    this.port = port;
    this.session = session;
    this.filePath = filePath;
    this.callbackEnd = callbackEnd;
  }

  @Override
  public void run() {
    try {
      NetSocket netSocket = new NetSocket(port, true);
      netSocket.listen((data, ack) -> {
        if (new String(data.getData()).equals(String.valueOf(session))) {
          ack.setCallBack(d -> {
            netSocket.switchToNonBlock();
            netSocket.setTargetAddress(data.getFrom());
            FileNet.sendFile(netSocket, filePath, false, session);
            callbackEnd.finish();
          });
        } else {
          System.out.println("[ERROR] Invalid sessionID "+ session);
          ack.setData("ERROR".getBytes());
        }
        return ack;
      }, 10000);
    } catch (IOException e) {
      System.out.println("[ERROR] Port "+ port + " already in use!");
    }
  }

  public void start() {
    System.out.println("[INFO] Start send file");
    if (t == null) {
      t = new Thread(this);
      t.start();
    }
  }
}

