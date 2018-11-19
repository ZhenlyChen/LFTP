package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.NetSocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


@Command(name = "lsend", mixinStandardHelpOptions = true, description = "Send file to server.")
public class Send implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.")
  private String server;

  @Parameters(description = "file path")
  private List<String> files;

  @Option(names = {"-c", "--control"}, description = "Control port.", defaultValue = "9000")
  private int controlPort;

  @Option(names = {"-p", "--send"}, description = "Send port.", defaultValue = "9001")
  private int sendPort;

  private Util.AddressInfo target;

  @Override
  public void run() {
    target = Util.parseIPAddr(server);
    if (!target.valid) {
      return;
    }
    // 解析文件
    if (files.size() == 0) {
      System.out.println("[ERROR] no file to send.");
      return;
    } else if (files.size() > 1) {
      System.out.println("[INFO] LFTP is not support multiple files currently. :(");
    }
    // 发送文件
    String fileName = files.get(0);
    System.out.printf("[INFO] File: %s ready to send.%n", fileName);
    File file = new File(fileName);
    if (!file.exists() || !file.isFile()) {
      System.out.printf("[ERROR] %s is not a file.%n", fileName);
      return;
    }
    try (NetSocket netSocket = new NetSocket(controlPort, target.ip, target.port)) {
      netSocket.send("SEND".getBytes(), data -> {
        String str = new String(data.getData());
        if (!str.substring(0, 4).equals("PORT")) {
          System.out.println("[ERROR] System error!");
          return;
        }
        int port = -1;
        try {
          port = Integer.parseInt(str.substring(4));
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
        sendFile(file, port);
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // 发送文件
  private void sendFile(File file, int port) {
    try {
      NetSocket netSocket;
      netSocket = new NetSocket(sendPort, target.ip, port);
      InputStream inStream = new FileInputStream(file);
      byte[] buf = new byte[1024];
      int fileID = 0;
      while (inStream.read(buf) != -1) {
        System.out.println("Data:");
        System.out.println(new String(buf));
        netSocket.send("SEND".getBytes(), data -> {
          String str = new String(data.getData());
          netSocket.close();
        });
        fileID++;
      }
    } catch (IOException e) {
      System.out.printf("[ERROR] Can't read file: %s%n", file.getName());
    }
  }
}

