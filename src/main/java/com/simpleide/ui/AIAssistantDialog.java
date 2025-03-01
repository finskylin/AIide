package com.simpleide.ui;

import com.simpleide.ai.DeepseekService;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class AIAssistantDialog extends JDialog {
    private JTextArea chatHistory;
    private JTextArea inputArea;
    private DeepseekService aiService;
    private MainFrame mainFrame;

    public AIAssistantDialog(MainFrame parent, String apiKey) {
        super(parent, "AI编程助手", false);
        this.mainFrame = parent;
        this.aiService = new DeepseekService();
        initComponents();
        setSize(600, 800);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // 聊天历史区域
        chatHistory = new JTextArea();
        chatHistory.setEditable(false);
        chatHistory.setLineWrap(true);
        chatHistory.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatHistory);

        // 输入区域
        inputArea = new JTextArea(5, 40);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        JScrollPane inputScrollPane = new JScrollPane(inputArea);

        // 发送按钮
        JButton sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendMessage());

        // 快捷操作按钮面板
        JPanel quickActionsPanel = new JPanel();
        quickActionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        addQuickActionButton(quickActionsPanel, "生成类", "请帮我生成一个Java类");
        addQuickActionButton(quickActionsPanel, "修复错误", "请帮我修复代码中的错误");
        addQuickActionButton(quickActionsPanel, "优化代码", "请帮我优化这段代码");

        // 底部面板
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputScrollPane, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        // 添加组件
        add(quickActionsPanel, BorderLayout.NORTH);
        add(chatScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addQuickActionButton(JPanel panel, String text, String prompt) {
        JButton button = new JButton(text);
        button.addActionListener(e -> {
            String currentCode = mainFrame.getCurrentEditorText();
            inputArea.setText(prompt + "\n\n" + currentCode);
        });
        panel.add(button);
    }

    private void sendMessage() {
        String message = inputArea.getText();
        if (message.trim().isEmpty()) {
            return;
        }

        chatHistory.append("You: " + message + "\n\n");
        inputArea.setText("");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return aiService.getAIResponse(message);
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    chatHistory.append("AI: " + response + "\n\n");
                } catch (Exception e) {
                    chatHistory.append("Error: " + e.getMessage() + "\n\n");
                }
            }
        }.execute();
    }
} 