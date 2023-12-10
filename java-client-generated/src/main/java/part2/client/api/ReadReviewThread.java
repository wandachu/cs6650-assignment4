package part2.client.api;

import io.swagger.client.utils.ApiClient;
import io.swagger.client.utils.ApiException;
import io.swagger.client.utils.ApiResponse;
import io.swagger.client.utils.api.LikeApi;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static io.swagger.client.utils.Utils.produceToQueue;
import static part2.client.api.MultithreadApi.MAX_RETRIES;


public class ReadReviewThread implements Runnable {
    private volatile boolean exit = false;
    private String name;
    private ApiClient apiClient;
    private BlockingQueue<String[]> queue;
    private AtomicInteger successCountGet;
    private AtomicInteger failureCountGet;
    private AtomicInteger successCountPost; // to be used for generate randomID
    private static Random random = new Random();


    public ReadReviewThread(String name, ApiClient apiClient, BlockingQueue<String[]> queue,
                            AtomicInteger successCountGet, AtomicInteger failureCountGet, AtomicInteger successCountPost) {
        this.name = name;
        this.apiClient = apiClient;
        this.queue = queue;
        this.successCountGet = successCountGet;
        this.failureCountGet = failureCountGet;
        this.successCountPost = successCountPost;
    }

    @Override
    public void run() {
        System.out.printf("-------The ReadReview Thread %s has started.-------\n", this.name);
        int count = 0;
        // Each thread uses a separate instance of ApiClient
        LikeApi likeApi = new LikeApi(apiClient);

        while (!exit) {
            count++;
            int randomID = getRandomID();
            // send GET review request
            boolean isSuccessGet = sendApiGetRequest(likeApi, String.valueOf(randomID));
            if (isSuccessGet) {
                this.successCountGet.incrementAndGet();
            } else {
                System.out.println("There is one failure GET for the id " + randomID);
                this.failureCountGet.incrementAndGet();
            }
        }

        System.out.printf("-------The ReadReview Thread %s is stopped. It has sent %d requests------\n", this.name, count);
    }

    public void stop() {
        this.exit = true;
    }

    private int getRandomID() {
        int maxRange = this.successCountPost.get() / 4; // as we have 1 POST album with 3 POST review
        return random.nextInt(maxRange) + 1;
    }

    private boolean sendApiGetRequest(LikeApi likeApi, String albumID) {
        int retryCount = 0;
        boolean isNotSuccess = true;
        ApiResponse resp;
        while (isNotSuccess && retryCount < MAX_RETRIES) {
            try {
                long startTime = System.currentTimeMillis();
                resp = likeApi.getLikesWithHttpInfo(albumID);
                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;
                // write into a csv
                produceToQueue(this.queue, new String[]{
                        String.valueOf(startTime), // 0
                        "GET", // 1
                        String.valueOf(latency), // 2
                        String.valueOf(resp.getStatusCode())}); // 3
                isNotSuccess = false;
            } catch (ApiException ex) {
                retryCount++;
                System.out.printf("Get Exception. Retry %s %d for %s. Exception is %s\n", "GET", retryCount, Thread.currentThread().getName(), ex);
            }
        }
        return !isNotSuccess;
    }

}
