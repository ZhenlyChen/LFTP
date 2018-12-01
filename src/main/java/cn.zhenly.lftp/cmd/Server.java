package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.NetSocket;
import cn.zhenly.lftp.service.FileIO;
import cn.zhenly.lftp.service.FileNet;
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

  @Option(names = {"-c", "--client"}, description = "The number of clients at the same time.", defaultValue = "20")
  private int clientCount;

  @Option(names = {"-d", "--dir"}, description = "Server data folder.", defaultValue = "./serverData")
  private String dir;

  private int[] usedPort;

  @Override
  public void run() {

    if (dir == null) {
      System.out.println("[ERROR] invalid dir");
      return;
    }
    if (!isPortAvailable(port)) {
      System.out.println("[ERROR] Port " + port + " is using.");
      return;
    }
    for (int i = portPoolStart; i < portPoolStart + clientCount; i++) {
      if (!isPortAvailable(i)) {
        System.out.println("[ERROR] Port " + i + " is using.");
        return;
      }
    }
    System.out.println("[INFO] Port pool: " + portPoolStart + " - " + (portPoolStart + clientCount - 1));
    // 初始化地址池
    this.usedPort = new int[clientCount];
    for (int i = 0; i < clientCount; i++) {
      this.usedPort[i] = -1;
    }
    File file = FileIO.checkDir(dir);
    if (file == null) return;
    NetSocket netSocket;
    try {
      netSocket = new NetSocket(port, true);
    } catch (IOException e) {
      System.out.println("[ERROR] Port " + port + " already in use!");
      return;
    }
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
              sb.append(f.getName()).append("-----").append(FileNet.getSize(f.length())).append("\n");
            }
          }
          ack.setData(sb.toString().getBytes());
          break;
        case "SEND":
          // 从端口池中新开端口，等待客户端连接
          int sendSessionId = 0;
          try {
            sendSessionId = Integer.parseInt(recStr.substring(4));
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
          int sendPortIndex = getSessionPortIndex(sendSessionId);
          if (sendPortIndex == -1) sendPortIndex = getFreePortIndex();
          int sendPort = portPoolStart + sendPortIndex;
          if (sendPortIndex == -1 || !isPortAvailable(sendPort)) {
            ack.setData("BUSY".getBytes());
          } else {
            ack.setData(("PORT" + sendPort).getBytes());
            int finalSendPortIndex = sendPortIndex;
            ReceiveThread receiveThread = new ReceiveThread(sendSessionId, sendPort, dir, () -> usedPort[finalSendPortIndex] = -1);
            receiveThread.start();
            usedPort[sendPortIndex] = sendSessionId;
          }
          break;
        case "GETS":
          int getSessionId = 0;
          try {
            getSessionId = Integer.parseInt(recStr.substring(4, recStr.indexOf('-')));
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
          // 从端口池中新开端口，等待客户端连接
          int getPortIndex = getSessionPortIndex(getSessionId);
          if (getPortIndex == -1) getPortIndex = getFreePortIndex();
          int getPort = portPoolStart + getPortIndex;
          if (getPortIndex == -1 || !isPortAvailable(getPort)) {
            ack.setData("BUSY".getBytes());
          } else {
            ack.setData(("PORT" + getPort).getBytes());
            String getFilePath = dir + "/" + recStr.substring(recStr.indexOf('-') + 1);
            File fileOfSend = new File(getFilePath);
            if (!fileOfSend.exists() || !fileOfSend.isFile()) {
              System.out.printf("[ERROR] %s is not a file.%n", getFilePath);
              ack.setData("NOTHING".getBytes());
              return ack;
            }
            int finalGetPortIndex = getPortIndex;
            SendThread receiveThread =
                    new SendThread(getSessionId, getPort, getFilePath, () -> usedPort[finalGetPortIndex] = -1);
            receiveThread.start();
            usedPort[getPortIndex] = getSessionId;
          }
          break;
      }
      return ack;
    }, 0);
  }

  private int getFreePortIndex() {
    for (int i = 0; i < usedPort.length; i++) {
      if (usedPort[i] == -1) {
        return i;
      }
    }
    return -1;
  }

  private int getSessionPortIndex(int sessionId) {
    for (int i = 0; i < usedPort.length; i++) {
      if (usedPort[i] == sessionId) {
        return i;
      }
    }
    return -1;
  }

}
