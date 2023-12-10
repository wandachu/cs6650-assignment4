package part2.client.api;

import com.tdunning.math.stats.TDigest;
import io.swagger.client.utils.api.DefaultApi;
import io.swagger.client.utils.api.LikeApi;
import io.swagger.client.utils.model.ImageMetaData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import io.swagger.client.utils.ApiClient;
import io.swagger.client.utils.ApiException;
import io.swagger.client.utils.ApiResponse;
import io.swagger.client.utils.model.AlbumsProfile;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static io.swagger.client.utils.Utils.produceToQueue;

public class MultithreadApi {
    public static final int MAX_RETRIES = 5;
    private static final int NUM_READ_THREADS = 3;
    private static final int NUM_OF_PAIRS_OF_REQUESTS_PER_THREAD_ACTUAL_TEST = 100;
//    private static final String BASE = ":8080/assignment4_Web_exploded";
    private static final String BASE = ":8080/assignment4";
    public static final int NUM_OF_CALL_PER_THREAD_WARM_UP = 100;
    private final File image;
    private final AlbumsProfile profile;
    private final int threadGroupSize;
    private final int numThreadGroups;
    private final int delayInSeconds;
    private final String IPAddress;
    private final AtomicInteger successCountGet;
    private final AtomicInteger failureCountGet;
    private final AtomicInteger successCountPost;
    private final AtomicInteger failureCountPost;

    public static final String outputFileName = "output.csv";
    private static final String LIKE = "like";
    private static final String DISLIKE = "dislike";

    private final BlockingQueue<String[]> queue;

    public MultithreadApi(int threadGroupSize, int numThreadGroups, int delayInSeconds, String IPAddress,
                          AtomicInteger successCountGet, AtomicInteger failureCountGet,
                          AtomicInteger successCountPost, AtomicInteger failureCountPost) {
        this.threadGroupSize = threadGroupSize;
        this.numThreadGroups = numThreadGroups;
        this.delayInSeconds = delayInSeconds;
        this.IPAddress = IPAddress;
        this.successCountGet = successCountGet;
        this.failureCountGet = failureCountGet;
        this.successCountPost = successCountPost;
        this.failureCountPost = failureCountPost;
        image = new File("nmtb.png");
        profile = new AlbumsProfile();
        profile.setArtist("Jay Chow");
        profile.setYear("2002");
        profile.setTitle("Fantasy");
        queue = new LinkedBlockingQueue<>();
    }

