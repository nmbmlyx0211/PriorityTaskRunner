package assignments;


public class SampleTask implements Task {
    private final int priority;
    private final int id;

    public SampleTask(int priority, int id) {
        this.priority = priority;
        this.id = id;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void run() {
        int size = (int) Math.round(1000 * Math.random());
        int [] arr = new int[size];
        for (int i=0; i < arr.length; i++) {
            arr[i] = (int) Math.round(50 * Math.random());
        }
        int max = arr[0];
        for (int i=1; i < arr.length; i++) {
            max = max < arr[i] ? arr[i] : max;
        }
        System.out.println(Thread.currentThread().getName()+": found max ="+max);
    }

    @Override
    public int compareTo(Task o) {
        return Integer.compare(this.priority, o.getPriority());
    }
}
