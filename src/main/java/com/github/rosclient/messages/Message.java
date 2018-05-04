package com.github.rosclient.messages;

import org.json.JSONObject;

public class Message {

    private JSONObject msg;

    public Message() {

    }

    public Message(JSONObject msg) {
        this.msg = msg;
    }

    public JSONObject getMsg() {
        return msg;
    }

    public void setMsg(JSONObject msg) {
        this.msg = msg;
    }
}
