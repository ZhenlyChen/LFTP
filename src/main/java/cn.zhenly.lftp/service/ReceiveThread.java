package cn.zhenly.lftp.service;

import cn.zhenly.lftp.net.NetSocket;

public class ReceiveThread implements Runnable {
  private int port;
  private String dir;
  private Thread t;
  private CallbackEnd callbackEnd;

  public interface CallbackEnd {
    void finish();
  }

  public ReceiveThread(int port, String dir, CallbackEnd callbackEnd) {
    this.port = port;
    this.dir = dir;
    this.callbackEnd = callbackEnd;
  }

  @Override
  public void run() {
    FileNet.listenReceiveFile(new NetSocket(port), dir, false);
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
