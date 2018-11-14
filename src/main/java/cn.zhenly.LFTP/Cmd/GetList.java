package cn.zhenly.LFTP.Cmd;

import cn.zhenly.LFTP.NetUDP.NetUDP;
import cn.zhenly.LFTP.NetUDP.UDPPacket;
import picocli.CommandLine.*;

import java.io.IOException;

@Command(name = "list", mixinStandardHelpOptions = true, description = "list files in server.")
public class GetList implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.")
  String server;

  private Util.AddressInfo target;

  @Override
  public void run() {
    target = Util.parseIPAddr(server);
    if (!target.valid) return;
    try {
      NetUDP netUDP;
      netUDP = new NetUDP(9000, target.ip, target.port);
      netUDP.send("LIST".getBytes(), (UDPPacket data) -> {
        System.out.println("File List:");
        System.out.println(new String(data.getData()));
      });
      netUDP.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}