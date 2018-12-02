package cn.zhenly.lftp.service;

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
    sb.append((double) (Math.round(percentage * 10000)) / 100);
    sb.append("%\t[");
    for (int j = 0; j < 100; j++) sb.append(j < percentageInt ? "=" : (percentageInt == j ? ">" : " "));
    System.out.print(sb.toString() + "]\t");
    if (lastTime == 0) {
      lastTime = System.currentTimeMillis();
    }
    long nowTime = System.currentTimeMillis();
    double speed = ((fileSize / 1024.0) * percentage) / ((nowTime - lastTime + 1) / 1000.0);
    double time = (nowTime - lastTime + 1) / 1000.0;
    if (speed > 1024) {
      speed = speed / 1024;
      System.out.print((double) (Math.round(speed * 100)) / 100 + "MB/s\t");
    } else {
      System.out.print((double) (Math.round(speed * 100)) / 100 + "KB/s\t");
    }
    System.out.print("in " + (double) (Math.round(time * 100)) / 100 + "s");
    if (percentage == 1) System.out.print('\n');
  }



}
