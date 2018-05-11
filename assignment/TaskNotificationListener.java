package assignments;


public interface TaskNotificationListener {
    // called when the task t commences execution
    void onTaskCommence(Task t);
    // called when the task t completes execution
    void onTaskCompletion(Task t);
}
