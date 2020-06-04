package me.nov.cafecompare.remapping;

import java.util.*;
import java.util.concurrent.*;

import org.objectweb.asm.tree.MethodNode;

import me.nov.cafecompare.asm.Access;
import me.nov.cafecompare.diff.DiffMath;
import me.nov.cafecompare.io.*;
import me.nov.cafecompare.swing.dialog.ProcessingDialog;

public class MappingFactory {
  private final Map<String, String> mappings = new HashMap<>();
  
  public static float CLASS_INTERRUPT_CONF = 90;
  public static float MIN_CLASS_CONF = 25;
  
  public static float METH_INTERRUPT_CONF = 95;
  public static float MIN_METH_CONF = 65;

  public MappingFactory remapMethods(Clazz source, Clazz target, ProcessingDialog p) {
    p.setText("Calculating code...");
    HashMap<MethodNode, String> bytecode = new HashMap<>();
    for (MethodNode mn : source.node.methods) {
      bytecode.put(mn, Conversion.textify(mn));
    }
    for (MethodNode mn : target.node.methods) {
      bytecode.put(mn, Conversion.textify(mn));
    }
    p.setText("Comparing methods...");
    List<MethodNode> methods = target.node.methods;
    float size = methods.size();
    for (int i = 0; i < size; i++) {
      MethodNode method = methods.get(i);
      if (method.instructions.size() < 4)
        continue;
      String targetCode = bytecode.get(method);
      MethodNode bestMatch = null;
      float bestConfidence = 0;
      for (MethodNode equivalent : source.node.methods) {
        float confidence = DiffMath.confidencePercent(targetCode, bytecode.get(equivalent));
        if (confidence > bestConfidence) {
          bestConfidence = confidence;
          bestMatch = equivalent;
        }
        if (confidence > METH_INTERRUPT_CONF)
          break;
      }
      p.publish(i / size * 100);
      if (bestConfidence > MIN_METH_CONF) {
        mappings.put(target.node.name + "." + method.name + method.desc, bestMatch.name);
      }
    }
    return this;
  }

  public Map<String, String> get() {
    return mappings;
  }

  public MappingFactory with(String oldName, String newName) {
    mappings.put(oldName, newName);
    return this;
  }

  private int finished = 0;

  public MappingFactory remap(List<Clazz> source, List<Clazz> target, ProcessingDialog p) {
    p.setText("Calculating code...");
    Map<Clazz, String> bytecode = new HashMap<>();
    for (Clazz cz : target) {
      bytecode.put(cz, Conversion.textify(cz.node));
    }
    for (Clazz cz : source) {
      bytecode.put(cz, Conversion.textify(cz.node));
    }
    p.setText("Comparing class...");
    float size = target.size();

    int index = 0;
    finished = 0;

    int threads = Runtime.getRuntime().availableProcessors();
    ExecutorService service = Executors.newFixedThreadPool(threads);
    while (index < size) {
      Clazz clazz = target.get(index);
      p.setText("Comparing class " + clazz.node.name);
      service.execute(() -> {
        findCommon(new ArrayList<>(source), bytecode, clazz);
        finished++;
        p.publish(finished / size * 100);
      });
      // slot is free, add thread and go
      index++;
    }
    service.shutdown();
    try {
      service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
    }
    return this;
  }

  private void findCommon(List<Clazz> source, Map<Clazz, String> bytecode, Clazz original) {
    String targetCode = bytecode.get(original);
    Clazz bestMatch = null;
    float bestConfidence = 0;
    boolean abstr = Access.isAbstract(original.node.access);
    boolean itf = Access.isInterface(original.node.access);
    int methods = original.node.methods.size();
    int fields = original.node.fields.size();

    Collections.sort(source, (a, b) -> {
      int adif = (Math.abs(a.node.methods.size() - methods) + 1) * (Math.abs(a.node.fields.size() - fields) + 1);
      int bdif = (Math.abs(b.node.methods.size() - methods) + 1) * (Math.abs(b.node.fields.size() - fields) + 1);
      return Integer.compare(adif, bdif);
    });
    for (Clazz cz : source) {
      if (abstr != Access.isAbstract(cz.node.access))
        continue;
      if (itf != Access.isInterface(cz.node.access))
        continue;

      float confidence = DiffMath.confidencePercent(targetCode, bytecode.get(cz));
      if (confidence > bestConfidence) {
        bestConfidence = confidence;
        bestMatch = cz;
      }
      if (confidence > CLASS_INTERRUPT_CONF)
        break;
    }
    if (bestConfidence > MIN_CLASS_CONF) {
      synchronized (mappings) {
        mappings.put(original.node.name, bestMatch.node.name);
      }
    }
  }
}
