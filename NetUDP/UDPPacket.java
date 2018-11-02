package NetUDP;

import java.io.*;

public class UDPPacket implements Serializable {
    private String content;
    private int id;
    private PacketType type;

    public UDPPacket (int id, String content, PacketType type) {
        this.id = id;
        this.content = content;
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }
}