package com.github.rosclient;

import com.github.rosclient.messages.Request;
import com.github.rosclient.messages.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RosApi {

    private RosClient rosClient;

    public RosApi(RosClient rosClient) {
        this.rosClient = rosClient;
    }

    public List<String> getNodes() {
        Service service = new Service(rosClient, "/rosapi/nodes");
        Response resp = service.callServiceAndWait(Request.EMPTY);
        JSONObject values = resp.getValues();
        JSONArray nodes = values.getJSONArray("nodes");
        List<String> list = new ArrayList<>();
        for(Object item : nodes) {
            list.add((String)item);
        }
        return list;
    }

    public List<String> getServices() {
        Service service = new Service(rosClient, "/rosapi/services");
        Response resp = service.callServiceAndWait(Request.EMPTY);
        JSONObject values = resp.getValues();
        JSONArray services = values.getJSONArray("services");
        List<String> list = new ArrayList<>();
        for(Object item : services) {
            list.add((String)item);
        }
        return list;
    }

    public List<String> getTopics() {
        Service service = new Service(rosClient, "/rosapi/topics");
        Response resp = service.callServiceAndWait(Request.EMPTY);
        JSONObject values = resp.getValues();
        JSONArray topics = values.getJSONArray("topics");
        List<String> list = new ArrayList<>();
        for(Object item : topics) {
            list.add((String)item);
        }
        return list;
    }
}
