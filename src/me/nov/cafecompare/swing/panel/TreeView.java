package me.nov.cafecompare.swing.panel;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;

import javax.swing.*;
import javax.swing.tree.*;

import org.apache.commons.io.FilenameUtils;
import org.objectweb.asm.tree.ClassNode;

import com.github.weisj.darklaf.components.loading.LoadingIndicator;
import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.cafecompare.Cafecompare;
import me.nov.cafecompare.io.*;
import me.nov.cafecompare.swing.component.JTreeWithHint;
import me.nov.cafecompare.swing.drop.*;
import me.nov.cafecompare.swing.panel.tree.ClassTreeNode;
import me.nov.cafecompare.swing.panel.tree.renderer.ClassTreeCellRenderer;
import name.fraser.neil.plaintext.DiffMatchPatch;
import name.fraser.neil.plaintext.DiffMatchPatch.Operation;

public class TreeView extends JPanel {
  private static final long serialVersionUID = 1L;

  public static boolean autoSelect;

  private Cafecompare cafecompare;

  private ClassTree top;
  private ClassTree bottom;

  private JSplitPane split;

  private static final Icon analysis = IconLoader.get().loadSVGIcon("res/analysis.svg", false);

  public TreeView(Cafecompare cafecompare) {
    this.cafecompare = cafecompare;
    this.setLayout(new BorderLayout());
    top = new ClassTree(true);
    bottom = new ClassTree(false);
    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
    split.setResizeWeight(0.5);
    this.add(split, BorderLayout.CENTER);
  }

  public void updateAllNames(ClassTreeNode root) {
    root.updateClassName();
    for (int i = 0; i < root.getChildCount(); i++) {
      ClassTreeNode child = (ClassTreeNode) root.getChildAt(i);
      updateAllNames(child);
    }
  }

  public class ClassTree extends JTreeWithHint implements ILoader {
    private static final long serialVersionUID = 1L;
    public File inputFile;
    public List<Clazz> classes = new ArrayList<>();
    public DefaultTreeModel model;
    private boolean topPos;

    public ClassTree(boolean topPosition) {
      super(topPosition ? "Drag the old version of the jar or class file here" : "Drag the new version of the jar or class file here");
      this.topPos = topPosition;
      this.setRootVisible(false);
      this.setShowsRootHandles(true);
      this.setFocusable(true);
      this.setCellRenderer(new ClassTreeCellRenderer());
      ClassTreeNode root = new ClassTreeNode("");
      model = new DefaultTreeModel(root);
      this.setModel(model);
      ToolTipManager.sharedInstance().registerComponent(this);
      this.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
      this.addMouseListener(new MouseAdapter() {

        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2) {
            ClassTreeNode tn = (ClassTreeNode) getLastSelectedPathComponent();
            if (tn != null && tn.member != null) {
              cafecompare.code.load(topPosition, tn.member);

              if (autoSelect) {
                ClassTree opposite = topPosition ? bottom : top;
                Clazz corresponding = opposite.classes.stream().filter(c -> c.node.name.equals(tn.member.node.name)).findFirst().orElse(null);
                if (corresponding != null)
                  cafecompare.code.load(!topPosition, corresponding);
                else
                  JOptionPane.showMessageDialog(TreeView.this.getParent(), "No class with the same name in opposite jar.");
              }
            }
          }
          if (SwingUtilities.isRightMouseButton(e)) {
            ClassTreeNode tn = (ClassTreeNode) getLastSelectedPathComponent();
            if (tn != null && tn.member != null) {
              JPopupMenu pm = new JPopupMenu();
              JMenuItem edit = new JMenuItem("Find matching class by bytecode", analysis);
              edit.addActionListener(a -> {
                ClassTree target = topPosition ? bottom : top;
                long millis = target.classes.size() * (50 + 5); // 5 for ASMifier
                String warning = String.format("This will take about %d minutes and %d seconds. Are you sure?", TimeUnit.MILLISECONDS.toMinutes(millis),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
                if (JOptionPane.showConfirmDialog(TreeView.this.getParent(), warning, "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                  cafecompare.code.load(topPosition, tn.member);
                  selectMostMatching(tn.member, target, !topPosition);
                }
              });
              pm.add(edit);
              pm.show(ClassTree.this, e.getX(), e.getY());
            }
          }
        }
      });
      this.setTransferHandler(new JarDropHandler(this));
    }

