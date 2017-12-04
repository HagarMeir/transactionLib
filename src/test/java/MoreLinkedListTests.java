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

public class MoreLinkedListTests {

    private static final int ITERATIONS = 100;
    private static final int RANGE = 10000;
    private static final int THREADS = 10;

    @Test
    public void testSingleThread() {
        LinkedList LL = new LinkedList();
        while (true) {
            try {
                try {
                    TX.TXbegin();
                    Assert.assertEquals(null, LL.put(5, "5"));
                    Assert.assertEquals(null, LL.put(4, "4"));
                    Assert.assertEquals(true, LL.containsKey(5));
                    Assert.assertEquals(false, LL.containsKey(3));
                    Assert.assertEquals(null, LL.remove(3));
                    Assert.assertEquals("5", LL.remove(5));
                    Assert.assertEquals(false, LL.containsKey(5));
                    Assert.assertEquals(null, LL.put(3, "3"));
                    Assert.assertEquals("3", LL.put(3, "3"));
                    Assert.assertEquals(null, LL.put(9, "9"));
                    Assert.assertEquals(null, LL.put(6, "6"));
                    Assert.assertEquals(true, LL.containsKey(3));
                    Assert.assertEquals(true, LL.containsKey(6));
                    Assert.assertEquals("3", LL.remove(3));
                    Assert.assertEquals(false, LL.containsKey(3));
                    Assert.assertEquals(null, LL.put(-1, "-1"));
                    Assert.assertEquals(true, LL.containsKey(-1));
                    Assert.assertEquals("-1", LL.remove(-1));
                    Assert.assertEquals(false, LL.containsKey(-1));
                } finally {
                    TX.TXend();
                }
            } catch (TXLibExceptions.AbortException exp) {
                continue;
            }
            break;
        }

        while (true) {
            try {
                try {
                    TX.TXbegin();
                    Assert.assertEquals("9", LL.remove(9));
                    Assert.assertEquals("6", LL.remove(6));
                    Assert.assertEquals(null, LL.put(12, "12"));
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
    public void testMultiThreadSimpleTransaction() throws InterruptedException {
        // System.out.println("testMultiThreadPutTransaction");
        CountDownLatch latch = new CountDownLatch(1);
        LinkedList LL = new LinkedList();
        ArrayList<Thread> threads = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            threads.add(new Thread(new RunSimpleTransaction(latch, LL)));
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
    public void testMultiSingletons() throws InterruptedException {
        // System.out.println("testMultiSingletons");
        CountDownLatch latch = new CountDownLatch(1);
        LinkedList LL = new LinkedList();
        ArrayList<Thread> threads = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            threads.add(new Thread(new RunSingleton(latch, LL)));
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
    public void testMultiTransactions() throws InterruptedException {
        // System.out.println("testMultiTransactions");
        CountDownLatch latch = new CountDownLatch(1);
        LinkedList LL = new LinkedList();
        ConcurrentSkipListMap<Long, ArrayList<Pair>> map = new ConcurrentSkipListMap<>();
        ArrayList<Thread> threads = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            threads.add(new Thread(new RunTransactions(latch, LL, map)));
        }
        for (int i = 0; i < THREADS; i++) {
            threads.get(i).start();
        }
        latch.countDown();
        for (int i = 0; i < THREADS; i++) {
            threads.get(i).join();
        }

        SortedSet<Integer> set = new TreeSet<>();
        // go over map in ascending order of keys (so by version)
        // System.out.print("\n");
        for (Entry<Long, ArrayList<Pair>> entry : map.entrySet()) {
            // System.out.print(entry.getKey() + ":");
            ArrayList<Pair> ops = entry.getValue();
            for (Pair p : ops) {
                if (p.first) {
                    set.add(p.second);
                } else {
                    set.remove(p.second);
                }
            }
        }
        // System.out.print("\n");

        // LL.printLinkedListNotInTX();
        // System.out.println(set.size() + " in set");
        // System.out.print("-2147483648--");
        // for (Integer k : set) {
        // System.out.print(k + "--");
        // }
        // System.out.print("\n");

        LNode node = LL.head.next;
        for (Integer k : set) {
            Assert.assertNotNull(node);
            Assert.assertEquals(k, node.key);
            node = node.next;
        }
        Assert.assertNull(node);
    }

    class RunSimpleTransaction implements Runnable {

        LinkedList LL;
        CountDownLatch latch;

        RunSimpleTransaction(CountDownLatch l, LinkedList ll) {
            latch = l;
            LL = ll;
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
                            LL.putIfAbsent(n + 3, n + 3);
                            LL.put(n + 6, n + 6);
                            LL.putIfAbsent(n + 9, n + 9);
                            LL.putIfAbsent(n + 12, n + 12);
                            LL.putIfAbsent(n + 15, n + 15);
                            LL.put(n + 7, n + 7);
                            LL.putIfAbsent(n + 11, n + 11);
                            assertEquals(true, LL.containsKey(n + 12));
                            assertEquals(n + 12, LL.get(n + 12));
                            assertEquals(true, LL.containsKey(n + 15));
                            assertEquals(n + 15, LL.get(n + 15));
                            assertEquals(n, LL.get(n));
                            assertEquals(n + 11, LL.get(n + 11));
                            assertEquals(n + 7, LL.get(n + 7));
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

    class RunSingleton implements Runnable {

        LinkedList LL;
        CountDownLatch latch;

        RunSingleton(CountDownLatch l, LinkedList ll) {
            latch = l;
            LL = ll;
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

                for (int j = 0; j < numOps; j++) {
                    Pair p = ops.get(j);
                    if (p.first) {
                        LL.putIfAbsent(p.second, p.second);
                    } else {
                        LL.remove(p.second);

                    }
                }

            }
        }
    }

    class Pair {
        protected boolean first;
        protected int second;

        Pair(boolean first, int second) {
            this.first = first;
            this.second = second;
        }

    }

    class RunTransactions implements Runnable {

        ConcurrentSkipListMap<Long, ArrayList<Pair>> map;
        LinkedList LL;
        CountDownLatch latch;

        RunTransactions(CountDownLatch l, LinkedList ll, ConcurrentSkipListMap<Long, ArrayList<Pair>> m) {
            latch = l;
            LL = ll;
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
                                    LL.put(p.second, p.second);
                                    assertEquals(true, LL.containsKey(p.second));
                                } else {
                                    LL.remove(p.second);
                                    assertEquals(false, LL.containsKey(p.second));
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

}
