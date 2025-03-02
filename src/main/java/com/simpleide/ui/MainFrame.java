
package com.simpleide.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
// 文件和IO
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
// 工具类
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 基础 Swing 组件
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

// RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

// 项目内部类
import com.simpleide.ai.AIService;
import com.simpleide.ai.ChatMessage;
import com.simpleide.ai.CodeFormatter;
import com.simpleide.ai.CodeModification;
import com.simpleide.ai.DeepseekService;
import com.simpleide.compiler.CompilationManager;
import com.simpleide.formatter.JavaCodeFormatter;
import com.simpleide.util.LogManager;

public class MainFrame extends JFrame
{

	private RSyntaxTextArea codeEditor;
	private JTree projectTree;
	private JTextArea consoleOutput;
	private File currentFile;
	private String projectRoot;
	private ProjectTreeManager projectTreeManager;
	private AIService aiService;
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenu projectMenu;
	private JSplitPane mainSplitPane;
	private JSplitPane rightSplitPane;
	private JPanel aiPanel;
	private RSyntaxTextArea aiChatHistory;
	private JTextArea aiInputArea;
	private boolean isAIPanelVisible = false;
	private CompilationManager compilationManager;
	private JavaCodeFormatter codeFormatter;
	private List<ChatMessage> chatHistory = new ArrayList<>();
	private JPanel chatPanel;
	private JPanel responsePanel;
	private List<CodeModification> pendingModifications = new ArrayList<>();
	private List<String> selectedFilePaths = new ArrayList<>();
	private List<String> contextFilePaths = new ArrayList<>();
	private DefaultListModel<String> contextListModel = new DefaultListModel<>();
	private DefaultListModel<String> contextFileListModel;
	private JSplitPane horizontalSplit;
	private LogManager logManager;

	public MainFrame()
	{

		super("Simple IDE");
		logManager = LogManager.getInstance();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1200, 800);
		setLocationRelativeTo(null);
		aiService = new DeepseekService();
        logManager.info("AI service (Deepseek) initialized successfully");
        
		// 初始化项目根目录
		projectRoot = System.getProperty("user.dir");

		// 初始化组件
		initComponents();

		// 初始化编译管理器
		compilationManager = new CompilationManager(projectRoot);

		// 初始化代码格式化器
		codeFormatter = new JavaCodeFormatter();

		// 初始化快捷键
		initShortcuts();

