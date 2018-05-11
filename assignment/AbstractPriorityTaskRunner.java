package assignments;


import java.util.PriorityQueue;

public abstract class AbstractPriorityTaskRunner {

    protected final PriorityQueue<Task> taskQueue;
    protected final Thread [] taskThreads;

    /**
     * Constructor
     * @param nThreads number of threads to be created
     */
    protected AbstractPriorityTaskRunner(int nThreads) {
        taskQueue = new PriorityQueue<>();
        taskThreads = new Thread[nThreads];
    }

    /**
     * Set thread-safe runnable instance to be used by all threads
     * NOTE : Call this in your constructor of sub class
     * @param runnable Runnable instance to be used by each thread
     */
    protected void setRunnable(Runnable runnable) {
        for (int i=0; i < taskThreads.length; i++) {
            taskThreads[i] = new Thread(runnable, "Task thread " +i);
        }
    }

    /**
     * Start all threads but wait until all have entered run() method before returning from the call
     */
    public abstract void start();


    /**
     * Schedule task to be executed by a task thread depending on priority and task thread availability.
     * @param t task to be added
     * @param listener listener instance to be notified of task commencement and completion events
     */
    public abstract void scheduleTask(Task t, TaskNotificationListener listener);


    /**
     * Execute the task using the task thread but wait for it to terminate
     * @param t task to execute
     */
    public abstract void executeTask(Task t);

    /**
     * Stop the task threads and wait until they have been terminated (DO NOT use stop() method of thread!!)
     */
    public abstract void stop();

    /**
     * @return number of tasks waiting for execution in the task queue
     */
    public abstract int numTasks();
}
