package com.simpleide;

import com.simpleide.ui.MainFrame;
import javax.swing.*;

public class SimpleIDE {
    public static void main(String[] args) {
        try {
            // 设置系统默认的Look and Feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
            ex.printStackTrace();
        }
        
        // 启动主界面
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
} 