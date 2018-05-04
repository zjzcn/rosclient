package com.github.rosclient;

import com.github.rosclient.callback.TopicCallback;
import com.github.rosclient.listener.ConnectionListener;
import com.github.rosclient.messages.Message;
import org.json.JSONObject;

public class RosClientTest {

    public static void main(String[] args) throws InterruptedException {
//        final RosClient rosClient = RosClient.create("ws://10.12.33.216:9090");
//        final RosClient rosClient = RosClient.create("ws://localhost:9090");
//        final RosClient rosClient = RosClient.create("ws://192.168.10.10:9090");
        final RosClient rosClient = RosClient.create("ws://10.12.32.147:9090");
        rosClient.setListener(new ConnectionListener() {
            @Override
            public void onConnect() {
                System.out.println("onConnect");

//                Service addTwoInts = new Service(rosClient, "/add_two_ints", "rospy_tutorials/AddTwoInts");
//
//                ServiceRequest request = new ServiceRequest("{\"a\": 10, \"b\": 20}");
//                ServiceResponse response = addTwoInts.callServiceAndWait(request);
//                System.out.println(response.toString());
            }

            @Override
            public void onDisconnect(boolean normal, int code, String reason) {
                System.out.println("onDisconnect");
            }

            @Override
            public void onError(Exception e) {
                System.out.println("onDisconnect" + e.getMessage());
            }
        });

        rosClient.connectBlocking();

        RosApi rosApi = new RosApi(rosClient);
        System.out.println(rosApi.getTopics());
        System.out.println(rosApi.getServices());
        System.out.println(rosApi.getNodes());
        final Topic echo = new Topic(rosClient, "/echo", "std_msgs/String");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Message toSend = new Message(new JSONObject("{\"data\": \"hello, world!\"}"));
                    echo.publish(toSend);

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        Topic echoBack = new Topic(rosClient, "/echo", "std_msgs/String");
        echoBack.subscribe(new TopicCallback() {
            @Override
            public void handleMessage(Message message) {
                System.out.println("From ROS: " + message.getMsg());
            }
        });

    }
}
