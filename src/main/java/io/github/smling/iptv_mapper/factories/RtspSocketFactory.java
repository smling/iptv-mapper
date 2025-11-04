package io.github.smling.iptv_mapper.factories;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RtspSocketFactory {
    public static Socket of(String scheme, String host, int port) throws Exception {
        if ("rtsps".equals(scheme)) {
            // TLS socket; trust default JVM CAs (works for public CAs). For self-signed cams, supply a custom SSLContext.
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket ssl = (SSLSocket) factory.createSocket();
            ssl.connect(new InetSocketAddress(host, port), 5000);
            ssl.startHandshake();
            return ssl;
        } else {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), 5000);
            return s;
        }
    }
}
