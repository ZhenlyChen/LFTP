package NetUDP;

import Util.Serialize;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class NetUDP {
    private InetAddress targetIP;
    private int targetPort;
    DatagramSocket socket;

    public NetUDP(int port) {
        initSocket(port);
    }

    public NetUDP(int port, InetAddress targetIP, int targetPort) {
        initSocket(port);
        setTarget(targetIP, targetPort);
    }

    // 设置目标
    public void setTarget(InetAddress targetIP, int targetPort) {
        this.targetPort = targetPort;
        this.targetIP = targetIP;
    }

    // 初始化自身socket
    private void initSocket(int port) {
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(2000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    // 发送数据
    public void send(String sendData) {
        UDPSend(sendData);
        // UDPPacket udpPacket = (UDPPacket) Serialize.ReadString(UDPRecive());
        // System.out.println(udpPacket.getContent());
        System.out.println(UDPRecive());
    }

    private String UDPRecive() {
        byte[] buf = new byte[1024];
        DatagramPacket p = new DatagramPacket(buf, 1024);
        try {
            socket.receive(p);
            return new String(p.getData());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean UDPSend(String sendData) {
        UDPPacket packetData = new UDPPacket(0, sendData, PacketType.DATA);
        String packetText = Serialize.GetString(packetData);
        System.out.println(packetText);
        DatagramPacket packet = new DatagramPacket(packetText.getBytes(), packetText.length(), this.targetIP, this.targetPort);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
