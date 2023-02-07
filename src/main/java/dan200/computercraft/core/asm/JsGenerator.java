package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.ILuaContext;
import org.teavm.metaprogramming.*;
import org.teavm.metaprogramming.reflect.ReflectMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CompileTime
public class JsGenerator {
    public static List<NamedMethod<LuaMethod>> getMethods(Class<?> klass) {
        List<NamedMethod<LuaMethod>> out = new ArrayList<>();
        getMethodsImpl(klass, (name, method, nonYielding) -> out.add(new NamedMethod<>(name, method, nonYielding, null)));
        return out;
    }

    @Meta
    private static native void getMethodsImpl(Class<?> type, MakeMethod make);

    private static void getMethodsImpl(ReflectClass<Object> klass, Value<MakeMethod> make) {
        if (!klass.getName().startsWith("dan200.computercraft.") && !klass.getName().startsWith("cc.tweaked.web.peripheral")) {
            return;
        }
        if (klass.getName().contains("lambda")) return;

        Class<?> actualClass;
        try {
            actualClass = Metaprogramming.getClassLoader().loadClass(klass.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        for (var method : Internal.LUA_METHOD.getMethods(actualClass)) {
            String name = method.getName();
            boolean nonYielding = method.nonYielding();
            ReflectMethod actualMethod = method.getMethod().getMethod("<init>");

            Metaprogramming.emit(() -> make.get().make(name, (LuaMethod) actualMethod.construct(), nonYielding));
        }
    }

    public interface MakeMethod {
        void make(String name, LuaMethod method, boolean nonYielding);
    }

    private static class Internal {
        public static final Generator<LuaMethod> LUA_METHOD = new Generator<>(
            LuaMethod.class, Collections.singletonList(ILuaContext.class),
            m -> (target, context, args) -> context.executeMainThreadTask(() -> ResultHelpers.checkNormalResult(m.apply(target, context, args)))
        );
    }
}
