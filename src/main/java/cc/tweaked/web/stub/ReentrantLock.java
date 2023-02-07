package cc.tweaked.web.stub;

/**
 * @see java.util.concurrent.locks.ReentrantLock
 */
public class ReentrantLock {
    public boolean tryLock() {
        return true;
    }

    public void unlock() {
    }

    public void lockInterruptibly() throws InterruptedException {
    }
}
