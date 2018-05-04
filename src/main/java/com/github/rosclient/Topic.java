package com.github.rosclient;

import com.github.rosclient.callback.TopicCallback;
import com.github.rosclient.messages.Message;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Topic {

	private final RosClient rosClient;
	private final String name;
	private final String type;
	private boolean isAdvertised;
	private boolean isSubscribed;
	private final Fields.CompressionType compression;
	private final int throttleRate;

	// used to keep track of this object's callbacks
	private final List<TopicCallback> callbacks;

	// used to keep track of the subscription IDs
	private final List<String> ids;

	public Topic(RosClient rosClient, String name, String type) {
		this(rosClient, name, type, Fields.CompressionType.none, 0);
	}

	public Topic(RosClient rosClient, String name, String type,
				 Fields.CompressionType compression) {
		this(rosClient, name, type, compression, 0);
	}

	public Topic(RosClient rosClient, String name, String type, int throttleRate) {
		this(rosClient, name, type, Fields.CompressionType.none, throttleRate);
	}

	public Topic(RosClient rosClient, String name, String type,
				 Fields.CompressionType compression, int throttleRate) {
		this.rosClient = rosClient;
		this.name = name;
		this.type = type;
		this.isAdvertised = false;
		this.isSubscribed = false;
		this.compression = compression;
		this.throttleRate = throttleRate;
		this.callbacks = new ArrayList<>();
		this.ids = new ArrayList<>();
	}

	public RosClient getRosClient() {
		return this.rosClient;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * Return the message type of this topic.
	 *
	 * @return The message type of this topic.
	 */
	public String getType() {
		return this.type;
	}

	public boolean isAdvertised() {
		return this.isAdvertised;
	}

	public boolean isSubscribed() {
		return this.isSubscribed;
	}

	public Fields.CompressionType getCompression() {
		return this.compression;
	}

	public int getThrottleRate() {
		return this.throttleRate;
	}

	public void subscribe(TopicCallback cb) {
		rosClient.registerTopicCallback(this.name, cb);
		callbacks.add(cb);

		String id = Fields.OP_CODE_SUBSCRIBE + ":" + name + ":" + rosClient.nextId();
		ids.add(id);

		JSONObject call = new JSONObject()
				.put(Fields.FIELD_OP, Fields.OP_CODE_SUBSCRIBE)
				.put(Fields.FIELD_ID, id)
				.put(Fields.FIELD_TYPE, this.type)
				.put(Fields.FIELD_TOPIC, this.name)
				.put(Fields.FIELD_COMPRESSION, this.compression.toString())
				.put(Fields.FIELD_THROTTLE_RATE, this.throttleRate);
		rosClient.send(call);

		this.isSubscribed = true;
	}

	public void unsubscribe() {
		for (TopicCallback cb : this.callbacks) {
			this.rosClient.unregisterTopicCallback(this.name, cb);
		}
		this.callbacks.clear();

		for (String id : this.ids) {
			JSONObject call = new JSONObject()
					.put(Fields.FIELD_OP, Fields.OP_CODE_UNSUBSCRIBE)
					.put(Fields.FIELD_ID, id)
					.put(Fields.FIELD_TOPIC, this.name);
			this.rosClient.send(call);
		}

		this.isSubscribed = false;
	}

	public void advertise() {
		String id = Fields.OP_CODE_ADVERTISE + ":" + name + ":" + rosClient.nextId();
		JSONObject call = new JSONObject()
				.put(Fields.FIELD_OP, Fields.OP_CODE_ADVERTISE)
				.put(Fields.FIELD_ID, id)
				.put(Fields.FIELD_TYPE, this.type)
				.put(Fields.FIELD_TOPIC, this.name);
		this.rosClient.send(call);

		this.isAdvertised = true;
	}

	public void unadvertise() {
		String id = Fields.OP_CODE_UNADVERTISE + ":" + name + ":" + rosClient.nextId();
		JSONObject call = new JSONObject()
				.put(Fields.FIELD_OP, Fields.OP_CODE_UNADVERTISE)
				.put(Fields.FIELD_ID, id)
				.put(Fields.FIELD_TOPIC, this.name);
		this.rosClient.send(call);

		this.isAdvertised = false;
	}

	public void publish(Message message) {
		if (!this.isAdvertised()) {
			this.advertise();
		}

		String id = Fields.OP_CODE_PUBLISH + ":" + name + ":" + rosClient.nextId();
		JSONObject call = new JSONObject()
				.put(Fields.FIELD_OP, Fields.OP_CODE_PUBLISH)
				.put(Fields.FIELD_ID, id)
				.put(Fields.FIELD_TOPIC, this.name)
				.put(Fields.FIELD_MESSAGE, message.getMsg());
		this.rosClient.send(call);
	}
}
