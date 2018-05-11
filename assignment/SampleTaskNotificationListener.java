package assignments;


public class SampleTaskNotificationListener implements TaskNotificationListener {

    // called when the task t commences execution
    @Override
    public void onTaskCommence(Task t) {
        System.out.println("Task with id " + t.getId() + " has commenced");
    }

    // called when the task t completes execution
    @Override
    public void onTaskCompletion(Task t) {
        System.out.println("Task with id " + t.getId() + " has completed");
    }
}

