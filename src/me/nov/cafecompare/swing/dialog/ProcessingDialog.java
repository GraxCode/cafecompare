package me.nov.cafecompare.swing.dialog;

import java.awt.*;
import java.util.function.Consumer;

import javax.swing.*;

import com.github.weisj.darklaf.components.loading.LoadingIndicator;

public class ProcessingDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  private LoadingIndicator li;
  private JProgressBar pb;
  private Consumer<ProcessingDialog> consumer;
  private Runnable then;

  public ProcessingDialog(Component parent, boolean progressBar, Consumer<ProcessingDialog> consumer) {
    this.consumer = consumer;
    this.setLocationRelativeTo(parent);
    this.setLayout(new BorderLayout());
    JPanel cp = new JPanel(new BorderLayout(16, 16));
    cp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    this.setTitle("Please be patient");
    li = new LoadingIndicator("Processing...");
    li.setRunning(true);
    JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER));
    center.add(li);
    cp.add(center, BorderLayout.CENTER);
    if (progressBar) {
      cp.add(pb = new JProgressBar(), BorderLayout.SOUTH);
      pb.setStringPainted(false);
    }
    this.add(cp, BorderLayout.CENTER);
    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    this.setAlwaysOnTop(true);
    this.setMinimumSize(new Dimension(600, 100));
    this.pack();
  }

  public void publish(float pc) {
    pb.setValue((int) pc);
  }

  public void setText(String text) {
    li.setText(text);
  }

  public ProcessingDialog go() {
    SwingUtilities.invokeLater(() -> {
      new Thread(() -> {
        this.setVisible(true);
        consumer.accept(ProcessingDialog.this);
        this.dispose();
        this.setVisible(false);
        Toolkit.getDefaultToolkit().beep();
        if (then != null)
          then.run();
      }).start();
    });
    return this;
  }

  public void then(Runnable then) {
    this.then = then;
  }
}
