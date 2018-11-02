package Server;

import NetUDP.UDPPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Server {
  public static void main(String[] args) throws IOException {
    String str_send = "Hello UDPclient";
    byte[] buf = new byte[1024];
    // 服务端在3000端口监听接收到的数据
    DatagramSocket ds = new DatagramSocket(3000);
    // 接收从客户端发送过来的数据
    DatagramPacket dp_receive = new DatagramPacket(buf, 1024);
    System.out.println("server is on，waiting for client to send data......");
    while (true) {
      // 服务器端接收来自客户端的数据
      ds.receive(dp_receive);
      System.out.println("server received data from client：");
      UDPPacket udpPacket = (UDPPacket)Util.Serialize.ReadString(new String(dp_receive.getData(), 0, dp_receive.getLength()));
      System.out.println(dp_receive.getAddress().getHostAddress() + ":" + dp_receive.getPort());
      if (udpPacket != null) {
        System.out.println(udpPacket.getContent());
        System.out.println(udpPacket.getId());
        System.out.println(udpPacket.getType());
      }
      // 数据发动到客户端的3000端口
      DatagramPacket dp_send = new DatagramPacket(str_send.getBytes(), str_send.length(), dp_receive.getAddress(),
          9000);
      ds.send(dp_send);
      // 由于dp_receive在接收了数据之后，其内部消息长度值会变为实际接收的消息的字节数，
      // 所以这里要将dp_receive的内部消息长度重新置为1024
      dp_receive.setLength(1024);
    }
    // ds.close();
  }
}
