package transactionLib;

import java.util.concurrent.locks.ReentrantLock;

public class LockQueue {
	
	private ReentrantLock lock;
	// TODO which lock to use?
	
	protected LockQueue() {
		lock = new ReentrantLock();
	}
	
	protected void lock(){
		if(lock.isHeldByCurrentThread() == false){
			lock.lock();
		}	
	}
	
	protected void unlock(){
		assert(lock.isHeldByCurrentThread() == true);
		lock.unlock();
	}
	
	protected boolean tryLock(){
		if(lock.isHeldByCurrentThread() == false){
			if(lock.tryLock() == false){
				return false;
			}
		}
		return true;
	}
	
}
