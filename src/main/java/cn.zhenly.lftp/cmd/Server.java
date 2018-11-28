package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.NetSocket;
import cn.zhenly.lftp.service.FileIO;
import cn.zhenly.lftp.service.ReceiveThread;
import cn.zhenly.lftp.service.SendThread;
import picocli.CommandLine.*;

import java.io.File;
import java.io.IOException;

import static cn.zhenly.lftp.cmd.CmdParameter.isPortAvailable;

@Command(name = "server", mixinStandardHelpOptions = true, description = "Send and receive big file by udp.")
public class Server implements Runnable {

  @Option(names = {"-p", "--port"}, description = "Server listen port.", defaultValue = "3000")
  private int port;

  @Option(names = {"-s", "--start"}, description = "Start port pool", defaultValue = "20480")
  private int portPoolStart;

  @Option(names = {"-c", "--client"}, description = "The number of clients at the same time.", defaultValue = "10")
  private int clientCount;

  @Option(names = {"-d", "--dir"}, description = "Server dir dir.")
  private String dir;

  private boolean[] usedPort;


  @Override
  public void run() {
    if (dir == null) {
      System.out.println("[ERROR] invalid dir");
      return;
    }
    for (int i = portPoolStart; i < portPoolStart + clientCount; i++) {
      if (!isPortAvailable(i)) {
        System.out.println("[ERROR] Port " + i + " is using.");
        return;
      }
    }
    System.out.println("[INFO] Start port pool: " + portPoolStart);
    System.out.println("[INFO] End port pool: " + (portPoolStart + clientCount - 1));
    this.usedPort = new boolean[clientCount];
    for (int i = 0; i < clientCount; i++) {
      this.usedPort[i] = false;
    }
    File file = FileIO.getDir(dir);
    if (file == null) return;
    NetSocket netSocket = new NetSocket(port);
    System.out.println("[INFO] Listening in localhost:" + port);
    netSocket.listen((packet, ack) -> {
      String recStr = new String(packet.getData());
      switch (recStr.substring(0, 4)) {
        case "LIST":
          File[] fileList = file.listFiles();
          if (fileList == null || fileList.length == 0) {
            return ack;
          }
          StringBuilder sb = new StringBuilder();
          for (File f : fileList) {
            if (f.isFile()) {
              sb.append(f.getName());
              sb.append("\n");
            }
          }
          ack.setData(sb.toString().getBytes());
          break;
        case "SEND":
          // 从端口池中新开端口，等待客户端连接
          int sendPortIndex = getFreePortIndex();
          if (sendPortIndex == -1) {
            ack.setData("BUSY".getBytes());
          } else {
            int sendPort = portPoolStart + sendPortIndex;
            ack.setData(("PORT" + sendPort).getBytes());
            ReceiveThread receiveThread = new ReceiveThread(sendPort, dir, () -> usedPort[sendPortIndex] = false);
            receiveThread.start();
            usedPort[sendPortIndex] = true;
          }
          break;
        case "GETS":
          // 从端口池中新开端口，等待客户端连接
          int getPortIndex = getFreePortIndex();
          if (getPortIndex == -1) {
            ack.setData("BUSY".getBytes());
          } else {
            int getPort = portPoolStart + getPortIndex;
            ack.setData(("PORT" + getPort).getBytes());
            String getFilePath = dir + "./" + recStr.substring(4);
            File fileOfSend = new File(getFilePath);
            if (!fileOfSend.exists() || !fileOfSend.isFile()) {
              System.out.printf("[ERROR] %s is not a file.%n", getFilePath);
            }
            SendThread receiveThread =
                    new SendThread(getPort, fileOfSend, netSocket.getTarget(), () -> usedPort[getPortIndex] = false);
            receiveThread.start();
            usedPort[getPortIndex] = true;
          }
          break;
      }
      return ack;
    });
  }

  private int getFreePortIndex() {
    for (int i = 0; i < usedPort.length; i++) {
      if (!usedPort[i]) {
        return i;
      }
    }
    return -1;
  }

}
