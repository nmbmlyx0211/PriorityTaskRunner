package assignments;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Completed by Yinxia Li.
 */

public class PriorityTaskRunner extends AbstractPriorityTaskRunner implements Runnable {

    private final Object lock = new Object();
    private final CountDownLatch countDownLatch;
    private CountDownLatch executingCountDown;
    private AtomicBoolean stopped = new AtomicBoolean(false);
    private Task executingTask;
    private Map<Task, TaskNotificationListener> notificationMap = new HashMap<>();
    private Queue<Task> executingQueue = new PriorityQueue<>(1);

    // maintains a priority queue for tasks and executes them using task runner threads

    public PriorityTaskRunner(int nThreads) {
       super(nThreads);
       setRunnable(this);
       countDownLatch = new CountDownLatch(nThreads);
    }

    @Override
    public void run() {
       countDownLatch.countDown();
       while (! stopped.get()) {//keeping running until stop function is called
           Task currentTask;
           synchronized (lock) {
              currentTask = executingQueue.poll();//try to execute executeTask first
              if(currentTask == null) {
                  currentTask = taskQueue.poll();//run task from task queue based on the priority
              }
           }

           if (currentTask != null) {
        	   		// note for commence
	    	   	   notificationMap.get(currentTask).onTaskCommence(currentTask);
	           currentTask.run();
	           //note for completion
	           notificationMap.get(currentTask).onTaskCompletion(currentTask);
	           //executeTask only runs one task;
	           if (executingTask == currentTask) {
	       			executingCountDown.countDown();
	           }
           }
           // sleeping to release cpu
           try {
              Thread.sleep(10);
           } catch (InterruptedException e) {
              e.printStackTrace();
           }

       }
    }


    /**
     * Start all threads but wait until all have entered run() method before returning from the call
     */
    @Override
    public void start() {
       for (Thread taskTread : taskThreads) {
           taskTread.start();
       }
       //all thread enter run() before returning from the call
       try {
           countDownLatch.await();
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
    }

    /**
     * Schedule task to be executed by a task thread depending on priority anf task thread
     * availability.
     * 
     * @param t task to be added
     * @param listener listener instance to be notified of task commencement and completion events
     */
    public void scheduleTask(Task t, TaskNotificationListener listener) {
       synchronized (lock) {
           taskQueue.add(t);
       }
       notificationMap.put(t, listener);
    }

    /**
     * Execute the task using the task thread but wait for it to terminate
     * 
     * @param t task to execute
     */
    public synchronized void executeTask(Task t) {
       // only one task can be run by executeTask.
       // so we need track this task, make sure it's terminated.
       executingTask = t;
       executingQueue.add(t);
       //run only one task
       executingCountDown = new CountDownLatch(1);
       notificationMap.put(t, new SampleTaskNotificationListener());
       try {
           executingCountDown.await();// wait for count to be 0
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
       executingTask = null;//reset executingTask
    }

    /**
     * Stop the task threads and wait until they have been terminated (DO NOT use stop() method of
     * thread!!)
     */
    public void stop() {
       stopped.set(true);
       for (Thread thr : taskThreads) {
           try {
        	      thr.join();// wait for all threads to terminate before stopping
           } catch (InterruptedException e) {
              e.printStackTrace();
           }
       }
    }

    /**
     * @return number of tasks waiting for execution in the task queue
     */
    public int numTasks() {
       return taskQueue.size();
    }
}