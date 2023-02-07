package cc.tweaked.web.peripheral;

import dan200.computercraft.api.peripheral.IPeripheral;

public interface TickablePeripheral extends IPeripheral {
    void tick();
}
