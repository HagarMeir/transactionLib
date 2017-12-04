import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;

public class MultiLinkedListTest {

    private static final int ITERATIONS = 100;
    private static final int RANGE = 10000;
    private static final int THREADS = 10;

    @Test
    public void testMultiTransactions() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        LinkedList LL1 = new LinkedList();
        LinkedList LL2 = new LinkedList();
        ConcurrentSkipListMap<Long, ArrayList<Pair>> map = new ConcurrentSkipListMap<>();
        ArrayList<Thread> threads = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            threads.add(new Thread(new RunTransactionsMultiLL(latch, LL1, LL2, map)));
        }
        for (int i = 0; i < THREADS; i++) {
            threads.get(i).start();
        }
        latch.countDown();
        for (int i = 0; i < THREADS; i++) {
            threads.get(i).join();
        }

        SortedSet<Integer> set1 = new TreeSet<>();
        SortedSet<Integer> set2 = new TreeSet<>();
        // go over map in ascending order of keys (so by version)
        for (Entry<Long, ArrayList<Pair>> entry : map.entrySet()) {
            ArrayList<Pair> ops = entry.getValue();
            for (Pair p : ops) {
                if (p.first) {
                    set1.add(p.second);
                    set2.remove(p.second);
                } else {
                    set1.remove(p.second);
                    set2.add(p.second);
                }
            }
        }

        LNode node = LL1.head.next;
        for (Integer k : set1) {
            Assert.assertNotNull(node);
            Assert.assertEquals(k, node.key);
            node = node.next;
        }
        Assert.assertNull(node);

        node = LL2.head.next;
        for (Integer k : set2) {
            Assert.assertNotNull(node);
            Assert.assertEquals(k, node.key);
            node = node.next;
        }
        Assert.assertNull(node);
    }

    @Test
    public void testMultiThreadSimpleTransactionMultiLL() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        LinkedList LL1 = new LinkedList();
        LinkedList LL2 = new LinkedList();
        ArrayList<Thread> threads = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            threads.add(new Thread(new RunSimpleTransactionMultiLL(latch, LL1, LL2)));
        }
        for (int i = 0; i < THREADS; i++) {
            threads.get(i).start();
        }
        latch.countDown();
        for (int i = 0; i < THREADS; i++) {
            threads.get(i).join();
        }
    }

    @Test
    public void testSingleThreadMultiLinkedList() {
        LinkedList LL1 = new LinkedList();
        LinkedList LL2 = new LinkedList();
        while (true) {
            try {
                try {
                    TX.TXbegin();
                    Assert.assertEquals(null, LL1.put(5, "5"));
                    Assert.assertEquals(null, LL2.put(4, "4"));
                    Assert.assertEquals(true, LL1.containsKey(5));
                    Assert.assertEquals(false, LL1.containsKey(3));
                    Assert.assertEquals(null, LL1.remove(3));
                    Assert.assertEquals("5", LL1.remove(5));
                    Assert.assertEquals(false, LL1.containsKey(5));
                    Assert.assertEquals(null, LL1.put(3, "3"));
                    Assert.assertEquals("3", LL1.put(3, "3"));
                    Assert.assertEquals(null, LL1.put(9, "9"));
                    Assert.assertEquals(null, LL2.put(6, "6"));
                    Assert.assertEquals(true, LL1.containsKey(3));
                    Assert.assertEquals(true, LL2.containsKey(6));
                    Assert.assertEquals("3", LL1.remove(3));
                    Assert.assertEquals(false, LL1.containsKey(3));
                    Assert.assertEquals(null, LL1.put(-1, "-1"));
                    Assert.assertEquals(true, LL1.containsKey(-1));
                    Assert.assertEquals(null, LL2.put(2, "2"));
                    Assert.assertEquals(true, LL2.containsKey(4));
                    Assert.assertEquals(true, LL2.containsKey(2));
                    Assert.assertEquals("-1", LL1.remove(-1));
                    Assert.assertEquals(false, LL1.containsKey(-1));
                    Assert.assertEquals("2", LL2.remove(2));
                    Assert.assertEquals(false, LL2.containsKey(2));
                    Assert.assertEquals(true, LL2.containsKey(4));
                    Assert.assertEquals(null, LL2.put(8, "8"));
                    Assert.assertEquals(null, LL1.put(20, "20"));
                    Assert.assertEquals(null, LL2.put(20, "20"));
                    Assert.assertEquals(true, LL2.containsKey(20));
                    Assert.assertEquals(true, LL1.containsKey(20));
                    Assert.assertEquals(null, LL1.put(21, "21"));
                    Assert.assertEquals(null, LL2.put(21, "21"));
                    Assert.assertEquals(true, LL2.containsKey(21));
                    Assert.assertEquals(true, LL1.containsKey(21));
                    Assert.assertEquals("20", LL2.remove(20));
                } finally {
                    TX.TXend();
                }
            } catch (TXLibExceptions.AbortException exp) {
                continue;
            }
            break;
        }

        Assert.assertEquals(true, LL2.containsKey(8));
        Assert.assertEquals("8", LL2.remove(8));
        Assert.assertEquals(false, LL2.containsKey(8));
        Assert.assertEquals(null, LL2.put(12, "12"));
        Assert.assertEquals("9", LL1.remove(9));
        Assert.assertEquals(true, LL2.containsKey(4));
        Assert.assertEquals(false, LL2.containsKey(2));
        Assert.assertEquals(null, LL1.put(13, "13"));
        Assert.assertEquals(null, LL1.put(11, "11"));
        Assert.assertEquals("21", LL1.remove(21));

        while (true) {
            try {
                try {
                    TX.TXbegin();
                    Assert.assertEquals(true, LL2.containsKey(4));
                    Assert.assertEquals("4", LL2.remove(4));
                    Assert.assertEquals(null, LL1.remove(9));
                    Assert.assertEquals("6", LL2.remove(6));
                    Assert.assertEquals(true, LL1.containsKey(13));
                    Assert.assertEquals(true, LL1.containsKey(11));
                    Assert.assertEquals("11", LL1.remove(11));
                    Assert.assertEquals("13", LL1.remove(13));
                    Assert.assertEquals("12", LL2.put(12, "12"));
                    Assert.assertEquals("20", LL1.remove(20));
                    Assert.assertEquals("21", LL2.remove(21));
                } finally {
                    TX.TXend();
                }
            } catch (TXLibExceptions.AbortException exp) {
                continue;
            }
            break;
        }

    }

    @Test
    public void testMultiLinkedListSingletonMultiThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        LinkedList LL1 = new LinkedList();
        LinkedList LL2 = new LinkedList();
        Thread T1 = new Thread(new RunMultiLinkedListLMultiSingleton("T1", latch, LL1, LL2));
        Thread T2 = new Thread(new RunMultiLinkedListLMultiSingleton("T2", latch, LL1, LL2));
        Thread T3 = new Thread(new RunMultiLinkedListLMultiSingleton("T3", latch, LL1, LL2));
        T1.start();
        T2.start();
        T3.start();
        latch.countDown();
        T1.join();
        T2.join();
        T3.join();
    }

    class Pair {
        protected boolean first;
        protected int second;

        Pair(boolean first, int second) {
            this.first = first;
            this.second = second;
        }

    }

    class RunTransactionsMultiLL implements Runnable {

        ConcurrentSkipListMap<Long, ArrayList<Pair>> map;
        LinkedList LL1;
        LinkedList LL2;
        CountDownLatch latch;

        RunTransactionsMultiLL(CountDownLatch l, LinkedList ll1, LinkedList ll2, ConcurrentSkipListMap<Long, ArrayList<Pair>> m) {
            latch = l;
            LL1 = ll1;
            LL2 = ll2;
            map = m;
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
                int numOps = rand.nextInt(10) + 1;
                // rand.nextInt((max - min) + 1) + min
                ArrayList<Pair> ops = new ArrayList<>(numOps);
                for (int j = 0; j < numOps; j++) {
                    ops.add(new Pair(rand.nextBoolean(), rand.nextInt(RANGE + 1)));
                }

                while (true) {
                    try {
                        try {
                            TX.TXbegin();
                            for (int j = 0; j < numOps; j++) {
                                Pair p = ops.get(j);
                                if (p.first) {
                                    LL1.put(p.second, p.second);
                                    assertEquals(true, LL1.containsKey(p.second));
                                    LL2.remove(p.second);
                                    assertEquals(false, LL2.containsKey(p.second));
                                } else {
                                    LL1.remove(p.second);
                                    assertEquals(false, LL1.containsKey(p.second));
                                    LL2.put(p.second, p.second);
                                    assertEquals(true, LL2.containsKey(p.second));
                                }
                            }
                        } finally {
                            TX.TXend();
                        }
                    } catch (TXLibExceptions.AbortException exp) {
                        continue;
                    }
                    break;
                }

                LocalStorage ls = TX.lStorage.get();
                map.put(ls.writeVersion, ops);

            }
        }
    }

    class RunSimpleTransactionMultiLL implements Runnable {

        LinkedList LL1;
        LinkedList LL2;
        CountDownLatch latch;

        RunSimpleTransactionMultiLL(CountDownLatch l, LinkedList ll1, LinkedList ll2) {
            latch = l;
            LL1 = ll1;
            LL2 = ll2;
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
                            LL1.put(n, n);
                            LL2.put(n + 3, n + 3);
                            LL1.put(n + 6, n + 6);
                            LL2.put(n + 9, n + 9);
                            LL1.put(n + 12, n + 12);
                            LL2.put(n + 15, n + 15);
                            LL1.put(n + 7, n + 7);
                            LL2.put(n + 11, n + 11);
                            assertEquals(true, LL1.containsKey(n + 12));
                            assertEquals(n + 12, LL1.get(n + 12));
                            assertEquals(true, LL2.containsKey(n + 15));
                            assertEquals(n + 15, LL2.get(n + 15));
                            assertEquals(n, LL1.get(n));
                            assertEquals(n + 11, LL2.get(n + 11));
                            assertEquals(n + 7, LL1.get(n + 7));
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

    class RunMultiLinkedListLMultiSingleton implements Runnable {

        LinkedList LL1;
        LinkedList LL2;
        String threadName;
        CountDownLatch latch;

        RunMultiLinkedListLMultiSingleton(String name, CountDownLatch l, LinkedList ll1, LinkedList ll2) {
            threadName = name;
            latch = l;
            LL1 = ll1;
            LL2 = ll2;
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
            Integer k_a = 10 + threadName.charAt(1);
            Integer k_b = 20 + threadName.charAt(1);
            Integer k_c = 30 + threadName.charAt(1);

            assertEquals(null, LL1.put(k_a, a));
            assertEquals(a, LL1.get(k_a));
            assertEquals(null, LL2.put(k_b, b));
            assertEquals(b, LL2.get(k_b));
            assertEquals(null, LL1.put(k_c, c));
            assertEquals(c, LL1.get(k_c));
            assertEquals(a, LL1.remove(k_a));
            assertEquals(b, LL2.remove(k_b));
            assertEquals(c, LL1.remove(k_c));
            assertEquals(false, LL1.containsKey(k_a));
            assertEquals(false, LL2.containsKey(k_b));
            assertEquals(false, LL1.containsKey(k_c));

            assertEquals(null, LL2.put(k_a, a));
            assertEquals(a, LL2.get(k_a));
            assertEquals(null, LL1.put(k_b, b));
            assertEquals(b, LL1.get(k_b));
            assertEquals(null, LL2.put(k_c, c));
            assertEquals(c, LL2.get(k_c));
            assertEquals(a, LL2.remove(k_a));
            assertEquals(b, LL1.remove(k_b));
            assertEquals(c, LL2.remove(k_c));
            assertEquals(false, LL2.containsKey(k_a));
            assertEquals(false, LL1.containsKey(k_b));
            assertEquals(false, LL2.containsKey(k_c));

            assertEquals(null, LL1.put(k_a, a));
            assertEquals(a, LL1.get(k_a));
            assertEquals(null, LL1.put(k_b, b));
            assertEquals(b, LL1.get(k_b));
            assertEquals(null, LL2.put(k_a, a));
            assertEquals(a, LL2.get(k_a));
            assertEquals(null, LL2.put(k_b, b));
            assertEquals(b, LL2.get(k_b));
            assertEquals(a, LL2.remove(k_a));
            assertEquals(b, LL1.remove(k_b));
            assertEquals(a, LL1.remove(k_a));
            assertEquals(b, LL2.remove(k_b));
            assertEquals(false, LL2.containsKey(k_a));
            assertEquals(false, LL1.containsKey(k_b));
            assertEquals(false, LL1.containsKey(k_a));
            assertEquals(false, LL2.containsKey(k_b));
        }
    }

}
