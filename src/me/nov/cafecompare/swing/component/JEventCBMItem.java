package me.nov.cafecompare.swing.component;

import java.awt.event.*;
import java.util.function.Consumer;

import javax.swing.JCheckBoxMenuItem;

public class JEventCBMItem extends JCheckBoxMenuItem implements ActionListener {
  private static final long serialVersionUID = 1L;
  private Consumer<Boolean> event;

  public JEventCBMItem(String option, Consumer<Boolean> event) {
    super(option);
    this.event = event;
    this.addActionListener(this);
    
    // to make sure gui settings equal to program settings
    this.setSelected(false);
    this.actionPerformed(null);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    event.accept(isSelected());
  }
}
