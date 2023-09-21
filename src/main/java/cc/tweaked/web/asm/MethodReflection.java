package cc.tweaked.web.asm;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.PeripheralType;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.NamedMethod;
import org.teavm.metaprogramming.*;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

@CompileTime
public class MethodReflection {
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

        for (var method : Internal.getMethods(actualClass)) {
            String name = method.name();
            boolean nonYielding = method.nonYielding();
            var actualField = method.method().getField("INSTANCE");

            Metaprogramming.emit(() -> make.get().make(name, (LuaMethod) actualField.get(null), nonYielding));
        }
    }

    public interface MakeMethod {
        void make(String name, LuaMethod method, boolean nonYielding);
    }

    private static class Internal {
        private static final LoadingCache<Class<?>, List<NamedMethod<ReflectClass<LuaMethod>>>> CLASS_CACHE = CacheBuilder
            .newBuilder()
            .build(CacheLoader.from(Internal::getMethodsImpl));

        private static final Generator<LuaMethod> GENERATOR = new Generator<>(LuaMethod.class, Collections.singletonList(ILuaContext.class));

        static List<NamedMethod<ReflectClass<LuaMethod>>> getMethods(Class<?> klass) {
            try {
                return CLASS_CACHE.get(klass);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        private static List<NamedMethod<ReflectClass<LuaMethod>>> getMethodsImpl(Class<?> klass) {
            ArrayList<NamedMethod<ReflectClass<LuaMethod>>> methods = null;

            // Find all methods on the current class
            for (var method : klass.getMethods()) {
                var annotation = method.getAnnotation(LuaFunction.class);
                if (annotation == null) continue;

                if (Modifier.isStatic(method.getModifiers())) {
                    System.err.printf("LuaFunction method %s.%s should be an instance method.\n", method.getDeclaringClass(), method.getName());
                    continue;
                }

                var instance = GENERATOR.getMethod(method).orElse(null);
                if (instance == null) continue;

                if (methods == null) methods = new ArrayList<>();
                addMethod(methods, method, annotation, null, instance);
            }

            if (methods == null) return List.of();
            methods.trimToSize();
            return Collections.unmodifiableList(methods);
        }

        private static void addMethod(List<NamedMethod<ReflectClass<LuaMethod>>> methods, Method method, LuaFunction annotation, @Nullable PeripheralType genericType, ReflectClass<LuaMethod> instance) {
            var names = annotation.value();
            var isSimple = method.getReturnType() != MethodResult.class && !annotation.mainThread();
            if (names.length == 0) {
                methods.add(new NamedMethod<>(method.getName(), instance, isSimple, genericType));
            } else {
                for (var name : names) {
                    methods.add(new NamedMethod<>(name, instance, isSimple, genericType));
                }
            }
        }
    }
}