		// 初始化编辑器右键菜单
		initEditorPopupMenu();

	}

	private void initComponents()
	{

		// 创建主分割面板
		mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		// 项目树
		projectTree = new JTree();
		JScrollPane treeScrollPane = new JScrollPane(projectTree);

		// 代码编辑器
		codeEditor = new RSyntaxTextArea(20, 60);
		codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		codeEditor.setCodeFoldingEnabled(true);

		// 添加文档监听器实现自动编译
		codeEditor.getDocument().addDocumentListener(new DocumentListener()
		{

			private Timer compileTimer = new Timer(1000, e -> autoCompile());

			private void scheduleCompile()
			{

				compileTimer.setRepeats(false);
				compileTimer.restart();

			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{

				scheduleCompile();

			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{

				scheduleCompile();

			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{

				scheduleCompile();

			}
		});

		RTextScrollPane editorScrollPane = new RTextScrollPane(codeEditor);

		// 初始化上下文文件相关的变量
		contextFileListModel = new DefaultListModel<>();
		contextFilePaths = new ArrayList<>();

		// 控制台输出
		consoleOutput = new JTextArea();
		consoleOutput.setEditable(false);
		JScrollPane consoleScrollPane = new JScrollPane(consoleOutput);

		// 创建一个分层面板来容纳控制台和浮动按钮
		JLayeredPane layeredPane = new JLayeredPane();

		// 创建悬浮按钮
		JButton floatingButton = new JButton("添加到AI中");
		floatingButton.setVisible(false);
		floatingButton.setBackground(new Color(64, 64, 64)); // 深灰色背景
		floatingButton.setForeground(Color.WHITE); // 白色文字
		floatingButton.setBorder(BorderFactory.createLineBorder(new Color(48, 48, 48))); // 深色边框
		floatingButton.setFocusPainted(false); // 去除焦点边框
		floatingButton.setOpaque(true); // 确保背景色可见

		// 设置分层面板的布局管理器
		layeredPane.setLayout(new LayeredPaneLayout());

		// 添加组件到分层面板
		layeredPane.add(consoleScrollPane, JLayeredPane.DEFAULT_LAYER);
		layeredPane.add(floatingButton, JLayeredPane.POPUP_LAYER);

		// 修改选择监听器中的按钮位置计算
		consoleOutput.addCaretListener(e ->
		{

			String selectedText = consoleOutput.getSelectedText();

			if (selectedText != null && !selectedText.trim().isEmpty())
			{

				try
				{

					// 设置按钮位置在右上角
					int buttonWidth = 120;
					int buttonHeight = 25;
					int margin = 10;
					floatingButton.setBounds(layeredPane.getWidth() - buttonWidth - margin, // 右边距离
							margin, // 上边距离
							buttonWidth, buttonHeight);
					floatingButton.setVisible(true);
					layeredPane.revalidate();
					layeredPane.repaint();

				} catch (Exception ex)
				{

					ex.printStackTrace();

				}

			} else
			{

				floatingButton.setVisible(false);

			}

		});

		// 修改按钮点击事件，确保更新AI面板并保持按钮可见
		floatingButton.addActionListener(e ->
		{

			String selectedText = consoleOutput.getSelectedText();

			if (selectedText != null && !selectedText.trim().isEmpty())
			{

				try
				{

					// 创建临时目录
					File tempDir = new File(projectRoot, "temp");

					if (!tempDir.exists())
					{

						tempDir.mkdirs();

					}

					// 使用时间戳创建唯一的文件名
					String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
					File tempFile = new File(tempDir, "console_" + timestamp + ".txt");

					// 写入选中的内容
					Files.write(tempFile.toPath(), selectedText.getBytes());

					// 添加到上下文文件列表
					String relativePath = Paths.get(projectRoot).relativize(tempFile.toPath()).toString();

					// 确保在EDT中更新UI
					SwingUtilities.invokeLater(() ->
					{

						// 更新"选择上下文文件"按钮的文件列表
						if (!contextFilePaths.contains(relativePath))
						{

							contextFilePaths.add(relativePath);
							contextListModel.addElement(relativePath);

						}

					});

					// 显示简短的成功提示
					JOptionPane.showMessageDialog(this, "已添加到AI上下文", "成功", JOptionPane.INFORMATION_MESSAGE);

				} catch (IOException ex)
				{

					JOptionPane.showMessageDialog(this, "添加失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);

				}

			}

		});

		// 初始化右侧分割面板
		rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		rightSplitPane.setTopComponent(editorScrollPane);
		rightSplitPane.setBottomComponent(layeredPane);

		// 设置分割面板
		mainSplitPane.setLeftComponent(treeScrollPane);
		mainSplitPane.setRightComponent(rightSplitPane);

		// 创建工具栏
		JToolBar toolBar = createToolBar();

		// 添加到框架
		add(toolBar, BorderLayout.NORTH);
		add(mainSplitPane, BorderLayout.CENTER);

		// 设置分割面板的分隔位置
		mainSplitPane.setDividerLocation(200);
		rightSplitPane.setDividerLocation(600);

		// 初始化项目树管理器
		projectTreeManager = new ProjectTreeManager(projectTree, projectRoot);

		// 添加树节点双击事件
		projectTree.addMouseListener(new MouseAdapter()
		{

			public void mouseClicked(MouseEvent e)
			{

				if (e.getClickCount() == 2)
				{

					DefaultMutableTreeNode node = (DefaultMutableTreeNode) projectTree.getLastSelectedPathComponent();

					if (node != null && node.isLeaf())
					{

						openFile(new File(projectRoot, getFilePath(node)));

					}

				}

			}
		});

		// 添加项目树的右键菜单
		projectTree.addMouseListener(new MouseAdapter()
		{

			@Override
			public void mousePressed(MouseEvent e)
			{

				if (e.isPopupTrigger())
				{

					showTreePopupMenu(e);

				}

			}

			@Override
			public void mouseReleased(MouseEvent e)
			{

				if (e.isPopupTrigger())
				{

					showTreePopupMenu(e);

				}

			}
		});

		// 创建菜单栏
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

	}

	private JToolBar createToolBar()
	{

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		// ===== 文件操作按钮 =====
		JButton fileButton = new JButton("文件");
		JPopupMenu fileMenu = new JPopupMenu();

		// 项目相关子菜单
		JMenu projectMenu = new JMenu("项目");
		projectMenu.add(createMenuItem("新建项目", e -> createNewProject()));
		projectMenu.add(createMenuItem("打开项目", e -> openProject()));
		projectMenu.add(createMenuItem("编译项目", e -> compileProject()));
		projectMenu.add(createMenuItem("清理项目", e -> cleanProject()));
		projectMenu.add(createMenuItem("格式化代码", e -> formatAllFiles()));
		projectMenu.addSeparator();
		projectMenu.add(createMenuItem("配置JDK", e -> configureJdk()));
		projectMenu.add(createMenuItem("项目设置", e -> showProjectSettings()));

		// 新建子菜单
		JMenu newMenu = new JMenu("新建");
		// Java子菜单
		JMenu newJavaMenu = new JMenu("Java");
		newJavaMenu.add(createMenuItem("Class", e -> showNewFileDialog("Java Class")));
		newJavaMenu.add(createMenuItem("Interface", e -> showNewFileDialog("Java Interface")));
		newJavaMenu.add(createMenuItem("Enum", e -> showNewFileDialog("Java Enum")));

		// 文件子菜单
		JMenu newFileMenu = new JMenu("文件");
		newFileMenu.add(createMenuItem("XML File", e -> showNewFileDialog("XML File")));
		newFileMenu.add(createMenuItem("YAML File", e -> showNewFileDialog("YAML File")));
		newFileMenu.add(createMenuItem("Properties File", e -> showNewFileDialog("Properties File")));
		newFileMenu.add(createMenuItem("Text File", e -> showNewFileDialog("Text File")));

		newMenu.add(newJavaMenu);
		newMenu.add(newFileMenu);
		newMenu.addSeparator();
		newMenu.add(createMenuItem("目录", e -> createNewDirectory()));

		// 添加到文件菜单
		fileMenu.add(projectMenu);
		fileMenu.addSeparator();
		fileMenu.add(newMenu);
		fileMenu.add(createMenuItem("打开文件", e -> openFileChooser()));
		fileMenu.add(createMenuItem("保存", e -> saveFile()));
		fileMenu.add(createMenuItem("另存为", e -> saveFileAs()));
		fileMenu.addSeparator();
		fileMenu.add(createMenuItem("删除", e -> deleteCurrentFile()));
		fileMenu.addSeparator();
		fileMenu.add(createMenuItem("刷新", e -> refreshProject()));

		fileButton.addMouseListener(new MouseAdapter()
		{

			public void mousePressed(MouseEvent e)
			{

				fileMenu.show(fileButton, 0, fileButton.getHeight());

			}
		});

		// ===== 运行/调试按钮 =====
		JButton runButton = new JButton("运行/调试");
		JPopupMenu runMenu = new JPopupMenu();
		runMenu.add(createMenuItem("运行程序", e -> runCode()));
		runMenu.add(createMenuItem("调试程序", e -> debugCode()));

		runButton.addMouseListener(new MouseAdapter()
		{

			public void mousePressed(MouseEvent e)
			{

				runMenu.show(runButton, 0, runButton.getHeight());

			}
		});

		// ===== Git操作按钮 =====
		JButton gitButton = new JButton("Git");
		JPopupMenu gitMenu = new JPopupMenu();
		gitMenu.add(createMenuItem("克隆仓库", e ->
		{

		}));
		gitMenu.add(createMenuItem("提交", e ->
		{

		}));
		gitMenu.add(createMenuItem("推送", e ->
		{

		}));
		gitMenu.add(createMenuItem("拉取", e ->
		{

		}));

		gitButton.addMouseListener(new MouseAdapter()
		{

			public void mousePressed(MouseEvent e)
			{

				gitMenu.show(gitButton, 0, gitButton.getHeight());

			}
		});

		// ===== Maven操作按钮 =====
		JButton mavenButton = new JButton("Maven");
		JPopupMenu mavenMenu = new JPopupMenu();
		mavenMenu.add(createMenuItem("clean", e -> executeMavenCommand("clean")));
		mavenMenu.add(createMenuItem("compile", e -> executeMavenCommand("compile")));
		mavenMenu.add(createMenuItem("test", e -> executeMavenCommand("test")));
		mavenMenu.add(createMenuItem("package", e -> executeMavenCommand("package")));
		mavenMenu.add(createMenuItem("install", e -> executeMavenCommand("install")));

		mavenButton.addMouseListener(new MouseAdapter()
		{

			public void mousePressed(MouseEvent e)
			{

				mavenMenu.show(mavenButton, 0, mavenButton.getHeight());

			}
		});

		// ===== AI助手按钮 =====
		JButton aiButton = new JButton("AI助手");
		aiButton.addActionListener(e -> showAIAssistant());

		// 添加所有按钮到工具栏
		toolBar.add(fileButton);
		toolBar.add(runButton);
		toolBar.add(gitButton);
		toolBar.add(mavenButton);
		toolBar.add(aiButton);

		return toolBar;

	}

	// 显示新建文件菜单的辅助方法
	private void showNewFileMenu(Component parent)
	{

		JPopupMenu newMenu = new JPopupMenu();

		// Java相关子菜单
		JMenu newJavaMenu = new JMenu("Java");
		newJavaMenu.add(createMenuItem("Class", e -> showNewFileDialog("Java Class")));
		newJavaMenu.add(createMenuItem("Interface", e -> showNewFileDialog("Java Interface")));
		newJavaMenu.add(createMenuItem("Enum", e -> showNewFileDialog("Java Enum")));

		// 文件相关子菜单
		JMenu newFileMenu = new JMenu("文件");
		newFileMenu.add(createMenuItem("XML File", e -> showNewFileDialog("XML File")));
		newFileMenu.add(createMenuItem("YAML File", e -> showNewFileDialog("YAML File")));
		newFileMenu.add(createMenuItem("Properties File", e -> showNewFileDialog("Properties File")));
		newFileMenu.add(createMenuItem("Text File", e -> showNewFileDialog("Text File")));

		// 添加到新建菜单
		newMenu.add(newJavaMenu);
		newMenu.add(newFileMenu);
		newMenu.addSeparator();
		newMenu.add(createMenuItem("目录", e -> createNewDirectory()));

		newMenu.show(parent, 0, parent.getHeight());

	}

	// 创建菜单项的辅助方法
	private JMenuItem createMenuItem(String text, ActionListener listener)
	{

		JMenuItem item = new JMenuItem(text);
		item.addActionListener(listener);
		return item;

	}

	// 新建目录的方法
	private void createNewDirectory()
	{

		String dirName = JOptionPane.showInputDialog(this, "输入目录名称:");

		if (dirName != null && !dirName.trim().isEmpty())
		{

			String targetDir = currentFile != null ? currentFile.getParent() : projectRoot;
			File newDir = new File(targetDir, dirName);

			if (newDir.mkdir())
			{

				projectTreeManager.refreshTree();

			} else
			{

				JOptionPane.showMessageDialog(this, "目录创建失败");

			}

		}

	}

	// 修改showNewFileDialog方法
	private void showNewFileDialog(String fileType)
	{

		String fileName = JOptionPane.showInputDialog(this, "输入" + fileType + "名称:");

		if (fileName != null && !fileName.trim().isEmpty())
		{

			createNewFile(fileType, fileName);

		}

	}

	private void runCode()
	{

		if (currentFile != null)
		{

			try
			{

				saveFile();

				if (compilationManager.compile(currentFile))
				{

					// 运行编译后的代码
					consoleOutput.append("正在运行 " + currentFile.getName() + "...\n");

					// TODO: 实现运行逻辑
				}

			} catch (Exception e)
			{

				consoleOutput.append("运行失败: " + e.getMessage() + "\n");

			}

		} else
		{

			JOptionPane.showMessageDialog(this, "请先打开或保存文件");

		}

	}

	private void debugCode()
	{

		if (currentFile != null)
		{

			try
			{

				saveFile();
				consoleOutput.append("开始调试 " + currentFile.getName() + "...\n");

				// TODO: 实现调试逻辑
			} catch (Exception e)
			{

				consoleOutput.append("调试失败: " + e.getMessage() + "\n");

			}

		} else
		{

			JOptionPane.showMessageDialog(this, "请先打开或保存文件");

		}

	}

	private void showGitDialog()
	{

		// TODO: 实现Git操作对话框
	}

	private void showAIAssistant()
	{

		if (!isAIPanelVisible)
		{

			// 如果AI面板不存在，创建它
			if (aiPanel == null)
			{

				aiPanel = createAIPanel();

			}

			// 如果水平分割面板不存在，创建它
			if (horizontalSplit == null)
			{

				horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

			}

			// 设置分割面板的组件
			horizontalSplit.setLeftComponent(mainSplitPane);
			horizontalSplit.setRightComponent(aiPanel);

			// 保存当前布局
			Container contentPane = getContentPane();
			Component[] components = contentPane.getComponents();

			// 保存工具栏引用
			JToolBar toolBar = null;

			for (Component comp : components)
			{

				if (comp instanceof JToolBar)
				{

					toolBar = (JToolBar) comp;
					break;

				}

			}

			// 清除内容并重新添加组件
			contentPane.removeAll();

			if (toolBar != null)
			{

				contentPane.add(toolBar, BorderLayout.NORTH);

			}

			contentPane.add(horizontalSplit, BorderLayout.CENTER);

			// 设置分割位置（AI面板宽度400像素）
			horizontalSplit.setDividerLocation(getWidth() - 400);

			isAIPanelVisible = true;

		} else
		{

			// 恢复原始布局
			Container contentPane = getContentPane();
			Component[] components = contentPane.getComponents();

			// 保存工具栏引用
			JToolBar toolBar = null;

			for (Component comp : components)
			{

				if (comp instanceof JToolBar)
				{

					toolBar = (JToolBar) comp;
					break;

				}

			}

			// 清除内容并重新添加组件
			contentPane.removeAll();

			if (toolBar != null)
			{

				contentPane.add(toolBar, BorderLayout.NORTH);

			}

			contentPane.add(mainSplitPane, BorderLayout.CENTER);

			isAIPanelVisible = false;

		}

		// 重新验证和重绘
		validate();
		repaint();

		// 确保工具栏可见
		SwingUtilities.invokeLater(() ->
		{

			Component[] components = getContentPane().getComponents();

			for (Component comp : components)
			{

				if (comp instanceof JToolBar)
				{

					comp.setVisible(true);
					break;

				}

			}

		});

	}

	private JScrollPane createChatScrollPane(JPanel panel)
	{

		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		// 为整个面板添加鼠标滚轮监听器
		panel.addMouseWheelListener(e ->
		{

			JScrollBar vertical = scrollPane.getVerticalScrollBar();
			vertical.setValue(vertical.getValue() + e.getWheelRotation() * 30);

		});

		// 添加鼠标拖动支持
		MouseAdapter mouseAdapter = new MouseAdapter()
		{

			private Point lastPoint;
			private boolean isDragging = false;

			@Override
			public void mousePressed(MouseEvent e)
			{

				lastPoint = e.getPoint();
				isDragging = true;

			}

			@Override
			public void mouseReleased(MouseEvent e)
			{

				isDragging = false;

			}

			@Override
			public void mouseDragged(MouseEvent e)
			{

				if (isDragging)
				{

					Point currentPoint = e.getPoint();
					JScrollBar vertical = scrollPane.getVerticalScrollBar();
					int delta = lastPoint.y - currentPoint.y;
					vertical.setValue(vertical.getValue() + delta);
					lastPoint = currentPoint;

				}

			}
		};

		panel.addMouseListener(mouseAdapter);
		panel.addMouseMotionListener(mouseAdapter);

		return scrollPane;

	}

	private JPanel createAIPanel()
	{

		aiPanel = new JPanel(new BorderLayout());

		// 创建知识库文件列表面板
		JPanel knowledgeBasePanel = new JPanel(new BorderLayout());
		knowledgeBasePanel.setName("knowledgeBasePanel"); // 设置名称以便后续查找
		knowledgeBasePanel.setBorder(BorderFactory.createTitledBorder("知识库文件"));

		// 使用已初始化的contextFileListModel
		JList<String> fileList = new JList<>(contextFileListModel);
		fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane fileScrollPane = new JScrollPane(fileList);

		// 添加文件管理按钮
		JPanel fileButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton addFilesButton = new JButton("添加文件");
		JButton refreshButton = new JButton("刷新项目文件");
		JButton clearButton = new JButton("清除选择");

		addFilesButton.addActionListener(e ->
		{

			JFileChooser chooser = new JFileChooser(projectRoot);
			chooser.setMultiSelectionEnabled(true);

			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{

				File[] files = chooser.getSelectedFiles();

				for (File file : files)
				{

					String relativePath = Paths.get(projectRoot).relativize(file.toPath()).toString();

					if (!contextFileListModel.contains(relativePath))
					{

						contextFileListModel.addElement(relativePath);
						selectedFilePaths.add(relativePath);

					}

				}

			}

		});

		refreshButton.addActionListener(e ->
		{

			contextFileListModel.clear();
			selectedFilePaths.clear();

			try
			{

				Files.walk(Paths.get(projectRoot)).filter(path -> path.toString().endsWith(".java")).forEach(path ->
				{

					String relativePath = Paths.get(projectRoot).relativize(path).toString();
					contextFileListModel.addElement(relativePath);
					selectedFilePaths.add(relativePath);

				});

			} catch (IOException ex)
			{

				ex.printStackTrace();

			}

		});

		clearButton.addActionListener(e ->
		{

			// 只清除选中的文件
			int[] selectedIndices = fileList.getSelectedIndices();

			for (int i = selectedIndices.length - 1; i >= 0; i--)
			{

				String item = contextFileListModel.getElementAt(selectedIndices[i]);
				contextFileListModel.removeElementAt(selectedIndices[i]);
				selectedFilePaths.remove(item);

			}

		});

		fileButtonPanel.add(addFilesButton);
		fileButtonPanel.add(refreshButton);
		fileButtonPanel.add(clearButton);

		knowledgeBasePanel.add(fileButtonPanel, BorderLayout.NORTH);
		knowledgeBasePanel.add(fileScrollPane, BorderLayout.CENTER);

		// 创建聊天和修改列表的分割面板
		JSplitPane chatModSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		// 聊天历史面板
		chatPanel = new JPanel();
		chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
		JScrollPane chatScrollPane = createChatScrollPane(chatPanel);

		// 修改列表面板
		JPanel modificationPanel = new JPanel(new BorderLayout());
		modificationPanel.setBorder(BorderFactory.createTitledBorder("待应用修改"));

		DefaultListModel<String> modificationListModel = new DefaultListModel<>();
		JList<String> modificationList = new JList<>(modificationListModel);
		JScrollPane modificationScrollPane = createScrollablePanel(modificationList);

		JButton applyAllButton = new JButton("应用所有修改");
		applyAllButton.addActionListener(e -> applyAllModifications());

		modificationPanel.add(modificationScrollPane, BorderLayout.CENTER);
		modificationPanel.add(applyAllButton, BorderLayout.SOUTH);

		chatModSplitPane.setTopComponent(chatScrollPane);
		chatModSplitPane.setBottomComponent(modificationPanel);
		chatModSplitPane.setDividerLocation(400);

		// 创建主分割面板
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainSplitPane.setTopComponent(knowledgeBasePanel);
		mainSplitPane.setBottomComponent(chatModSplitPane);
		mainSplitPane.setDividerLocation(200);

		// 创建输入区域
		JPanel inputPanel = new JPanel(new BorderLayout());

		// 创建上下文文件列表面板
		JPanel contextFilesPanel = new JPanel(new BorderLayout());
		JList<String> contextList = new JList<>(contextListModel);
		contextList.setVisibleRowCount(3);
		JScrollPane contextScrollPane = new JScrollPane(contextList);

		// 添加删除按钮到每个列表项
		contextList.setCellRenderer(new DefaultListCellRenderer()
		{

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{

				JPanel panel = new JPanel(new BorderLayout());
				panel.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());

				JLabel label = new JLabel(value.toString());
				label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

				JButton deleteButton = new JButton("×");
				deleteButton.setFont(deleteButton.getFont().deriveFont(Font.BOLD));
				deleteButton.setPreferredSize(new Dimension(20, 20));

				panel.add(label, BorderLayout.CENTER);
				panel.add(deleteButton, BorderLayout.EAST);

				return panel;

			}
		});

		// 添加鼠标监听器处理删除按钮点击
		contextList.addMouseListener(new MouseAdapter()
		{

			@Override
			public void mouseClicked(MouseEvent e)
			{

				int index = contextList.locationToIndex(e.getPoint());

				if (index != -1)
				{

					Rectangle bounds = contextList.getCellBounds(index, index);

					if (bounds != null)
					{

						// 检查点击是否在删除按钮区域
						if (e.getX() > bounds.x + bounds.width - 25)
						{

							String item = contextListModel.getElementAt(index);
							contextListModel.removeElementAt(index);
							contextFilePaths.remove(item);

						}

					}

				}

			}
		});

		contextFilesPanel.add(contextScrollPane, BorderLayout.CENTER);

		// 创建输入区域的上半部分（包含上下文文件列表和选择按钮）
		JPanel upperInputPanel = new JPanel(new BorderLayout());
		JButton selectContextButton = new JButton("选择上下文文件");
		selectContextButton.addActionListener(e ->
		{

			JFileChooser chooser = new JFileChooser(projectRoot);
			chooser.setMultiSelectionEnabled(true);
			chooser.setFileFilter(new javax.swing.filechooser.FileFilter()
			{

				public boolean accept(File f)
				{

					return f.isDirectory() || f.getName().endsWith(".java");

				}

				public String getDescription()
				{

					return "Java Files (*.java)";

				}
			});

			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{

				File[] files = chooser.getSelectedFiles();

				for (File file : files)
				{

					String relativePath = Paths.get(projectRoot).relativize(file.toPath()).toString();

					if (!contextFilePaths.contains(relativePath))
					{

						contextFilePaths.add(relativePath);
						contextListModel.addElement(relativePath);

					}

				}

			}

		});

		upperInputPanel.add(selectContextButton, BorderLayout.WEST);
		upperInputPanel.add(contextFilesPanel, BorderLayout.CENTER);

		// 创建输入区域的下半部分（文本区域和发送按钮）
		JPanel lowerInputPanel = new JPanel(new BorderLayout());
		aiInputArea = new JTextArea(3, 20);
		aiInputArea.setLineWrap(true);
		aiInputArea.setWrapStyleWord(true);

		// 添加键盘监听器处理快捷键
		aiInputArea.addKeyListener(new KeyAdapter()
		{

			@Override
			public void keyPressed(KeyEvent e)
			{

				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{

					if (e.isControlDown())
					{

						// Ctrl+Enter 插入换行
						aiInputArea.append("\n");

					} else
					{

						// Enter 发送消息
						e.consume(); // 阻止默认的换行行为
						sendToAI();

					}

				}

			}
		});

		// 添加文档监听器处理自动调整高度
		aiInputArea.getDocument().addDocumentListener(new DocumentListener()
		{

			private void updateHeight()
			{

				SwingUtilities.invokeLater(() ->
				{

					// 获取文本的行数
					int lineCount = aiInputArea.getLineCount();
					// 设置新的首选高度（每行20像素，最小3行，最大10行）
					int newHeight = Math.min(Math.max(3, lineCount), 10) * 20;
					aiInputArea.setPreferredSize(new Dimension(aiInputArea.getWidth(), newHeight));
					lowerInputPanel.revalidate();

				});

			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{

				updateHeight();

			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{

				updateHeight();

			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{

				updateHeight();

			}
		});

		JScrollPane inputScrollPane = createScrollablePanel(aiInputArea);

		JButton sendButton = new JButton("发送");
		sendButton.addActionListener(e -> sendToAI());

		lowerInputPanel.add(inputScrollPane, BorderLayout.CENTER);
		lowerInputPanel.add(sendButton, BorderLayout.EAST);

		// 组装输入面板
		inputPanel.add(upperInputPanel, BorderLayout.NORTH);
		inputPanel.add(lowerInputPanel, BorderLayout.CENTER);

		aiPanel.add(mainSplitPane, BorderLayout.CENTER);
		aiPanel.add(inputPanel, BorderLayout.SOUTH);

		// 初始加载项目文件
		refreshButton.doClick();

		return aiPanel;

	}

	private void sendToAI()
	{

		StringBuilder content = new StringBuilder();

		// 1. 添加知识库文件内容作为整体项目理解
		content.append("=== 知识库文件 ===\n");

		for (String filePath : selectedFilePaths)
		{

			addFileContent(content, filePath);

		}

		// 2. 添加上下文文件内容作为本轮对话重点
		if (!contextFilePaths.isEmpty())
		{

			content.append("\n=== 上下文文件 ===\n");

			for (String filePath : contextFilePaths)
			{

				addFileContent(content, filePath);

			}

		}

		// 3. 添加用户问题
		String userQuestion = aiInputArea.getText();

		if (userQuestion.trim().isEmpty())
		{

			return;

		}

		// 构建最终的提示语
		String finalPrompt = String.format("请基于以下信息回答问题：\n\n" + "%s\n\n" + "用户问题：%s", content.toString(), userQuestion);

		// 添加到聊天历史
		ChatMessage userMessage = new ChatMessage(ChatMessage.Role.USER, userQuestion);
		chatHistory.add(userMessage);

		// 显示用户消息
		addChatMessage(userMessage);

		// 清除输入
		aiInputArea.setText("");

		// 发送请求
		new SwingWorker<String, Void>()
		{

			@Override
			protected String doInBackground() throws Exception
			{

				return aiService.getAIResponse(finalPrompt);

			}

			@Override
			protected void done()
			{

				try
				{

					String response = get();
					ChatMessage aiMessage = new ChatMessage(ChatMessage.Role.AI, response);
					chatHistory.add(aiMessage);
					addChatMessage(aiMessage);

					// 解析AI响应中的代码修改建议
					parseAndApplyCodeModifications(response, selectedFilePaths);

				} catch (Exception e)
				{

					addErrorMessage("Error: " + e.getMessage());

				}

			}
		}.execute();

	}

	private void addErrorMessage(String message)
	{

		// 显示错误消息
		JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);

	}

	private String getAllProjectFiles()
	{

		StringBuilder content = new StringBuilder();

		try
		{

			Files.walk(Paths.get(projectRoot)).filter(path -> path.toString().endsWith(".java")).forEach(path ->
			{

				try
				{

					Path relativePath = Paths.get(projectRoot).relativize(path);
					content.append("\n=== ").append(relativePath.toString()).append(" ===\n");
					content.append(new String(Files.readAllBytes(path))).append("\n");

				} catch (IOException e)
				{

					e.printStackTrace();

				}

			});

		} catch (IOException e)
		{

			e.printStackTrace();

		}

		return content.toString();

	}

	private void addFileContent(StringBuilder content, String filePath)
	{

		try
		{

			String fileContent = new String(Files.readAllBytes(Paths.get(projectRoot, filePath)));
			content.append("\n=== ").append(filePath).append(" ===\n").append(fileContent).append("\n");

		} catch (IOException e)
		{

			e.printStackTrace();

		}

	}

	private void initShortcuts()
	{

		InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = getRootPane().getActionMap();

		// 文件操作快捷键
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
				"saveAs");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "open");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "new");

		// 编辑操作快捷键
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "find");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "replace");

		// 运行操作快捷键
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "run");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "debug");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
				"format");

		// AI助手快捷键
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), "aiAssistant");

		// 注册动作
		actionMap.put("save", new AbstractAction()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{

				saveFile();

			}
		});

		actionMap.put("saveAs", new AbstractAction()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{

				saveFileAs();

			}
		});

		actionMap.put("open", new AbstractAction()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{

				openFileChooser();

			}
		});

		actionMap.put("run", new AbstractAction()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{

				runCode();

			}
		});

		actionMap.put("debug", new AbstractAction()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{

				debugCode();

			}
		});

		actionMap.put("format", new AbstractAction()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{

				formatCurrentFile();

			}
		});

		actionMap.put("aiAssistant", new AbstractAction()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{

				showAIAssistant();

			}
		});

	}

	private void initEditorPopupMenu()
	{

		JPopupMenu popupMenu = new JPopupMenu();

		// 基本编辑操作
		addMenuItem(popupMenu, "剪切", KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK, e -> codeEditor.cut());
		addMenuItem(popupMenu, "复制", KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, e -> codeEditor.copy());
		addMenuItem(popupMenu, "粘贴", KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK, e -> codeEditor.paste());
		popupMenu.addSeparator();

		// 文件操作
		addMenuItem(popupMenu, "保存", KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, e -> saveFile());
		addMenuItem(popupMenu, "另存为", KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
				e -> saveFileAs());
		popupMenu.addSeparator();

		// 运行操作
		addMenuItem(popupMenu, "运行", KeyEvent.VK_F5, 0, e -> runCode());
		addMenuItem(popupMenu, "调试", KeyEvent.VK_F9, 0, e -> debugCode());
		popupMenu.addSeparator();

		// 代码操作
		addMenuItem(popupMenu, "格式化", KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
				e -> formatCurrentFile());
		addMenuItem(popupMenu, "AI助手", KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK, e -> showAIAssistant());

		codeEditor.setComponentPopupMenu(popupMenu);

	}

	private void addMenuItem(JPopupMenu menu, String text, int keyCode, int modifiers, ActionListener action)
	{

		JMenuItem item = new JMenuItem(text);

		if (keyCode != 0)
		{

			item.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));

		}

		item.addActionListener(action);
		menu.add(item);

	}

	private void formatCurrentFile()
	{

		if (currentFile != null)
		{

			try
			{

				String content = codeEditor.getText();
				String formattedCode = codeFormatter.format(content);
				codeEditor.setText(formattedCode);
				saveFile();
				JOptionPane.showMessageDialog(this, "代码格式化完成");

			} catch (Exception e)
			{

				JOptionPane.showMessageDialog(this, "格式化失败: " + e.getMessage());

			}

		} else
		{

			JOptionPane.showMessageDialog(this, "请先打开或保存文件");

		}

	}

	// 添加代码修改解析和应用方法
	public void parseAndApplyCodeModifications(String response, List<String> filePaths)
	{

		// 首先提取所有文件路径
		Pattern filePathPattern = Pattern.compile("===\\s+([^=\\n]+)\\s+===");
		Matcher filePathMatcher = filePathPattern.matcher(response);
		Map<String, String> filePathMap = new HashMap<>();

		while (filePathMatcher.find())
		{

			String filePath = filePathMatcher.group(1).trim();
			String fileName = getFileNameFromPath(filePath);

			if (fileName != null && !fileName.isEmpty())
			{

				filePathMap.put(fileName, filePath);

			}

		}

		// 然后处理代码块
		String[] blocks = response.split("```");

		for (int i = 1; i < blocks.length; i += 2)
		{

			String block = blocks[i];
			String[] parts = block.split("\n", 2);
			if (parts.length < 2)
				continue;

			String header = parts[0].trim();
			String code = parts[1].trim();

			// 解析文件路径
			String filePath = null;

			if (header.contains(":"))
			{

				String[] headerParts = header.split(":", 2);
				String fileName = headerParts[1].trim();
				// 使用完整路径映射
				filePath = filePathMap.get(getFileNameFromPath(fileName));

				if (filePath == null)
				{

					// 如果在映射中找不到，使用原始文件名
					filePath = fileName;

				}

			} else if (!filePathMap.isEmpty())
			{

				// 如果没有冒号，使用最近的文件路径
				filePath = filePathMap.values().iterator().next();

			}

			// 确保文件路径不为空
			if (filePath != null && !filePath.trim().isEmpty())
			{

				// 如果文件路径不在允许的列表中且列表不为空，则跳过
				if (!filePaths.isEmpty() && !filePaths.contains(filePath))
				{

					continue;

				}

				// 创建新的代码修改对象
				CodeModification modification = new CodeModification(filePath, code);
				pendingModifications.add(modification);
				updateModificationList();

			}

		}

	}

	private String getFileNameFromPath(String path)
	{

		if (path == null || path.trim().isEmpty())
		{

			return null;

		}

		int lastSlash = path.lastIndexOf('/');
		return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;

	}

	private void updateModificationList()
	{

		// 获取修改列表面板中的 JList
		JSplitPane mainSplitPane = (JSplitPane) aiPanel.getComponent(0);
		JSplitPane chatModSplitPane = (JSplitPane) mainSplitPane.getBottomComponent();
		JPanel modificationPanel = (JPanel) chatModSplitPane.getBottomComponent();
		JScrollPane scrollPane = (JScrollPane) modificationPanel.getComponent(0);
		JList<?> list = (JList<?>) scrollPane.getViewport().getView();
		DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();

		// 更新列表内容
		model.clear();

		for (CodeModification mod : pendingModifications)
		{

			model.addElement(mod.getFilePath());

		}

	}

	private void applyAllModifications()
	{

		for (CodeModification mod : pendingModifications)
		{

			try
			{

				File file = new File(projectRoot, mod.getFilePath());
				Files.write(file.toPath(), mod.getModifiedCode().getBytes());

				if (file.equals(currentFile))
				{

					codeEditor.setText(mod.getModifiedCode());

				}

				consoleOutput.append("已应用修改到文件: " + mod.getFilePath() + "\n");

			} catch (IOException e)
			{

				consoleOutput.append("应用修改失败: " + e.getMessage() + "\n");

			}

		}

		pendingModifications.clear();
		updateModificationList();

	}

	private void openFile(File file)
	{

		try
		{

			String content = new String(Files.readAllBytes(file.toPath()));
			codeEditor.setText(content);
			currentFile = file;
			codeEditor.setCaretPosition(0);
			setTitle("Simple Java IDE - " + file.getName());

		} catch (IOException e)
		{

			JOptionPane.showMessageDialog(this, "打开文件失败: " + e.getMessage());

		}

	}

	private void saveFile()
	{

		if (currentFile != null)
		{

			try
			{

				Files.write(currentFile.toPath(), codeEditor.getText().getBytes());

			} catch (IOException e)
			{

				JOptionPane.showMessageDialog(this, "保存失败: " + e.getMessage());

			}

		} else
		{

			saveFileAs();

		}

	}

	private void saveFileAs()
	{

		JFileChooser chooser = new JFileChooser(projectRoot);
		chooser.setFileFilter(new javax.swing.filechooser.FileFilter()
		{

			public boolean accept(File f)
			{

				return f.isDirectory() || f.getName().endsWith(".java");

			}

			public String getDescription()
			{

				return "Java Files (*.java)";

			}
		});

		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{

			currentFile = chooser.getSelectedFile();

			if (!currentFile.getName().endsWith(".java"))
			{

				currentFile = new File(currentFile.getPath() + ".java");

			}

			saveFile();
			projectTreeManager.refreshTree();

		}

	}

	private void openFileChooser()
	{

		JFileChooser chooser = new JFileChooser(projectRoot);
		chooser.setFileFilter(new javax.swing.filechooser.FileFilter()
		{

			public boolean accept(File f)
			{

				return f.isDirectory() || f.getName().endsWith(".java");

			}

			public String getDescription()
			{

				return "Java Files (*.java)";

			}
		});

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{

			openFile(chooser.getSelectedFile());

		}

	}

	private void addChatMessage(ChatMessage message)
	{

		JPanel messagePanel = new JPanel(new BorderLayout());
		messagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// 添加时间戳和角色标签
		JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel timeLabel = new JLabel(new SimpleDateFormat("HH:mm:ss").format(new Date()));
		JLabel roleLabel = new JLabel(message.getRole() == ChatMessage.Role.USER ? "You:" : "AI:");
		headerPanel.add(timeLabel);
		headerPanel.add(roleLabel);

		// 消息内容面板
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		String content = message.getContent();

		// 如果是AI回复，检查是否包含代码块
		if (message.getRole() == ChatMessage.Role.AI)
		{

			// 首先提取所有文件路径
			Pattern filePathPattern = Pattern.compile("===\\s+([^=\\n]+)\\s+===");
			Matcher filePathMatcher = filePathPattern.matcher(content);
			Map<String, String> filePathMap = new HashMap<>();

			while (filePathMatcher.find())
			{

				String filePath = filePathMatcher.group(1).trim();
				filePathMap.put(getFileNameFromPath(filePath), filePath);

			}

			// 分析代码块
			String[] blocks = content.split("```");

			// 添加第一个文本块（如果存在）
			if (blocks.length > 0 && !blocks[0].trim().isEmpty())
			{

				addTextBlock(contentPanel, blocks[0].trim());

			}

			// 处理代码块
			for (int i = 1; i < blocks.length; i += 2)
			{

				if (i < blocks.length)
				{

					String block = blocks[i];
					String[] parts = block.split("\n", 2);
					String header = parts[0].trim();
					String code = parts[1].trim();

					// 解析语言和文件路径
					String language = null;
					String filePath = null;

					// 从代码块头部获取语言类型
					if (header.contains(":"))
					{

						String[] headerParts = header.split(":", 2);
						language = headerParts[0];

					}

					// 从文件路径映射中获取完整路径
					String fileName = getFileNameFromPath(
							header.contains(":") ? header.split(":", 2)[1].trim() : header);
					filePath = filePathMap.get(fileName);

					// 如果没有指定语言，从文件路径推断
					if (language == null && filePath != null)
					{

						language = CodeFormatter.getFileType(filePath);

					}

					// 创建代码预览面板
					JPanel codePanel = new JPanel(new BorderLayout());
					codePanel.setBorder(BorderFactory.createTitledBorder("文件: " + filePath));

					// 代码预览区域
					RSyntaxTextArea previewArea = new RSyntaxTextArea(8, 60);
					final String syntaxStyle = getSyntaxStyle(language);
					previewArea.setSyntaxEditingStyle(syntaxStyle);
					previewArea.setEditable(false);
					previewArea.setText(code);
					RTextScrollPane previewScroll = new RTextScrollPane(previewArea);

					// 按钮面板
					JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

					// 预览按钮
					JButton previewButton = new JButton("预览修改");
					previewButton.setBackground(new Color(230, 230, 255));
					previewButton.setForeground(Color.BLACK);
					previewButton.setFont(previewButton.getFont().deriveFont(Font.BOLD));
					final String finalFilePath = filePath;
					final String finalCode = code;
					previewButton.addActionListener(e -> showCodePreviewDialog(finalFilePath, finalCode, syntaxStyle));

					// 应用按钮
					JButton applyButton = new JButton("应用修改");
					applyButton.setBackground(new Color(200, 255, 200));
					applyButton.setForeground(new Color(0, 100, 0));
					applyButton.setFont(applyButton.getFont().deriveFont(Font.BOLD));
					applyButton.addActionListener(e -> applyCodeModification(finalFilePath, finalCode));

					// 添加按钮到面板
					buttonPanel.add(previewButton);
					buttonPanel.add(Box.createHorizontalStrut(10)); // 添加间距
					buttonPanel.add(applyButton);

					codePanel.add(previewScroll, BorderLayout.CENTER);
					codePanel.add(buttonPanel, BorderLayout.SOUTH);
					contentPanel.add(codePanel);
					contentPanel.add(Box.createVerticalStrut(10));

					// 添加到待应用修改列表
					if (filePath != null)
					{

						CodeModification modification = new CodeModification(filePath, code);
						pendingModifications.add(modification);
						updateModificationList();

					}

				}

				// 添加代码块之后的文本（如果存在）
				if (i + 1 < blocks.length && !blocks[i + 1].trim().isEmpty())
				{

					addTextBlock(contentPanel, blocks[i + 1].trim());

				}

			}

		} else
		{

			// 用户消息，直接添加文本
			addTextBlock(contentPanel, content);

		}

		messagePanel.add(headerPanel, BorderLayout.NORTH);
		messagePanel.add(contentPanel, BorderLayout.CENTER);

		chatPanel.add(messagePanel);
		chatPanel.revalidate();
		chatPanel.repaint();

		// 自动滚动到底部
		SwingUtilities.invokeLater(() ->
		{

			Container parent = SwingUtilities.getAncestorOfClass(JScrollPane.class, chatPanel);

			if (parent instanceof JScrollPane)
			{

				JScrollPane parentScrollPane = (JScrollPane) parent;
				JScrollBar vertical = parentScrollPane.getVerticalScrollBar();
				vertical.setValue(vertical.getMaximum());

			}

		});

	}

	private String getSyntaxStyle(String language)
	{

		if (language == null)
			return SyntaxConstants.SYNTAX_STYLE_NONE;

		switch (language.toLowerCase())
		{

			case "text/java":
				return SyntaxConstants.SYNTAX_STYLE_JAVA;

			case "text/xml":
				return SyntaxConstants.SYNTAX_STYLE_XML;

			case "text/properties":
				return SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;

			case "text/yaml":
				return SyntaxConstants.SYNTAX_STYLE_YAML;

			case "text/json":
				return SyntaxConstants.SYNTAX_STYLE_JSON;

			case "text/html":
				return SyntaxConstants.SYNTAX_STYLE_HTML;

			case "text/css":
				return SyntaxConstants.SYNTAX_STYLE_CSS;

			case "text/javascript":
				return SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;

			case "text/sql":
				return SyntaxConstants.SYNTAX_STYLE_SQL;

			case "text/markdown":
				return SyntaxConstants.SYNTAX_STYLE_MARKDOWN;

			case "text/unix":
				return SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL;

			default:
				return SyntaxConstants.SYNTAX_STYLE_NONE;

		}

	}

	private void showCodePreviewDialog(String filePath, String code, String syntaxStyle)
	{

		JDialog previewDialog = new JDialog(this, "代码预览 - " + filePath, true);
		previewDialog.setLayout(new BorderLayout());

		// 创建代码预览区域
		RSyntaxTextArea previewArea = new RSyntaxTextArea(20, 80);
		previewArea.setSyntaxEditingStyle(syntaxStyle);
		previewArea.setEditable(false);
		previewArea.setText(code);
		RTextScrollPane scrollPane = new RTextScrollPane(previewArea);

		// 创建按钮面板
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton applyButton = new JButton("应用修改");
		JButton closeButton = new JButton("关闭");

		applyButton.setBackground(new Color(200, 255, 200));

		applyButton.addActionListener(e ->
		{

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

	private void applyCodeModification(String filePath, String code)
	{

		if (filePath == null || filePath.trim().isEmpty())
		{

			JOptionPane.showMessageDialog(this, "无效的文件路径", "错误", JOptionPane.ERROR_MESSAGE);
			return;

		}

		if (code == null || code.trim().isEmpty())
		{

			JOptionPane.showMessageDialog(this, "无效的代码内容", "错误", JOptionPane.ERROR_MESSAGE);
			return;

		}

		try
		{

			File file = new File(projectRoot, filePath.trim());

			// 如果文件不存在，创建新文件
			if (!file.exists())
			{

				// 确保目录存在
				File parentDir = file.getParentFile();

				if (parentDir != null && !parentDir.exists())
				{

					if (!parentDir.mkdirs())
					{

						throw new IOException("无法创建目录: " + parentDir.getPath());

					}

				}

				// 创建新文件
				if (file.createNewFile())
				{

					Files.write(file.toPath(), code.getBytes());
					JOptionPane.showMessageDialog(this, "已创建新文件: " + filePath, "创建成功", JOptionPane.INFORMATION_MESSAGE);

				} else
				{

					throw new IOException("无法创建文件");

				}

			} else
			{

				// 如果文件存在，提示用户确认覆盖
				int option = JOptionPane.showConfirmDialog(this, "文件 " + filePath + " 已存在，是否覆盖？", "确认覆盖",
						JOptionPane.YES_NO_OPTION);

				if (option == JOptionPane.YES_OPTION)
				{

					Files.write(file.toPath(), code.getBytes());
					JOptionPane.showMessageDialog(this, "已更新文件: " + filePath, "更新成功", JOptionPane.INFORMATION_MESSAGE);

				}

			}

			// 刷新项目树
			projectTreeManager.refreshTree();

			// 如果是当前打开的文件，更新编辑器内容
			if (currentFile != null && currentFile.equals(file))
			{

				codeEditor.setText(code);

			}

		} catch (IOException e)
		{

			JOptionPane.showMessageDialog(this, "应用代码失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);

		}

	}

	private <T extends JComponent> JScrollPane createScrollablePanel(T component)
	{

		JScrollPane scrollPane = new JScrollPane(component);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		// 添加鼠标滚轮监听器
		component.addMouseWheelListener(e ->
		{

			JScrollBar vertical = scrollPane.getVerticalScrollBar();
			int notches = e.getWheelRotation();
			int delta = notches * 30; // 每次滚动30个像素
			vertical.setValue(vertical.getValue() + delta);

		});

		// 添加鼠标拖动监听器
		MouseAdapter mouseAdapter = new MouseAdapter()
		{

			private Point lastPoint;
			private boolean isDragging = false;

			@Override
			public void mousePressed(MouseEvent e)
			{

				lastPoint = e.getPoint();
				isDragging = true;

			}

			@Override
			public void mouseReleased(MouseEvent e)
			{

				isDragging = false;

			}

			@Override
			public void mouseDragged(MouseEvent e)
			{

				if (isDragging)
				{

					Point currentPoint = e.getPoint();
					JScrollBar vertical = scrollPane.getVerticalScrollBar();
					int delta = lastPoint.y - currentPoint.y;
					vertical.setValue(vertical.getValue() + delta);
					lastPoint = currentPoint;

				}

			}
		};

		component.addMouseListener(mouseAdapter);
		component.addMouseMotionListener(mouseAdapter);

		return scrollPane;

	}

	private String getFilePath(DefaultMutableTreeNode node)
	{

		StringBuilder path = new StringBuilder(node.toString());
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();

		while (parent != null && parent.getParent() != null)
		{

			path.insert(0, parent.toString() + File.separator);
			parent = (DefaultMutableTreeNode) parent.getParent();

		}

		return path.toString();

	}

	private void showTreePopupMenu(MouseEvent e)
	{

		TreePath path = projectTree.getPathForLocation(e.getX(), e.getY());

		if (path != null)
		{

			projectTree.setSelectionPath(path);
			JPopupMenu popupMenu = new JPopupMenu();

			// 添加新建菜单项
			JMenu newMenu = new JMenu("新建");
			newMenu.add(new JMenuItem(new AbstractAction("Java类")
			{

				@Override
				public void actionPerformed(ActionEvent e)
				{

					showNewFileDialog("Java Class");

				}
			}));
			popupMenu.add(newMenu);

			// 添加删除菜单项
			popupMenu.add(new JMenuItem(new AbstractAction("删除")
			{

				@Override
				public void actionPerformed(ActionEvent e)
				{

					DefaultMutableTreeNode node = (DefaultMutableTreeNode) projectTree.getLastSelectedPathComponent();

					if (node != null)
					{

						File file = new File(projectRoot, getFilePath(node));

						if (file.exists())
						{

							int result = JOptionPane.showConfirmDialog(MainFrame.this,
									"确定要删除文件 " + file.getName() + " 吗？", "确认删除", JOptionPane.YES_NO_OPTION,
									JOptionPane.WARNING_MESSAGE);

							if (result == JOptionPane.YES_OPTION)
							{

								try
								{

									if (file.delete())
									{

										if (currentFile != null && currentFile.equals(file))
										{

											currentFile = null;
											codeEditor.setText("");

										}

										refreshProject();
										JOptionPane.showMessageDialog(MainFrame.this, "文件已删除", "成功",
												JOptionPane.INFORMATION_MESSAGE);

									} else
									{

										JOptionPane.showMessageDialog(MainFrame.this, "删除文件失败", "错误",
												JOptionPane.ERROR_MESSAGE);

									}

								} catch (SecurityException ex)
								{

									JOptionPane.showMessageDialog(MainFrame.this, "删除文件失败: " + ex.getMessage(), "错误",
											JOptionPane.ERROR_MESSAGE);

								}

							}

						}

					}

				}
			}));

			// 添加刷新菜单项
			popupMenu.add(new JMenuItem(new AbstractAction("刷新")
			{

				@Override
				public void actionPerformed(ActionEvent e)
				{

					refreshProject();

				}
			}));

			popupMenu.show(projectTree, e.getX(), e.getY());

		}

	}

	private void createNewProject()
	{

		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{

			projectRoot = chooser.getSelectedFile().getAbsolutePath();
			projectTreeManager.setProjectRoot(projectRoot);
			projectTreeManager.refreshTree();

		}

	}

	private void openProject()
	{

		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{

			projectRoot = chooser.getSelectedFile().getAbsolutePath();
			projectTreeManager.setProjectRoot(projectRoot);
			projectTreeManager.refreshTree();

		}

	}

	public void refreshProject()
	{

		projectTreeManager.refreshTree();

	}

	private void executeMavenCommand(String command)
	{

		try
		{

			ProcessBuilder pb = new ProcessBuilder("mvn", command);
			pb.directory(new File(projectRoot));
			pb.redirectErrorStream(true);
			Process process = pb.start();

			// 读取输出
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
			{

				String line;

				while ((line = reader.readLine()) != null)
				{

					consoleOutput.append(line + "\n");

				}

			}

			process.waitFor();

		} catch (Exception e)
		{

			consoleOutput.append("执行Maven命令失败: " + e.getMessage() + "\n");

		}

	}

	private void createNewFile(String fileType, String fileName)
	{

		String extension = ".java";
		String template = "public class " + fileName + " {\n    \n}";

		File newFile = new File(projectRoot, fileName + extension);

		try
		{

			if (newFile.createNewFile())
			{

				Files.write(newFile.toPath(), template.getBytes());
				projectTreeManager.refreshTree();
				openFile(newFile);

			} else
			{

				JOptionPane.showMessageDialog(this, "文件已存在");

			}

		} catch (IOException e)
		{

			JOptionPane.showMessageDialog(this, "创建文件失败: " + e.getMessage());

		}

	}

	public String getCurrentEditorText()
	{

		return codeEditor != null ? codeEditor.getText() : "";

	}

	private void addTextBlock(JPanel parent, String text)
	{

		JTextArea textArea = new JTextArea(text);
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setBackground(new Color(250, 250, 250));
		textArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(500, Math.min(200, textArea.getPreferredSize().height)));

		parent.add(scrollPane);
		parent.add(Box.createVerticalStrut(10));

	}

	private void compileProject()
	{

		try
		{

			boolean success = compilationManager.compileProject();

			if (success)
			{

				JOptionPane.showMessageDialog(this, "项目编译成功", "编译完成", JOptionPane.INFORMATION_MESSAGE);

			} else
			{

				StringBuilder errorMsg = new StringBuilder("编译失败:\n");

				for (String error : compilationManager.getCompilationErrors())
				{

					errorMsg.append(error).append("\n");

				}

				JOptionPane.showMessageDialog(this, errorMsg.toString(), "编译错误", JOptionPane.ERROR_MESSAGE);

			}

		} catch (IOException e)
		{

			JOptionPane.showMessageDialog(this, "编译失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);

		}

	}

	private void cleanProject()
	{

		File targetDir = new File(projectRoot, "target");

		if (targetDir.exists())
		{

			try
			{

				Files.walk(targetDir.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile)
						.forEach(File::delete);
				JOptionPane.showMessageDialog(this, "项目清理完成", "清理完成", JOptionPane.INFORMATION_MESSAGE);

			} catch (IOException e)
			{

				JOptionPane.showMessageDialog(this, "清理失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);

			}

		}

	}

	private void formatAllFiles()
	{

		try
		{

			Files.walk(Paths.get(projectRoot, "src")).filter(path -> path.toString().endsWith(".java")).forEach(path ->
			{

				try
				{

					String content = new String(Files.readAllBytes(path));
					String formatted = codeFormatter.format(content);
					Files.write(path, formatted.getBytes());

				} catch (IOException e)
				{

					e.printStackTrace();

				}

			});
			JOptionPane.showMessageDialog(this, "代码格式化完成", "格式化", JOptionPane.INFORMATION_MESSAGE);

		} catch (IOException e)
		{

			JOptionPane.showMessageDialog(this, "格式化失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);

		}

	}

	private void configureJdk()
	{

		JDialog dialog = new JDialog(this, "JDK 配置", true);
		dialog.setLayout(new BorderLayout());

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);

		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(new JLabel("JAVA_HOME:"), gbc);

		JTextField javaHomeField = new JTextField(30);
		String currentJavaHome = System.getProperty("java.home");
		javaHomeField.setText(currentJavaHome);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(javaHomeField, gbc);

		JButton browseButton = new JButton("浏览...");
		gbc.gridx = 2;
		gbc.fill = GridBagConstraints.NONE;
		panel.add(browseButton, gbc);

		JButton okButton = new JButton("确定");
		JButton cancelButton = new JButton("取消");

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		browseButton.addActionListener(e ->
		{

			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION)
			{

				javaHomeField.setText(chooser.getSelectedFile().getAbsolutePath());

			}

		});

		okButton.addActionListener(e ->
		{

			String newJavaHome = javaHomeField.getText();

			if (validateJavaHome(newJavaHome))
			{

				System.setProperty("java.home", newJavaHome);
				dialog.dispose();
				JOptionPane.showMessageDialog(this, "JDK 配置已更新，请重启IDE生效", "配置更新", JOptionPane.INFORMATION_MESSAGE);

			} else
			{

				JOptionPane.showMessageDialog(this, "无效的 JDK 路径", "错误", JOptionPane.ERROR_MESSAGE);

			}

		});

		cancelButton.addActionListener(e -> dialog.dispose());

		dialog.add(panel, BorderLayout.CENTER);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		dialog.setSize(400, 300);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

	}

	private boolean validateJavaHome(String path)
	{

		if (path == null || path.trim().isEmpty())
		{

			return false;

		}

		File javaHome = new File(path);

		if (!javaHome.exists() || !javaHome.isDirectory())
		{

			return false;

		}

		// 检查是否存在 java 可执行文件
		File javaExe = new File(javaHome,
				"bin/java" + (System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : ""));
		return javaExe.exists() && javaExe.canExecute();

	}

	private void showProjectSettings()
	{

		JDialog dialog = new JDialog(this, "项目设置", true);
		dialog.setLayout(new BorderLayout());

		JTabbedPane tabbedPane = new JTabbedPane();

		// 编译设置面板
		JPanel compilePanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);

		gbc.gridx = 0;
		gbc.gridy = 0;
		compilePanel.add(new JLabel("源代码版本:"), gbc);

		String[] versions = { "1.8", "11", "17", "21" };
		JComboBox<String> sourceVersion = new JComboBox<>(versions);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		compilePanel.add(sourceVersion, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		compilePanel.add(new JLabel("目标版本:"), gbc);

		JComboBox<String> targetVersion = new JComboBox<>(versions);
		gbc.gridx = 1;
		compilePanel.add(targetVersion, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		compilePanel.add(new JLabel("编码:"), gbc);

		String[] encodings = { "UTF-8", "GBK", "ISO-8859-1" };
		JComboBox<String> encoding = new JComboBox<>(encodings);
		gbc.gridx = 1;
		compilePanel.add(encoding, gbc);

		tabbedPane.addTab("编译设置", compilePanel);

		// 格式化设置面板
		JPanel formatPanel = new JPanel(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);

		gbc.gridx = 0;
		gbc.gridy = 0;
		formatPanel.add(new JLabel("缩进字符:"), gbc);

		String[] indentTypes = { "空格", "Tab" };
		JComboBox<String> indentType = new JComboBox<>(indentTypes);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		formatPanel.add(indentType, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		formatPanel.add(new JLabel("缩进大小:"), gbc);

		SpinnerModel spinnerModel = new SpinnerNumberModel(4, 1, 8, 1);
		JSpinner indentSize = new JSpinner(spinnerModel);
		gbc.gridx = 1;
		formatPanel.add(indentSize, gbc);

		tabbedPane.addTab("格式化设置", formatPanel);

		// 按钮面板
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okButton = new JButton("确定");
		JButton cancelButton = new JButton("取消");

		okButton.addActionListener(e ->
		{

			// 保存设置
			saveProjectSettings(sourceVersion.getSelectedItem().toString(), targetVersion.getSelectedItem().toString(),
					encoding.getSelectedItem().toString(), indentType.getSelectedItem().toString(),
					(Integer) indentSize.getValue());
			dialog.dispose();

		});

		cancelButton.addActionListener(e -> dialog.dispose());

		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		dialog.add(tabbedPane, BorderLayout.CENTER);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		dialog.setSize(400, 300);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

	}

	private void saveProjectSettings(String sourceVersion, String targetVersion, String encoding, String indentType,
			int indentSize)
	{

		try
		{

			Properties props = new Properties();
			props.setProperty("source.version", sourceVersion);
			props.setProperty("target.version", targetVersion);
			props.setProperty("encoding", encoding);
			props.setProperty("indent.type", indentType);
			props.setProperty("indent.size", String.valueOf(indentSize));

			File settingsFile = new File(projectRoot, ".settings");

			if (!settingsFile.exists())
			{

				settingsFile.mkdir();

			}

			try (FileOutputStream out = new FileOutputStream(new File(settingsFile, "project.properties")))
			{

				props.store(out, "Project Settings");

			}

			JOptionPane.showMessageDialog(this, "项目设置已保存", "保存成功", JOptionPane.INFORMATION_MESSAGE);

		} catch (IOException e)
		{

			JOptionPane.showMessageDialog(this, "保存设置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);

		}

	}

	private void deleteCurrentFile()
	{

		if (currentFile != null)
		{

			int result = JOptionPane.showConfirmDialog(this, "确定要删除文件 " + currentFile.getName() + " 吗？", "确认删除",
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

			if (result == JOptionPane.YES_OPTION)
			{

				try
				{

					if (currentFile.delete())
					{

						JOptionPane.showMessageDialog(this, "文件已删除", "成功", JOptionPane.INFORMATION_MESSAGE);
						currentFile = null;
						codeEditor.setText("");
						refreshProject();

					} else
					{

						JOptionPane.showMessageDialog(this, "删除文件失败", "错误", JOptionPane.ERROR_MESSAGE);

					}

				} catch (SecurityException e)
				{

					JOptionPane.showMessageDialog(this, "删除文件失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);

				}

			}

		} else
		{

			JOptionPane.showMessageDialog(this, "请先打开一个文件", "提示", JOptionPane.INFORMATION_MESSAGE);

		}

	}

	// 添加自动编译方法
	private void autoCompile()
	{

		if (currentFile != null && currentFile.getName().endsWith(".java"))
		{

			try
			{

				// 先保存文件
				saveFile();

				// 执行编译
				boolean success = compilationManager.compile(currentFile);

				if (!success)
				{

					// 在控制台显示编译错误
					consoleOutput.setText("编译错误:\n");

					for (String error : compilationManager.getCompilationErrors())
					{

						consoleOutput.append(error + "\n");

					}

				} else
				{

					consoleOutput.setText("编译成功: " + currentFile.getName() + "\n");

				}

			} catch (IOException ex)
			{

				consoleOutput.setText("编译失败: " + ex.getMessage() + "\n");

			}

		}

	}

	// 添加一个自定义的LayeredPane布局管理器
	private class LayeredPaneLayout implements LayoutManager
	{

		@Override
		public void addLayoutComponent(String name, Component comp)
		{

		}

		@Override
		public void removeLayoutComponent(Component comp)
		{

		}

		@Override
		public Dimension preferredLayoutSize(Container parent)
		{

			return parent.getSize();

		}

		@Override
		public Dimension minimumLayoutSize(Container parent)
		{

			return new Dimension(0, 0);

		}

		@Override
		public void layoutContainer(Container parent)
		{

			// 设置滚动面板大小为整个容器大小
			Component[] components = parent.getComponents();

			for (Component comp : components)
			{

				if (comp instanceof JScrollPane)
				{

					comp.setBounds(0, 0, parent.getWidth(), parent.getHeight());

				} else if (comp instanceof JButton)
				{

					// 保持按钮在右上角
					JButton button = (JButton) comp;

					if (button.isVisible())
					{

						int buttonWidth = 120;
						int buttonHeight = 25;
						int margin = 10;
						button.setBounds(parent.getWidth() - buttonWidth - margin, margin, buttonWidth, buttonHeight);

					}

				}

			}

		}
	}

	// 添加刷新AI面板的方法
	private void refreshAIPanel()
	{

		if (aiPanel != null)
		{

			// 重新创建AI面板
			Container parent = aiPanel.getParent();

			if (parent != null)
			{

				parent.remove(aiPanel);
				aiPanel = createAIPanel();
				parent.add(aiPanel);
				parent.revalidate();
				parent.repaint();

			}

		}

	}

	// 添加新方法来更新知识库列表
	private void updateKnowledgeBaseList(String relativePath)
	{

		for (Component comp : aiPanel.getComponents())
		{

			if (comp instanceof JPanel && "knowledgeBasePanel".equals(comp.getName()))
			{

				JPanel knowledgeBasePanel = (JPanel) comp;

				for (Component listComp : knowledgeBasePanel.getComponents())
				{

					if (listComp instanceof JScrollPane)
					{

						JScrollPane scrollPane = (JScrollPane) listComp;

						if (scrollPane.getViewport().getView() instanceof JList)
						{

							@SuppressWarnings("unchecked")
							JList<String> fileList = (JList<String>) scrollPane.getViewport().getView();
							DefaultListModel<String> model = (DefaultListModel<String>) fileList.getModel();

							if (!model.contains(relativePath))
							{

								model.addElement(relativePath);

							}

							// 确保列表刷新显示
							fileList.revalidate();
							fileList.repaint();
							break;

						}

					}

				}

				break;

			}

		}

	}
}
