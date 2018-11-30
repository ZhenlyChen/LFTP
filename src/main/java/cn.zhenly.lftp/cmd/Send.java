package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.AddressInfo;
import cn.zhenly.lftp.net.NetSocket;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;

import cn.zhenly.lftp.net.Util;
import cn.zhenly.lftp.service.FileNet;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "lsend", mixinStandardHelpOptions = true, description = "Send file to server.")
public class Send implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.")
  private String server;

  @Parameters(description = "file path", defaultValue = "./data")
  private List<String> files;

  @Option(names = {"-c", "--control"}, description = "Control port.", defaultValue = "9000")
  private int controlPort;

  @Option(names = {"-p", "--send"}, description = "Send port.", defaultValue = "9001")
  private int sendPort;

  @Override
  public void run() {
    CmdParameter cmdParameter = new CmdParameter(server, files);
    AddressInfo target = cmdParameter.target;

    File file = new File(cmdParameter.fileName);
    if (!file.exists() || !file.isFile()) {
      System.out.printf("[ERROR] %s is not a file.%n", cmdParameter.fileName);
    }
    try (NetSocket netSocket = new NetSocket(controlPort, new InetSocketAddress(target.ip, target.port), true)) {
      netSocket.send("SEND".getBytes(), data -> {
        int port = Util.getPortFromData(data.getData());
        if (port != -1) FileNet.sendFile(new NetSocket(sendPort,new InetSocketAddress(target.ip, port), false), cmdParameter.fileName, true);
      }, true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}


