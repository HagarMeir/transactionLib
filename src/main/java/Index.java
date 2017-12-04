/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License version 2 for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Random;

public class Index {

    /**
     * Special value used to identify base-level header
     */
    private static final Object BASE_HEADER = new Object();
    /**
     * Unsafe mechanics
     */
    private static final Unsafe UNSAFE;
    private static final long headOffset;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
            Class<?> k = Index.class;
            headOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("head"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * The topmost head index of the skiplist.
     */
    private volatile HeadIndex head;

    /**
     * Constructor
     */
    Index(LNode headNode) {
        headNode.val = BASE_HEADER;
        head = new HeadIndex(headNode, null, null, 1);
    }

    /**
     * compareAndSet head node
     */
    private boolean casHead(HeadIndex cmp, HeadIndex val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    /**
     * Returns a node with key strictly less than given key,
     * or the header if there is no such node.  Also
     * unlinks indexes to deleted nodes found along the way.  Callers
     * rely on this side-effect of clearing indices to deleted nodes.
     *
     * @return a predecessor of key
     */
    private LNode findPredecessor(LNode node) {
        for (; ; ) {
            for (IndexNode q = head, r = q.right, d; ; ) {
                if (r != null) {
                    LNode n = r.node;
                    if (n.val == null) {
                        if (!q.unlink(r))
                            break;           // restart
                        r = q.right;         // reread r
                        continue;
                    }
                    if (node.key.compareTo(n.key) > 0) {
                        q = r;
                        r = r.right;
                        continue;
                    }
                }
                if ((d = q.down) == null)
                    return q.node;
                q = d;
                r = d.right;
            }
        }
    }


    void add(final LNode nodeToAdd) {
        LNode node = nodeToAdd;
        if (node == null)
            throw new NullPointerException();
        int rnd = RandomNumberGenerator.randomNumber();
        if ((rnd & 0x80000001) == 0) { // test highest and lowest bits
            int level = 1, max;
            while (((rnd >>>= 1) & 1) != 0)
                ++level;
            IndexNode idx = null;
            HeadIndex h = head;
            if (level <= (max = h.level)) {
                for (int i = 1; i <= level; ++i)
                    idx = new IndexNode(node, idx, null);
            } else { // try to grow by one level
                level = max + 1; // hold in array and later pick the one to use
                IndexNode[] idxs = new IndexNode[level + 1];
                for (int i = 1; i <= level; ++i)
                    idxs[i] = idx = new IndexNode(node, idx, null);
                for (; ; ) {
                    h = head;
                    int oldLevel = h.level;
                    if (level <= oldLevel) // lost race to add level
                        break;
                    HeadIndex newh = h;
                    LNode oldbase = h.node;
                    for (int j = oldLevel + 1; j <= level; ++j)
                        newh = new HeadIndex(oldbase, newh, idxs[j], j);
                    if (casHead(h, newh)) {
                        h = newh;
                        idx = idxs[level = oldLevel];
                        break;
                    }
                }
            }
            // find insertion points and splice in
            splice:
            for (int insertionLevel = level; ; ) {
                int j = h.level;
                for (IndexNode q = h, r = q.right, t = idx; ; ) {
                    if (q == null || t == null)
                        break splice;
                    if (r != null) {
                        LNode n = r.node;
                        // compare before deletion check avoids needing recheck
                        int c = node.key.compareTo(n.key);
                        if (n.val == null) {
                            if (!q.unlink(r))
                                break;
                            r = q.right;
                            continue;
                        }
                        if (c > 0) {
                            q = r;
                            r = r.right;
                            continue;
                        }
                    }

                    if (j == insertionLevel) {
                        if (!q.link(r, t))
                            break; // restart
                        if (t.node.val == null) {
                            break splice;
                        }
                        if (--insertionLevel == 0)
                            break splice;
                    }

                    if (--j >= insertionLevel && j < level)
                        t = t.down;
                    q = q.down;
                    r = q.right;
                }
            }
        }
    }

    void remove(final LNode node) {
        if (node == null)
            throw new NullPointerException();
        findPredecessor(node); // clean index
        if (head.right == null)
            tryReduceLevel();
    }

    /**
     * Possibly reduce head level if it has no nodes.  This method can
     * (rarely) make mistakes, in which case levels can disappear even
     * though they are about to contain index nodes. This impacts
     * performance, not correctness.  To minimize mistakes as well as
     * to reduce hysteresis, the level is reduced by one only if the
     * topmost three levels look empty. Also, if the removed level
     * looks non-empty after CAS, we try to change it back quick
     * before anyone notices our mistake! (This trick works pretty
     * well because this method will practically never make mistakes
     * unless current thread stalls immediately before first CAS, in
     * which case it is very unlikely to stall again immediately
     * afterwards, so will recover.)
     * <p>
     * We put up with all this rather than just let levels grow
     * because otherwise, even a small map that has undergone a large
     * number of insertions and removals will have a lot of levels,
     * slowing down access more than would an occasional unwanted
     * reduction.
     */
    private void tryReduceLevel() {
        HeadIndex h = head;
        HeadIndex d;
        HeadIndex e;
        if (h.level > 3 &&
                (d = (HeadIndex) h.down) != null &&
                (e = (HeadIndex) d.down) != null &&
                e.right == null &&
                d.right == null &&
                h.right == null &&
                casHead(h, d) && // try to set
                h.right != null) // recheck
            casHead(d, h);   // try to backout
    }

    LNode getPred(final LNode node) {
        if (node == null)
            throw new NullPointerException();
        for (; ; ) {
            LNode b = findPredecessor(node);
            if (b.val != null) // not deleted
                return b;
        }
    }

    public static class RandomNumberGenerator {
        /**
         * Generates the initial random seed for the cheaper
         * per-instance random number generators used in randomLevel.
         */
        private static final Random seedGenerator = new Random();

        /**
         * Seed for simple random number generator.
         * Not volatile since it doesn't matter too much
         * if different threads don't see updates.
         */
        private static int randomSeed = seedGenerator.nextInt() | 0x0100;

        /**
         * Returns a random number.
         * Hardwired to k=1, p=0.5, max 31
         * (see above and Pugh's "Skip List Cookbook", sec 3.4).
         * <p>
         * This uses the simplest of the generators described in
         * George Marsaglia's "Xorshift RNGs" paper.
         * This is not a high-quality generator but is acceptable here.
         */
        static int randomNumber() {
            int x = randomSeed;
            x ^= x << 13;
            x ^= x >>> 17;
            randomSeed = x ^= x << 5;
            if (x == 0)
                randomSeed = x = 1; // avoid zero
            return x;
        }
    }

    /**
     * Index nodes represent the levels of the skip list.
     */
    static class IndexNode {
        /**
         * Unsafe mechanics
         */
        private static final Unsafe UNSAFE;
        private static final long rightOffset;

        static {
            try {

                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                UNSAFE = (Unsafe) f.get(null);
                Class<?> k = IndexNode.class;
                rightOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("right"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        final LNode node;
        final IndexNode down;
        volatile IndexNode right;

        /**
         * Creates index node with given values.
         */
        IndexNode(LNode node, IndexNode down, IndexNode right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }

        /**
         * compareAndSet right field
         */
        final boolean casRight(IndexNode cmp, IndexNode val) {
            return UNSAFE.compareAndSwapObject(this, rightOffset, cmp, val);
        }

        /**
         * Tries to CAS newSucc as successor.  To minimize races with
         * unlink that may lose this index node, if the node being
         * indexed is known to be deleted, it doesn't try to link in.
         *
         * @param succ    the expected current successor
         * @param newSucc the new successor
         * @return true if successful
         */
        final boolean link(IndexNode succ, IndexNode newSucc) {
            LNode n = node;
            newSucc.right = succ;
            return n.val != null && casRight(succ, newSucc);
        }

        /**
         * Tries to CAS right field to skip over apparent successor
         * succ.  Fails (forcing a retraversal by caller) if this node
         * is known to be deleted.
         *
         * @param succ the expected current successor
         * @return true if successful
         */
        final boolean unlink(IndexNode succ) {
            return node.val != null && casRight(succ, succ.right);
        }

    }

    /**
     * Nodes heading each level keep track of their level.
     */
    static final class HeadIndex extends IndexNode {
        final int level;

        HeadIndex(LNode node, IndexNode down, IndexNode right, int level) {
            super(node, down, right);
            this.level = level;
        }
    }

}
