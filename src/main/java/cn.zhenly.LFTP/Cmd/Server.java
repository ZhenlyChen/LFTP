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
    NetUDP netUDP = new NetUDP(port);
    while (true) {
      UDPPacket data = netUDP.UDPReceive();
      System.out.println("server received data from client：");
      if (data != null) {
        System.out.println(data.getPacket().getAddress().getHostAddress() + ":" + data.getPacket().getPort());
        System.out.println(new String(data.getContent()));
        System.out.println(data.getId());
        System.out.println(data.getType());
        netUDP.setTarget(data.getPacket().getAddress(), data.getPacket().getPort());
        netUDP.UDPSend(new UDPPacket(data.getId(), "ACK".getBytes(), PacketType.ACK));
      }
    }
  }
}
