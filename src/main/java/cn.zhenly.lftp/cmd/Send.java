package cn.zhenly.lftp.cmd;

import cn.zhenly.lftp.net.FileChunk;
import cn.zhenly.lftp.net.NetSocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static cn.zhenly.lftp.cmd.Util.showPercentage;
import static cn.zhenly.lftp.net.Util.getByte;


@Command(name = "lsend", mixinStandardHelpOptions = true, description = "Send file to server.")
public class Send implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.")
  private String server;

  @Parameters(description = "file path")
  private List<String> files;

  @Option(names = {"-c", "--control"}, description = "Control port.", defaultValue = "9000")
  private int controlPort;

  @Option(names = {"-p", "--send"}, description = "Send port.", defaultValue = "9001")
  private int sendPort;

  private Util.AddressInfo target;

  @Override
  public void run() {
    target = Util.parseIPAddr(server);
    if (!target.valid) {
      return;
    }
    // 解析文件
    if (files.size() == 0) {
      System.out.println("[ERROR] no file to send.");
      return;
    } else if (files.size() > 1) {
      System.out.println("[INFO] LFTP is not support multiple files currently. :(");
    }
    // 发送文件
    String fileName = files.get(0);
    System.out.printf("[INFO] File: %s ready to send.%n", fileName);
    File file = new File(fileName);
    if (!file.exists() || !file.isFile()) {
      System.out.printf("[ERROR] %s is not a file.%n", fileName);
      return;
    }
    try (NetSocket netSocket = new NetSocket(controlPort, target.ip, target.port)) {
      netSocket.send("SEND".getBytes(), data -> {
        String str = new String(data.getData());
        if (!str.substring(0, 4).equals("PORT")) {
          System.out.println("[ERROR] System error!");
          return;
        }
        int port = -1;
        try {
          port = Integer.parseInt(str.substring(4));
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
        System.out.println("[INFO] Get send port: " + port);
        sendFile(file, port);
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // 发送文件
  private void sendFile(File file, int port) {
    try {
      NetSocket netSocket;
      netSocket = new NetSocket(sendPort, target.ip, port);
      InputStream inStream = new FileInputStream(file);
      List<byte[]> buffs = new ArrayList<>();
      byte[] buf = new byte[1024];
      // 分割文件
      while (inStream.read(buf) != -1) {
        buffs.add(buf.clone());
      }
      boolean[] finishChunk = new boolean[buffs.size()];
      FileChunk initChunk = new FileChunk(file.getName(), -1, buffs.size(), new byte[1024]);
      netSocket.send(getByte(initChunk), data-> {
        System.out.println("[INFO] Start to send file " + file.getName() + " (" + file.length() / 1024.0 + "KB)");
        if (new String(data.getData()).equals("OK")) {
          for (int i = 0; i < buffs.size(); i++) {
            FileChunk fileChunk = new FileChunk(file.getName(), i, buffs.size(), buffs.get(i));
            try {
              netSocket.send(getByte(fileChunk), d -> {
                int id = Integer.parseInt(new String(d.getData()));
                if (id >= 0 && id < buffs.size()) {
                  showPercentage((float)(id+1) / buffs.size(), file.length());
                  finishChunk[id] = true;
                  if (isFinishChunk(finishChunk, buffs.size())) {
                    try {
                      netSocket.disconnect(null);
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                  }
                }
              });
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        } else {
          System.out.println("[ERROR] Can't connect to server");
        }
      });
    } catch (IOException e) {
      System.out.printf("[ERROR] Can't read file: %s%n", file.getName());
    }
  }


  private boolean isFinishChunk(boolean[] finishChunk, int size) {
    for (int i = 0; i < size; i++) {
      if (!finishChunk[i]) return false;
    }
    return true;
  }
}


