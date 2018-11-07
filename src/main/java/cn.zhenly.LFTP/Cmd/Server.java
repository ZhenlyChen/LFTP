package cn.zhenly.LFTP.Cmd;

import cn.zhenly.LFTP.NetUDP.NetUDP;
import cn.zhenly.LFTP.NetUDP.PacketType;
import cn.zhenly.LFTP.NetUDP.UDPPacket;
import picocli.CommandLine;
import picocli.CommandLine.*;

@Command(name = "server", mixinStandardHelpOptions = true, description = "Send and receive big file by udp.")
public class Server implements Runnable {

  @Option(names = {"-p", "--port"}, description = "Server listen port.")
  int port;


  @Option(names = {"-d", "--data"}, description = "Server data dir.")
  String data;

  @Override
  public void run() {
    if (port <= 1024 || data == null) {
      System.out.println("Error port or data");
      return;
    }
    NetUDP netUDP = new NetUDP(port);
    netUDP.listen();
  }
}
