package me.nov.cafecompare.swing.panel.tree.renderer;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.cafecompare.asm.Access;
import me.nov.cafecompare.io.Clazz;
import me.nov.cafecompare.swing.panel.tree.ClassTreeNode;

public class ClassTreeCellRenderer extends DefaultTreeCellRenderer implements Opcodes {
  private static final long serialVersionUID = 1L;

  private Icon pack, clazz, enu, itf;

  public ClassTreeCellRenderer() {
    this.pack = IconLoader.get().loadSVGIcon("res/package.svg", false);
    this.clazz = IconLoader.get().loadSVGIcon("res/class.svg", false);
    this.enu = IconLoader.get().loadSVGIcon("res/enum.svg", false);
    this.itf = IconLoader.get().loadSVGIcon("res/interface.svg", false);
  }

  @Override
  public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
    super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    if (node instanceof ClassTreeNode) {
      Clazz clazz = ((ClassTreeNode) node).member;
      if (clazz != null) {
        ClassNode cn = clazz.node;
        if (Access.isInterface(cn.access)) {
          this.setIcon(this.itf);
        } else if (Access.isEnum(cn.access)) {
          this.setIcon(this.enu);
        } else {
          this.setIcon(this.clazz);
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