    public void startThreads() {
        runWarmUpTest();

        // Start consumer thread
        Consumer consumer = new Consumer(queue);
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();

        // start actual test
        long startTime = System.currentTimeMillis();
        int totalNumThreads = this.threadGroupSize * this.numThreadGroups;
        CountDownLatch completed = new CountDownLatch(totalNumThreads);
        CountDownLatch groupOneCompleted = new CountDownLatch(this.threadGroupSize);

        for (int group = 0; group < this.numThreadGroups; group++) {
            // within each group, start threadGroupSize of threads
            for (int i = 0; i < this.threadGroupSize; i++) {
                // each thread call 100 POST API
                // start the first group using the groupOneCompleted countdown
                Thread thread = createThreadToSendRequestsWithCountdown(completed, NUM_OF_PAIRS_OF_REQUESTS_PER_THREAD_ACTUAL_TEST, true, i == 0, groupOneCompleted);
                thread.start();
            }
            // once all the threads are running, wait for a period of delay seconds
            try {
                Thread.sleep(delayInSeconds * 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // After the first group is completed, start the GET threads
        try {
            groupOneCompleted.await();
            float wallTimeFirstGroup = (System.currentTimeMillis() - startTime) / 1000f;
            System.out.printf("First thread group has completed...It took %fs. Now starting the %d GET threads.......\n", wallTimeFirstGroup, NUM_READ_THREADS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // Start the GET threads
        ApiClient apiClient = new ApiClient();
        String url = "http://" + this.IPAddress + BASE;
        apiClient.setBasePath(url);
        ReadReviewThread[] readReviewThreads = new ReadReviewThread[NUM_READ_THREADS];
        long startTimeRead = System.currentTimeMillis();
        for (int i = 0; i < NUM_READ_THREADS; i++) {
            ReadReviewThread readReviewThread = new ReadReviewThread("ReadReviewThread" + i,
                    apiClient, this.queue, successCountGet, failureCountGet,  successCountPost);
            Thread newReadReviewThread = new Thread(readReviewThread);
            readReviewThreads[i] = readReviewThread;
            newReadReviewThread.start();
        }

        // When all POST groups have completed, stop the three GET threads if they are still running
        try {
            completed.await();
            // stop the 3 readReviewThreads
            for (ReadReviewThread readReviewThread : readReviewThreads) {
                readReviewThread.stop();
            }
            // produce the terminal signal to the queue
            queue.put(new String[]{});
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long endTime = System.currentTimeMillis();

        // Let the READ threads to fully stop updating the atomicInteger variables
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // output wall time and throughput result
        float wallTime = (endTime - startTime) / 1000f;
        System.out.printf("Number of total successful requests is %d (including /review and /albums READ and POST requests)\n",
                successCountGet.get() + successCountPost.get());
        System.out.printf("Number of total failed requests is %d\n", this.failureCountPost.get());
        System.out.printf("*** Total Wall time: %fs ***\n", wallTime);
        float throughputPost = this.successCountPost.get() / wallTime;
        System.out.printf("*** POST Throughput: %f requests per second ***\n", throughputPost);
        float wallTimeRead = (endTime - startTimeRead) / 1000f;
        float throughputReadOnly = this.successCountGet.get() / wallTimeRead;
        System.out.printf("*** GET wall time: %fs ***\n", wallTimeRead);
        System.out.printf("*** GET Throughput: %f requests per second ***\n", throughputReadOnly);

        // do data analysis
        try {
            consumerThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        outputStatistics();
    }

    private void runWarmUpTest() {
        // use 10 threads to do warm-up test
        long startWarmup = System.currentTimeMillis();
        int initialThread = 10;
        CountDownLatch completedInitial = new CountDownLatch(initialThread);
        for (int i = 0; i < initialThread; i++) {
            // create 10 threads, each calls the POST API followed by the GET API 100 times
            Thread thread = createThreadToSendRequestsWithCountdown(completedInitial, NUM_OF_CALL_PER_THREAD_WARM_UP,
                    false, false, null);
            thread.start();
        }
        try {
            completedInitial.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long endWarmup = System.currentTimeMillis();
        System.out.println("Warm up takes " + (endWarmup - startWarmup) + " milliseconds");
    }

    private void outputStatistics() {
        float minResponseTimeGET = Float.MAX_VALUE;
        float minResponseTimePOST = Float.MAX_VALUE;
        float maxResponseTimeGET = 0;
        float maxResponseTimePOST = 0;
        float totalTimeGET = 0;
        float totalTimePOST = 0;


        final int columnIndexType = 1;
        final int columnIndexLatency = 2;
        TDigest tDigestGET = TDigest.createDigest(100);
        TDigest tDigestPOST = TDigest.createDigest(100);

        // calculate min, max, and mean
        try (FileReader reader = new FileReader(outputFileName);
             CSVParser csvParser = CSVFormat.DEFAULT.parse(reader)) {
            for (CSVRecord csvRecord : csvParser) {
                String requestType = csvRecord.get(columnIndexType);
                String latencyValue = csvRecord.get(columnIndexLatency);
                if ("GET".equals(requestType)) {
                    float latencyFloat = Float.parseFloat(latencyValue);
                    totalTimeGET += latencyFloat;
                    minResponseTimeGET = Math.min(minResponseTimeGET, latencyFloat);
                    maxResponseTimeGET = Math.max(maxResponseTimeGET, latencyFloat);
                    tDigestGET.add(latencyFloat);
                } else if ("POST".equals(requestType)) {
                    float latencyFloat = Float.parseFloat(latencyValue);
                    totalTimePOST += latencyFloat;
                    minResponseTimePOST = Math.min(minResponseTimePOST, latencyFloat);
                    maxResponseTimePOST = Math.max(maxResponseTimePOST, latencyFloat);
                    tDigestPOST.add(latencyFloat);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        outputResults(tDigestGET, tDigestPOST, totalTimeGET, totalTimePOST,
                minResponseTimeGET, minResponseTimePOST, maxResponseTimeGET, maxResponseTimePOST);
    }

    private void outputResults(TDigest tDigestGET, TDigest tDigestPOST,
                               float totalTimeGET, float totalTimePOST, float minResponseTimeGET,
                               float minResponseTimePOST, float maxResponseTimeGET, float maxResponseTimePOST) {
        System.out.printf("\nPrinting the analysis results for the %d successful and %d failed GET requests...\n",
                this.successCountGet.get(), this.failureCountGet.get());
        System.out.println("------------------------------------------------------");
        System.out.printf("p99 response time for GET requests: %f milliseconds\n", tDigestGET.quantile(99 / 100.0));
        System.out.printf("Median response time for GET requests: %f milliseconds\n", tDigestGET.quantile(50 / 100.0));
        System.out.printf("Mean response time for GET requests: %f millisecond\n", totalTimeGET / this.successCountGet.get());
        System.out.printf("Minimum response time for GET requests: %f millisecond\n", minResponseTimeGET);
        System.out.printf("Maximum response time for GET requests: %f millisecond\n", maxResponseTimeGET);
        System.out.println();
        System.out.printf("\nPrinting the analysis results for the %d successful and %d failed POST requests...\n",
                this.successCountPost.get(), this.failureCountPost.get());
        System.out.println("------------------------------------------------------");
        System.out.printf("p99 response time for POST requests: %f milliseconds\n", tDigestPOST.quantile(99 / 100.0));
        System.out.printf("Median response time for POST requests: %f milliseconds\n", tDigestPOST.quantile(50 / 100.0));
        System.out.printf("Mean response time for POST requests: %f millisecond\n", totalTimePOST / this.successCountPost.get());
        System.out.printf("Minimum response time for POST requests: %f millisecond\n", minResponseTimePOST);
        System.out.printf("Maximum response time for POST requests: %f millisecond\n", maxResponseTimePOST);
    }

    private Thread createThreadToSendRequestsWithCountdown(
            CountDownLatch completed, int numOfCallPerThread, boolean isActualTest, boolean isFirstGroup, CountDownLatch firstGroupCompleted) {
        ApiClient apiClient = new ApiClient();
        String url = "http://" + this.IPAddress + BASE;
        apiClient.setBasePath(url);
        return new Thread(() -> {
            int successCountPOST = 0;
            int failureCountPOST = 0;
            // Each thread uses a separate instance of ApiClient
            DefaultApi defaultApi = new DefaultApi(apiClient);
            LikeApi likeApi = new LikeApi(apiClient);
            for (int i = 0; i < numOfCallPerThread; i++) {
                // send POST album request
                String postRequestResultAlbumID = sendApiPostRequest(defaultApi, isActualTest);
                boolean isSuccessPost = postRequestResultAlbumID != null;
                if (isSuccessPost) {
                    // send POST review request
                    // TODO: to uncomment
                    sendReviewPostRequest(likeApi, LIKE, postRequestResultAlbumID);
                    sendReviewPostRequest(likeApi, LIKE, postRequestResultAlbumID);
                    sendReviewPostRequest(likeApi, DISLIKE, postRequestResultAlbumID);
                }
                if (isActualTest) { // only count the actual tests, not warm up
                    if (isSuccessPost) {
                        successCountPOST += 4; // 1 albums post and 3 review posts
                    } else {
                        failureCountPOST += 4;
                    }
                }
            }
            if (isActualTest) { // only count the actual tests, not warm up
                this.successCountPost.addAndGet(successCountPOST);
                this.failureCountPost.addAndGet(failureCountPOST);
            }
            completed.countDown();
            if (isFirstGroup) {
                firstGroupCompleted.countDown();
            }
        });
    }

    private void sendReviewPostRequest(LikeApi likeApi, String likeOrNot, String albumID) {
        int retryCount = 0;
        boolean isNotSuccess = true;
        while (isNotSuccess && retryCount < MAX_RETRIES) {
            try {
                likeApi.review(likeOrNot, albumID);
                isNotSuccess = false;
            } catch (ApiException ex) {
                retryCount++;
                System.out.printf("Get Exception. Retry %s %d for %s. Exception is %s\n", "POST", retryCount, Thread.currentThread().getName(), ex);
            }
        }
    }

    /**
     * Send a POST request and produce to the Queue if this is not a warm-up test.
     * Will retry at most 5 times if the request is not successful.
     * @param defaultApi
     * @param isActualTest
     * @return the generated AlbumID if the request is successful. Otherwise, return null.
     */
    private String sendApiPostRequest(DefaultApi defaultApi, boolean isActualTest) {
        int retryCount = 0;
        boolean isNotSuccess = true;
        ApiResponse<ImageMetaData> resp;
        String requestResult = null;
        while (isNotSuccess && retryCount < MAX_RETRIES) {
            try {
                long startTime = System.currentTimeMillis();
                resp = defaultApi.newAlbumWithHttpInfo(image, profile);
                if (isActualTest) { // only write to CSV for the actual tests, not warm-up test
                    long endTime = System.currentTimeMillis();
                    long latency = endTime - startTime;
                    // write into a csv
                    produceToQueue(this.queue, new String[]{
                            String.valueOf(startTime), // 0
                            "POST", // 1
                            String.valueOf(latency), // 2
                            String.valueOf(resp.getStatusCode())}); // 3
                }
                requestResult = resp.getData().getAlbumID();
                isNotSuccess = false;
            } catch (ApiException ex) {
                retryCount++;
                System.out.printf("Get Exception. Retry %s %d for %s. Exception is %s\n", "POST", retryCount, Thread.currentThread().getName(), ex);
            }
        }
        return requestResult; // if not successful, this is null
    }


}
