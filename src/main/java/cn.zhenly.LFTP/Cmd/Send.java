package cn.zhenly.LFTP.Cmd;

import cn.zhenly.LFTP.NetUDP.NetUDP;
import cn.zhenly.LFTP.NetUDP.UDPPacket;
import picocli.CommandLine.*;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Command(name = "lsend", mixinStandardHelpOptions = true, description = "Send file to server.")
public class Send implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.")
  private
  String server;

  @Parameters(description = "file path")
  private
  List<String> files;

  private Util.AddressInfo target;

  @Override
  public void run() {
    target = Util.parseIPAddr(server);
    if (!target.valid) return;
    // 解析文件
    if (files.size() == 0) {
      System.out.println("[ERROR] no file to send.");
      return;
    }
    // 发送文件
    for (String f : files) {
      System.out.printf("[INFO] File: %s ready to send.%n", f);
      File file = new File(f);
      if (!file.exists() || !file.isFile()) {
        System.out.printf("[ERROR] %s is not a file.%n", f);
      }
      InputStream iStream;
      try {
        iStream = new FileInputStream(file);
      } catch (FileNotFoundException e) {
        System.out.printf("[ERROR] Can't read file: %s%n", f);
      }
      wantToSendFile(file.getName());
    }
  }

  private void wantToSendFile(String name) {
    try {
      NetUDP netUDP;
      netUDP = new NetUDP(9000, target.ip, target.port);
      netUDP.send("HELLO".getBytes(), (UDPPacket data) -> {
        System.out.println(data.getSeq());
        System.out.println(data.getAck());
        System.out.println(new String(data.getData()));
        System.out.println(data.isACK());
      });
      netUDP.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}