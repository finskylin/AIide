package com.simpleide.compiler;

import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class CompilationManagerTest {
    private CompilationManager compilationManager;
    private String projectRoot;
    private Path testSourcePath;

    @Before
    public void setUp() throws Exception {
        // 使用临时目录作为项目根目录
        projectRoot = System.getProperty("java.io.tmpdir") + "/testProject";
        new File(projectRoot).mkdirs();
        
        // 创建源代码目录
        testSourcePath = Paths.get(projectRoot, "src", "main", "java");
        Files.createDirectories(testSourcePath);
        
        compilationManager = new CompilationManager(projectRoot);
    }

    @Test
    public void testCompileSingleFile() throws Exception {
        // 创建测试Java文件
        String className = "TestClass";
        String sourceCode = 
            "public class " + className + " {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, World!\");\n" +
            "    }\n" +
            "}\n";
        
        Path sourcePath = testSourcePath.resolve(className + ".java");
        Files.write(sourcePath, sourceCode.getBytes());
        
        // 测试编译
        boolean success = compilationManager.compile(sourcePath.toFile());
        assertTrue("Compilation should succeed", success);
        assertTrue("Compilation errors should be empty", compilationManager.getCompilationErrors().isEmpty());
        
        // 验证生成的类文件
        Path classPath = Paths.get(projectRoot, "target", "classes", className + ".class");
        assertTrue("Class file should exist", Files.exists(classPath));
    }

    @Test
    public void testCompileInvalidFile() throws Exception {
        // 创建语法错误的Java文件
        String className = "InvalidClass";
        String sourceCode = 
            "public class " + className + " {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, World!\");\n" +
            "    }\n" + // 缺少闭合大括号
            "\n";
        
        Path sourcePath = testSourcePath.resolve(className + ".java");
        Files.write(sourcePath, sourceCode.getBytes());
        
        // 测试编译
        boolean success = compilationManager.compile(sourcePath.toFile());
        assertFalse("Compilation should fail", success);
        assertFalse("Compilation errors should not be empty", compilationManager.getCompilationErrors().isEmpty());
    }

    @Test
    public void testRunCompiledClass() throws Exception {
        // 创建并编译测试类
        String className = "RunTest";
        String sourceCode = 
            "public class " + className + " {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Test successful!\");\n" +
            "    }\n" +
            "}\n";
        
        Path sourcePath = testSourcePath.resolve(className + ".java");
        Files.write(sourcePath, sourceCode.getBytes());
        
        // 编译
        boolean compileSuccess = compilationManager.compile(sourcePath.toFile());
        assertTrue("Compilation should succeed", compileSuccess);
        
        // 运行
        boolean runSuccess = compilationManager.runClass(className);
        assertTrue("Class execution should succeed", runSuccess);
    }
} 