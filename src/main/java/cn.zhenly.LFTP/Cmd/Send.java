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

  private InetAddress targetIP;

  private int targetPort = 3000; // 默认端口

  @Override
  public void run() {
    System.out.println(server);
    String[] targetAddress = server.split(":");
    // 解析端口
    if (targetAddress.length == 2) {
      try {
        targetPort = Integer.parseInt(targetAddress[1]);
      } catch (NumberFormatException e) {
        System.out.printf("[ERROR] Invalid server port: %s%n", targetAddress[1]);
      }
    } else if (targetAddress.length > 2 || targetAddress.length < 1) {
      System.out.printf("[ERROR] Invalid server location: %s%n", server);
    }
    // 解析地址
    try {
      targetIP = InetAddress.getByName(targetAddress[0]);
    } catch (UnknownHostException e) {
      System.out.printf("[ERROR] Invalid server location: %s%n", server);
      return;
    }
      System.out.printf("[INFO] Server location: %s:%d%n", targetIP.toString(), targetPort);
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
    try {
      NetUDP netUDP;
      netUDP = new NetUDP(9000, targetIP, targetPort);
      netUDP.send("Hello, LFTP".getBytes(), null);
      // netUDP.send("Hello, LFTP2".getBytes());
      // netUDP.send("Hello, LFTP3".getBytes());
      netUDP.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private void wantToSendFile(String name) {
    try {
      NetUDP netUDP;
      netUDP = new NetUDP(9000, targetIP, targetPort);
      netUDP.send("Hello, LFTP".getBytes(), (UDPPacket data) -> {
        System.out.println(data.getSeq());
        System.out.println(data.getAck());
        System.out.println(data.isACK());
      });
      // netUDP.send("Hello, LFTP2".getBytes());
      // netUDP.send("Hello, LFTP3".getBytes());
      netUDP.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}