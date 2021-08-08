package other;

import com.pinggao.sequence.SequenceRangeManager;
import com.pinggao.sequence.api.SequenceRange;

public class Demo {
    public static void main(String[] args) {
        //test();
        System.out.printf("123");
    }

    private static void test() {
        SequenceRange range = new SequenceRangeManager("demo1");
        int sleepTime = 100;
        int limit = 1000;

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < limit; i++) {
                executeTask(range, sleepTime);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < limit; i++) {
                executeTask(range, sleepTime);
            }
        });

        Thread t3 = new Thread(() -> {
            for (int i = 0; i < limit; i++) {
                executeTask(range, sleepTime);
            }
        });

        t1.start();
        t2.start();
        t3.start();
    }

    private static void executeTask(SequenceRange range, int sleepTime) {
        long l = range.nextValue();
        System.out.println(Thread.currentThread().getName() + ":" + l);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
