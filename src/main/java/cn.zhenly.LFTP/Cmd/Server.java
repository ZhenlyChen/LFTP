package cn.zhenly.LFTP.Cmd;

import cn.zhenly.LFTP.NetUDP.NetUDP;
import picocli.CommandLine.*;

import java.io.File;

@Command(name = "server", mixinStandardHelpOptions = true, description = "Send and receive big file by udp.")
public class Server implements Runnable {

  @Option(names = {"-p", "--port"}, description = "Server listen port.")
  int port;


  @Option(names = {"-d", "--data"}, description = "Server data dir.")
  String data;

  @Override
  public void run() {
    if (port <= 1024 || data == null) {
      System.out.println("[ERROR] invalid port or data");
      return;
    }
    File file = new File(data);
    if (!file.exists()) {
      if (!file.mkdir()) {
        System.out.println("[ERROR] Can't make directory " + data + ".");
        return;
      }
    } else if (!file.isDirectory()) {
      System.out.println("[ERROR] File " + data + " has exist, can't create directory here.");
      return;
    }
    System.out.println("[INFO] Data directory: " + data);
    NetUDP netUDP = new NetUDP(port);
    System.out.println("[INFO] Listening in localhost:" + port);
    netUDP.listen();
  }
}
