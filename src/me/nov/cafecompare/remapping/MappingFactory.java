package me.nov.cafecompare.remapping;

import java.util.HashMap;

import org.objectweb.asm.tree.MethodNode;

import me.nov.cafecompare.diff.DiffMath;
import me.nov.cafecompare.io.*;

public class MappingFactory {
  private final HashMap<String, String> mappings = new HashMap<>();

  public MappingFactory remapMethods(Clazz source, Clazz target) {
    HashMap<MethodNode, String> bytecode = new HashMap<>();
    for (MethodNode mn : source.node.methods) {
      bytecode.put(mn, Conversion.textify(mn));
    }
    for (MethodNode mn : target.node.methods) {
      bytecode.put(mn, Conversion.textify(mn));
    }
    for (MethodNode method : target.node.methods) {
      if (method.instructions.size() < 5)
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
        if (confidence > 95)
          break;
      }
      if (bestConfidence > 50) {
        mappings.put(target.node.name + "." + method.name + method.desc, bestMatch.name);
      }
    }
    return this;
  }

  public HashMap<String, String> get() {
    return mappings;
  }

  public MappingFactory with(String oldName, String newName) {
    mappings.put(oldName, newName);
    return this;
  }
}
