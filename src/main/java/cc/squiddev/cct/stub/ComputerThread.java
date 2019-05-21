package cc.squiddev.cct.stub;

import cc.squiddev.cct.js.Callbacks;
import cc.squiddev.cct.js.Callbacks.Callback;
import dan200.computercraft.core.computer.ComputerExecutor;

/**
 * We've no threads in JS land, so we create a fake version which just executes
 * all the work a computer can do.
 */
public class ComputerThread {
    private static ComputerExecutor lastExecutor;
    private static final Callback CALLBACK = ComputerThread::workOnce;

    public static void queue(ComputerExecutor executor) {
        if (executor.onComputerQueue) throw new IllegalStateException("Cannot queue already queued executor");
        lastExecutor = executor;
        executor.onComputerQueue = true;
        Callbacks.setImmediate(CALLBACK);
    }

    private static void workOnce() {
        if (lastExecutor == null) throw new IllegalStateException("Working, but executor is null");
        if (!lastExecutor.onComputerQueue) throw new IllegalArgumentException("Working but not on queue");

        lastExecutor.beforeWork();
        try {
            lastExecutor.work();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (lastExecutor.afterWork()) {
            Callbacks.setImmediate(CALLBACK);
        } else {
            lastExecutor = null;
        }
    }

    public static boolean hasPendingWork() {
        return true;
    }

    public static long scaledPeriod() {
        return 50 * 1_000_000L;
    }
}
