package com.simpleide.compiler;

import javax.tools.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

public class CompilationManager {
    private final String projectRoot;
    private final JavaCompiler compiler;
    private final StandardJavaFileManager fileManager;
    private final List<String> compilationErrors;
    private final String javaHome;

    public CompilationManager(String projectRoot) {
        this.projectRoot = projectRoot;
        this.javaHome = System.getProperty("java.home");
        this.compiler = findJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No Java compiler found. Please ensure JDK is installed and JAVA_HOME is set correctly.");
        }
        this.fileManager = compiler.getStandardFileManager(null, null, null);
        this.compilationErrors = new ArrayList<>();
    }

    private JavaCompiler findJavaCompiler() {
        // 首先尝试系统默认的编译器
        JavaCompiler systemCompiler = ToolProvider.getSystemJavaCompiler();
        if (systemCompiler != null) {
            return systemCompiler;
        }

        // 如果系统默认编译器不可用，尝试从 JAVA_HOME 环境变量获取
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            File toolsJar = new File(javaHome, "lib/tools.jar");
            if (toolsJar.exists()) {
                try {
                    URLClassLoader loader = URLClassLoader.newInstance(
                        new URL[] { toolsJar.toURI().toURL() },
                        ClassLoader.getSystemClassLoader()
                    );
                    Class<?> compilerClass = Class.forName("com.sun.tools.javac.api.JavacTool", true, loader);
                    return (JavaCompiler) compilerClass.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    public boolean compile(File sourceFile) throws IOException {
        compilationErrors.clear();
        
        if (sourceFile == null || !sourceFile.exists()) {
            compilationErrors.add("Source file does not exist: " + (sourceFile != null ? sourceFile.getPath() : "null"));
            return false;
        }

        if (!sourceFile.getName().endsWith(".java")) {
            compilationErrors.add("Not a Java source file: " + sourceFile.getName());
            return false;
        }

        try {
            // 确保输出目录存在
            Path classesDir = Paths.get(projectRoot, "target", "classes");
            Files.createDirectories(classesDir);
            
            // 确保源代码目录结构正确
            Path sourcePath = Paths.get(projectRoot, "src", "main", "java");
            Files.createDirectories(sourcePath);

            // 设置编译选项
            List<String> options = new ArrayList<>();
            options.add("-d");
            options.add(classesDir.toString());
            
            // 添加源代码路径
            options.add("-sourcepath");
            options.add(sourcePath.toString());
            
            // 添加类路径
            options.add("-classpath");
            options.add(System.getProperty("java.class.path") + File.pathSeparator + classesDir.toString());

            // 获取源文件
            Iterable<? extends JavaFileObject> compilationUnits = 
                fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(sourceFile));

            // 创建诊断收集器
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            // 创建编译任务
            JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                options,
                null,
                compilationUnits
            );

            // 执行编译
            boolean success = task.call();

            // 收集编译错误
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    String error = String.format("Error on line %d in %s: %s",
                        diagnostic.getLineNumber(),
                        diagnostic.getSource().toUri(),
                        diagnostic.getMessage(null));
                    compilationErrors.add(error);
                }
            }

            return success;
        } catch (Exception e) {
            compilationErrors.add("Compilation failed: " + e.getMessage());
            return false;
        }
    }

    public boolean compileProject() throws IOException {
        compilationErrors.clear();
        
        // 获取所有Java源文件
        List<File> sourceFiles = new ArrayList<>();
        collectJavaFiles(new File(projectRoot, "src"), sourceFiles);

        if (sourceFiles.isEmpty()) {
            compilationErrors.add("No source files found in " + projectRoot);
            return false;
        }

        try {
            // 获取源文件对象
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);

            // 设置编译选项
            List<String> options = new ArrayList<>();
            options.add("-d");
            options.add(projectRoot + File.separator + "target/classes");
            
            // 添加源代码路径
            options.add("-sourcepath");
            options.add(Paths.get(projectRoot, "src", "main", "java").toString());

            // 确保输出目录存在
            Files.createDirectories(Paths.get(projectRoot, "target", "classes"));

            // 创建诊断收集器
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            // 创建编译任务
            JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                options,
                null,
                compilationUnits
            );

            // 执行编译
            boolean success = task.call();

            // 收集编译错误
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    String error = String.format("Error on line %d in %s: %s",
                        diagnostic.getLineNumber(),
                        diagnostic.getSource().toUri(),
                        diagnostic.getMessage(null));
                    compilationErrors.add(error);
                }
            }

            return success;
        } catch (Exception e) {
            compilationErrors.add("Project compilation failed: " + e.getMessage());
            return false;
        }
    }

    private void collectJavaFiles(File directory, List<File> files) {
        if (directory.exists() && directory.isDirectory()) {
            File[] entries = directory.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    if (entry.isDirectory()) {
                        collectJavaFiles(entry, files);
                    } else if (entry.getName().endsWith(".java")) {
                        files.add(entry);
                    }
                }
            }
        }
    }

    public List<String> getCompilationErrors() {
        return new ArrayList<>(compilationErrors);
    }

    public String getCompiledClassPath(String className) {
        return Paths.get(projectRoot, "target", "classes", className.replace('.', '/') + ".class").toString();
    }

    public boolean runClass(String className) {
        try {
            String classPath = projectRoot + File.separator + "target/classes";
            ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-cp",
                classPath,
                className
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            
            return process.waitFor() == 0;
        } catch (Exception e) {
            System.err.println("Error running class: " + e.getMessage());
            return false;
        }
    }
} 