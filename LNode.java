package transactionLib;

import java.util.concurrent.atomic.AtomicLong;

public class LNode {
	protected LNode next = null;
	protected Integer key = null; // TODO use templates
	protected Object val = null; // TODO maybe use templates

	// bit 60 is lock
	// bit 61 is deleted
	// bit 62 is singleton
	// 0 is false, 1 is true
	// we are missing a bit because this is signed
	private AtomicLong versionAndFlags = new AtomicLong();
	private static final long lockMask = 0x1000000000000000L;
	private static final long deleteMask = 0x2000000000000000L;
	private static final long singletonMask = 0x4000000000000000L;
	private static final long versionNegMask = lockMask | deleteMask | singletonMask;

	protected boolean tryLock() {
		long l = versionAndFlags.get();
		if ((l & lockMask) != 0) {
			return false;
		}
		long locked = l | lockMask;
		return versionAndFlags.compareAndSet(l, locked);
	}

	protected void unlock() {
		long l = versionAndFlags.get();
		assert ((l & lockMask) != 0);
		long unlocked = l & (~lockMask);
		boolean ret = versionAndFlags.compareAndSet(l, unlocked);
		assert (ret == true);
	}

	protected boolean isLocked() {
		long l = versionAndFlags.get();
		if ((l & lockMask) == 0) {
			return false;
		}
		return true;
	}

	protected boolean isDeleted() {
		long l = versionAndFlags.get();
		if ((l & deleteMask) == 0) {
			return false;
		}
		return true;
	}

	protected boolean isLockedOrDeleted() {
		long l = versionAndFlags.get();
		if (((l & deleteMask) == 0) && ((l & lockMask) == 0)) {
			return false;
		}
		return true;
	}

	protected void setDeleted(boolean value) {
		long l = versionAndFlags.get();
		assert ((l & lockMask) != 0);
		if (value == true) {
			l |= deleteMask;
			versionAndFlags.set(l);
			return;
		}
		l &= (~deleteMask);
		versionAndFlags.set(l);
	}

	protected boolean isSingleton() {
		long l = versionAndFlags.get();
		if ((l & singletonMask) == 0) {
			return false;
		}
		return true;
	}

	protected void setSingleton(boolean value) {
		long l = versionAndFlags.get();
		assert ((l & lockMask) != 0);
		if (value == true) {
			l |= singletonMask;
			versionAndFlags.set(l);
			return;
		}
		l &= (~singletonMask);
		versionAndFlags.set(l);
	}

	protected void setSingletonNoLockAssert(boolean value) {
		long l = versionAndFlags.get();
		if (value == true) {
			l |= singletonMask;
			versionAndFlags.set(l);
			return;
		}
		l &= (~singletonMask);
		versionAndFlags.set(l);
	}

	protected long getVersion() {
		return (versionAndFlags.get() & (~versionNegMask));
	}

	protected boolean isSameVersionAndSingleton(long version) {
		long l = versionAndFlags.get();
		if ((l & singletonMask) == 0) {
			l &= (~versionNegMask);
			if (l == version) {
				return true;
			}
		}
		return false;
	}

	protected void setVersion(long version) {
		long l = versionAndFlags.get();
		assert ((l & lockMask) != 0);
		l &= versionNegMask;
		l |= (version & (~versionNegMask));
		versionAndFlags.set(l);
	}

	protected void setVersionAndSingleton(long version, boolean value) {
		long l = versionAndFlags.get();
		assert ((l & lockMask) != 0);
		l &= versionNegMask;
		l |= (version & (~versionNegMask));
		if (value == true) {
			l |= singletonMask;
			versionAndFlags.set(l);
			return;
		}
		l &= (~singletonMask);
		versionAndFlags.set(l);
	}

	protected void setVersionAndSingletonNoLockAssert(long version, boolean value) {
		long l = versionAndFlags.get();
		l &= versionNegMask;
		l |= (version & (~versionNegMask));
		if (value == true) {
			l |= singletonMask;
			versionAndFlags.set(l);
			return;
		}
		l &= (~singletonMask);
		versionAndFlags.set(l);
	}

	protected void setVersionAndDeletedAndSingleton(long version, boolean deleted, boolean singleton) {
		long l = versionAndFlags.get();
		assert ((l & lockMask) != 0);
		l &= versionNegMask;
		l |= (version & (~versionNegMask));
		if (singleton == true) {
			l |= singletonMask;
		} else {
			l &= (~singletonMask);
		}
		if (deleted == true) {
			l |= deleteMask;
		} else {
			l &= (~deleteMask);
		}
		versionAndFlags.set(l);
	}

}
