package com.github.rosclient.messages;

import org.json.JSONObject;

public class Request {

    public static Request EMPTY = new Request(new JSONObject());

    private String id;

    private JSONObject args;

    public Request(JSONObject args) {
        this.args = args;
    }

    public JSONObject getArgs() {
        return args;
    }

    public void setArgs(JSONObject args) {
        this.args = args;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
