package cn.zhenly.LFTP.Cmd;

import cn.zhenly.LFTP.NetUDP.NetUDP;
import cn.zhenly.LFTP.NetUDP.PacketType;
import cn.zhenly.LFTP.NetUDP.UDPPacket;
import picocli.CommandLine.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Command(name = "lsend", mixinStandardHelpOptions = true, description = "Send file to server.")
public class Send implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.")
  String server;

  @Parameters(description = "file path")
  List<String> file;

  @Override
  public void run() {
    System.out.println(server);
    for (String f : file) {
      System.out.println(f);
    }
    try {
      NetUDP netUDP = new NetUDP(9000, InetAddress.getLocalHost(), 3000);
      netUDP.setTimeOut(2000);
      netUDP.UDPSend(new UDPPacket(0, "Hello world!".getBytes(), PacketType.DATA));
      UDPPacket res = netUDP.UDPReceive();
      if (res != null) {
        System.out.println(res.getType().toString());
      }
      netUDP.close();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }
}