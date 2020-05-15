package me.nov.cafecompare.io;

import java.io.*;
import java.util.ArrayList;
import java.util.jar.*;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;

public class JarIO {

  public static ArrayList<Clazz> loadClasses(File jarFile) throws IOException {
    ArrayList<Clazz> classes = new ArrayList<Clazz>();
    JarFile jar = new JarFile(jarFile);
    Stream<JarEntry> str = jar.stream();
    str.forEach(z -> readEntry(jar, z, classes));
    jar.close();
    return classes;
  }

  private static ArrayList<Clazz> readEntry(JarFile jar, JarEntry en, ArrayList<Clazz> classes) {
    try (InputStream jis = jar.getInputStream(en)) {
      byte[] bytes = IOUtils.toByteArray(jis);
      if (isClassFile(bytes)) {
        try {
          final ClassNode cn = Conversion.toNode(bytes);
          if (cn != null && (cn.superName != null || (cn.name != null && cn.name.equals("java/lang/Object")))) {
            classes.add(new Clazz(cn, en, jar));
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return classes;
  }

  public static File writeTempJar(String name, byte[] clazz) {
    try {
      File temp = File.createTempFile("temp-jar", ".jar");
      JarOutputStream out = new JarOutputStream(new FileOutputStream(temp));
      out.putNextEntry(new JarEntry(name + ".class"));
      out.write(clazz);
      out.closeEntry();
      out.close();
      return temp;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isClassFile(byte[] bytes) {
    return bytes.length >= 4 && String.format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]).equals("CAFEBABE");
  }
}
