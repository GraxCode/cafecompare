package me.nov.cafecompare.remapping;

import java.util.*;

import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.ClassNode;

import me.nov.cafecompare.io.Clazz;

public class FullRemapper {
  private List<Clazz> classes;

  public FullRemapper(List<Clazz> classes) {
    this.classes = classes;
  }

  public void remap(Map<String, String> mappings) {
    for (Clazz original : classes) {
      ClassNode updated = new ClassNode();
      original.node.accept(new ClassRemapper(updated, new Remapper() {
        @Override
        public String map(String internalName) {
          return mappings.getOrDefault(internalName, internalName);
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
          return mappings.getOrDefault(owner + "." + name + descriptor, name);
        }
      }));
      original.node = updated;
    }
  }
}
