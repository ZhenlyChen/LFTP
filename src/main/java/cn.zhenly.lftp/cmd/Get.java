package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.NetSocket;
import cn.zhenly.lftp.service.FileIO;
import cn.zhenly.lftp.service.FileNet;
import picocli.CommandLine.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

@Command(name = "lget", mixinStandardHelpOptions = true, description = "Get file from server.")
public class Get implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.", defaultValue = "")
  private String server;

  @Option(names = {"-d", "--dir"}, description = "Your file will download to this folder.", defaultValue = "./download")
  private String dir;

  @Parameters(description = "File path", defaultValue = "./recvFile.")
  private List<String> files;

  @Option(names = {"-c", "--control"}, description = "Control Port", defaultValue = "9000")
  private int controlPort;

  @Option(names = {"-p", "--port"}, description = "Data Port.", defaultValue = "9001")
  private int dataPort;

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
        if (new String(data.getData()).equals("NOTHING")) {
          System.out.println("[ERROR] File "+ fileName +" isn't exist in server.");
          System.out.println("[INFO] You can use `list` to see the list of files in server.");
          return;
        }
        int port = Util.getPortFromData(data.getData());
        netSocket.close();
        if (port != -1) {
          try {
            NetSocket dataSocket = new NetSocket(dataPort, new InetSocketAddress(target.ip, port), true);
            dataSocket.send(String.valueOf(sessionId).getBytes(), d->FileNet.listenReceiveFile(dataSocket, dir, true, sessionId));
          } catch (IOException e) {
            System.out.println("[ERROR] Port "+ dataPort + " already in use!");
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