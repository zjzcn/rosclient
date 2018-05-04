package com.github.rosclient.callback;

import com.github.rosclient.messages.Response;

public interface ServiceCallback {

	void handleServiceResponse(Response response);
}
