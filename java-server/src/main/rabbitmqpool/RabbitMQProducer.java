package main.rabbitmqpool;

import java.time.Duration;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import org.apache.commons.pool2.impl.*;

public class RabbitMQProducer {
    // For Apache pool example, this allows the pool size to grow to ~= the same number of concurrent threads
    // that utilize the pool. Pass to config.setMaxWait(..) method to allow this behaviour
    private static final int ON_DEMAND = -1;
    // Number of channels to add to pools
    private static final int NUM_CHANS = 50;
    // Sets the maximum duration the borrowObject() method should block before throwing an exception when the pool is exhausted and getBlockWhenExhausted() is true.
    private static final int WAIT_TIME_SECS = 5;

    public static GenericObjectPool<Channel> setup(Connection conn) throws InterruptedException { // using Apache

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(NUM_CHANS);
        // clients will block when pool is exhausted, for a maximum duration of WAIT_TIME_SECS
        config.setBlockWhenExhausted(true);
        config.setMaxWait(Duration.ofSeconds(WAIT_TIME_SECS));

        // The channel factory generates new channels on demand, as needed by the GenericObjectPool
        RMQChannelFactory chanFactory = new RMQChannelFactory(conn);

        //create the pool
        return new GenericObjectPool<>(chanFactory, config);
    }

    public static RMQChannelPool createChannelPool(Connection conn) { // using BlockingQueue
        // The channel factory generates new channels on demand, as needed by the channel pool
        RMQChannelFactory chanFactory = new RMQChannelFactory (conn);
        // create the fixed size channel pool
        return new RMQChannelPool(NUM_CHANS, chanFactory);
    }
}
