package cc.squiddev.cct;

/**
 * Basic callback for terminals, which just checks
 */
public class TerminalMonitor implements Runnable {
    private boolean changed = true;

    @Override
    public void run() {
        changed = true;
    }

    public boolean pollChanged() {
        boolean changed = this.changed;
        this.changed = false;
        return changed;
    }
}
