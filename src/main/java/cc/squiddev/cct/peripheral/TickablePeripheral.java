package cc.squiddev.cct.peripheral;

import dan200.computercraft.api.peripheral.IPeripheral;

public interface TickablePeripheral extends IPeripheral {
    void tick();
}
