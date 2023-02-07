package cc.tweaked.web.asm;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.asm.LuaMethod;
import dan200.computercraft.core.asm.NamedMethod;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import cc.tweaked.web.stub.Logger;
import cc.tweaked.web.stub.LoggerFactory;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.metaprogramming.ReflectClass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

@CompileTime
public final class ClassGenerator<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ClassGenerator.class);
    private static final AtomicInteger METHOD_ID = new AtomicInteger();

    private static final String METHOD_NAME = "apply";
    private static final String[] EXCEPTIONS = new String[]{Type.getInternalName(LuaException.class)};

    private static final String INTERNAL_METHOD_RESULT = Type.getInternalName(MethodResult.class);
    private static final String DESC_METHOD_RESULT = Type.getDescriptor(MethodResult.class);

    private static final String INTERNAL_ARGUMENTS = Type.getInternalName(IArguments.class);
    private static final String DESC_ARGUMENTS = Type.getDescriptor(IArguments.class);

    // Needs to be last as it depends on above state.
    public static final ClassGenerator<LuaMethod> LUA_METHOD = new ClassGenerator<>(LuaMethod.class, Collections.singletonList(ILuaContext.class));

    private final Class<T> base;
    private final List<Class<?>> context;

    private final String[] interfaces;
    private final String methodDesc;

    private final LoadingCache<Class<?>, List<NamedMethod<ReflectClass<T>>>> classCache = CacheBuilder
        .newBuilder()
        .build(CacheLoader.from(this::build));

    private final LoadingCache<Method, Optional<ReflectClass<T>>> methodCache = CacheBuilder
        .newBuilder()
        .build(CacheLoader.from(this::build));

    ClassGenerator(Class<T> base, List<Class<?>> context) {
        this.base = base;
        this.context = context;
        this.interfaces = new String[]{Type.getInternalName(base)};

        StringBuilder methodDesc = new StringBuilder().append("(Ljava/lang/Object;");
        for (Class<?> klass : context) methodDesc.append(Type.getDescriptor(klass));
        methodDesc.append(DESC_ARGUMENTS).append(")").append(DESC_METHOD_RESULT);
        this.methodDesc = methodDesc.toString();
    }

    @Nonnull
    public List<NamedMethod<ReflectClass<T>>> getMethods(@Nonnull Class<?> klass) {
        try {
            return classCache.get(klass);
        } catch (ExecutionException e) {
            LOG.error("Error getting methods for {}.", klass.getName(), e.getCause());
            return Collections.emptyList();
        }
    }

    @Nonnull
    private List<NamedMethod<ReflectClass<T>>> build(Class<?> klass) {
        LOG.warn("Loading methods from {}.", klass.getName());

        ArrayList<NamedMethod<ReflectClass<T>>> methods = null;
        for (Method method : klass.getMethods()) {
            LuaFunction annotation = method.getAnnotation(LuaFunction.class);
            if (annotation == null) continue;

            if (Modifier.isStatic(method.getModifiers())) {
                LOG.warn("LuaFunction method {}.{} should be an instance method.", method.getDeclaringClass(), method.getName());
                continue;
            }

            ReflectClass<T> instance = methodCache.getUnchecked(method).orElse(null);
            if (instance == null) continue;

            if (methods == null) methods = new ArrayList<>();
            addMethod(methods, method, annotation, instance);
        }

        if (methods == null) return Collections.emptyList();
        methods.trimToSize();
        return Collections.unmodifiableList(methods);
    }

    private void addMethod(List<NamedMethod<ReflectClass<T>>> methods, Method method, LuaFunction annotation, ReflectClass<T> instance) {
        String[] names = annotation.value();
        boolean isSimple = method.getReturnType() != MethodResult.class && !annotation.mainThread();
        if (names.length == 0) {
            methods.add(new NamedMethod<>(method.getName(), instance, isSimple, null));
        } else {
            for (String name : names) {
                methods.add(new NamedMethod<>(name, instance, isSimple, null));
            }
        }
    }

    @Nonnull
    private Optional<ReflectClass<T>> build(Method method) {
        String name = method.getDeclaringClass().getName() + "." + method.getName();
        int modifiers = method.getModifiers();

        // Instance methods must be final - this prevents them being overridden and potentially exposed twice.
        if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
            LOG.warn("Lua Method {} should be final.", name);
        }

        if (!Modifier.isPublic(modifiers)) {
            LOG.error("Lua Method {} should be a public method.", name);
            return Optional.empty();
        }

        if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            LOG.error("Lua Method {} should be on a public class.", name);
            return Optional.empty();
        }

        LOG.debug("Generating method wrapper for {}.", name);

        Class<?>[] exceptions = method.getExceptionTypes();
        for (Class<?> exception : exceptions) {
            if (exception != LuaException.class) {
                LOG.error("Lua Method {} cannot throw {}.", name, exception.getName());
                return Optional.empty();
            }
        }

        LuaFunction annotation = method.getAnnotation(LuaFunction.class);
        if (annotation.unsafe() && annotation.mainThread()) {
            LOG.error("Lua Method {} cannot use unsafe and mainThread", name);
            return Optional.empty();
        }

        // We have some rather ugly handling of static methods in both here and the main generate function. Static methods
        // only come from generic sources, so this should be safe.
        Class<?> target = Modifier.isStatic(modifiers) ? method.getParameterTypes()[0] : method.getDeclaringClass();

        try {
            String className = method.getDeclaringClass().getName() + "$cc$" + method.getName() + METHOD_ID.getAndIncrement();
            byte[] bytes = generate(className, target, method, annotation.unsafe());
            if (bytes == null) return Optional.empty();

            ReflectClass<T> klass = Metaprogramming.createClass(bytes).asSubclass(base);
            return Optional.of(klass);
        } catch (ClassFormatError | RuntimeException e) {
            LOG.error("Error generating wrapper for {}.", name, e);
            return Optional.empty();
        }
    }

    @Nullable
    private byte[] generate(String className, Class<?> target, Method method, boolean unsafe) {
        String internalName = className.replace(".", "/");

        // Construct a public final class which extends Object and implements MethodInstance.Delegate
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, internalName, null, "java/lang/Object", interfaces);
        cw.visitSource("CC generated method", null);

        { // Constructor just invokes super.
            MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mw.visitCode();
            mw.visitVarInsn(ALOAD, 0);
            mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mw.visitInsn(RETURN);
            mw.visitMaxs(0, 0);
            mw.visitEnd();
        }

        {
            MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, METHOD_NAME, methodDesc, null, EXCEPTIONS);
            mw.visitCode();

            // If we're an instance method, load the this parameter.
            if (!Modifier.isStatic(method.getModifiers())) {
                mw.visitVarInsn(ALOAD, 1);
                mw.visitTypeInsn(CHECKCAST, Type.getInternalName(target));
            }

            int argIndex = 0;
            for (java.lang.reflect.Type genericArg : method.getGenericParameterTypes()) {
                Boolean loadedArg = loadArg(mw, target, method, unsafe, genericArg, argIndex);
                if (loadedArg == null) return null;
                if (loadedArg) argIndex++;
            }

            mw.visitMethodInsn(
                Modifier.isStatic(method.getModifiers()) ? INVOKESTATIC : INVOKEVIRTUAL,
                Type.getInternalName(method.getDeclaringClass()), method.getName(),
                Type.getMethodDescriptor(method), false
            );

            // We allow a reasonable amount of flexibility on the return value's type. Alongside the obvious MethodResult,
            // we convert basic types into an immediate result.
            Class<?> ret = method.getReturnType();
            if (ret != MethodResult.class) {
                if (ret == void.class) {
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "()" + DESC_METHOD_RESULT, false);
                } else if (ret.isPrimitive()) {
                    Class<?> boxed = Primitives.wrap(ret);
                    mw.visitMethodInsn(INVOKESTATIC, Type.getInternalName(boxed), "valueOf", "(" + Type.getDescriptor(ret) + ")" + Type.getDescriptor(boxed), false);
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "(Ljava/lang/Object;)" + DESC_METHOD_RESULT, false);
                } else if (ret == Object[].class) {
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "([Ljava/lang/Object;)" + DESC_METHOD_RESULT, false);
                } else {
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "(Ljava/lang/Object;)" + DESC_METHOD_RESULT, false);
                }
            }

            mw.visitInsn(ARETURN);

            mw.visitMaxs(0, 0);
            mw.visitEnd();
        }

        cw.visitEnd();

        return cw.toByteArray();
    }

    private Boolean loadArg(MethodVisitor mw, Class<?> target, Method method, boolean unsafe, java.lang.reflect.Type genericArg, int argIndex) {
        if (genericArg == target) {
            mw.visitVarInsn(ALOAD, 1);
            mw.visitTypeInsn(CHECKCAST, Type.getInternalName(target));
            return false;
        }

        Class<?> arg = Reflect.getRawType(method, genericArg, true);
        if (arg == null) return null;

        if (arg == IArguments.class) {
            mw.visitVarInsn(ALOAD, 2 + context.size());
            return false;
        }

        int idx = context.indexOf(arg);
        if (idx >= 0) {
            mw.visitVarInsn(ALOAD, 2 + idx);
            return false;
        }

        if (arg == Optional.class) {
            Class<?> klass = Reflect.getRawType(method, TypeToken.of(genericArg).resolveType(Reflect.OPTIONAL_IN).getType(), false);
            if (klass == null) return null;

            if (Enum.class.isAssignableFrom(klass) && klass != Enum.class) {
                mw.visitVarInsn(ALOAD, 2 + context.size());
                Reflect.loadInt(mw, argIndex);
                mw.visitLdcInsn(Type.getType(klass));
                mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "optEnum", "(ILjava/lang/Class;)Ljava/util/Optional;", true);
                return true;
            }

            String name = Reflect.getLuaName(Primitives.unwrap(klass), unsafe);
            if (name != null) {
                mw.visitVarInsn(ALOAD, 2 + context.size());
                Reflect.loadInt(mw, argIndex);
                mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "opt" + name, "(I)Ljava/util/Optional;", true);
                return true;
            }
        }

        if (Enum.class.isAssignableFrom(arg) && arg != Enum.class) {
            mw.visitVarInsn(ALOAD, 2 + context.size());
            Reflect.loadInt(mw, argIndex);
            mw.visitLdcInsn(Type.getType(arg));
            mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "getEnum", "(ILjava/lang/Class;)Ljava/lang/Enum;", true);
            mw.visitTypeInsn(CHECKCAST, Type.getInternalName(arg));
            return true;
        }

        String name = arg == Object.class ? "" : Reflect.getLuaName(arg, unsafe);
        if (name != null) {
            if (Reflect.getRawType(method, genericArg, false) == null) return null;

            mw.visitVarInsn(ALOAD, 2 + context.size());
            Reflect.loadInt(mw, argIndex);
            mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "get" + name, "(I)" + Type.getDescriptor(arg), true);
            return true;
        }

        LOG.error("Unknown parameter type {} for method {}.{}.",
            arg.getName(), method.getDeclaringClass().getName(), method.getName());
        return null;
    }
}

