package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.NetUDP;
import picocli.CommandLine.*;

import java.io.File;

@Command(name = "server", mixinStandardHelpOptions = true, description = "Send and receive big file by udp.")
public class Server implements Runnable {

  @Option(names = {"-p", "--port"}, description = "Server listen port.", defaultValue = "3000")
  int port;

  @Option(names = {"-s", "--start"}, description = "Start port pool", defaultValue = "20480")
  int portPoolStart;

  @Option(names = {"-c", "--client"}, description = "The number of clients at the same time.", defaultValue = "10")
  int clientCount;

  @Option(names = {"-d", "--dir"}, description = "Server dir dir.")
  String dir;

  boolean usedPort[];

  @Override
  public void run() {
    if (dir == null) {
      System.out.println("[ERROR] invalid dir");
      return;
    }
    for (int i = portPoolStart; i < portPoolStart + clientCount; i++) {
      if (!Util.isPortAvailable(i)) {
        System.out.println("[ERROR] Port " + i + " is using.");
        return;
      }
    }
    System.out.println("[INFO] Start port pool: " + portPoolStart);
    System.out.println("[INFO] End port pool: " + (portPoolStart + clientCount - 1));
    this.usedPort = new boolean[clientCount];
    for (int i = 0; i < clientCount; i++) {
      this.usedPort[i] = false;
    }
    File file = new File(dir);
    if (!file.exists()) {
      if (!file.mkdir()) {
        System.out.println("[ERROR] Can't make directory " + dir + ".");
        return;
      }
    } else if (!file.isDirectory()) {
      System.out.println("[ERROR] File " + dir + " has exist, can't create directory here.");
      return;
    }
    System.out.println("[INFO] Data directory: " + dir);
    NetUDP netUDP = new NetUDP(port);
    System.out.println("[INFO] Listening in localhost:" + port);
    netUDP.listen((packet, ack) -> {
      String recStr = new String(packet.getData());
      switch (recStr.substring(0, 4)) {
        case "LIST":
          File[] fileList = file.listFiles();
          if (fileList == null || fileList.length == 0) {
            return ack;
          }
          StringBuilder sb = new StringBuilder();
          for (File f : fileList) {
            if (f.isFile()) {
              sb.append(f.getName());
              sb.append("\n");
            }
          }
          ack.setData(sb.toString().getBytes());
          break;
        case "SEND":
          // TODO 从端口池中新开端口，等待客户端连接
          ack.setData("88".getBytes());
          break;
        case "GET":
          // TODO 从端口池中新开端口，等待客户端连接
          ack.setData("66".getBytes());
          break;
      }
      return ack;
    });
  }

}