package dan200.computercraft.core.asm;

import cc.tweaked.web.asm.MethodReflection;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.ObjectSource;

import java.util.List;

/**
 * A {@link MethodSupplier} for {@link LuaMethod}s, which uses {@link MethodReflection} to fabricate the classes.
 */
public class LuaMethodSupplier implements MethodSupplier<LuaMethod> {
    static final LuaMethodSupplier INSTANCE = new LuaMethodSupplier();

    private LuaMethodSupplier() {
    }

    @Override
    public boolean forEachSelfMethod(Object object, UntargetedConsumer<LuaMethod> consumer) {
        var methods = MethodReflection.getMethods(object.getClass());
        for (var method : methods) consumer.accept(method.name(), method.method(), method);

        return !methods.isEmpty();
    }

    @Override
    public boolean forEachMethod(Object object, TargetedConsumer<LuaMethod> consumer) {
        var methods = MethodReflection.getMethods(object.getClass());
        for (var method : methods) consumer.accept(object, method.name(), method.method(), method);

        var hasMethods = !methods.isEmpty();

        if (object instanceof ObjectSource source) {
            for (var extra : source.getExtra()) {
                var extraMethods = MethodReflection.getMethods(extra.getClass());
                if (!extraMethods.isEmpty()) hasMethods = true;
                for (var method : extraMethods) consumer.accept(extra, method.name(), method.method(), method);
            }
        }

        return hasMethods;
    }

    public static MethodSupplier<LuaMethod> create(List<GenericMethod> genericMethods) {
        return LuaMethodSupplier.INSTANCE;
    }
}
