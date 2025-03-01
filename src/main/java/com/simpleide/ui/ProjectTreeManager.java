package com.simpleide.ui;

import javax.swing.*;
import javax.swing.tree.*;
import java.io.File;
import java.nio.file.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ProjectTreeManager {
    private JTree projectTree;
    private DefaultTreeModel treeModel;
    private String projectRoot;
    private ImageIcon folderIcon;
    private ImageIcon fileIcon;
    private ImageIcon javaIcon;
    private PropertyChangeSupport propertyChangeSupport;

    public ProjectTreeManager(JTree tree, String rootPath) {
        this.projectTree = tree;
        this.projectRoot = rootPath;
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        loadIcons();
        initTree();
        addTreePopupMenu();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    private void loadIcons() {
        // 加载图标
        folderIcon = new ImageIcon(getClass().getResource("/icons/folder.png"));
        if (folderIcon.getIconWidth() == -1) {
            // 如果无法加载图标，创建一个简单的彩色方块作为图标
            folderIcon = createColorIcon(new Color(255, 200, 100));
        }

        fileIcon = new ImageIcon(getClass().getResource("/icons/file.png"));
        if (fileIcon.getIconWidth() == -1) {
            fileIcon = createColorIcon(new Color(200, 200, 255));
        }

        // 为 Java 文件创建特殊图标
        javaIcon = createColorIcon(new Color(100, 200, 100));
    }

    private ImageIcon createColorIcon(Color color) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        
        // 设置抗锯齿
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 绘制背景
        g2.setColor(color);
        g2.fillRect(0, 0, 16, 16);
        
        // 如果是 Java 图标，添加字母 'J'
        if (color.equals(new Color(100, 200, 100))) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Dialog", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int x = (16 - fm.stringWidth("J")) / 2;
            int y = ((16 - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString("J", x, y);
        }
        
        g2.dispose();
        return new ImageIcon(image);
    }

    public void setProjectRoot(String rootPath) {
        this.projectRoot = rootPath;
        refreshTree();
    }

    private void initTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new FileNode(new File(projectRoot)));
        buildTree(root, new File(projectRoot));
        treeModel = new DefaultTreeModel(root);
        projectTree.setModel(treeModel);
        
        // 设置树的渲染器
        projectTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                    boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node.getUserObject() instanceof FileNode) {
                    FileNode fileNode = (FileNode) node.getUserObject();
                    File file = fileNode.getFile();
                    
                    if (file.isDirectory()) {
                        setIcon(folderIcon);
                    } else if (file.getName().endsWith(".java")) {
                        setIcon(javaIcon);
                    } else {
                        setIcon(fileIcon);
                    }
                }
                return this;
            }
        });
    }

    private static class FileNode {
        private File file;
        
        public FileNode(File file) {
            this.file = file;
        }
        
        public File getFile() {
            return file;
        }
        
        @Override
        public String toString() {
            return file.getName();
        }
    }

    private void buildTree(DefaultMutableTreeNode node, File file) {
        File[] files = file.listFiles();
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    if (f1.isDirectory() && !f2.isDirectory()) {
                        return -1;
                    } else if (!f1.isDirectory() && f2.isDirectory()) {
                        return 1;
                    } else {
                        return f1.getName().compareTo(f2.getName());
                    }
                }
            });
            
            for (File child : files) {
                if (child.isHidden() || shouldSkip(child.getName())) {
                    continue;
                }
                
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(child));
                node.add(childNode);
                
                if (child.isDirectory()) {
                    buildTree(childNode, child);
                }
            }
        }
    }

    private boolean shouldSkip(String name) {
        return name.equals("target") || 
               name.equals("bin") || 
               name.equals(".git") || 
               name.equals(".idea") ||
               name.equals("node_modules");
    }

    public void refreshTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new FileNode(new File(projectRoot)));
        buildTree(root, new File(projectRoot));
        
        if (treeModel == null) {
            treeModel = new DefaultTreeModel(root);
            projectTree.setModel(treeModel);
        } else {
            treeModel.setRoot(root);
        }
    }

    private void addTreePopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        
        // 新建菜单（包含子菜单）
        JMenu newMenu = new JMenu("新建");
        
        // Java相关子菜单
        JMenu newJavaMenu = new JMenu("Java");
        JMenuItem newClassItem = new JMenuItem("Class");
        JMenuItem newInterfaceItem = new JMenuItem("Interface");
        JMenuItem newEnumItem = new JMenuItem("Enum");
        
        newJavaMenu.add(newClassItem);
        newJavaMenu.add(newInterfaceItem);
        newJavaMenu.add(newEnumItem);
        
        // 文件相关子菜单
        JMenu newFileMenu = new JMenu("文件");
        JMenuItem newXmlItem = new JMenuItem("XML File");
        JMenuItem newYamlItem = new JMenuItem("YAML File");
        JMenuItem newPropertiesItem = new JMenuItem("Properties File");
        JMenuItem newTextItem = new JMenuItem("Text File");
        
        newFileMenu.add(newXmlItem);
        newFileMenu.add(newYamlItem);
        newFileMenu.add(newPropertiesItem);
        newFileMenu.add(newTextItem);
        
        // 目录选项
        JMenuItem newFolderItem = new JMenuItem("目录");
        
        // 添加所有新建选项
        newMenu.add(newJavaMenu);
        newMenu.add(newFileMenu);
        newMenu.addSeparator();
        newMenu.add(newFolderItem);
        
        // 文件操作菜单项
        JMenuItem openItem = new JMenuItem("打开");
        JMenuItem deleteItem = new JMenuItem("删除");
        JMenuItem renameItem = new JMenuItem("重命名");
        JMenuItem copyPathItem = new JMenuItem("复制路径");
        
        // 添加事件监听器
        newClassItem.addActionListener(e -> createNewItem("Java Class"));
        newInterfaceItem.addActionListener(e -> createNewItem("Java Interface"));
        newEnumItem.addActionListener(e -> createNewItem("Java Enum"));
        newXmlItem.addActionListener(e -> createNewItem("XML File"));
        newYamlItem.addActionListener(e -> createNewItem("YAML File"));
        newPropertiesItem.addActionListener(e -> createNewItem("Properties File"));
        newTextItem.addActionListener(e -> createNewItem("Text File"));
        newFolderItem.addActionListener(e -> createNewItem("Directory"));
        
        openItem.addActionListener(e -> openSelectedItem());
        deleteItem.addActionListener(e -> deleteSelectedItem());
        renameItem.addActionListener(e -> renameSelectedItem());
        copyPathItem.addActionListener(e -> copySelectedPath());
        
        // 组装菜单
        popupMenu.add(newMenu);
        popupMenu.addSeparator();
        popupMenu.add(openItem);
        popupMenu.add(deleteItem);
        popupMenu.add(renameItem);
        popupMenu.add(copyPathItem);
        
        // 添加右键菜单
        projectTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }
            
            private void showPopup(MouseEvent e) {
                TreePath path = projectTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    projectTree.setSelectionPath(path);
                    popupMenu.show(projectTree, e.getX(), e.getY());
                }
            }
        });
    }

    private void createNewItem(String itemType) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
            projectTree.getLastSelectedPathComponent();
        if (node != null) {
            String path = getNodePath(node);
            File parentFile = new File(projectRoot, path);
            File targetDir = parentFile;
            
            // 如果选中的是文件，使用其父目录
            if (!parentFile.isDirectory()) {
                targetDir = parentFile.getParentFile();
            }
            
            String name;
            if ("Directory".equals(itemType)) {
                name = JOptionPane.showInputDialog(projectTree, "输入目录名称:");
                if (name != null && !name.trim().isEmpty()) {
                    File newDir = new File(targetDir, name);
                    if (newDir.mkdir()) {
                        refreshTree();
                    } else {
                        JOptionPane.showMessageDialog(projectTree, "目录创建失败");
                    }
                }
            } else {
                name = JOptionPane.showInputDialog(projectTree, "输入" + itemType + "名称:");
                if (name != null && !name.trim().isEmpty()) {
                    String extension = getFileExtension(itemType);
                    if (!name.endsWith(extension)) {
                        name += extension;
                    }
                    
                    File newFile = new File(targetDir, name);
                    try {
                        if (newFile.createNewFile()) {
                            String template = getFileTemplate(itemType, name);
                            Files.write(newFile.toPath(), template.getBytes());
                            refreshTree();
                        } else {
                            JOptionPane.showMessageDialog(projectTree, "文件已存在");
                        }
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(projectTree, "创建失败: " + e.getMessage());
                    }
                }
            }
        }
    }

    private String getFileExtension(String fileType) {
        switch (fileType) {
            case "Java Class":
            case "Java Interface":
            case "Java Enum":
                return ".java";
            case "XML File":
                return ".xml";
            case "YAML File":
                return ".yml";
            case "Properties File":
                return ".properties";
            default:
                return ".txt";
        }
    }

    private void deleteSelectedItem() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
            projectTree.getLastSelectedPathComponent();
            
        if (node != null && node.getUserObject() instanceof FileNode) {
            FileNode fileNode = (FileNode) node.getUserObject();
            File file = fileNode.getFile();
            
            int result = JOptionPane.showConfirmDialog(
                projectTree,
                "确定要删除 " + file.getName() + " 吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                try {
                    if (deleteFileRecursively(file)) {
                        ((DefaultTreeModel) projectTree.getModel()).removeNodeFromParent(node);
                    } else {
                        JOptionPane.showMessageDialog(
                            projectTree,
                            "删除失败",
                            "错误",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(
                        projectTree,
                        "删除失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
    }

    private boolean deleteFileRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!deleteFileRecursively(f)) {
                        return false;
                    }
                }
            }
        }
        return Files.deleteIfExists(file.toPath());
    }

    private void renameSelectedItem() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
            projectTree.getLastSelectedPathComponent();
        if (node != null) {
            String newName = JOptionPane.showInputDialog(
                projectTree,
                "输入新名称:",
                node.toString()
            );
            if (newName != null && !newName.trim().isEmpty()) {
                String path = getNodePath(node);
                File file = new File(projectRoot, path);
                File newFile = new File(file.getParent(), newName);
                if (file.renameTo(newFile)) {
                    refreshTree();
                } else {
                    JOptionPane.showMessageDialog(projectTree, "重命名失败");
                }
            }
        }
    }

    private String getNodePath(DefaultMutableTreeNode node) {
        StringBuilder path = new StringBuilder(node.toString());
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        while (parent != null && parent.getParent() != null) {
            path.insert(0, parent.toString() + File.separator);
            parent = (DefaultMutableTreeNode) parent.getParent();
        }
        return path.toString();
    }

    private String getFileTemplate(String fileType, String fileName) {
        String baseName = fileName.contains(".") ? 
            fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        
        switch (fileType) {
            case "Java Class":
                return String.format(
                    "public class %s {\n" +
                    "    public %s() {\n" +
                    "    }\n\n" +
                    "    public static void main(String[] args) {\n" +
                    "        // TODO: Add your code here\n" +
                    "    }\n" +
                    "}",
                    baseName, baseName
                );
                
            case "Java Interface":
                return String.format(
                    "public interface %s {\n" +
                    "    // TODO: Add your interface methods\n" +
                    "}",
                    baseName
                );
                
            case "Java Enum":
                return String.format(
                    "public enum %s {\n" +
                    "    // TODO: Add your enum constants\n" +
                    "}",
                    baseName
                );
                
            case "XML File":
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                       "<root>\n" +
                       "    <!-- TODO: Add your XML content -->\n" +
                       "</root>";
                
            case "YAML File":
                return "# " + baseName + " configuration\n\n" +
                       "# TODO: Add your YAML content\n";
                
            case "Properties File":
                return "# " + baseName + " properties\n\n" +
                       "# TODO: Add your properties\n" +
                       "# property.name=value\n";
                
            default:
                return "# " + baseName + "\n";
        }
    }

    private void openSelectedItem() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
            projectTree.getLastSelectedPathComponent();
        if (node != null && node.getUserObject() instanceof FileNode) {
            FileNode fileNode = (FileNode) node.getUserObject();
            File file = fileNode.getFile();
            if (!file.isDirectory()) {
                // 通知 MainFrame 打开文件
                firePropertyChange("openFile", null, file);
            }
        }
    }

    private void copySelectedPath() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
            projectTree.getLastSelectedPathComponent();
        if (node != null && node.getUserObject() instanceof FileNode) {
            FileNode fileNode = (FileNode) node.getUserObject();
            File file = fileNode.getFile();
            try {
                String path = file.getCanonicalPath();
                java.awt.Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(path), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
} 