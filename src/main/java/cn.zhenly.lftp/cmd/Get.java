package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.AddressInfo;
import cn.zhenly.lftp.net.NetSocket;
import cn.zhenly.lftp.net.Util;
import cn.zhenly.lftp.service.FileIO;
import cn.zhenly.lftp.service.FileNet;
import picocli.CommandLine.*;

import java.io.File;
import java.util.List;

@Command(name = "lget", mixinStandardHelpOptions = true, description = "get file from server.")
public class Get implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.")
  private String server;

  @Option(names = {"-d", "--dir"}, description = "Save file dir.", defaultValue = "./download")
  private String dir;

  @Parameters(description = "file path")
  private List<String> files;

  @Option(names = {"-c", "--control"}, description = "Control port.", defaultValue = "9000")
  private int controlPort;

  @Option(names = {"-p", "--send"}, description = "Send port.", defaultValue = "9001")
  private int sendPort;

  @Override
  public void run() {
    CmdParameter cmdParameter = new CmdParameter(server, files);
    AddressInfo target = cmdParameter.target;
    String fileName = cmdParameter.fileName;
    if (!target.valid) {
      return;
    }
    if (FileIO.getDir(dir) == null) return;
    try (NetSocket netSocket = new NetSocket(controlPort, target.ip, target.port)) {
      netSocket.send(("GETS" + fileName).getBytes(), data -> {
        int port = Util.getPortFromData(data.getData());
        if (port != -1) FileNet.listenReceiveFile(netSocket, dir, true);
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}