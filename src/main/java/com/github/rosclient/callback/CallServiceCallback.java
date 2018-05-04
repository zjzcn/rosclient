package com.github.rosclient.callback;

import com.github.rosclient.messages.Request;

public interface CallServiceCallback {

    void handleServiceCall(Request request);
}
