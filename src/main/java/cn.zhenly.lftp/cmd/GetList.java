package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.NetSocket;
import cn.zhenly.lftp.net.UDPPacket;
import picocli.CommandLine.*;

import java.io.IOException;
import java.net.InetSocketAddress;


@Command(name = "list", mixinStandardHelpOptions = true, description = "Get file list in server.")
public class GetList implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.", defaultValue = "")
  private String server;

  @Option(names = {"-p", "--port"}, description = "Control port.", defaultValue = "9000")
  private int sendPort;

  @Override
  public void run() {
    CmdParameter.AddressInfo target = CmdParameter.parseIPAddr(server);
    if (!target.valid) return;
    try {
      NetSocket netSocket = new NetSocket(sendPort, new InetSocketAddress(target.ip, target.port), true);
      netSocket.send("LIST".getBytes(), (UDPPacket data) -> {
        System.out.println("Server file list:");
        System.out.println("-----------------------");
        if (data.getData() != null)  {
          System.out.println(new String(data.getData()));
        } else {
          System.out.println("Nothing.");
        }
        System.out.println("-----------------------");
        netSocket.close();
      });
    } catch (IOException e) {
      System.out.println("[ERROR] Port "+ sendPort + " already in use!");
    }
  }
}