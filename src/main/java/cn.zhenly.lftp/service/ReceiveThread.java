package cn.zhenly.lftp.service;

import cn.zhenly.lftp.net.NetSocket;

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
    FileNet.listenReceiveFile(new NetSocket(port, true), dir, false ,session);
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
