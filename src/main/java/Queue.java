import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Queue {
    //	private static final long lockMask = 0x2000000000000000L;
    private static final long singletonMask = 0x4000000000000000L;
    //	private static final long versionMask = lockMask | singletonMask;
    private static final long versionNegMask = singletonMask;
    private LockQueue qLock = new LockQueue();
    private QNode head;
    private QNode tail;
    private int size;
    //	// bit 61 is lock
    // bit 62 is singleton
    // 0 is false, 1 is true
    // we are missing a bit because this is signed
    private AtomicLong versionAndFlags = new AtomicLong();

    protected long getVersion() {
        return (versionAndFlags.get() & (~versionNegMask));
    }

    protected void setVersion(long version) {
        long l = versionAndFlags.get();
//		assert ((l & lockMask) != 0);
        l &= versionNegMask;
        l |= (version & (~versionNegMask));
        versionAndFlags.set(l);
    }

    protected boolean isSingleton() {
        long l = versionAndFlags.get();
        return (l & singletonMask) != 0;
    }

    protected void setSingleton(boolean value) {
        long l = versionAndFlags.get();
//		assert ((l & lockMask) != 0);
        if (value) {
            l |= singletonMask;
            versionAndFlags.set(l);
            return;
        }
        l &= (~singletonMask);
        versionAndFlags.set(l);
    }

    private void lock() {
        qLock.lock();
    }

    protected boolean tryLock() {
        return qLock.tryLock();
    }

    protected void unlock() {
        qLock.unlock();
    }

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// another implementation of queueLock:
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// 	
//	private void lock() {
//		if(TX.lStorage.getTX() == true){
//			// locking only in singleton
//			if(TX.lStorage.getQueueMap().get(this).isLockedByMe == true){
//				assert ((flags.get() & lockMask) != 0);
//				assert(false); 
//				return;
//			}
//		}
//		long l, locked;
//		do {
//			l = flags.get();
//			while ((l & lockMask) != 0) {
//				// wait till lock is free
//				l = flags.get();
//			}
//			locked = l | lockMask;
//		} while (flags.compareAndSet(l, locked) == false);
//		if(TX.lStorage.getTX() == true){
//			HashMap<Queue, LocalQueue> qMap = TX.lStorage.getQueueMap();
//			LocalQueue lQueue = qMap.get(this);
//			if(lQueue == null){
//				lQueue = new LocalQueue();
//			}
//			lQueue.isLockedByMe = true;
//			qMap.put(this, lQueue);
//			TX.lStorage.setQueueMap(qMap);
//			assert ((flags.get() & lockMask) != 0);
//		}
//	}

//	protected boolean tryLock() {
//		if(TX.lStorage.getTX() == true){
//			HashMap<Queue, LocalQueue> qMap = TX.lStorage.getQueueMap();
//			LocalQueue lQueue = qMap.get(this);
//			if(lQueue != null){
//				if(TX.lStorage.getQueueMap().get(this).isLockedByMe == true){
//					if(TX.DEBUG_MODE_QUEUE){
//						System.out.println("queue try lock - I have the lock");
//					}
//					assert ((flags.get() & lockMask) != 0);
//					return true;
//				}
//			}
//		}
//		long l = flags.get();
//		if ((l & lockMask) != 0) {
//			return false;
//		}
//		long locked = l | lockMask;
//		if(TX.DEBUG_MODE_QUEUE){
//			System.out.println("queue try lock - this is the locked " + locked);
//		}
//		if(flags.compareAndSet(l, locked)==true){
//			if(TX.lStorage.getTX() == true){
//				HashMap<Queue, LocalQueue> qMap = TX.lStorage.getQueueMap();
//				LocalQueue lQueue = qMap.get(this);
//				if(lQueue == null){
//					lQueue = new LocalQueue();
//				}
//				lQueue.isLockedByMe = true;
//				qMap.put(this, lQueue);
//				TX.lStorage.setQueueMap(qMap);
//			}
//			if(TX.DEBUG_MODE_QUEUE){
//				System.out.println("queue try lock - managed to lock");
//			}
//			assert ((flags.get() & lockMask) != 0);
//			return true;
//		}
//		return false;
//	}
//
//	protected void unlock() {
//		long l = flags.get();
//		assert ((l & lockMask) != 0);
//		if(TX.lStorage.getTX() == true){
//			assert(TX.lStorage.getQueueMap().get(this).isLockedByMe == true);
//		}
//		long unlocked = l & (~lockMask);
//		assert (flags.compareAndSet(l, unlocked) == true);
//		if(TX.lStorage.getTX() == true){
//			HashMap<Queue, LocalQueue> qMap = TX.lStorage.getQueueMap();
//			LocalQueue lQueue = qMap.get(this);
//			if(lQueue == null){
//				lQueue = new LocalQueue();
//			}
//			lQueue.isLockedByMe = false;
//			qMap.put(this, lQueue);
//			TX.lStorage.setQueueMap(qMap);
//		}
//	}

    protected void enqueueNodes(LocalQueue lQueue) {
        assert (lQueue != null);
        if (TX.DEBUG_MODE_QUEUE) {
            System.out.println("Queue enqueueNodes");
        }
        try {
            while (!lQueue.isEmpty()) {
                if (TX.DEBUG_MODE_QUEUE) {
                    System.out.println("Queue enqueueNodes - lQueue is not empty");
                }
                QNode node = new QNode();
                node.val = lQueue.dequeue();
                if (TX.DEBUG_MODE_QUEUE) {
                    System.out.println("Queue enqueueNodes - lQueue node val is " + node.val);
                }
                node.next = null;
                node.prev = tail;
                size++;
                if (tail == null) {
                    tail = node;
                    head = node;
                } else {
                    tail.next = node;
                    tail = node;
                }
            }
        } catch (TXLibExceptions.QueueIsEmptyException e) {
            if (TX.DEBUG_MODE_QUEUE) {
                System.out.println("Queue enqueueNodes - local queue is empty");
            }
        }

    }

    protected void dequeueNodes(QNode upToNode) {

        if (upToNode == null) {
            if (TX.DEBUG_MODE_QUEUE) {
                System.out.println("Queue dequeueNodes - upToNode is null");
            }
            return;
        }

        if (TX.DEBUG_MODE_QUEUE) {
            System.out.println("Queue dequeueNodes");
        }

        QNode curr = head;
        while (curr != null && curr != upToNode.next) {
            if (TX.DEBUG_MODE_QUEUE) {
                System.out.println("Queue dequeueNodes - dequeueing");
            }
            size--;
            curr = curr.next;
        }
        head = curr;
        if (curr == null) {
            tail = null;
            assert (size == 0);
        }
    }

    public void enqueue(Object val) throws TXLibExceptions.AbortException {

        LocalStorage localStorage = TX.lStorage.get();

        // SINGLETON
        if (!localStorage.TX) {

            if (TX.DEBUG_MODE_QUEUE) {
                System.out.println("Queue enqueue - singleton");
            }

            QNode node = new QNode();
            node.val = val;
            node.next = null;
            node.prev = tail;

            lock();
            size++;
            if (tail == null) {
                tail = node;
                head = node;

            } else {
                tail.next = node;
                tail = node;
            }

            setVersion(TX.getVersion());
            setSingleton(true);

            unlock();
            return;
        }

        // TX

        if (TX.DEBUG_MODE_QUEUE) {
            System.out.println("Queue enqueue - in TX");
        }

        if (localStorage.readVersion < getVersion()) {
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }
        if ((localStorage.readVersion == getVersion()) && (isSingleton())) {
            TX.incrementAndGetVersion();
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }

        HashMap<Queue, LocalQueue> qMap = localStorage.queueMap;
        LocalQueue lQueue = qMap.get(this);
        if (lQueue == null) {
            lQueue = new LocalQueue();
        }
        lQueue.enqueue(val);
        qMap.put(this, lQueue);

    }

    public Object dequeue() throws TXLibExceptions.QueueIsEmptyException, TXLibExceptions.AbortException {

        LocalStorage localStorage = TX.lStorage.get();

        // SINGLETON
        if (!localStorage.TX) {

            if (TX.DEBUG_MODE_QUEUE) {
                System.out.println("Queue dequeue - singleton");
            }

            lock();
            if (head == null) {
                unlock();
                TXLibExceptions excep = new TXLibExceptions();
                throw excep.new QueueIsEmptyException();
            }
            QNode temp = head;
            Object ret = temp.val;
            head = head.next;
            if (head == null) {
                tail = null;
            } else {
                head.prev = null;
            }
            size--;
            setVersion(TX.getVersion());
            setSingleton(true);
            unlock();
            return ret;

        }

        // TX

        if (TX.DEBUG_MODE_QUEUE) {
            System.out.println("Queue dequeue - in TX");
        }

        if (localStorage.readVersion < getVersion()) {
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }
        if ((localStorage.readVersion == getVersion()) && (isSingleton())) {
            TX.incrementAndGetVersion();
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }

        if (!tryLock()) { // if queue is locked by another thread
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();

        }

        // now we have the lock
        HashMap<Queue, LocalQueue> qMap = localStorage.queueMap;
        LocalQueue lQueue = qMap.get(this);
        if (lQueue == null) {
            lQueue = new LocalQueue();
        }

        if (lQueue.firstDeq) {
            if (TX.DEBUG_MODE_QUEUE) {
                System.out.println("Queue dequeue - first dequeue");
            }
            // if this is the first dequeue then try to dequeue the tail
            lQueue.firstDeq = false;
            lQueue.nodeToDeq = head;
        } else if (lQueue.nodeToDeq != null) {
            lQueue.nodeToDeq = lQueue.nodeToDeq.next;
        }

        if (lQueue.nodeToDeq != null) { // dequeue from the queue

            Object ret = lQueue.nodeToDeq.val;
            qMap.put(this, lQueue);

            return ret;
        }

        if (TX.DEBUG_MODE_QUEUE) {
            System.out.println("Queue dequeue - nodeToDeq is null");
        }

        // there is no node in queue, then try the localQueue
        qMap.put(this, lQueue);
        return lQueue.dequeue(); // can throw an exception

    }

    public boolean isEmpty() throws TXLibExceptions.AbortException {

        LocalStorage localStorage = TX.lStorage.get();

        // SINGLETON
        if (!localStorage.TX) {
            if (TX.DEBUG_MODE_QUEUE) {
                System.out.println("Queue isEmpty - singleton");
            }
            int ret = size;
            lock();
            setVersion(TX.getVersion());
            setSingleton(true);
            unlock();
            return (ret <= 0);
        }

        // TX
        if (TX.DEBUG_MODE_QUEUE) {
            System.out.println("Queue isEmpty - in TX");
        }

        if (TX.DEBUG_MODE_QUEUE) {
            System.out.println("Queue isEmpty - now not locked by me");
        }

        if (localStorage.readVersion < getVersion()) {
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }
        if ((localStorage.readVersion == getVersion()) && (isSingleton())) {
            TX.incrementAndGetVersion();
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }

        if (!tryLock()) { // if queue is locked by another thread

            if (TX.DEBUG_MODE_QUEUE) {
                System.out.println("Queue isEmpty - couldn't lock");
            }

            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();

        }

        // now we have the lock
        if (size > 0) {
            return false;
        }

        // check lQueue
        HashMap<Queue, LocalQueue> qMap = localStorage.queueMap;
        LocalQueue lQueue = qMap.get(this);
        if (lQueue == null) {
            lQueue = new LocalQueue();
        }
        qMap.put(this, lQueue);

        return lQueue.isEmpty();

    }

}
