import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class QueueLinkedListTests {

    private static final int ITERATIONS = 100;
    private static final int RANGE = 10000;
    private static final int THREADS = 10;

    @Test
    public void testSingleThreadTX() throws TXLibExceptions.AbortException, TXLibExceptions.QueueIsEmptyException {

        Queue Q = new Queue();
        LinkedList LL = new LinkedList();

        Assert.assertEquals(true, Q.isEmpty());
        Q.enqueue(1);
        Q.enqueue(2);
        Q.enqueue(3);
        Q.enqueue(13);
        Q.enqueue(12);
        Q.enqueue(11);
        Assert.assertEquals(false, Q.isEmpty());

        Assert.assertEquals(null, LL.put(1, Q.dequeue()));
        Assert.assertEquals(true, LL.containsKey(1));
        Assert.assertEquals(1, LL.get(1));
        Assert.assertEquals(null, LL.put(2, Q.dequeue()));
        Assert.assertEquals(null, LL.put(3, Q.dequeue()));
        Assert.assertEquals(3, LL.get(3));
        Assert.assertEquals(2, LL.get(2));

        Q.enqueue(10);
        Assert.assertEquals(null, LL.put(13, Q.dequeue()));
        Assert.assertEquals(null, LL.put(12, Q.dequeue()));
        Assert.assertEquals(null, LL.put(11, Q.dequeue()));
        Assert.assertEquals(null, LL.put(10, Q.dequeue()));

        try {
            Assert.assertEquals(null, LL.put(0, Q.dequeue()));
            Assert.fail("did not throw QueueIsEmptyException");
        } catch (TXLibExceptions.QueueIsEmptyException ignored) {
        }

        Assert.assertEquals(10, LL.get(10));
        Assert.assertEquals(11, LL.get(11));
        Assert.assertEquals(13, LL.get(13));
        Assert.assertEquals(12, LL.get(12));


        while (true) {
            try {
                try {
                    TX.TXbegin();
                    Assert.assertEquals(true, Q.isEmpty());
                    Q.enqueue(LL.remove(13));
                    Q.enqueue(LL.remove(12));
                    Q.enqueue(LL.remove(11));
                    Q.enqueue(LL.remove(10));
                    Q.enqueue(LL.remove(1));
                    Q.enqueue(LL.remove(2));
                    Q.enqueue(LL.remove(3));
                    Assert.assertEquals(false, LL.containsKey(13));
                } finally {
                    TX.TXend();
                }
            } catch (TXLibExceptions.AbortException exp) {
                continue;
            }
            break;
        }

        Assert.assertEquals(false, Q.isEmpty());
        Q.dequeue();
        Q.dequeue();
        Q.dequeue();
        Q.dequeue();
        Q.dequeue();
        Q.dequeue();
        Q.dequeue();

        try {
            Q.dequeue();
            Assert.fail("did not throw QueueIsEmptyException");
        } catch (TXLibExceptions.QueueIsEmptyException ignored) {
        }
    }

    @Test
    public void testMultiThreadSimpleTransaction() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        LinkedList LL = new LinkedList();
        Queue Q = new Queue();
        ArrayList<Thread> threads = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            threads.add(new Thread(new RunSimpleTransaction(latch, LL, Q)));
        }
        for (int i = 0; i < THREADS; i++) {
            threads.get(i).start();
        }
        latch.countDown();
        for (int i = 0; i < THREADS; i++) {
            threads.get(i).join();
        }
    }

    class RunSimpleTransaction implements Runnable {

        LinkedList LL;
        Queue Q;
        CountDownLatch latch;

        RunSimpleTransaction(CountDownLatch l, LinkedList ll, Queue q) {
            latch = l;
            LL = ll;
            Q = q;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println("InterruptedException");
            }
            for (int i = 0; i < ITERATIONS; i++) {

                Random rand = new Random();
                int n = rand.nextInt((RANGE) + 1);
                // rand.nextInt((max - min) + 1) + min

                while (true) {
                    try {
                        try {
                            TX.TXbegin();
                            LL.put(n, n);
                            LL.put(n + 3, n + 3);
                            LL.put(n + 6, n + 6);
                            LL.put(n + 9, n + 9);
                            LL.put(n + 12, n + 12);
                            LL.put(n + 15, n + 15);
                            LL.put(n + 7, n + 7);
                            LL.put(n + 11, n + 11);
                            assertEquals(true, LL.containsKey(n + 12));
                            assertEquals(n + 12, LL.get(n + 12));
                            assertEquals(true, LL.containsKey(n + 15));
                            assertEquals(n + 15, LL.get(n + 15));
                            assertEquals(n, LL.get(n));
                            assertEquals(n + 11, LL.get(n + 11));
                            assertEquals(n + 7, LL.get(n + 7));
                            Q.enqueue(LL.remove(n + 7));
                            Q.enqueue(LL.remove(n + 9));
                            Q.enqueue(LL.remove(n + 11));
                            assertEquals(false, Q.isEmpty());
                            assertEquals(null, LL.put(n + 7, Q.dequeue()));
                            assertEquals(null, LL.put(n + 9, Q.dequeue()));
                            assertEquals(false, Q.isEmpty());
                            Q.enqueue(LL.remove(n + 15));
                            assertEquals(null, LL.put(n + 11, Q.dequeue()));
                            assertEquals(null, LL.put(n + 15, Q.dequeue()));
                            assertEquals(true, Q.isEmpty());
                            assertEquals(true, LL.containsKey(n + 12));
                            assertEquals(n + 12, LL.get(n + 12));
                            assertEquals(true, LL.containsKey(n + 15));
                            assertEquals(n + 15, LL.get(n + 15));
                            assertEquals(n, LL.get(n));
                            assertEquals(n + 11, LL.get(n + 11));
                            assertEquals(n + 7, LL.get(n + 7));
                        } catch (TXLibExceptions.QueueIsEmptyException exp) {
                            fail("Queue should not be empty");
                        } finally {
                            TX.TXend();
                        }
                    } catch (TXLibExceptions.AbortException exp) {
                        continue;
                    }
                    break;
                }
            }
        }
    }

}
