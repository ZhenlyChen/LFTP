package cn.zhenly.LFTP.Cmd;

import cn.zhenly.LFTP.NetUDP.NetUDP;
import cn.zhenly.LFTP.NetUDP.PacketType;
import cn.zhenly.LFTP.NetUDP.UDPPacket;
import picocli.CommandLine.*;

import java.io.IOException;
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
    if (file.size() == 0) {
      System.out.println("no file to send.");
      return;
    }
    for (String f : file) {
      System.out.println(f);
    }
    try {
      NetUDP netUDP = new NetUDP(9000, InetAddress.getLocalHost(), 3000);
      netUDP.send("Hello, LFTP".getBytes());
      netUDP.send("Hello, LFTP2".getBytes());
      netUDP.send("Hello, LFTP3".getBytes());
      netUDP.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}