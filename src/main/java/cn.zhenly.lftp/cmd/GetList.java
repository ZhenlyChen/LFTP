package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.NetSocket;
import cn.zhenly.lftp.net.UDPPacket;
import picocli.CommandLine.*;

import java.net.InetSocketAddress;


@Command(name = "list", mixinStandardHelpOptions = true, description = "list files in server.")
public class GetList implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.")
  private String server;

  @Override
  public void run() {
    CmdParameter.AddressInfo target = CmdParameter.parseIPAddr(server);
    if (!target.valid) return;
    NetSocket netSocket;
    netSocket = new NetSocket(9000, new InetSocketAddress(target.ip, target.port), true);
    netSocket.send("LIST".getBytes(), (UDPPacket data) -> {
      System.out.println("Server file list:");
      System.out.println("-----------------------");
      System.out.println(new String(data.getData()));
      System.out.println("-----------------------");
      netSocket.close();
    }, true, 0);
  }
}