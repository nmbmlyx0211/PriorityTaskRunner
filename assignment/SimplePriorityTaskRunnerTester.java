

package assignments;

import assignments.PriorityTaskRunner;
import assignments.SampleTask;
import assignments.SampleTaskNotificationListener;
import assignments.Task;

import java.util.*;
import java.util.concurrent.CountDownLatch;


public class SimplePriorityTaskRunnerTester {

    private int passCnt = 0;
    private int points = 0;
    private List<Integer> commencedTaskIds = new ArrayList<>();
    private List<Integer> finishedTaskIds = new ArrayList<>();
    private Set<Long> activeTaskRunnerThreadIds = new HashSet<>();

    private int getActiveThreadCount() {
        Set<Thread> keys = Thread.getAllStackTraces().keySet();
        return keys.size();
    }

    private void clear() {
        commencedTaskIds.clear();
        finishedTaskIds.clear();
        activeTaskRunnerThreadIds.clear();
    }

    private void addTaskId(List<Integer> targetIdList, int id) {
        synchronized (targetIdList) {
            targetIdList.add(id);
        }
    }

    private void addActiveTaskRunnerThreadId(long id) {
        synchronized (activeTaskRunnerThreadIds) {
            activeTaskRunnerThreadIds.add(id);
        }
    }

    private int getNumCommencedTasks() {
        synchronized (commencedTaskIds) {
            return commencedTaskIds.size();
        }
    }

    private int getActiveTaskRunnerThreadCnt() {
        synchronized (activeTaskRunnerThreadIds) {
            return activeTaskRunnerThreadIds.size();
        }
    }
    private boolean isActiveTaskRunnerThread(long threadId) {
        synchronized (activeTaskRunnerThreadIds) {
            return activeTaskRunnerThreadIds.contains(threadId);
        }
    }
    private boolean taskIdsSame(List<Integer> targetIdList, List<Integer> ids, boolean  inOrder) {
        synchronized(targetIdList) {
            if (targetIdList.size() != ids.size()) {
                return false;
            }
            if (!inOrder) {
                ids.sort(Integer::compareTo);
            }
            List<Integer> cList = new ArrayList<Integer>(targetIdList);
            if (!inOrder) {
                cList.sort(Integer::compareTo);
            }
            return cList.equals(ids);
        }
    }

    private class TestTask extends SampleTask {

        private final CountDownLatch commenceSignal;
        private final CountDownLatch blockSignal;
        private final boolean notifyCompletion;

        public TestTask(int priority, int id,
                        CountDownLatch commenceSignal,
                        CountDownLatch blockSignal) {
            this(priority, id, commenceSignal, blockSignal, false);
        }

        public TestTask(int priority, int id,
                        CountDownLatch commenceSignal,
                        CountDownLatch blockSignal,
                        boolean notifyCompletion) {
            super(priority, id);
            this.commenceSignal = commenceSignal;
            this.blockSignal = blockSignal;
            this.notifyCompletion = notifyCompletion;
        }

