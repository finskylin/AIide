package com.simpleide.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JTextArea;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LogManager {
    private static LogManager instance;
    private JTextArea consoleOutput;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();
    private final Thread logThread;
    private static final String ERROR_PREFIX = "[ERROR] ";
    private static final String INFO_PREFIX = "[INFO] ";
    private static final String DEBUG_PREFIX = "[DEBUG] ";
    private static final String AI_PREFIX = "[AI] ";
    
    private LogManager() {
        // 启动日志处理线程
        logThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    LogEntry entry = logQueue.take();
                    writeLog(entry);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        logThread.setDaemon(true);
        logThread.start();
    }
    
    public static LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }
    
    public void setConsoleOutput(JTextArea console) {
        this.consoleOutput = console;
    }
    
    public void error(String message) {
        log(ERROR_PREFIX + message, LogLevel.ERROR);
    }
    
    public void error(String message, Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(ERROR_PREFIX).append(message).append("\n");
        sb.append("Exception: ").append(e.getClass().getName()).append("\n");
        sb.append("Message: ").append(e.getMessage()).append("\n");
        sb.append("Stack trace:\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        log(sb.toString(), LogLevel.ERROR);
    }
    
    public void info(String message) {
        log(INFO_PREFIX + message, LogLevel.INFO);
    }
    
    public void debug(String message) {
        log(DEBUG_PREFIX + message, LogLevel.DEBUG);
    }
    
    public void aiLog(String message) {
        log(AI_PREFIX + message, LogLevel.INFO);
    }
    
    private void log(String message, LogLevel level) {
        String timestamp = dateFormat.format(new Date());
        logQueue.offer(new LogEntry(timestamp + " " + message, level));
    }
    
    private void writeLog(LogEntry entry) {
        if (consoleOutput != null) {
            try {
                String formattedMessage = entry.message + "\n";
                consoleOutput.append(formattedMessage);
                // 自动滚动到底部
                consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private static class LogEntry {
        final String message;
        final LogLevel level;
        
        LogEntry(String message, LogLevel level) {
            this.message = message;
            this.level = level;
        }
    }
    
    private enum LogLevel {
        ERROR, INFO, DEBUG
    }
} 