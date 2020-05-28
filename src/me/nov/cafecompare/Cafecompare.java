package me.nov.cafecompare;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import com.github.weisj.darklaf.icons.IconLoader;
import com.github.weisj.darklaf.settings.ThemeSettings;

import me.nov.cafecompare.decompiler.*;
import me.nov.cafecompare.io.JarIO;
import me.nov.cafecompare.swing.Utils;
import me.nov.cafecompare.swing.component.*;
import me.nov.cafecompare.swing.laf.LookAndFeel;
import me.nov.cafecompare.swing.listener.ExitListener;
import me.nov.cafecompare.swing.panel.*;
import me.nov.cafecompare.swing.panel.tree.renderer.ClassTreeCellRenderer;

public class Cafecompare extends JFrame {
  private static final long serialVersionUID = 1L;

  public TreeView trees;
  public CodeView code;

  public Cafecompare() {
    this.initBounds();
    this.setTitle("Cafecompare " + Utils.getVersion());
    this.setIconImage(Utils.iconToImage(IconLoader.get().loadSVGIcon("res/analysis.svg", 64, 64, false)));
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new ExitListener(this));
    this.initializeFrame();
    this.initializeMenu();
  }

  private void initializeMenu() {
    JMenuBar bar = new JMenuBar();
    JMenu file = new JMenu("File");
    JMenuItem ws = new JMenuItem("Reset Workspace");
    ws.addActionListener(l -> {
      if (JOptionPane.showConfirmDialog(Cafecompare.this, "Do you really want to reset your workspace?", "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        this.dispose();
        new Cafecompare().setVisible(true);
      }
    });
    file.add(ws);
    JMenuItem save = new JMenuItem("Save bottom file");
    save.addActionListener(l -> {
      File inputFile = trees.bottom.inputFile;
      if (inputFile == null) {
        JOptionPane.showMessageDialog(this, "You have to load a jar file first.");
        return;
      }
      JFileChooser jfc = new JFileChooser(inputFile.getParentFile());
      jfc.setAcceptAllFileFilterUsed(false);
      jfc.setSelectedFile(new File(FilenameUtils.removeExtension(inputFile.getAbsolutePath()) + ".jar"));
      jfc.setDialogTitle("Save transformed jar archive");
      jfc.setFileFilter(new FileNameExtensionFilter("Java Package (*.jar)", "jar"));
      int result = jfc.showSaveDialog(this);
      if (result == JFileChooser.APPROVE_OPTION) {
        File output = jfc.getSelectedFile();
        JarIO.saveAsJar(inputFile, output, trees.bottom.classes);
      }
      save.setEnabled(true);
    });
    file.add(save);
    bar.add(file);
    JMenu tools = new JMenu("Tools");
    JMenuItem remap = new JMenuItem("Remap class names by similarity");
    remap.addActionListener(l -> trees.remapByClassNames());
    tools.add(remap);
    JMenu treeTools = new JMenu("Tree");
    JMenuItem hide = new JMenuItem("Hide classes with same hashes");
    hide.addActionListener(l -> trees.hideEqual());
    treeTools.add(hide);
    JMenuItem reload = new JMenuItem("Reload tree");
    reload.addActionListener(l -> trees.reload());
    treeTools.add(reload);
    JMenuItem swap = new JMenuItem("Swap trees");
    swap.addActionListener(l -> trees.swap());
    treeTools.add(swap);
    tools.add(treeTools);
    bar.add(tools);
    JMenu options = new JMenu("Options");

    JMenu decompiler = new JMenu("Decompiler");
    ButtonGroup group = new ButtonGroup();
    JRadioButtonMenuItem cfr = new JEventRBMItem("CFR 0.149", group, () -> CodeView.decompilerBridge = new CFRBridge());
    JRadioButtonMenuItem fernflower = new JEventRBMItem("Fernflower 15-05-20", group, () -> CodeView.decompilerBridge = new FernflowerBridge());
    fernflower.setSelected(true);
    JRadioButtonMenuItem asmifier = new JEventRBMItem("Bytecode", group, () -> CodeView.decompilerBridge = new ASMifierBridge());
    decompiler.add(cfr);
    decompiler.add(fernflower);
    decompiler.add(asmifier);

    options.add(decompiler);
    JMenu tree = new JMenu("Tree");
    tree.add(new JEventCBMItem("Auto select relative class", selected -> TreeView.autoSelect = selected));

    tree.add(new JEventCBMItem("Highlight new / removed files", selected -> ClassTreeCellRenderer.viewDiffs = selected, true));

    options.add(tree);
    bar.add(options);
    JMenu help = new JMenu("Help");
    JMenuItem laf = new JMenuItem("Look and feel settings");
    laf.addActionListener(l -> ThemeSettings.showSettingsDialog(this));
    JMenuItem about = new JMenuItem("About cafecompare " + Utils.getVersion());
    about.addActionListener(l -> JOptionPane.showMessageDialog(this, "<html>Threadtear was made by <i>noverify</i> a.k.a <i>GraxCode</i> in 2020.<br><br>"
        + "This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.<br>You are welcome to contribute to this project on GitHub!", "About", JOptionPane.INFORMATION_MESSAGE));
    help.add(about);
    help.add(laf);
    bar.add(help);
    this.setJMenuBar(bar);
  }

  private void initializeFrame() {
    this.setLayout(new BorderLayout(16, 16));
    trees = new TreeView(this);
    code = new CodeView(this);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, Utils.addTitleAndBorder("Files to compare", trees), code);
    Utils.setDividerLocation(split, 0.3);
    split.putClientProperty("JSplitPane.style", "invisible");
    JPanel content = new JPanel(new BorderLayout());
    content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    content.add(split, BorderLayout.CENTER);
    this.add(content, BorderLayout.CENTER);
  }

  private void initBounds() {
    Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    int width = (int) (screenSize.width * 0.75);
    int height = (int) (screenSize.height * 0.75);
    setBounds(screenSize.width / 2 - width / 2, screenSize.height / 2 - height / 2, width, height);
    setMinimumSize(new Dimension((int) (width / 1.25), (int) (height / 1.25)));
  }

  public static void main(String[] args) throws Exception {
    LookAndFeel.setLookAndFeel();
    configureEnvironment();
    new Cafecompare().setVisible(true);
  }

  private static void configureEnvironment() throws Exception {
    System.setProperty("file.encoding", "UTF-8");
    Field charset = Charset.class.getDeclaredField("defaultCharset");
    charset.setAccessible(true);
    charset.set(null, null);
  }
}
