package cn.zhenly.lftp.cmd;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

public class Util {

  static class AddressInfo {
    InetAddress ip;
    int port;
    boolean valid;
  }
  static long lastTime;

  static void showPercentage(float percentage, long fileSize) {
    int percentageInt = Math.round(percentage * 100);
    System.out.print('\r');
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < 100; j++) sb.append(j < percentageInt ? "=" : (percentageInt == j ? ">" : " "));
    System.out.print(sb.toString() + "\t" + percentage * 100 + "%");
    if (lastTime == 0) {
      lastTime = (new Date()).getTime();
    }
    long now = (new Date()).getTime();
    double speed = ((fileSize / 1024.0)  * percentage) / ((now - lastTime + 1) / 1000.0);
    double time = (now - lastTime + 1) / 1000.0;
    System.out.print("   Speed: " + (double)(Math.round(speed*100))/100 + "KB/s   ");
    System.out.print("   Time: " +  (double)(Math.round(time*100))/100+ "s");
  }

  static void resetPercentage() {
    lastTime = 0;
  }


  private static void bindPort(String host, int port) throws Exception {
    Socket s = new Socket();
    s.bind(new InetSocketAddress(host, port));
    s.close();
  }

  static boolean isPortAvailable(int port) {
    Socket s = new Socket();
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
