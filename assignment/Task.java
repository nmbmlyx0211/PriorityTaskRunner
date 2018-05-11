package assignments;


public interface Task extends Comparable<Task> {
    int getPriority();
    int getId();
    void run();
}
