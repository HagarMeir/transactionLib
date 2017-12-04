import java.util.concurrent.locks.ReentrantLock;

public class LockQueue {

    private ReentrantLock lock;
    // TODO which lock to use?

    LockQueue() {
        lock = new ReentrantLock();
    }

    protected void lock() {
        if (!lock.isHeldByCurrentThread()) {
            lock.lock();
        }
    }

    protected void unlock() {
        assert (lock.isHeldByCurrentThread());
        lock.unlock();
    }

    protected boolean tryLock() {
        return lock.isHeldByCurrentThread() || lock.tryLock();
    }

}
