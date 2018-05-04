package com.github.rosclient;


import com.github.rosclient.callback.CallServiceCallback;
import com.github.rosclient.callback.ServiceCallback;
import com.github.rosclient.messages.Request;
import com.github.rosclient.messages.Response;
import org.json.JSONObject;

import java.util.concurrent.TimeoutException;

public class Service {

    private static final int DEFAULT_TIMEOUT_MS = 20 * 1000;

    private final RosClient rosClient;
    private final String name;
    private final String type;
    private boolean isAdvertised;

    private int timeoutMs;

    public Service(RosClient rosClient, String name) {
        this(rosClient, name, "", DEFAULT_TIMEOUT_MS);
    }
    public Service(RosClient rosClient, String name, String type) {
        this(rosClient, name, type, DEFAULT_TIMEOUT_MS);
    }

    public Service(RosClient rosClient, String name, String type, int timeoutMs) {
        this.rosClient = rosClient;
        this.name = name;
        this.type = type;
        this.timeoutMs = timeoutMs;
        this.isAdvertised = false;
    }

    public RosClient getRosClient() {
        return this.rosClient;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public boolean isAdvertised() {
        return this.isAdvertised;
    }

    public void callService(Request request, ServiceCallback cb) {
        String id = Fields.OP_CODE_CALL_SERVICE + ":" + name + ":" + rosClient.nextId();

        rosClient.registerServiceCallback(id, cb);

        JSONObject call = new JSONObject()
                .put(Fields.FIELD_OP, Fields.OP_CODE_CALL_SERVICE)
                .put(Fields.FIELD_ID, id)
                .put(Fields.FIELD_SERVICE, this.name)
                .put(Fields.FIELD_ARGS, request);
        rosClient.send(call);
    }

    public void sendResponse(Response response, String id) {
        JSONObject call = new JSONObject()
                .put(Fields.FIELD_OP, Fields.OP_CODE_SERVICE_RESPONSE)
                .put(Fields.FIELD_ID, id)
                .put(Fields.FIELD_SERVICE, this.name)
                .put(Fields.FIELD_VALUES, response)
                .put(Fields.FIELD_RESULT, response.isResult());
        rosClient.send(call);
    }

    public void advertiseService(CallServiceCallback cb) {
        rosClient.registerCallServiceCallback(this.name, cb);

        JSONObject call = new JSONObject()
                .put(Fields.FIELD_OP, Fields.OP_CODE_ADVERTISE_SERVICE)
                .put(Fields.FIELD_TYPE, this.type)
                .put(Fields.FIELD_SERVICE, this.name);
        rosClient.send(call);

        isAdvertised = true;
    }

    public void unadvertiseService() {
        rosClient.unregisterCallServiceCallback(this.name);

        JSONObject call = new JSONObject()
                .put(Fields.FIELD_OP, Fields.OP_CODE_UNADVERTISE_SERVICE)
                .put(Fields.FIELD_SERVICE, this.name);
        rosClient.send(call);

        this.isAdvertised = false;
    }

    public Response callServiceAndWait(Request request) {
        String id = Fields.OP_CODE_CALL_SERVICE + ":" + name + ":" + rosClient.nextId();
        BlockingCallback cb = new BlockingCallback(id);
        rosClient.registerServiceCallback(id, cb);

        JSONObject call = new JSONObject()
                .put(Fields.FIELD_OP, Fields.OP_CODE_CALL_SERVICE)
                .put(Fields.FIELD_ID, id)
                .put(Fields.FIELD_TYPE, this.type)
                .put(Fields.FIELD_SERVICE, this.name)
                .put(Fields.FIELD_ARGS, request.getArgs());
        try {
            rosClient.send(call);
        } catch (Exception e) {
            cb.onFailure(e);
        }

        return cb.getResponse();
    }

    private class BlockingCallback implements ServiceCallback {

        private Object lock = new Object();

        private String requestId;
        private Response response;
        private Exception exception;
        private boolean isDone = false;
        private long createTime = System.currentTimeMillis();

        public BlockingCallback(String requestId) {
            this.requestId = requestId;
        }

        public void onSuccess(Response response) {
            this.response = response;
            done();
        }

        public void onFailure(Exception e) {
            this.exception = e;
            done();
        }

        @Override
        public void handleServiceResponse(Response response) {
            if (response.isResult()) {
                onSuccess(response);
            } else {
                onFailure(new RosException("Service response result is false. id=" + requestId + ", values=" + response.getValues()));
            }
        }

        public Response getResponse() {
            synchronized (lock) {
                if (isDone) {
                    if (exception != null) {
                        throwException();
                    }
                    return response;
                }

                long costTime = System.currentTimeMillis() - createTime;
                if (costTime < timeoutMs) {
                    for (;;) {
                        try {
                            lock.wait(timeoutMs - costTime);
                        } catch (InterruptedException e) {
                            // nothing
                        }

                        if (isDone) {
                            break;
                        }

                        costTime = System.currentTimeMillis() - createTime;
                        if (costTime >= timeoutMs) {
                            break;
                        }
                    }
                }

                if (!isDone) {
                    onFailure(new TimeoutException("Service call timeout: id=" + requestId + ", timeoutMs=" + timeoutMs));
                }
            }
            if (exception != null) {
                throwException();
            }
            return response;
        }

        private void done() {
            synchronized (lock) {
                if (isDone) {
                    return;
                }

                isDone = true;
                lock.notifyAll();
                rosClient.unregisterCallServiceCallback(requestId);
            }
        }

        private void throwException() {
            throw new RosException("Ros call service exception", exception);
        }
    }
}
