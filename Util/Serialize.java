package Util;

import java.io.*;
import java.util.Objects;

public class Serialize {
    private static final String TEMP_ENCODING = "ISO-8859-1";
    private static final String DEFAULT_ENCODING = "UTF-8";
    public static String GetString(Object obj) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        String serStr = null;
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(obj);
            serStr = byteArrayOutputStream.toString(TEMP_ENCODING);
            serStr = java.net.URLEncoder.encode(serStr, DEFAULT_ENCODING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serStr;
    }

    public static Object ReadString(String str) throws IOException {
        ByteArrayInputStream byteArrayInputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            String deserStr = java.net.URLDecoder.decode(str, DEFAULT_ENCODING);
            byteArrayInputStream = new ByteArrayInputStream(deserStr.getBytes(TEMP_ENCODING));
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            Objects.requireNonNull(objectInputStream).close();
            Objects.requireNonNull(byteArrayInputStream).close();
        }
        return null;
    }

}
