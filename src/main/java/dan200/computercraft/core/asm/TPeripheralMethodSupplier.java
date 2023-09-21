package dan200.computercraft.core.asm;

import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;

import java.util.List;

/**
 * A {@link MethodSupplier} implementation which lifts {@link LuaMethod} to {@link PeripheralMethod}.
 */
public class TPeripheralMethodSupplier implements MethodSupplier<PeripheralMethod> {
    static final TPeripheralMethodSupplier INSTANCE = new TPeripheralMethodSupplier();

    private TPeripheralMethodSupplier() {
    }

    @Override
    public boolean forEachSelfMethod(Object object, UntargetedConsumer<PeripheralMethod> consumer) {
        return TLuaMethodSupplier.INSTANCE.forEachSelfMethod(object, (name, method, info) -> consumer.accept(name, cast(method), null));
    }

    @Override
    public boolean forEachMethod(Object object, TargetedConsumer<PeripheralMethod> consumer) {
        return TLuaMethodSupplier.INSTANCE.forEachMethod(object, (target, name, method, info) -> consumer.accept(target, name, cast(method), null));
    }

    private static PeripheralMethod cast(LuaMethod method) {
        return (target, context, computer, args) -> method.apply(target, context, args);
    }

    public static MethodSupplier<PeripheralMethod> create(List<GenericMethod> genericMethods) {
        return INSTANCE;
    }
}
