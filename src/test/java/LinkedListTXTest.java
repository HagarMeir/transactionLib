import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;

public class LinkedListTXTest {

    @Test
    public void testLinkedListMultiThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        LinkedList LL = new LinkedList();
        Thread T1 = new Thread(new Run("T1", latch, LL));
        Thread T2 = new Thread(new Run("T2", latch, LL));
        Thread T3 = new Thread(new Run("T3", latch, LL));
        T1.start();
        T2.start();
        T3.start();
        latch.countDown();
        T1.join();
        T2.join();
        T3.join();
    }

    class Run implements Runnable {

        LinkedList LL;
        String threadName;
        CountDownLatch latch;

        Run(String name, CountDownLatch l, LinkedList ll) {
            threadName = name;
            latch = l;
            LL = ll;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println(threadName + ": InterruptedException");
            }
            String a = threadName + "-a";
            String b = threadName + "-b";
            String c = threadName + "-c";
            String empty = "";
            Integer k_a = 10 + threadName.charAt(1);
            Integer k_b = 20 + threadName.charAt(1);
            Integer k_c = 30 + threadName.charAt(1);

            while (true) {
                try {
                    try {
                        TX.TXbegin();
                        assertEquals(false, LL.containsKey(k_c));
                        assertEquals(null, LL.get(k_c));
                        assertEquals(null, LL.put(k_c, c));
                        assertEquals(true, LL.containsKey(k_c));
                        assertEquals(false, LL.containsKey(k_a));
                        assertEquals(false, LL.containsKey(k_b));
                        assertEquals(null, LL.get(k_b));
                        assertEquals(null, LL.put(k_a, a));
                        assertEquals(null, LL.put(k_b, b));
                        assertEquals(true, LL.containsKey(k_b));
                        assertEquals(true, LL.containsKey(k_a));
                        assertEquals(a, LL.put(k_a, a));
                        assertEquals(b, LL.put(k_b, b));
                        assertEquals(c, LL.get(k_c));
                        assertEquals(a, LL.get(k_a));
                        assertEquals(b, LL.get(k_b));
                        assertEquals(null, LL.remove(-1));
                        assertEquals(b, LL.remove(k_b));
                        assertEquals(null, LL.remove(k_b));
                        assertEquals(false, LL.containsKey(k_b));
                        assertEquals(a, LL.remove(k_a));
                        assertEquals(c, LL.remove(k_c));
                        assertEquals(null, LL.remove(k_a));
                        assertEquals(null, LL.get(k_c));
                        assertEquals(null, LL.put(k_b, b));
                        assertEquals(b, LL.get(k_b));
                        assertEquals(b, LL.put(k_b, empty));
                        assertEquals(empty, LL.get(k_b));
                        assertEquals(null, LL.put(k_c, c));
                        assertEquals(c, LL.get(k_c));
                        assertEquals(c, LL.put(k_c, empty));
                        assertEquals(empty, LL.get(k_c));
                        assertEquals(empty, LL.remove(k_b));
                        assertEquals(empty, LL.remove(k_c));
                        assertEquals(null, LL.putIfAbsent(k_c, c));
                        assertEquals(c, LL.putIfAbsent(k_c, empty));
                        assertEquals(c, LL.putIfAbsent(k_c, empty));
                        assertEquals(c, LL.get(k_c));
                        assertEquals(null, LL.putIfAbsent(k_b, b));
                        assertEquals(b, LL.putIfAbsent(k_b, empty));
                        assertEquals(b, LL.putIfAbsent(k_b, empty));
                        assertEquals(b, LL.get(k_b));
                        assertEquals(c, LL.remove(k_c));
                        assertEquals(null, LL.putIfAbsent(k_c, c));
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
