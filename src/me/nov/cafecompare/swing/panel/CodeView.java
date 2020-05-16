package me.nov.cafecompare.swing.panel;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.objectweb.asm.tree.MethodNode;

import com.github.weisj.darklaf.components.loading.LoadingIndicator;
import com.github.weisj.darklaf.icons.IconLoader;
import com.github.weisj.darklaf.ui.text.DarkTextUI;

import me.nov.cafecompare.Cafecompare;
import me.nov.cafecompare.decompiler.*;
import me.nov.cafecompare.diff.DiffMath;
import me.nov.cafecompare.io.*;
import me.nov.cafecompare.remapping.FullRemapper;
import me.nov.cafecompare.swing.Utils;
import me.nov.cafecompare.swing.textarea.*;
import me.nov.cafecompare.thread.ThreadKiller;
import me.nov.cafecompare.utils.Strings;
import name.fraser.neil.plaintext.DiffMatchPatch;

public class CodeView extends JPanel implements ActionListener {
  private static final long serialVersionUID = 1L;

  public DecompilerTextArea left;
  public DecompilerTextArea right;
  private JSplitPane split;

  private JCheckBox aggress;
  private JLabel similarity;

  private JComboBox<String> accuracy;

  public static IDecompilerBridge decompilerBridge = new FernflowerBridge();
  public static int editCost = 4;

  public CodeView(Cafecompare cafecompare) {
    this.setLayout(new BorderLayout());
    JPanel inner = new JPanel();
    inner.setLayout(new GridLayout(1, 2, 16, 16));
    left = new DecompilerTextArea();
    right = new DecompilerTextArea();
    split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, Utils.addTitleAndBorder("Code of top file", new WelcomePane()), Utils.addTitleAndBorder("Code of bottom file", new WelcomePane()));
    split.putClientProperty("JSplitPane.style", "invisible");
    split.setResizeWeight(0.5);
    inner.add(split);
    this.add(inner, BorderLayout.CENTER);
    JPanel leftActionPanel = new JPanel();
    leftActionPanel.setLayout(new GridBagLayout());
    leftActionPanel.add(aggress = new JCheckBox("Decompile aggressively"));
    aggress.addActionListener(this);
    leftActionPanel.add(accuracy = new JComboBox<String>(new String[] { "Fast", "Accurate", "Very accurate", "High precision" }));
    accuracy.setSelectedIndex(1);
    accuracy.addActionListener(this);
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(0, 8, 0, 0);
    leftActionPanel.add(similarity = new JLabel(""), c);
    JPanel rightActionPanel = new JPanel();
    rightActionPanel.setLayout(new GridBagLayout());
    JButton reload = new JButton(IconLoader.get().loadSVGIcon("res/refresh.svg", false));
    reload.addActionListener(this);
    JTextField search = new JTextField();
    search.putClientProperty(DarkTextUI.KEY_DEFAULT_TEXT, "Search for text or regex");
    search.setPreferredSize(new Dimension(200, reload.getPreferredSize().height));
    search.addActionListener(l -> {
      highlightSearch(left, search.getText());
      highlightSearch(right, search.getText());
    });

