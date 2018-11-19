package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.NetUDP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


@Command(name = "lsend", mixinStandardHelpOptions = true,
        description = "Send file to server.")
public class Send implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.")
  private String server;

  @Parameters(description = "file path")
  private List<String> files;

  private Util.AddressInfo target;
  private static int SEND_PORT = 9000;

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
    }
    // 发送文件
    for (String f : files) {
      System.out.printf("[INFO] File: %s ready to send.%n", f);
      File file = new File(f);
      if (!file.exists() || !file.isFile()) {
        System.out.printf("[ERROR] %s is not a file.%n", f);
      }
      InputStream inStream;
      try {
        inStream = new FileInputStream(file);
      } catch (FileNotFoundException e) {
        System.out.printf("[ERROR] Can't read file: %s%n", f);
      }
      wantToSendFile(file.getName());
    }
  }

  private void wantToSendFile(final String name) {
    try {
      NetUDP netUDP;
      netUDP = new NetUDP(SEND_PORT, target.ip, target.port);
      netUDP.send("SEND".getBytes(), data -> {
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
