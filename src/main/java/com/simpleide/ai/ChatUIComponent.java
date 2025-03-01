package com.simpleide.ai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import com.simpleide.ui.MainFrame;

public class ChatUIComponent extends JPanel {
    private JScrollPane scrollPane;
    private JPanel messagePanel;
    private boolean isDragging = false;
    private Point lastMousePosition;
    private MainFrame mainFrame;
    private String projectRoot;
    
    public ChatUIComponent(MainFrame mainFrame, String projectRoot) {
        this.mainFrame = mainFrame;
        this.projectRoot = projectRoot;
        setLayout(new BorderLayout());
        
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        scrollPane = new JScrollPane(messagePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // 添加鼠标事件监听器实现拖动
        scrollPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isDragging = true;
                lastMousePosition = e.getPoint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });
        
        scrollPane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    Point currentPoint = e.getPoint();
                    JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
                    int delta = lastMousePosition.y - currentPoint.y;
                    verticalScrollBar.setValue(verticalScrollBar.getValue() + delta);
                    lastMousePosition = currentPoint;
                }
            }
        });
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void addMessage(String content, boolean isAI) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 设置背景色
        messagePanel.setBackground(isAI ? new Color(240, 240, 255) : new Color(255, 240, 240));
        
        String[] blocks = content.split("```");
        
        // 添加第一个文本块（如果存在）
        if (blocks.length > 0 && !blocks[0].trim().isEmpty()) {
            addTextBlock(messagePanel, blocks[0].trim());
        }
        
        // 处理代码块
        for (int i = 1; i < blocks.length; i += 2) {
            if (i < blocks.length) {
                String block = blocks[i];
                if (block.startsWith("java:")) {
                    addCodeBlock(messagePanel, block);
                }
                
                // 添加代码块之后的文本（如果存在）
                if (i + 1 < blocks.length && !blocks[i + 1].trim().isEmpty()) {
                    addTextBlock(messagePanel, blocks[i + 1].trim());
                }
            }
        }
        
        add(messagePanel);
        revalidate();
        repaint();
        
        // 滚动到底部
        scrollToBottom();
    }
    
    private void addTextBlock(JPanel parent, String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(new Color(250, 250, 250));
        textArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 计算首选高度
        int preferredHeight = Math.min(200, textArea.getPreferredSize().height);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, preferredHeight));
        
        parent.add(scrollPane);
        parent.add(Box.createVerticalStrut(10));
    }
    
    private void addCodeBlock(JPanel parent, String block) {
        String[] parts = block.split("\n", 2);
        String filePath = parts[0].substring(5).trim();
        String code = parts[1].trim();
        
        // 创建代码块面板
        JPanel codeBlockPanel = new JPanel(new BorderLayout());
        codeBlockPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createTitledBorder(filePath)
        ));
        
        // 代码预览区域
        RSyntaxTextArea previewArea = new RSyntaxTextArea(8, 60);
        previewArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        previewArea.setEditable(false);
        previewArea.setText(code);
        RTextScrollPane previewScroll = new RTextScrollPane(previewArea);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton previewButton = new JButton("预览修改");
        JButton applyButton = new JButton("应用修改");
        
        // 设置按钮样式
        previewButton.setBackground(new Color(230, 230, 255));
        applyButton.setBackground(new Color(200, 255, 200));
        
        // 添加按钮事件
        previewButton.addActionListener(e -> showPreviewDialog(filePath, code));
        applyButton.addActionListener(e -> applyCodeModification(filePath, code));
        
        buttonPanel.add(previewButton);
        buttonPanel.add(applyButton);
        
        codeBlockPanel.add(previewScroll, BorderLayout.CENTER);
        codeBlockPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        parent.add(codeBlockPanel);
        parent.add(Box.createVerticalStrut(10));
    }
    
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            Container parent = getParent();
            while (parent != null && !(parent instanceof JScrollPane)) {
                parent = parent.getParent();
            }
            if (parent instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) parent;
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
    }
    
    private void showPreviewDialog(String filePath, String code) {
        JDialog previewDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "代码预览 - " + filePath);
        previewDialog.setModal(true);
        previewDialog.setLayout(new BorderLayout());
        
        // 创建代码预览区域
        RSyntaxTextArea previewArea = new RSyntaxTextArea(20, 80);
        previewArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        previewArea.setEditable(false);
        previewArea.setText(code);
        RTextScrollPane scrollPane = new RTextScrollPane(previewArea);
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("应用修改");
        JButton closeButton = new JButton("关闭");
        
        applyButton.setBackground(new Color(200, 255, 200));
        
        applyButton.addActionListener(e -> {
            applyCodeModification(filePath, code);
            previewDialog.dispose();
        });
        closeButton.addActionListener(e -> previewDialog.dispose());
        
        buttonPanel.add(applyButton);
        buttonPanel.add(closeButton);
        
        previewDialog.add(scrollPane, BorderLayout.CENTER);
        previewDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        previewDialog.setSize(800, 600);
        previewDialog.setLocationRelativeTo(this);
        previewDialog.setVisible(true);
    }
    
    private void applyCodeModification(String filePath, String code) {
        try {
            File file = new File(projectRoot, filePath);
            
            // 如果文件不存在，创建新文件
            if (!file.exists()) {
                // 确保目录存在
                file.getParentFile().mkdirs();
                
                // 创建新文件
                if (file.createNewFile()) {
                    Files.write(file.toPath(), code.getBytes());
                    JOptionPane.showMessageDialog(this,
                        "已创建新文件: " + filePath,
                        "创建成功",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    throw new IOException("无法创建文件");
                }
            } else {
                // 如果文件存在，提示用户确认覆盖
                int option = JOptionPane.showConfirmDialog(this,
                    "文件 " + filePath + " 已存在，是否覆盖？",
                    "确认覆盖",
                    JOptionPane.YES_NO_OPTION);
                
                if (option == JOptionPane.YES_OPTION) {
                    Files.write(file.toPath(), code.getBytes());
                    JOptionPane.showMessageDialog(this,
                        "已更新文件: " + filePath,
                        "更新成功",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
            
            // 刷新项目树
            refreshProjectTree();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "应用代码失败: " + e.getMessage(),
                "错误",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refreshProjectTree() {
        mainFrame.refreshProject();
    }
} 