package cn.zhenly.lftp.service;

import cn.zhenly.lftp.net.NetSocket;

import java.io.IOException;

public class ReceiveThread implements Runnable {
  private int port;
  private int session;
  private String dir;
  private Thread t;
  private CallbackEnd callbackEnd;

  public interface CallbackEnd {
    void finish();
  }

  public ReceiveThread(int session, int port, String dir, CallbackEnd callbackEnd) {
    this.port = port;
    this.dir = dir;
    this.session = session;
    this.callbackEnd = callbackEnd;
  }

  @Override
  public void run() {
    try {
      NetSocket netSocket = new NetSocket(port, true);
      FileNet.listenReceiveFile(netSocket, dir, false ,session);
    } catch (IOException e) {
      System.out.println("[ERROR] Port "+ port + " already in use!");
    }
    callbackEnd.finish();
  }

  public void start() {
    System.out.println("[INFO] Start receive file");
    if (t == null) {
      t = new Thread(this);
      t.start();
    }
  }
}
