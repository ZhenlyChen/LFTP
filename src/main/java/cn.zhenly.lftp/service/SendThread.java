package cn.zhenly.lftp.service;

import cn.zhenly.lftp.net.AddressInfo;
import cn.zhenly.lftp.net.NetSocket;

import java.io.File;
import java.net.InetAddress;

public class SendThread implements Runnable {
  private int port;
  private Thread t;
  private File file;
  private CallbackEnd callbackEnd;
  private AddressInfo addressInfo;

  public interface CallbackEnd {
    void finish();
  }

  public SendThread(int port, File file, AddressInfo addressInfo, CallbackEnd callbackEnd) {
    this.port = port;
    this.file = file;
    this.addressInfo= addressInfo;
    this.callbackEnd = callbackEnd;
  }

  @Override
  public void run() {
    NetSocket netSocket = new NetSocket(port, addressInfo.ip, addressInfo.port);
    FileNet.sendFile(netSocket, file, false);
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

