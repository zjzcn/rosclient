package com.github.rosclient.listener;

public interface ConnectionListener {

    void onConnect();

    void onDisconnect(boolean normal, int code, String reason);

    void onError(Exception e);

}
