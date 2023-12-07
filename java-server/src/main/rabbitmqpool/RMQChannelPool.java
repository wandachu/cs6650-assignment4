package main.rabbitmqpool;

import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RMQChannelPool {
    // used to store and distribute channels
    private final BlockingQueue<Channel> pool;
    // fixed size pool
    private final int capacity;
    // used to create channels
    private final RMQChannelFactory factory;


    public RMQChannelPool(int maxSize, RMQChannelFactory factory) {
        this.capacity = maxSize;
        pool = new LinkedBlockingQueue<>(capacity);
        this.factory = factory;
        for (int i = 0; i < capacity; i++) {
            Channel chan;
            try {
                chan = factory.create();
                pool.put(chan);
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(RMQChannelPool.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public Channel borrowObject() throws IOException {
        try {
            return pool.take();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error: no channels available" + e);
        }
    }

    public void returnObject(Channel channel) throws Exception {
        if (channel != null) {
            pool.add(channel);
        }
    }

    public void close() {
        // pool.close();
    }
}
