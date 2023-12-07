package part2.client;

import part2.client.api.MultithreadApi;

import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final AtomicInteger successCountGet = new AtomicInteger(0);
    private static final AtomicInteger successCountPost = new AtomicInteger(0);
    private static final AtomicInteger failureCountGet = new AtomicInteger(0);
    private static final AtomicInteger failureCountPost = new AtomicInteger(0);


    public static void main(String[] args) {
        String localhost = "localhost";
        String loadBalancer = "updatedlb-973683078.us-west-2.elb.amazonaws.com";
        String elasticIP = "54.203.240.37";
        MultithreadApi multithreadApi = new MultithreadApi(
                10, 30, 2, elasticIP,
                successCountGet, failureCountGet, successCountPost, failureCountPost);
        multithreadApi.startThreads();
    }
}
