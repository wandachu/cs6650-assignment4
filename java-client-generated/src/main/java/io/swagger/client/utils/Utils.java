package io.swagger.client.utils;

import java.util.concurrent.BlockingQueue;

public class Utils {
    public static void produceToQueue(BlockingQueue<String[]> queue, String[] str) {
        try {
            queue.put(str);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
