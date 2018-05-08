package com.github.rosclient;

import com.github.rosclient.callback.CallServiceCallback;
import com.github.rosclient.callback.ServiceCallback;
import com.github.rosclient.callback.TopicCallback;
import com.github.rosclient.listener.ConnectionListener;
import com.github.rosclient.messages.Message;
import com.github.rosclient.messages.Request;
import com.github.rosclient.messages.Response;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class RosClient extends WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(RosClient.class);

    private Long uid = 0L;

    // keeps track of callback functions for a given topic
    private final HashMap<String, ArrayList<TopicCallback>> topicCallbacks;

    // keeps track of callback functions for a given service request
    private final HashMap<String, ServiceCallback> serviceCallbacks;

    // keeps track of callback functions for a given advertised service
    private final HashMap<String, CallServiceCallback> callServiceCallbacks;

    // keeps track of handlers for this connection
    private ConnectionListener listener;


    private RosClient(URI serverUri) {
        super(serverUri);
        this.topicCallbacks = new HashMap<>();
        this.serviceCallbacks = new HashMap<>();
        this.callServiceCallbacks = new HashMap<>();
    }

    public static RosClient create(String serverUri) {
        RosClient client = null;
        try {
            URI uri = new URI(serverUri);
            client = new RosClient(uri);
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        return client;
    }

    public void setListener(ConnectionListener listener) {
        this.listener = listener;
    }

    public synchronized String nextId() {
        uid++;
        return uid.toString();
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        if (listener != null) {
            listener.onConnect();
        }
    }

    @Override
    public void onMessage(String message) {
        logger.debug("RosClient <={}.", message);
        try {
            JSONObject jo = new JSONObject(message);
            String op = jo.getString(Fields.FIELD_OP);
            if (op.equals(Fields.OP_CODE_PUBLISH)) {
                String topic = jo.getString(Fields.FIELD_TOPIC);
                List<TopicCallback> callbacks = topicCallbacks.get(topic);
                if (callbacks != null) {
                    Message msg = new Message(jo.getJSONObject(Fields.FIELD_MESSAGE));
                    for (TopicCallback cb : callbacks) {
                        cb.handleMessage(msg);
                    }
                }
            } else if (op.equals(Fields.OP_CODE_SERVICE_RESPONSE)) {
                String id = jo.getString(Fields.FIELD_ID);

                ServiceCallback cb = serviceCallbacks.get(id);
                if (cb != null) {
                    Boolean success = jo.getBoolean(Fields.FIELD_RESULT);
                    if (success == null) {
                        success = true;
                    }
                    if (success) {
                        JSONObject values = jo.getJSONObject(Fields.FIELD_VALUES);
                        Response response = new Response(success, values);
                        cb.handleServiceResponse(response);
                    } else {
                        String values = jo.getString(Fields.FIELD_VALUES);
                        Response response = new Response(success, values);
                        cb.handleServiceResponse(response);
                    }
                }
            } else if (op.equals(Fields.OP_CODE_CALL_SERVICE)) {
                String id = jo.getString(Fields.FIELD_ID);
                String service = jo.getString(Fields.FIELD_SERVICE);

                CallServiceCallback cb = callServiceCallbacks.get(service);
                if (cb != null) {
                    JSONObject args = jo.getJSONObject(Fields.FIELD_ARGS);
                    Request request = new Request(args);
                    request.setId(id);
                    cb.handleServiceCall(request);
                }
            } else {
                logger.error("Unknown op code: message={}", message);
            }
        } catch (Exception e) {
            logger.error("Invalid incoming ros bridge protocol: message={}", message, e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (listener != null) {
            boolean normal = (remote || (code == CloseFrame.NORMAL));
            listener.onDisconnect(normal, code, reason);
        }
    }

    @Override
    public void onError(Exception e) {
        if (listener != null) {
            listener.onError(e);
        }
    }

    public void send(JSONObject jo) {
        String message = jo.toString();
        logger.debug("RosClient =>{}", message);
        super.send(message);
    }

    /**
     * Sends an authorization request to the server.
     *
     * @param mac    The MAC (hash) string given by the trusted source.
     * @param client The IP of the client.
     * @param dest   The IP of the destination.
     * @param rand   The random string given by the trusted source.
     * @param t      The time of the authorization request.
     * @param level  The user level as a string given by the client.
     * @param end    The end time of the client's session.
     */
    public void auth(String mac, String client, String dest,
                             String rand, int t, String level, int end) {
        JSONObject call = new JSONObject()
                .put(Fields.FIELD_OP, Fields.OP_CODE_AUTH)
                .put(Fields.FIELD_MAC, mac)
                .put(Fields.FIELD_CLIENT, client)
                .put(Fields.FIELD_DESTINATION, dest)
                .put(Fields.FIELD_RAND, rand)
                .put(Fields.FIELD_TIME, t)
                .put(Fields.FIELD_LEVEL, level)
                .put(Fields.FIELD_END_TIME, end);
        this.send(call);
    }


    public void registerTopicCallback(String topic, TopicCallback cb) {
        if (!this.topicCallbacks.containsKey(topic)) {
            this.topicCallbacks.put(topic, new ArrayList<TopicCallback>());
        }

        this.topicCallbacks.get(topic).add(cb);
    }

    public void unregisterTopicCallback(String topic, TopicCallback cb) {
        if (this.topicCallbacks.containsKey(topic)) {
            ArrayList<TopicCallback> callbacks = this.topicCallbacks.get(topic);
            callbacks.remove(cb);

            if (callbacks.size() == 0) {
                this.topicCallbacks.remove(topic);
            }
        }
    }

    public void registerServiceCallback(String serviceCallId, ServiceCallback cb) {
        this.serviceCallbacks.put(serviceCallId, cb);
    }

    public void registerCallServiceCallback(String serviceName, CallServiceCallback cb) {
        this.callServiceCallbacks.put(serviceName, cb);
    }

    public void unregisterCallServiceCallback(String serviceName) {
        callServiceCallbacks.remove(serviceName);
    }
}
