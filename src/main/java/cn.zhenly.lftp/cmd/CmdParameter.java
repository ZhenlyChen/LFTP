package cn.zhenly.lftp.cmd;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

class CmdParameter {

  public static class AddressInfo {
    InetAddress ip;
    int port;
    boolean valid;
  }

  AddressInfo target;
  String fileName;
  CmdParameter(String server, List<String> files) {
    target = parseIPAddr(server);
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
    fileName = files.get(0);
    System.out.printf("[INFO] File: %s ready to send.%n", fileName);
  }

  private static void bindPort(String host, int port) throws Exception {
    Socket s = new Socket();
    s.bind(new InetSocketAddress(host, port));
    s.close();
  }

  static boolean isPortAvailable(int port) {
    try {
      bindPort("0.0.0.0", port);
      bindPort(InetAddress.getLocalHost().getHostAddress(), port);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  static AddressInfo parseIPAddr(String ipStr) {
    AddressInfo info = new AddressInfo();
    info.valid = false;
    if (ipStr.equals("")) {
      System.out.printf("[ERROR] Invalid server location%n");
      return info;
    }
    String[] targetAddress = ipStr.split(":");
    // 解析端口
    if (targetAddress.length == 2) {
      try {
        info.port = Integer.parseInt(targetAddress[1]);
      } catch (NumberFormatException e) {
        System.out.printf("[ERROR] Invalid server port: %s%n", targetAddress[1]);
        return info;
      }
    } else if (targetAddress.length > 2 || targetAddress.length < 1) {
      System.out.printf("[ERROR] Invalid server location: %s%n", ipStr);
      return info;
    }
    // 解析地址
    try {
      info.ip = InetAddress.getByName(targetAddress[0]);
    } catch (UnknownHostException e) {
      System.out.printf("[ERROR] Invalid server location: %s%n", ipStr);
      return info;
    }
    info.valid = true;
    System.out.printf("[INFO] Server location: %s:%d%n", info.ip.toString(), info.port);
    return info;
  }
}
