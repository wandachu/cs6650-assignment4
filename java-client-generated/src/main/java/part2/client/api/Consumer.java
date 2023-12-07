package part2.client.api;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class Consumer implements Runnable {
    private final BlockingQueue<String[]> queue;
    private final CSVWriter writer;

    public Consumer(BlockingQueue<String[]> queue) {
        this.queue = queue;
        // open the file to be ready for writing
        try {
            this.writer = new CSVWriter(new FileWriter(MultithreadApi.outputFileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        boolean active = true;
        while (active) {
            try {
                String[] s = queue.take();
                if (s.length == 0) { // empty string as the termination signal
                    active = false;
                } else {
                    writer.writeNext(s);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        // close the file and terminate consumer
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Finished writing results to CSV file. Consumer terminated.");
    }
}