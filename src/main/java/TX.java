import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

public class TX {

    public static final boolean DEBUG_MODE_LL = false;
    public static final boolean DEBUG_MODE_QUEUE = false;
    private static final boolean DEBUG_MODE_TX = false;
    private static final boolean DEBUG_MODE_VERSION = false;

    protected static ThreadLocal<LocalStorage> lStorage = ThreadLocal.withInitial(LocalStorage::new);

    private static AtomicLong GVC = new AtomicLong();

    protected static long getVersion() {
        return GVC.get();
    }

    protected static long incrementAndGetVersion() {
        return GVC.incrementAndGet();
    }

    public static void TXbegin() {

        if (DEBUG_MODE_TX) {
            System.out.println("TXbegin");
        }

        LocalStorage localStorage = lStorage.get();
        localStorage.TX = true;
        localStorage.readVersion = getVersion();
    }

    public static boolean TXend() throws TXLibExceptions.AbortException {

        if (DEBUG_MODE_TX) {
            System.out.println("TXend");
        }

        boolean abort = false;

        LocalStorage localStorage = lStorage.get();

        if (!localStorage.TX) {
            if (DEBUG_MODE_TX) {
                System.out.println("TXend - abort the TX");
            }
            abort = true;
        }

        // locking write set

        HashMap<LNode, WriteElement> writeSet = localStorage.writeSet;

        HashSet<LNode> lockedLNodes = new HashSet<>();

        if (!abort) {

            for (Entry<LNode, WriteElement> entry : writeSet.entrySet()) {
                LNode node = entry.getKey();
                if (!node.tryLock()) {
                    abort = true;
                    break;
                }
                lockedLNodes.add(node);
            }

        }
        // locking queues
        HashMap<Queue, LocalQueue> qMap = localStorage.queueMap;

        if (!abort) {

            for (Entry<Queue, LocalQueue> entry : qMap.entrySet()) {

                Queue queue = entry.getKey();

                if (!queue.tryLock()) { // if queue is locked by another thread
                    abort = true;
                    break;

                }

                LocalQueue lQueue = entry.getValue();
                lQueue.isLockedByMe = true;

            }
        }

        // validate read set

        HashSet<LNode> readSet = localStorage.readSet;

        if (!abort) {

            for (LNode node : readSet) {
                if (!lockedLNodes.contains(node) && node.isLocked()) {
                    // someone else holds the lock
                    abort = true;
                    break;
                } else if (node.getVersion() > localStorage.readVersion) {
                    abort = true;
                    break;
                } else if (node.getVersion() == localStorage.readVersion && node.isSingleton()) {
                    incrementAndGetVersion(); // increment GVC
                    node.setSingleton(false);
                    if (DEBUG_MODE_VERSION) {
                        System.out.println("singleton - increment GVC");
                    }
                    abort = true;
                    break;
                }

            }

        }

        // validate queue

        if (!abort) {

            for (Entry<Queue, LocalQueue> entry : qMap.entrySet()) {

                Queue queue = entry.getKey();
                if (queue.getVersion() > localStorage.readVersion) {
                    abort = true;
                    break;
                } else if (queue.getVersion() == localStorage.readVersion && queue.isSingleton()) {
                    incrementAndGetVersion(); // increment GVC
                    abort = true;
                    break;
                }

            }

        }

        // increment GVC

        long writeVersion = 0;

        if (!abort && !localStorage.readOnly) {
            writeVersion = incrementAndGetVersion();
            assert (writeVersion > localStorage.readVersion);
            localStorage.writeVersion = writeVersion;
        }

        // commit
        if (!abort && !localStorage.readOnly) {
            // LinkedList

            for (Entry<LNode, WriteElement> entry : writeSet.entrySet()) {
                LNode node = entry.getKey();
                WriteElement we = entry.getValue();

                node.next = we.next;
                node.val = we.val; // when node val changed because of put
                if (we.deleted) {
                    node.setDeleted(true);
                    node.val = null; // for index
                }
                node.setVersion(writeVersion);
                node.setSingleton(false);
            }
        }

        if (!abort) {
            // Queue

            for (Entry<Queue, LocalQueue> entry : qMap.entrySet()) {

                Queue queue = entry.getKey();
                LocalQueue lQueue = entry.getValue();

                queue.dequeueNodes(lQueue.nodeToDeq);
                queue.enqueueNodes(lQueue);
                if (TX.DEBUG_MODE_QUEUE) {
                    System.out.println("commit queue before set version");
                }
                queue.setVersion(writeVersion);
                queue.setSingleton(false);

            }

        }

        // release locks, even if abort

        lockedLNodes.forEach(LNode::unlock);

        for (Entry<Queue, LocalQueue> entry : qMap.entrySet()) {

            Queue queue = entry.getKey();
            LocalQueue lQueue = entry.getValue();
            if (lQueue.isLockedByMe) {
                queue.unlock();
                lQueue.isLockedByMe = false;
            }
        }

        // update index
        if (!abort && !localStorage.readOnly) {
            // adding to index
            HashMap<LinkedList, ArrayList<LNode>> indexMap = localStorage.indexAdd;
            for (Entry<LinkedList, ArrayList<LNode>> entry : indexMap.entrySet()) {
                LinkedList list = entry.getKey();
                ArrayList<LNode> nodes = entry.getValue();
                nodes.forEach(node -> list.index.add(node));
            }
            // removing from index
            indexMap = localStorage.indexRemove;
            for (Entry<LinkedList, ArrayList<LNode>> entry : indexMap.entrySet()) {
                LinkedList list = entry.getKey();
                ArrayList<LNode> nodes = entry.getValue();
                nodes.forEach(node -> list.index.remove(node));
            }
        }

        // cleanup

        localStorage.queueMap.clear();
        localStorage.writeSet.clear();
        localStorage.readSet.clear();
        localStorage.indexAdd.clear();
        localStorage.indexRemove.clear();
        localStorage.TX = false;
        localStorage.readOnly = true;

        if (DEBUG_MODE_TX) {
            if (abort) {
                System.out.println("TXend - aborted");
            }
            System.out.println("TXend - is done");
        }

        if (abort) {
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }

        return true;

    }

}