        @Override
        public void run() {
            try {
                commenceSignal.countDown();
                addActiveTaskRunnerThreadId(Thread.currentThread().getId());
                super.run();
                if (blockSignal != null) {
                    blockSignal.await();
                }
                if (notifyCompletion) {
                    addTaskId(finishedTaskIds, getId());
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private class TestTaskNotificationListener extends SampleTaskNotificationListener {
        private final CountDownLatch finishSignal;
        public TestTaskNotificationListener(CountDownLatch finishSignal) {
            this.finishSignal = finishSignal;
        }
        @Override
        public void onTaskCommence(Task t) {
            super.onTaskCommence(t);
            addTaskId(commencedTaskIds, t.getId());
        }
        @Override
        public void onTaskCompletion(Task t) {
            try {
                super.onTaskCompletion(t);
                addTaskId(finishedTaskIds, t.getId());
                finishSignal.countDown();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    // check if TaskRunner start/stop works correctly
    public void test1() {
        clear();
        boolean passed = true;
        try {
            int startActiveThreadCount = getActiveThreadCount();
            PriorityTaskRunner taskRunner = new PriorityTaskRunner(2);
            try {
                assert getActiveThreadCount() == startActiveThreadCount;
                points += 3;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            taskRunner.start();
            try {
                assert getActiveThreadCount() == startActiveThreadCount + 2;
                points += 4;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            taskRunner.stop();
            try {
                assert getActiveThreadCount() == startActiveThreadCount;
                points += 3;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            System.out.println("\nTest1 passed\n");
            passCnt += passed ? 1 : 0;
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("\nTest1 failed\n");
        }
    }

    // check if task runner can shut down while threads are running
    public void test2() {
        boolean passed = true;
        try {
            int startActiveThreadCount = getActiveThreadCount();
            PriorityTaskRunner taskRunner = new PriorityTaskRunner(2);
            taskRunner.start();
            CountDownLatch commenceSignal = new CountDownLatch(2);
            CountDownLatch finishSignal = new CountDownLatch(2);
            taskRunner.scheduleTask(new TestTask(15,100,commenceSignal,null), new TestTaskNotificationListener(finishSignal));
            taskRunner.scheduleTask(new TestTask(12,200,commenceSignal,null), new TestTaskNotificationListener(finishSignal));
            taskRunner.scheduleTask(new TestTask(5,300,commenceSignal,null), new TestTaskNotificationListener(finishSignal));
            taskRunner.scheduleTask(new TestTask(11,400,commenceSignal,null), new TestTaskNotificationListener(finishSignal));
            taskRunner.scheduleTask(new TestTask(14,500,commenceSignal,null), new TestTaskNotificationListener(finishSignal));
            taskRunner.scheduleTask(new TestTask(20,600,commenceSignal,null), new TestTaskNotificationListener(finishSignal));
            taskRunner.stop();
            try {
                assert getActiveThreadCount() == startActiveThreadCount;
                points += 2;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            System.out.println("\nTest2 passed\n");
            passCnt += passed ? 1 : 0;
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("\nTest2 failed\n");
        }
    }

    //check if addTask works correctly when tasks have been added prior to start
    public void test3() {
        boolean passed = true;
        clear();
        CountDownLatch commenceSignal = new CountDownLatch(2);
        CountDownLatch finishSignal = new CountDownLatch(2);
        PriorityTaskRunner taskRunner = null;
        int startActiveThreadCount = getActiveThreadCount();
        try {
            taskRunner = new PriorityTaskRunner(2);
            // add Task should add to the queue but not run it
            taskRunner.scheduleTask(new TestTask(15,100,commenceSignal,null), new TestTaskNotificationListener(finishSignal));
            taskRunner.scheduleTask(new TestTask(12,200,commenceSignal,null), new TestTaskNotificationListener(finishSignal));
            try {
                assert taskRunner.numTasks() == 2;
                points += 2;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            taskRunner.start();
            commenceSignal.await();
            try {
                assert taskIdsSame(commencedTaskIds, Arrays.asList(200, 100), false);
                points += 4;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            finishSignal.await();
            try {
                assert taskIdsSame(finishedTaskIds, Arrays.asList(200, 100), false);
                assert taskRunner.numTasks() == 0;
                points += 4;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            try {
                assert !isActiveTaskRunnerThread(Thread.currentThread().getId());
                points += 4;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            try {
                assert getActiveThreadCount() == startActiveThreadCount + 2;
                points += 2;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            System.out.println("\nTest3 passed\n");
            passCnt += passed ? 1 : 0;
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("\nTest3 failed\n");
        } finally {
            if (taskRunner != null) {
                taskRunner.stop();
            }
        }
    }

    //check if addTask works correctly when tasks are added after start
    public void test4() {
        boolean passed = true;
        clear();
        CountDownLatch commenceSignal = new CountDownLatch(3);
        CountDownLatch finishSignal = new CountDownLatch(3);
        PriorityTaskRunner taskRunner = null;
        int startActiveThreadCount = getActiveThreadCount();
        try {
            taskRunner = new PriorityTaskRunner(2);
            taskRunner.scheduleTask(new TestTask(15,100,commenceSignal,null), new TestTaskNotificationListener(finishSignal));
            taskRunner.scheduleTask(new TestTask(12,200,commenceSignal,null), new TestTaskNotificationListener(finishSignal));
            //assert taskRunner.numTasks() == 2;
            taskRunner.start();
            taskRunner.scheduleTask(new TestTask(10,300,commenceSignal,null), new TestTaskNotificationListener(finishSignal));
            commenceSignal.await();
            try {
                assert taskIdsSame(commencedTaskIds, Arrays.asList(200, 100, 300), true);
                assert taskRunner.numTasks() == 0;
                points += 5;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            //assert taskIdsSame(commencedTaskIds, Arrays.asList(200,100,300),false);
            finishSignal.await();
            try {
                assert taskIdsSame(finishedTaskIds, Arrays.asList(200, 100, 300), false);
                assert taskRunner.numTasks() == 0;
                points += 5;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            try {
                assert !isActiveTaskRunnerThread(Thread.currentThread().getId());
                points += 2;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            try {
                assert getActiveThreadCount() == startActiveThreadCount + 2;
                points += 2;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            System.out.println("\nTest4 passed\n");
            passCnt += passed ? 1 : 0;
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("\nTest4 failed\n");
        } finally {
            if (taskRunner != null) {
                taskRunner.stop();
            }
        }
    }

    //check if tasks are executed concurrently
    public void test5() {
        boolean passed = true;
        clear();
        CountDownLatch commenceSignal = new CountDownLatch(4);
        CountDownLatch finishSignal = new CountDownLatch(4);
        CountDownLatch [] blockSignals = new CountDownLatch[4];
        for (int i=0; i < blockSignals.length; i++) {
            blockSignals[i] = new CountDownLatch(1);
        }
        PriorityTaskRunner taskRunner = null;
        try {
            taskRunner = new PriorityTaskRunner(4);
            taskRunner.scheduleTask(new TestTask(15,100,commenceSignal,blockSignals[0]), new TestTaskNotificationListener(finishSignal));
            taskRunner.scheduleTask(new TestTask(12,200,commenceSignal,blockSignals[1]), new TestTaskNotificationListener(finishSignal));
            //assert taskRunner.numTasks() == 2;
            taskRunner.start();
            taskRunner.scheduleTask(new TestTask(10,300,commenceSignal,blockSignals[2]), new TestTaskNotificationListener(finishSignal));
            taskRunner.scheduleTask(new TestTask(14,400,commenceSignal,blockSignals[2]), new TestTaskNotificationListener(finishSignal));
            commenceSignal.await();
            //assert taskIdsSame(commencedTaskIds, Arrays.asList(200,100,300,400),true);
            try {
                assert taskIdsSame(commencedTaskIds, Arrays.asList(200, 100, 300, 400), false);
                assert taskRunner.numTasks() == 0;
                points += 3;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            try {
                assert getActiveTaskRunnerThreadCnt() == 4;
                points += 3;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            for (int i=0; i < blockSignals.length; i++) {
                blockSignals[i].countDown();
            }
            finishSignal.await();
            try {
                assert taskIdsSame(finishedTaskIds, Arrays.asList(200, 100, 300, 400), false);
                assert taskRunner.numTasks() == 0;
                points += 5;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            System.out.println("\nTest5 passed\n");
            passCnt += passed ? 1 : 0;
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("\nTest5 failed\n");
        } finally {
            if (taskRunner != null) {
                taskRunner.stop();
            }
        }
    }

    // Check if executeTask() works correctly in a blocked fashion
    public void test6() {
        boolean passed = true;
        clear();
        CountDownLatch commenceSignal1 = new CountDownLatch(1);
        CountDownLatch commenceSignal2 = new CountDownLatch(1);
        PriorityTaskRunner taskRunner = null;
        try {
            taskRunner = new PriorityTaskRunner(2);
            taskRunner.start();
            taskRunner.executeTask(new TestTask(15,100,commenceSignal1,null,true));
            commenceSignal1.await();
            try {
                assert taskRunner.numTasks() == 0;
                assert taskIdsSame(finishedTaskIds, Arrays.asList(100), true);
                points += 5;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            taskRunner.executeTask(new TestTask(11,200,commenceSignal2,null,true));
            commenceSignal2.await();
            try {
                assert taskRunner.numTasks() == 0;
                assert taskIdsSame(finishedTaskIds, Arrays.asList(100, 200), true);
                points += 5;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            System.out.println("\nTest6 passed\n");
            passCnt += passed ? 1 : 0;
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("\nTest6 failed\n");
        } finally {
            if (taskRunner != null) {
                taskRunner.stop();
            }
        }
    }

    // check if task passed by executeTask is executed by a task runner thread
    public void test7() {
        clear();
        CountDownLatch commenceSignal1 = new CountDownLatch(1);
        CountDownLatch commenceSignal2 = new CountDownLatch(1);
        PriorityTaskRunner taskRunner = null;
        try {
            taskRunner = new PriorityTaskRunner(2);
            taskRunner.start();
            TestTask task = new TestTask(15,100,commenceSignal1,null,true);
            taskRunner.executeTask(task);
            commenceSignal1.await();
            assert taskRunner.numTasks() == 0;
            assert taskIdsSame(finishedTaskIds, Arrays.asList(100), true);
            assert !isActiveTaskRunnerThread(Thread.currentThread().getId());
            points += 5;
            System.out.println("\nTest7 passed\n");
            passCnt++;
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("\nTest7 failed\n");
        } finally {
            if (taskRunner != null) {
                taskRunner.stop();
            }
        }
    }

    //check if added tasks are executed concurrently with takes executed via executeTask
    public void test8() {
        boolean passed = true;
        clear();
        CountDownLatch commenceSignal = new CountDownLatch(4);
        CountDownLatch finishSignal = new CountDownLatch(3);
        CountDownLatch [] blockSignals = new CountDownLatch[3];
        for (int i=0; i < blockSignals.length; i++) {
            blockSignals[i] = new CountDownLatch(1);
        }
        PriorityTaskRunner taskRunner = null;
        try {
            taskRunner = new PriorityTaskRunner(4);
            taskRunner.scheduleTask(new TestTask(15,100,commenceSignal,blockSignals[0]), new TestTaskNotificationListener(finishSignal));
            taskRunner.scheduleTask(new TestTask(12,200,commenceSignal,blockSignals[1]), new TestTaskNotificationListener(finishSignal));
            //assert taskRunner.numTasks() == 2;
            taskRunner.start();
            taskRunner.executeTask(new TestTask(11,300,commenceSignal,null,true));
            taskRunner.scheduleTask(new TestTask(10,400,commenceSignal,blockSignals[1]), new TestTaskNotificationListener(finishSignal));
            commenceSignal.await();
            //assert taskIdsSame(commencedTaskIds, Arrays.asList(200,100,400),true);
            try {
                assert taskIdsSame(commencedTaskIds, Arrays.asList(200, 100, 400), false);
                assert taskRunner.numTasks() == 0;
                points += 6;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            for (int i=0; i < blockSignals.length; i++) {
                blockSignals[i].countDown();
            }
            finishSignal.await();
            try {
                assert taskIdsSame(finishedTaskIds, Arrays.asList(200, 100, 300, 400), false);
                assert taskRunner.numTasks() == 0;
                points += 6;
            } catch (Throwable t) {
                t.printStackTrace();
                passed = false;
            }
            System.out.println("\nTest8 passed\n");
            passCnt += passed ? 1 : 0;
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("\nTest8 failed\n");
        } finally {
            if (taskRunner != null) {
                taskRunner.stop();
            }
        }
    }


    public void runTests() {
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
        test7();
        test8();
    }

    public static void main(String [] args) {
        SimplePriorityTaskRunnerTester tester = new SimplePriorityTaskRunnerTester();
        tester.runTests();
        System.out.println(String.format("%d tests passed",tester.passCnt));
        System.out.println(String.format("total test points=%d",tester.points));
    }
}


/*
package edu.njit.cs602.s2018.assignments;

import edu.njit.cs602.s2018.assignments.PriorityTaskRunner;
import edu.njit.cs602.s2018.assignments.SampleTask;
import edu.njit.cs602.s2018.assignments.SampleTaskNotificationListener;


public class SimplePriorityTaskRunnerTester {

    public static void main(String [] args) {

        PriorityTaskRunner taskRunner = new PriorityTaskRunner(3);
        taskRunner.scheduleTask(new SampleTask(15, 100), new SampleTaskNotificationListener());
        taskRunner.scheduleTask(new SampleTask(10, 200), new SampleTaskNotificationListener());
        taskRunner.scheduleTask(new SampleTask(5, 400), new SampleTaskNotificationListener());
        System.out.println("Number of tasks waiting="+taskRunner.numTasks());
        taskRunner.start();
        System.out.println("Number of tasks waiting="+taskRunner.numTasks());
        taskRunner.scheduleTask(new SampleTask(11, 500), new SampleTaskNotificationListener());
        taskRunner.executeTask(new SampleTask(21, 600));
        System.out.println(taskRunner.numTasks());
        taskRunner.stop();
    }
}
*/
