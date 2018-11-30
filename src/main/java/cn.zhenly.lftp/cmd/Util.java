package cn.zhenly.lftp.cmd;

class Util {
  static int getPortFromData(byte[] data) {
    String str = new String(data);
    if (str.substring(0, 4).equals("BUYS")) {
      System.out.println("[ERROR] Server is buys now");
      return -1;
    }
    if (!str.substring(0, 4).equals("PORT")) {
      System.out.println("[ERROR] System error!");
      return -1;
    }
    int port = -1;
    try {
      port = Integer.parseInt(str.substring(4));
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    if (port == -1) {
      System.out.println("[INFO] Can't get port from " + str);
    } else {
      System.out.println("[INFO] Get send port: " + port);
    }
    return port;
  }
}
