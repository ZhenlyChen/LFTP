package cn.zhenly.lftp.service;

import cn.zhenly.lftp.net.NetSocket;

import java.io.File;
import java.net.InetSocketAddress;

public class SendThread implements Runnable {
  private int port;
  private Thread t;
  private String filePath;
  private CallbackEnd callbackEnd;
  private InetSocketAddress addressInfo;

  public interface CallbackEnd {
    void finish();
  }

  public SendThread(int port, String filePath, InetSocketAddress addressInfo, CallbackEnd callbackEnd) {
    this.port = port;
    this.filePath = filePath;
    this.addressInfo= addressInfo;
    this.callbackEnd = callbackEnd;
  }

  @Override
  public void run() {
    NetSocket netSocket = new NetSocket(port, addressInfo, false);
    FileNet.sendFile(netSocket, filePath, false);
    callbackEnd.finish();
  }

  public void start() {
    System.out.println("[INFO] Start send file");
    if (t == null) {
      t = new Thread(this);
      t.start();
    }
  }
}

