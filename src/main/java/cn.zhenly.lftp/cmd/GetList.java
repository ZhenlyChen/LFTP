package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.NetSocket;
import cn.zhenly.lftp.net.UDPPacket;
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
      NetSocket netSocket;
      netSocket = new NetSocket(9000, target.ip, target.port);
      netSocket.send("LIST".getBytes(), (UDPPacket data) -> {
        System.out.println("File List:");
        System.out.println(new String(data.getData()));
      });
      netSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}