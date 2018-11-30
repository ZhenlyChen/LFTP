package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.NetSocket;
import cn.zhenly.lftp.service.FileIO;
import cn.zhenly.lftp.service.FileNet;
import picocli.CommandLine.*;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

@Command(name = "lget", mixinStandardHelpOptions = true, description = "get file from server.")
public class Get implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.", defaultValue = "")
  private String server;

  @Option(names = {"-d", "--dir"}, description = "Save file dir.", defaultValue = "./download")
  private String dir;

  @Parameters(description = "file path", defaultValue = "./recvFile")
  private List<String> files;

  @Option(names = {"-p", "--port"}, description = "Port.", defaultValue = "9000")
  private int controlPort;

  @Override
  public void run() {
    CmdParameter cmdParameter = new CmdParameter(server, files);
    CmdParameter.AddressInfo target = cmdParameter.target;
    String fileName = cmdParameter.fileName;
    if (!target.valid) {
      return;
    }
    if (FileIO.checkDir(dir) == null) return;
    try {
      NetSocket netSocket = new NetSocket(controlPort, new InetSocketAddress(target.ip, target.port), true);
      int sessionId = new Random().nextInt(10000);
      netSocket.send(("GETS" + sessionId + "-" + fileName).getBytes(), data -> {
        int port = Util.getPortFromData(data.getData());
        if (port != -1) FileNet.listenReceiveFile(netSocket, dir, true, sessionId);
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}