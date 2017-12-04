import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;

public class LinkedListSingletonTest {

    @Test
    public void testTXLinkedListSingleton() {
        LinkedList LL = new LinkedList();
        Integer zero = 0;
        Assert.assertEquals(false, LL.containsKey(zero));
        Assert.assertEquals(null, LL.get(zero));
        String zeroS = "zero";
        String empty = "";
        Assert.assertEquals(null, LL.put(zero, zeroS));
        Assert.assertEquals(zeroS, LL.put(zero, empty));
        Assert.assertEquals(true, LL.containsKey(zero));
        Assert.assertEquals(empty, LL.get(zero));
        Integer one = 1;
        String oneS = "one";
        Integer two = 2;
        String twoS = "two";
        Assert.assertEquals(null, LL.put(one, oneS));
        Assert.assertEquals(null, LL.put(two, twoS));
        Assert.assertEquals(true, LL.containsKey(one));
        Assert.assertEquals(oneS, LL.get(one));
        Assert.assertEquals(true, LL.containsKey(two));
        Assert.assertEquals(twoS, LL.get(two));
        Assert.assertEquals(null, LL.remove(-1));
        Assert.assertEquals(twoS, LL.remove(two));
        Assert.assertEquals(null, LL.remove(two));
        Assert.assertEquals(empty, LL.remove(zero));
        Assert.assertEquals(null, LL.remove(zero));
        Assert.assertEquals(oneS, LL.remove(one));
        Assert.assertEquals(null, LL.remove(one));
        Assert.assertEquals(false, LL.containsKey(one));
        Assert.assertEquals(null, LL.put(one, oneS));
        Assert.assertEquals(oneS, LL.remove(one));
        Assert.assertEquals(null, LL.put(zero, zeroS));
        Assert.assertEquals(zeroS, LL.putIfAbsent(zero, zeroS));
        Assert.assertEquals(zeroS, LL.remove(zero));
        Assert.assertEquals(null, LL.putIfAbsent(zero, zeroS));
        Assert.assertEquals(true, LL.containsKey(zero));
        Assert.assertEquals(zeroS, LL.get(zero));
        Assert.assertEquals(zeroS, LL.putIfAbsent(zero, empty));
        Assert.assertEquals(zeroS, LL.get(zero));


        Assert.assertEquals(null, LL.putIfAbsent(10, 10));
        Assert.assertEquals(null, LL.putIfAbsent(12, 12));
        Assert.assertEquals(null, LL.putIfAbsent(9, 9));
        Assert.assertEquals(null, LL.putIfAbsent(11, 11));
        Assert.assertEquals(null, LL.putIfAbsent(8, 8));

    }

