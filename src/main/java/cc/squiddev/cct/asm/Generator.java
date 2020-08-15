package cc.squiddev.cct.asm;

import dan200.computercraft.core.asm.LuaMethod;
import dan200.computercraft.core.asm.NamedMethod;
import org.teavm.metaprogramming.*;
import org.teavm.metaprogramming.reflect.ReflectMethod;

import java.util.ArrayList;
import java.util.List;

@CompileTime
public class Generator {
    public static List<NamedMethod<LuaMethod>> getMethods(Class<?> klass) {
        List<NamedMethod<LuaMethod>> out = new ArrayList<>();
        getMethodsImpl(klass, (name, method, nonYielding) -> out.add(new NamedMethod<>(name, method, nonYielding)));
        return out;
    }

    @Meta
    private static native void getMethodsImpl(Class<?> type, MakeMethod make);


    private static void getMethodsImpl(ReflectClass<Object> klass, Value<MakeMethod> make) {
        if (!klass.getName().startsWith("dan200.computercraft.")) return;
        if (klass.getName().contains("lambda")) return;

        Class<?> actualClass;
        try {
            actualClass = Metaprogramming.getClassLoader().loadClass(klass.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        for (NamedMethod<ReflectClass<LuaMethod>> method : ClassGenerator.LUA_METHOD.getMethods(actualClass)) {
            String name = method.getName();
            boolean nonYielding = method.nonYielding();
            ReflectMethod actualMethod = method.getMethod().getMethod("<init>");

            Metaprogramming.emit(() -> make.get().make(name, (LuaMethod) actualMethod.construct(), nonYielding));
        }

    }

    public interface MakeMethod {
        void make(String name, LuaMethod method, boolean nonYielding);
    }
}
