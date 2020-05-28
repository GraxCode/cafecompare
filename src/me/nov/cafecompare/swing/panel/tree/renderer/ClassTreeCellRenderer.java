package me.nov.cafecompare.swing.panel.tree.renderer;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.cafecompare.asm.Access;
import me.nov.cafecompare.io.Clazz;
import me.nov.cafecompare.swing.panel.TreeView;
import me.nov.cafecompare.swing.panel.tree.ClassTreeNode;

public class ClassTreeCellRenderer extends DefaultTreeCellRenderer implements Opcodes {
  private static final long serialVersionUID = 1L;

  private Icon pack, clazz, enu, itf, added, removed, equal;

  private TreeView treeView;

  public static boolean viewDiffs = true;

  public ClassTreeCellRenderer(TreeView treeView) {
    this.treeView = treeView;
    this.pack = IconLoader.get().loadSVGIcon("res/package.svg", false);
    this.clazz = IconLoader.get().loadSVGIcon("res/class.svg", false);
    this.enu = IconLoader.get().loadSVGIcon("res/enum.svg", false);
    this.itf = IconLoader.get().loadSVGIcon("res/interface.svg", false);
    this.added = IconLoader.get().loadSVGIcon("res/add.svg", 10, 10, false);
    this.equal = IconLoader.get().loadSVGIcon("res/equal.svg", 10, 10, false);
    this.removed = IconLoader.get().loadSVGIcon("res/remove.svg", 10, 10, false);
  }

  @Override
  public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
    super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    if (node instanceof ClassTreeNode) {
      Clazz member = ((ClassTreeNode) node).member;
      if (member != null) {
        ClassNode cn = member.node;
        if (Access.isInterface(cn.access)) {
          this.setIcon(this.itf);
        } else if (Access.isEnum(cn.access)) {
          this.setIcon(this.enu);
        } else {
          this.setIcon(this.clazz);
        }
        if (viewDiffs && treeView.bottom != null) {
          if (tree == treeView.bottom) {
            if (treeView.top.classes.stream().noneMatch(c -> c.node.name.equals(member.node.name))) {
              this.setIcon(new OverlayIcon(getIcon(), added));
            } else if (treeView.top.classes.stream().anyMatch(cl -> cl.oldEntry.getCrc() == member.oldEntry.getCrc())) {
              this.setIcon(new OverlayIcon(getIcon(), equal));
            }
          } else {
            if (treeView.bottom.classes.stream().noneMatch(c -> c.node.name.equals(member.node.name))) {
              this.setIcon(new OverlayIcon(getIcon(), removed));
            } else if (treeView.bottom.classes.stream().anyMatch(cl -> cl.oldEntry.getCrc() == member.oldEntry.getCrc())) {
              this.setIcon(new OverlayIcon(getIcon(), equal));
            }
          }
        }
      } else {
        this.setIcon(this.pack);
      }
    }
    return this;
  }

  @Override
  public Font getFont() {
    return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
  }
}