    protected void selectMostMatching(Clazz member, ClassTree ct, boolean left) {
      if (ct.classes.contains(member))
        throw new IllegalArgumentException();
      String targetCode = Conversion.textify(member.node);
      Clazz bestMatch = null;
      int bestEditCount = (int) (targetCode.length() * 0.99);
      for (Clazz cz : ct.classes) {
        String bytecode = Conversion.textify(cz.node);
        DiffMatchPatch dmp = new DiffMatchPatch();
        dmp.Diff_Timeout = 0.05f;
        LinkedList<DiffMatchPatch.Diff> diff = dmp.diff_main(targetCode, bytecode);
        int edits = diff.stream().filter(d -> d.operation == Operation.INSERT || d.operation == Operation.DELETE).mapToInt(d -> d.text.length()).sum();
        if (edits < bestEditCount) {
          bestEditCount = edits;
          bestMatch = cz;
        }
        if (edits == 0)
          break;
      }
      if (bestMatch != null) {
        Toolkit.getDefaultToolkit().beep();
        cafecompare.code.load(left, bestMatch);
        JOptionPane.showMessageDialog(TreeView.this.getParent(), "Class found with " + Math.round((1.0 - (bestEditCount / (double) targetCode.length())) * 100) + "% match!");
      } else {
        JOptionPane.showMessageDialog(TreeView.this.getParent(), "No class matching more than 1% found.");
      }
    }

    @Override
    public Dimension getMinimumSize() {
      return new Dimension(200, 200);
    }

    @Override
    public void onFileDrop(File input) {
      String type = FilenameUtils.getExtension(input.getAbsolutePath());
      LoadingIndicator loadingLabel = new LoadingIndicator("Loading class file(s)... ", JLabel.CENTER);
      loadingLabel.setRunning(true);
      JPanel loadingPanel = new JPanel(new BorderLayout());
      int divLoc = split.getDividerLocation();
      if (topPos) {
        split.setLeftComponent(loadingPanel);
      } else {
        split.setRightComponent(loadingPanel);
      }
      loadingPanel.add(loadingLabel, BorderLayout.CENTER);
      split.setDividerLocation(divLoc);
      this.invalidate();
      this.validate();
      this.repaint();
      try {
        SwingUtilities.invokeLater(() -> {
          new Thread(() -> {
            this.inputFile = input;
            this.loadFile(type);
            loadTree(classes);
            model.reload();
            if (topPos) {
              split.setLeftComponent(new JScrollPane(ClassTree.this));
            } else {
              split.setRightComponent(new JScrollPane(ClassTree.this));
            }
            split.setDividerLocation(divLoc);
            this.invalidate();
            this.validate();
            this.repaint();
          }).start();
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    @SuppressWarnings("unchecked")
    public void loadTree(List<Clazz> classes) {
      ClassTreeNode root = new ClassTreeNode("");
      model = new DefaultTreeModel(root);
      classes.forEach(c -> {
        String[] packages = c.node.name.split("/");
        addToTree((ClassTreeNode) model.getRoot(), c, packages, 0);
      });
      for (Object n : Collections.list(root.depthFirstEnumeration())) {
        ClassTreeNode node = (ClassTreeNode) n;
        if (!node.isLeaf() && node != root) {
          if (node.getChildCount() == 1 && node.member == null) {
            ClassTreeNode child = (ClassTreeNode) node.getChildAt(0);
            if (child.member == null) {
              node.combinePackage(child);
            }
          }
        }
        node.sort();
      }
      this.setModel(model);
    }

    public void addToTree(ClassTreeNode current, Clazz c, String[] packages, int pckg) {
      String node = packages[pckg];
      if (packages.length - pckg <= 1) {
        current.add(new ClassTreeNode(c));
        return;
      }
      for (int i = 0; i < current.getChildCount(); i++) {
        ClassTreeNode child = (ClassTreeNode) current.getChildAt(i);
        if (child.toString().equals(node) && child.member == null) {
          addToTree(child, c, packages, ++pckg);
          return;
        }
      }
      ClassTreeNode newChild = new ClassTreeNode(node);
      current.add(newChild);
      addToTree(newChild, c, packages, ++pckg);
    }

    private void loadFile(String type) {
      try {
        switch (type) {
        case "jar":
          this.classes = JarIO.loadClasses(inputFile);
          if (classes.stream().anyMatch(c -> c.oldEntry.getCertificates() != null)) {
            JOptionPane.showMessageDialog(this,
                "<html>Warning: File is signed and may not load correctly, remove the signature<br>(<tt>META-INF\\MANIFEST.MF</tt>) and certificates (<tt>META-INF\\*.SF/.RSA</tt>) first!",
                "Signature warning", JOptionPane.WARNING_MESSAGE);
          }
          break;
        case "class":
          ClassNode node = Conversion.toNode(Files.readAllBytes(inputFile.toPath()));
          this.classes = new ArrayList<>(Collections.singletonList(new Clazz(node, new JarEntry(node.name), inputFile)));
          break;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

}
