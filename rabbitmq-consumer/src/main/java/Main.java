import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;


public class Main {
    private static final int NUM_THREADS = 60;
    private final static String QUEUE_NAME = "likeornot";
    private static final String SERVER = "localhost";


    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(SERVER);

        // we don't use auto close because we want to keep the connection on!
        Connection connection = factory.newConnection();

        // latch is used for the main thread to block until all test treads complete
        final CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            Runnable runnable = () -> {
                try {
                    final Channel channel = connection.createChannel();
                    // idempotent. we might start the consumer before the publisher
                    channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

                    channel.basicQos(1);

                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                        writeToDatabase(message);
                    };
                    channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
                } catch (IOException e) {
                    System.err.println("Error when consuming message: " + e);
                } finally {
                    latch.countDown();
                }
            };

            new Thread(runnable).start();
        }

        latch.await();
    }

    private static void writeToDatabase(String message) {
        String[] messageParts = message.split(":");
        String id = messageParts[0];
        String likeOrDislike = messageParts[1] + "s";
        int count = AlbumReviewDao.updateReview(id, likeOrDislike);
        if (count < 1) {
            System.err.println("Error when writing to database");
        }
    }
}