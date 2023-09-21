package cc.tweaked.builder;

import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.vm.TeaVMOptimizationLevel;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public class Builder {
    public static void main(String[] args) throws Exception {
        try (var scope = new CloseScope()) {
            run(scope);
        }
    }

    private static void run(CloseScope scope) throws Exception {
        var input = getPath(scope, "cct.input");
        var classpath = getPath(scope, "cct.classpath");
        var output = getFile("cct.output");

        var remapper = new RemappingClassLoader(classpath);
        // Really we should add all of these to TeaVM, but our current implementations are a bit of a hack.
        remapper.add("java/nio/channels/Channels", "cc/tweaked/web/stub/Channels");
        remapper.add("java/nio/channels/FileChannel", "cc/tweaked/web/stub/FileChannel");
        remapper.add("java/util/concurrent/locks/ReentrantLock", "cc/tweaked/web/stub/ReentrantLock");

        for (var file : input) {
            traverseClasses(file, (fullName, path) -> {
                var lastPart = fullName.lastIndexOf('/');
                var className = fullName.substring(lastPart + 1);
                if (className.startsWith("T") && Character.isUpperCase(className.charAt(1))) {
                    var originalName = fullName.substring(0, lastPart + 1) + className.substring(1);
                    System.out.printf("Replacing %s with %s\n", originalName, fullName);
                    remapper.add(fullName, originalName, path);
                }
            });
        }

        TeaVMTool tool = new TeaVMTool();
        tool.setTargetType(TeaVMTargetType.JAVASCRIPT);
        tool.setTargetDirectory(output.getParent().toFile());
        tool.setTargetFileName(output.getFileName().toString());
        tool.setClassLoader(remapper);
        tool.setMainClass("cc.tweaked.web.Main");

        tool.setOptimizationLevel(TeaVMOptimizationLevel.ADVANCED);
        tool.setObfuscated(true);

        tool.generate();
        TeaVMProblemRenderer.describeProblems(tool.getDependencyInfo().getCallGraph(), tool.getProblemProvider(), new ConsoleTeaVMToolLog(false));
        if (!tool.getProblemProvider().getSevereProblems().isEmpty()) System.exit(1);
    }

    private static void traverseClasses(Path root, BiConsumer<String, Path> child) throws IOException {
        try (var walker = Files.walk(root)) {
            walker.forEach(entry -> {
                if (Files.isDirectory(entry)) return;

                var name = root.relativize(entry).toString();
                if (!name.endsWith(".class")) return;

                var className = name.substring(0, name.length() - 6).replace(File.separatorChar, '/');

                child.accept(className, entry);
            });
        }
    }

    private static List<Path> getPath(CloseScope scope, String name) {
        return Arrays.stream(System.getProperty(name).split(File.pathSeparator))
            .map(x -> {
                var file = Path.of(x);
                try {
                    return Files.isDirectory(file) ? file : scope.add(FileSystems.newFileSystem(file)).getPath("/");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .toList();
    }

    private static Path getFile(String name) {
        return Path.of(System.getProperty(name));
    }
}
