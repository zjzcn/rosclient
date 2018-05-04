package com.github.rosclient;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class RosServerStub extends WebSocketServer {

    public RosServerStub(int port) {
        super(new InetSocketAddress(port));
    }

    public RosServerStub(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // ws连接的时候触发的代码，onOpen中我们不做任何操作
        System.out.println(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        //断开连接时候触发代码
        System.out.println(reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println(message);
        conn.send(message);

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        //错误时候触发的代码
        System.out.println("on error");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("onStart");
    }

    public static void main(String[] args) {
        RosServerStub server = new RosServerStub(9090);
        server.start();
    }
}
