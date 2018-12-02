package cn.zhenly.lftp.cmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.RunLast;


@Command(name = "lftp", mixinStandardHelpOptions = true, version = "lftp v0.0.1", description = "Send and receive large file by udp.")
public class Main implements Runnable {

  public static void main(String[] args) {
    CommandLine cmd = new CommandLine(new Main());
    cmd.addSubcommand("server", new Server());
    cmd.addSubcommand("lsend", new Send());
    cmd.addSubcommand("lget", new Get());
    cmd.addSubcommand("list", new GetList());
    try {
      cmd.parseWithHandler(new RunLast(), args);
    } catch (Exception ignored) {
      System.out.println("Lack of parameter for " + args[args.length - 1]);
    }
  }

  @Override
  public void run() {
    new CommandLine(this).usage(System.out);
  }
}