    @Test
    public void testLinkedListSingletonMultiThread() throws InterruptedException {
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
            String b_2 = threadName + "-b_2";
            String c = threadName + "-c";
            Integer k_a = 10 + threadName.charAt(1);
//			 System.out.println(threadName + ": " + k_a);
            Integer k_b = 20 + threadName.charAt(1);
//			 System.out.println(threadName + ": " + k_b);
            Integer k_c = 30 + threadName.charAt(1);
//			 System.out.println(threadName + ": " + k_c);

//			 System.out.println(threadName + ": put(k_a, a)");
            assertEquals(null, LL.put(k_a, a));
//			 System.out.println(threadName + ": put(k_a, a)");
            assertEquals(a, LL.put(k_a, a));
//			 System.out.println(threadName + ": containsKey(k_c)");
            assertEquals(false, LL.containsKey(k_c));
//			 System.out.println(threadName + ": put(k_c, c)");
            assertEquals(null, LL.put(k_c, c));
//			 System.out.println(threadName + ": containsKey(k_a)");
            assertEquals(true, LL.containsKey(k_a));
//			 System.out.println(threadName + ": containsKey(k_c)");
            assertEquals(true, LL.containsKey(k_c));
//			 System.out.println(threadName + ": containsKey(k_b)");
            assertEquals(false, LL.containsKey(k_b));
//			 System.out.println(threadName + ": put(k_b, b)");
            assertEquals(null, LL.put(k_b, b));
//			 System.out.println(threadName + ": containsKey(k_b)");
            assertEquals(true, LL.containsKey(k_b));
//			 System.out.println(threadName + ": containsKey(k_c)");
            assertEquals(true, LL.containsKey(k_c));
//			 System.out.println(threadName + ": put(k_c, c)");
            assertEquals(c, LL.put(k_c, c));
//			 System.out.println(threadName + ": remove(k_b)");
            assertEquals(b, LL.remove(k_b));
//			 System.out.println(threadName + ": remove(k_b)");
            assertEquals(null, LL.remove(k_b));
//			 System.out.println(threadName + ": remove(k_a)");
            assertEquals(a, LL.remove(k_a));
//			 System.out.println(threadName + ": remove(k_c)");
            assertEquals(c, LL.remove(k_c));
//			 System.out.println(threadName + ": remove(k_a)");
            assertEquals(null, LL.remove(k_a));
//			 System.out.println(threadName + ": remove(k_c)");
            assertEquals(null, LL.remove(k_c));
//			 System.out.println(threadName + ": containsKey(k_b)");
            assertEquals(false, LL.containsKey(k_b));
//			 System.out.println(threadName + ": containsKey(k_c)");
            assertEquals(false, LL.containsKey(k_c));
//			 System.out.println(threadName + ": put(k_a, a)");
            assertEquals(null, LL.put(k_a, a));
//			 System.out.println(threadName + ": put(k_b, b)");
            assertEquals(null, LL.put(k_b, b));
//			 System.out.println(threadName + ": containsKey(k_a)");
            assertEquals(true, LL.containsKey(k_a));
//			 System.out.println(threadName + ": put(k_b, b_2)");
            assertEquals(b, LL.put(k_b, b_2));
//			 System.out.println(threadName + ": get(k_a)");
            assertEquals(a, LL.get(k_a));
//			 System.out.println(threadName + ": containsKey(k_b)");
            assertEquals(true, LL.containsKey(k_b));
//			 System.out.println(threadName + ": get(k_b)");
            assertEquals(b_2, LL.get(k_b));
//			 System.out.println(threadName + ": remove(-1)");
            assertEquals(null, LL.remove(-1));
//			 System.out.println(threadName + ": remove(k_b)");
            assertEquals(b_2, LL.remove(k_b));
//			 System.out.println(threadName + ": remove(k_b)");
            assertEquals(null, LL.remove(k_b));
//			 System.out.println(threadName + ": put(k_c, c)");
            assertEquals(null, LL.put(k_c, c));
//			 System.out.println(threadName + ": remove(k_c)");
            assertEquals(c, LL.remove(k_c));
//			 System.out.println(threadName + ": remove(k_c)");
            assertEquals(null, LL.remove(k_c));
//			 System.out.println(threadName + ": remove(k_a)");
            assertEquals(a, LL.remove(k_a));
//			 System.out.println(threadName + ": remove(k_a)");
            assertEquals(null, LL.remove(k_a));
//			 System.out.println(threadName + ": containsKey(k_a)");
            assertEquals(false, LL.containsKey(k_a));
//			 System.out.println(threadName + ": put(k_a, a)");
            assertEquals(null, LL.put(k_a, a));
//			 System.out.println(threadName + ": remove(k_a)");
            assertEquals(a, LL.remove(k_a));

            assertEquals(null, LL.putIfAbsent(k_a, a));
            assertEquals(a, LL.putIfAbsent(k_a, ""));
            assertEquals(null, LL.putIfAbsent(k_c, c));
            assertEquals(c, LL.putIfAbsent(k_c, ""));
            assertEquals(null, LL.putIfAbsent(k_b, b));
            assertEquals(b, LL.putIfAbsent(k_b, ""));
            assertEquals(c, LL.remove(k_c));
            assertEquals(a, LL.remove(k_a));
            assertEquals(b, LL.remove(k_b));
//			 System.out.println(threadName + ": end");
        }
    }

}
