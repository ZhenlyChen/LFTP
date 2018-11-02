package cn.zhenly.LFTP.Client;

import cn.zhenly.LFTP.NetUDP.NetUDP;
import cn.zhenly.LFTP.NetUDP.PacketType;
import cn.zhenly.LFTP.NetUDP.UDPPacket;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Client {
	public static void main(String args[]) {
		try {
			NetUDP netUDP = new NetUDP(9000, InetAddress.getLocalHost(), 3000);
			netUDP.setTimeOut(2000);
			netUDP.UDPSend(new UDPPacket(0, "Hello world!".getBytes(), PacketType.DATA));
			UDPPacket res = netUDP.UDPReceive();
			if (res != null) {
				System.out.println(res.getType().toString());
			}
			netUDP.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}

