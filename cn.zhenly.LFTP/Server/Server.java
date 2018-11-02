package cn.zhenly.LFTP.Server;

import cn.zhenly.LFTP.NetUDP.NetUDP;
import cn.zhenly.LFTP.NetUDP.PacketType;
import cn.zhenly.LFTP.NetUDP.UDPPacket;

public class Server {
  public static void main(String[] args) {
    NetUDP netUDP = new NetUDP(3000);
    while (true) {
      UDPPacket data = netUDP.UDPReceive();
      System.out.println("server received data from client：");
      if (data != null) {
        System.out.println(data.getPacket().getAddress().getHostAddress() + ":" + data.getPacket().getPort());
        System.out.println(new String(data.getContent()));
        System.out.println(data.getId());
        System.out.println(data.getType());
        netUDP.setTarget(data.getPacket().getAddress(), data.getPacket().getPort());
        netUDP.UDPSend(new UDPPacket(data.getId(), "ACK".getBytes(), PacketType.ACK));
      }
    }
  }
}
