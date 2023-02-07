package cc.tweaked.web;

import cc.tweaked.web.js.Callbacks;
import cc.tweaked.web.js.Callbacks.Callback;
import dan200.computercraft.core.computer.ComputerExecutor;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

/**
 * We've no threads in JS land, so we create a fake version which just executes
 * all the work a computer can do.
 */
public class ComputerThread {
    public static final ComputerThread INSTANCE = new ComputerThread();

    private static final ArrayDeque<ComputerExecutor> executors = new ArrayDeque<>();
    private final Callback CALLBACK = this::workOnce;

    public void queue(ComputerExecutor executor) {
        if (executor.onComputerQueue) throw new IllegalStateException("Cannot queue already queued executor");
        executor.onComputerQueue = true;

        if (executors.isEmpty()) Callbacks.setImmediate(CALLBACK);
        executors.add(executor);
    }

    private void workOnce() {
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

    public boolean hasPendingWork() {
        return true;
    }

    public long scaledPeriod() {
        return 50 * 1_000_000L;
    }

    public boolean stop(long timeout, TimeUnit unit) {
        return true;
    }
}
