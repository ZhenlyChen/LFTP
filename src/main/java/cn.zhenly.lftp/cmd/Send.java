package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.NetSocket;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

import cn.zhenly.lftp.service.FileNet;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "lsend", mixinStandardHelpOptions = true, description = "Send file to server.")
public class Send implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.", defaultValue = "")
  private String server;

  @Parameters(description = "File path", defaultValue = "./data")
  private List<String> files;

  @Option(names = {"-c", "--control"}, description = "Control port.", defaultValue = "9000")
  private int controlPort;

  @Option(names = {"-p", "--port"}, description = "Data port.", defaultValue = "9001")
  private int sendPort;

  @Override
  public void run() {
    CmdParameter cmdParameter = new CmdParameter(server, files);
    CmdParameter.AddressInfo target = cmdParameter.target;
    if (!target.valid) return;
    File file = new File(cmdParameter.fileName);
    if (!file.exists() || !file.isFile()) {
      System.out.printf("[ERROR] %s is not a file.%n", cmdParameter.fileName);
      return;
    }
    try {
      NetSocket netSocket = new NetSocket(controlPort, new InetSocketAddress(target.ip, target.port), true);
      int sessionId = new Random().nextInt(10000);
      netSocket.send(("SEND" + sessionId).getBytes(), data -> {
        int port = Util.getPortFromData(data.getData());
        netSocket.close();
        if (port != -1) {
          try {
            FileNet.sendFile(new NetSocket(sendPort, new InetSocketAddress(target.ip, port), false), cmdParameter.fileName, true, sessionId);
          } catch (IOException e) {
            System.out.println("[ERROR] Port "+ sendPort + " already in use!");
          }
        } else {
          System.out.println("[ERROR] Server is busy, please try again later.");
        }
      });
    } catch (Exception e) {
      System.out.println("[ERROR] Port "+ controlPort + " already in use!");
    }
  }
}


