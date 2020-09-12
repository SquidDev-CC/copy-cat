package cc.squiddev.cct.stub;

import cc.squiddev.cct.js.Callbacks;
import cc.squiddev.cct.js.Callbacks.Callback;
import dan200.computercraft.core.computer.ComputerExecutor;

import java.util.ArrayDeque;

/**
 * We've no threads in JS land, so we create a fake version which just executes
 * all the work a computer can do.
 */
public class ComputerThread {
    private static final ArrayDeque<ComputerExecutor> executors = new ArrayDeque<>();
    private static final Callback CALLBACK = ComputerThread::workOnce;

    public static void queue(ComputerExecutor executor) {
        if (executor.onComputerQueue) throw new IllegalStateException("Cannot queue already queued executor");
        executor.onComputerQueue = true;

        if (executors.isEmpty()) Callbacks.setImmediate(CALLBACK);
        executors.add(executor);
    }

    private static void workOnce() {
        ComputerExecutor executor = executors.poll();
        if (executor == null) throw new IllegalStateException("Working, but executor is null");
        if (!executor.onComputerQueue) throw new IllegalArgumentException("Working but not on queue");

        executor.beforeWork();
        try {
            executor.work();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (executor.afterWork()) executors.push(executor);
        if (!executors.isEmpty()) Callbacks.setImmediate(CALLBACK);
    }

    public static boolean hasPendingWork() {
        return true;
    }

    public static long scaledPeriod() {
        return 50 * 1_000_000L;
    }
}
