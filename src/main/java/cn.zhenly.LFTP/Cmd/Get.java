package cn.zhenly.lftp.cmd;

import picocli.CommandLine.*;

import java.util.List;

@Command(name = "lget", mixinStandardHelpOptions = true, description = "get file from server.")
public class Get implements Runnable {

  @Option(names = {"-s", "--server"}, description = "Server location.")
  String server;

  @Parameters(description = "file path")
  List<String> file;

  @Override
  public void run() {
    System.out.println(server);
    for (String f : file) {
      System.out.println(f);
    }
  }
}