    JButton remap = new JButton("Remap right by left");
    remap.addActionListener(l -> {
      String oldName = right.last.node.name;
      String newName = left.last.node.name;
      List<Clazz> classes = cafecompare.trees.bottom.classes;
      HashMap<String, String> mappings = new HashMap<>();
      mappings.put(oldName, newName);
      HashMap<MethodNode, String> bytecode = new HashMap<>();
      for (MethodNode mn : left.last.node.methods) {
        bytecode.put(mn, Conversion.textify(mn));
      }
      for (MethodNode mn : right.last.node.methods) {
        bytecode.put(mn, Conversion.textify(mn));
      }

      for (MethodNode method : right.last.node.methods) {
        if (method.instructions.size() < 5)
          continue;
        String targetCode = bytecode.get(method);
        MethodNode bestMatch = null;
        float bestConfidence = 0;
        for (MethodNode equivalent : left.last.node.methods) {
          float confidence = DiffMath.confidencePercent(targetCode, bytecode.get(equivalent));
          if (confidence > bestConfidence) {
            bestConfidence = confidence;
            bestMatch = equivalent;
          }
          if (confidence > 95)
            break;
        }
        if (bestConfidence > 50) {
          mappings.put(oldName + "." + method.name + method.desc, bestMatch.name);
        }
      }
      new FullRemapper(classes).remap(mappings);
      cafecompare.trees.bottom.loadTree(classes);
      load(false, right.last);
    });
    rightActionPanel.add(remap);
    rightActionPanel.add(search);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.EAST;
    rightActionPanel.add(reload, gbc);
    JPanel topPanel = new JPanel();
    topPanel.setBorder(new EmptyBorder(1, 5, 0, 1));
    topPanel.setLayout(new BorderLayout());
    topPanel.add(leftActionPanel, BorderLayout.WEST);
    topPanel.add(rightActionPanel, BorderLayout.EAST);
    this.add(topPanel, BorderLayout.SOUTH);
  }

  private String lastSearchText;
  private int searchIndex;

  private void highlightSearch(DecompilerTextArea textArea, String searchText) {
    try {
      if (searchText.trim().isEmpty()) {
        textArea.getHighlighter().removeAllHighlights();
        return;
      }
      searchText = searchText.toLowerCase();
      if (!Objects.equals(searchText, lastSearchText)) {
        searchIndex = -1;
        lastSearchText = searchText;
      }
      String[] split = textArea.getText().split("\\r?\\n");
      int firstIndex = -1;
      boolean first = false;
      Label: {
        for (int i = 0; i < split.length; i++) {
          String line = split[i];
          if (Strings.containsRegex(line, searchText)) {
            if (i > searchIndex) {
              textArea.setCaretPosition(textArea.getDocument().getDefaultRootElement().getElement(i).getStartOffset());
              searchIndex = i;
              break Label;
            } else if (!first) {
              firstIndex = i;
              first = true;
            }
          }
        }
        Toolkit.getDefaultToolkit().beep();
        if (first) {
          // go back to first line
          textArea.setCaretPosition(textArea.getDocument().getDefaultRootElement().getElement(firstIndex).getStartOffset());
          searchIndex = firstIndex;
        }
      }
      Highlighter highlighter = textArea.getHighlighter();
      highlighter.removeAllHighlights();
      Document document = textArea.getDocument();
      String fullText = document.getText(0, document.getLength()).toLowerCase();
      int pos = fullText.indexOf(searchText);
      while (pos >= 0) {
        highlighter.addHighlight(pos, pos + searchText.length(), new DefaultHighlighter.DefaultHighlightPainter(new Color(0x0078d7)));
        pos = fullText.indexOf(searchText, pos + searchText.length());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setSimilarityText(float pc) {
    String color;
    if (pc > 50) {
      color = "<font color=\"green\">";
    } else if (pc > 25) {
      color = "<font color=\"yellow\">";
    } else {
      color = "<font color=\"red\">";
    }
    similarity.setText("<html>" + color + "  " + (Math.round(pc * 100f) / 100f) + "%</font> similarity");
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (left.last != null)
      load(true, left.last);
    if (right.last != null)
      load(false, right.last);

  }

  private float getAccuracy() {
    switch (accuracy.getSelectedIndex()) {
    case 0:
      return 0.25f;
    default:
    case 1:
      return 1f;
    case 2:
      return 2f;
    case 3:
      return 5f;
    }
  }

  public void load(boolean leftPanel, Clazz member) {
    LoadingIndicator loadingLabel = new LoadingIndicator("Decompiling class file... ", JLabel.CENTER);
    loadingLabel.setRunning(true);
    int divLoc = split.getDividerLocation();
    if (leftPanel) {
      split.setLeftComponent(loadingLabel);
    } else {
      split.setRightComponent(loadingLabel);
    }
    split.setDividerLocation(divLoc);
    this.invalidate();
    this.validate();
    this.repaint();
    SwingUtilities.invokeLater(() -> {
      Thread decompileThread = new Thread(() -> {
        synchronized (decompilerBridge) {
          decompilerBridge.setAggressive(aggress.isSelected());
          String decompiled = decompilerBridge.decompile(member.node.name, Conversion.toBytecode0(member.node));
          if (leftPanel) {
            left.setText(decompiled);
            split.setLeftComponent(Utils.addTitleAndBorder("Top tree class - " + member.node.name.replace('/', '.'), makeSP(left)));
            left.last = member;
          } else {
            right.setText(decompiled);
            split.setRightComponent(Utils.addTitleAndBorder("Bottom tree class - " + member.node.name.replace('/', '.'), makeSP(right)));
            right.last = member;
          }
          try {
            calculateDiff(getAccuracy());
          } catch (BadLocationException e) {
            e.printStackTrace();
          }
        }
        split.setDividerLocation(divLoc);
        invalidate();
        validate();
        repaint();
      });
      decompileThread.start();
      new ThreadKiller(decompileThread, 7500).start();
    });
  }

  private Component makeSP(DecompilerTextArea textArea) {
    RTextScrollPane scp = new RTextScrollPane(textArea);
    scp.getVerticalScrollBar().setUnitIncrement(16);
    return scp;
  }

  private static final Color red = new Color(0xe74c3c).darker().darker();
  private static final Color green = new Color(0x2ecc71).darker().darker();

  private void calculateDiff(float timeout) throws BadLocationException {
    if (left.getText().trim().isEmpty() || right.getText().trim().isEmpty()) {
      left.getHighlighter().removeAllHighlights();
      right.getHighlighter().removeAllHighlights();
      return;
    }
    DiffMatchPatch dmp = new DiffMatchPatch();
    dmp.Diff_Timeout = timeout;
    dmp.Diff_EditCost = (short) editCost;
    LinkedList<DiffMatchPatch.Diff> diff = dmp.diff_main(left.getText(), right.getText());
    dmp.diff_cleanupSemantic(diff);
    int leftpos = 0;
    int rightpos = 0;
    left.getHighlighter().removeAllHighlights();
    right.getHighlighter().removeAllHighlights();
    for (DiffMatchPatch.Diff d : diff) {
      switch (d.operation) {
      case DELETE:
        left.getHighlighter().addHighlight(leftpos, leftpos + d.text.length(), new DefaultHighlighter.DefaultHighlightPainter(red));
        leftpos += d.text.length();
        continue;
      case INSERT:
        right.getHighlighter().addHighlight(rightpos, rightpos + d.text.length(), new DefaultHighlighter.DefaultHighlightPainter(green));
        rightpos += d.text.length();
        break;
      case EQUAL:
        leftpos += d.text.length();
        rightpos += d.text.length();
        break;
      }
    }
    int edits = dmp.diff_levenshtein(diff);
    int size = diff.stream().mapToInt(d -> d.text.length()).sum();
    float sim = 100 - (100 * (edits / (float) size));
    setSimilarityText(sim);
  }
}
