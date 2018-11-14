package cn.zhenly.LFTP.Cmd;

import cn.zhenly.LFTP.NetUDP.NetUDP;
import picocli.CommandLine.*;

import java.io.File;

@Command(name = "server", mixinStandardHelpOptions = true, description = "Send and receive big file by udp.")
public class Server implements Runnable {

  @Option(names = {"-p", "--port"}, description = "Server listen port.")
  int port;


  @Option(names = {"-d", "--dir"}, description = "Server dir dir.")
  String dir;

  @Override
  public void run() {
    if (port <= 1024 || dir == null) {
      System.out.println("[ERROR] invalid port or dir");
      return;
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
      if (recStr.substring(0, 4).equals("LIST")) {
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
      } else if (recStr.substring(0, 4).equals("SEND")) {
        ack.setData("88".getBytes());
      }
      return ack;
    });
  }
}
