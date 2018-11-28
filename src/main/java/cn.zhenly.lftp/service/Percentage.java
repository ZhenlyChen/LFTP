package cn.zhenly.lftp.service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

// 进度条显示
class Percentage {
  private long lastTime;

  Percentage() {
    this.lastTime = 0;
  }

  void show(float percentage, long fileSize) {
    int percentageInt = Math.round(percentage * 100);
    System.out.print('\r');
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < 100; j++) sb.append(j < percentageInt ? "=" : (percentageInt == j ? ">" : " "));
    System.out.print(sb.toString() + "\t" + percentage * 100 + "%");
    if (lastTime == 0) {
      lastTime = (new Date()).getTime();
    }
    long nowTime = (new Date()).getTime();
    double speed = ((fileSize / 1024.0)  * percentage) / ((nowTime - lastTime + 1) / 1000.0);
    double time = (nowTime - lastTime + 1) / 1000.0;
    System.out.print("\tSpeed: " + (double)(Math.round(speed*100))/100 + "KB/s");
    System.out.print("\tTime: " +  (double)(Math.round(time*100))/100+ "s");
  }
}
