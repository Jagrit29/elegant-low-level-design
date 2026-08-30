package lowleveldesign.systems.jobscheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class WorkerPool {
    private final int size;
    private final BlockingQueue<Job> queue;
    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running = true;

    public WorkerPool(int size) {
        this(size, new LinkedBlockingQueue<>());
    }

    public WorkerPool(int size, BlockingQueue<Job> queue) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        this.size = size;
        this.queue = queue;
    }

    public void start() {
        for (int i = 0; i < size; i++) {
            Thread worker = new Thread(() -> {
                while (running || !queue.isEmpty()) {
                    try {
                        Job job = queue.poll(500, TimeUnit.MILLISECONDS);
                        if (job == null) {
                            continue;
                        }
                        process(job);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "worker-" + i);
            workers.add(worker);
            worker.start();
        }
    }

    public void submit(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("job cannot be null");
        }
        queue.offer(job);
    }

    public void shutdown() {
        running = false;
        for (Thread worker : workers) {
            worker.interrupt();
        }
    }

    private void process(Job job) {
        System.out.println("Processing job: " + job.getName() + " (" + job.getDuration() + "s)");
    }
}
