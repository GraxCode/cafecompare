package me.nov.cafecompare.asm;

import java.util.HashMap;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class EasyTextifer extends Textifier {
  private static final String CLASS_SUFFIX = ".class";
  private static final String DEPRECATED = "// DEPRECATED\n";
  private static final String RECORD = "// RECORD\n";
  private static final String INVISIBLE = " // invisible\n";

  private int access;
  private int numAnnotationValues;

  /**
   * Constructs a new {@link EasyTextifer}. <i>Subclasses must not use this constructor</i>. Instead, they must use the {@link #EasyTextifer(int)} version.
   *
   * @throws IllegalStateException If a subclass calls this constructor.
   */
  public EasyTextifer() {
    this(Opcodes.ASM8);
  }

  /**
   * Constructs a new {@link EasyTextifer}.
   *
   * @param api the ASM API version implemented by this visitor. Must be one of {@link Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}, {@link Opcodes#ASM7} or
   *            {@link Opcodes#ASM8}.
   */
  protected EasyTextifer(final int api) {
    super(api);
  }

  // -----------------------------------------------------------------------------------------------
  // Classes
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
    if ((access & Opcodes.ACC_MODULE) != 0) {
      // Modules are printed in visitModule.
      return;
    }
    this.access = access;
    stringBuilder.setLength(0);
    if ((access & Opcodes.ACC_DEPRECATED) != 0) {
      stringBuilder.append(DEPRECATED);
    }
    if ((access & Opcodes.ACC_RECORD) != 0) {
      stringBuilder.append(RECORD);
    }
    appendRawAccess(access);

    appendDescriptor(CLASS_SIGNATURE, signature);
    if (signature != null) {
      appendJavaDeclaration(name, signature);
    }

    appendAccess(access & ~(Opcodes.ACC_SUPER | Opcodes.ACC_MODULE));
    if ((access & Opcodes.ACC_ANNOTATION) != 0) {
      stringBuilder.append("@interface ");
    } else if ((access & Opcodes.ACC_INTERFACE) != 0) {
      stringBuilder.append("interface ");
    } else if ((access & Opcodes.ACC_ENUM) == 0) {
      stringBuilder.append("class ");
    }
    appendDescriptor(INTERNAL_NAME, name);

    if (superName != null && !"java/lang/Object".equals(superName)) {
      stringBuilder.append(" extends ");
      appendDescriptor(INTERNAL_NAME, superName);
    }
    if (interfaces != null && interfaces.length > 0) {
      stringBuilder.append(" implements ");
      for (int i = 0; i < interfaces.length; ++i) {
        appendDescriptor(INTERNAL_NAME, interfaces[i]);
        if (i != interfaces.length - 1) {
          stringBuilder.append(' ');
        }
      }
    }
    stringBuilder.append(" {\n\n");

    text.add(stringBuilder.toString());
  }

  @Override
  public void visitSource(final String file, final String debug) {
  }

  @Override
  public Printer visitModule(final String name, final int access, final String version) {
    stringBuilder.setLength(0);
    if ((access & Opcodes.ACC_OPEN) != 0) {
      stringBuilder.append("open ");
    }
    stringBuilder.append("module ").append(name).append(" { ").append(version == null ? "" : "// " + version).append("\n\n");
    text.add(stringBuilder.toString());
    return addNewTextifier(null);
  }

  @Override
  public void visitNestHost(final String nestHost) {
  }

  @Override
  public void visitOuterClass(final String owner, final String name, final String descriptor) {
  }

  @Override
  public EasyTextifer visitClassAnnotation(final String descriptor, final boolean visible) {
    text.add("\n");
    return visitAnnotation(descriptor, visible);
  }

  @Override
  public Printer visitClassTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    text.add("\n");
    return visitTypeAnnotation(typeRef, typePath, descriptor, visible);
  }

  @Override
  public void visitClassAttribute(final Attribute attribute) {
    text.add("\n");
    visitAttribute(attribute);
  }

  @Override
  public void visitNestMember(final String nestMember) {
  }

  @Override
  @Deprecated
  public void visitPermittedSubtypeExperimental(final String permittedSubtype) {
  }

  @Override
  public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
  }

  @Override
  public Printer visitRecordComponent(final String name, final String descriptor, final String signature) {
    return addNewTextifier(null);
  }

  @Override
  public EasyTextifer visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
    stringBuilder.setLength(0);
    stringBuilder.append('\n');
    if ((access & Opcodes.ACC_DEPRECATED) != 0) {
      stringBuilder.append(tab).append(DEPRECATED);
    }
    stringBuilder.append(tab);
    appendRawAccess(access);
    if (signature != null) {
      stringBuilder.append(tab);
      appendDescriptor(FIELD_SIGNATURE, signature);
      stringBuilder.append(tab);
      appendJavaDeclaration(name, signature);
    }

    stringBuilder.append(tab);
    appendAccess(access);

    appendDescriptor(FIELD_DESCRIPTOR, descriptor);
    stringBuilder.append(' ').append(name);
    if (value != null) {
      stringBuilder.append(" = ");
      if (value instanceof String) {
        stringBuilder.append('\"').append(value).append('\"');
      } else {
        stringBuilder.append(value);
      }
    }

    stringBuilder.append('\n');
    text.add(stringBuilder.toString());
    return addNewTextifier(null);
  }

  @Override
  public EasyTextifer visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
    stringBuilder.setLength(0);
    stringBuilder.append('\n');
    if ((access & Opcodes.ACC_DEPRECATED) != 0) {
      stringBuilder.append(tab).append(DEPRECATED);
    }
    stringBuilder.append(tab);
    appendRawAccess(access);

    if (signature != null) {
      stringBuilder.append(tab);
      appendDescriptor(METHOD_SIGNATURE, signature);
      stringBuilder.append(tab);
      appendJavaDeclaration(name, signature);
    }

    stringBuilder.append(tab);
    appendAccess(access & ~(Opcodes.ACC_VOLATILE | Opcodes.ACC_TRANSIENT));
    if ((access & Opcodes.ACC_NATIVE) != 0) {
      stringBuilder.append("native ");
    }
    if ((access & Opcodes.ACC_VARARGS) != 0) {
      stringBuilder.append("varargs ");
    }
    if ((access & Opcodes.ACC_BRIDGE) != 0) {
      stringBuilder.append("bridge ");
    }
    if ((this.access & Opcodes.ACC_INTERFACE) != 0 && (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC)) == 0) {
      stringBuilder.append("default ");
    }

    stringBuilder.append(name);
    appendDescriptor(METHOD_DESCRIPTOR, descriptor);
    if (exceptions != null && exceptions.length > 0) {
      stringBuilder.append(" throws ");
      for (String exception : exceptions) {
        appendDescriptor(INTERNAL_NAME, exception);
        stringBuilder.append(' ');
      }
    }

    stringBuilder.append('\n');
    text.add(stringBuilder.toString());
    return addNewTextifier(null);
  }

  @Override
  public void visitClassEnd() {
    text.add("}\n");
  }

  // -----------------------------------------------------------------------------------------------
  // Modules
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visitMainClass(final String mainClass) {
  }

  @Override
  public void visitPackage(final String packaze) {
  }

  @Override
  public void visitRequire(final String require, final int access, final String version) {
  }

  @Override
  public void visitExport(final String packaze, final int access, final String... modules) {
    visitExportOrOpen("exports ", packaze, access, modules);
  }

  @Override
  public void visitOpen(final String packaze, final int access, final String... modules) {
    visitExportOrOpen("opens ", packaze, access, modules);
  }

  private void visitExportOrOpen(final String method, final String packaze, final int access, final String... modules) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab).append(method);
    stringBuilder.append(packaze);
    if (modules != null && modules.length > 0) {
      stringBuilder.append(" to");
    } else {
      stringBuilder.append(';');
    }
    appendRawAccess(access);
    if (modules != null && modules.length > 0) {
      for (int i = 0; i < modules.length; ++i) {
        stringBuilder.append(tab2).append(modules[i]);
        stringBuilder.append(i != modules.length - 1 ? ",\n" : ";\n");
      }
    }
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitUse(final String use) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab).append("uses ");
    appendDescriptor(INTERNAL_NAME, use);
    stringBuilder.append(";\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitProvide(final String provide, final String... providers) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab).append("provides ");
    appendDescriptor(INTERNAL_NAME, provide);
    stringBuilder.append(" with\n");
    for (int i = 0; i < providers.length; ++i) {
      stringBuilder.append(tab2);
      appendDescriptor(INTERNAL_NAME, providers[i]);
      stringBuilder.append(i != providers.length - 1 ? ",\n" : ";\n");
    }
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitModuleEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Annotations
  // -----------------------------------------------------------------------------------------------

  // DontCheck(OverloadMethodsDeclarationOrder): overloads are semantically different.
  @Override
  public void visit(final String name, final Object value) {
    visitAnnotationValue(name);
    if (value instanceof String) {
      visitString((String) value);
    } else if (value instanceof Type) {
      visitType((Type) value);
    } else if (value instanceof Byte) {
      visitByte(((Byte) value).byteValue());
    } else if (value instanceof Boolean) {
      visitBoolean(((Boolean) value).booleanValue());
    } else if (value instanceof Short) {
      visitShort(((Short) value).shortValue());
    } else if (value instanceof Character) {
      visitChar(((Character) value).charValue());
    } else if (value instanceof Integer) {
      visitInt(((Integer) value).intValue());
    } else if (value instanceof Float) {
      visitFloat(((Float) value).floatValue());
    } else if (value instanceof Long) {
      visitLong(((Long) value).longValue());
    } else if (value instanceof Double) {
      visitDouble(((Double) value).doubleValue());
    } else if (value.getClass().isArray()) {
      stringBuilder.append('{');
      if (value instanceof byte[]) {
        byte[] byteArray = (byte[]) value;
        for (int i = 0; i < byteArray.length; i++) {
          maybeAppendComma(i);
          visitByte(byteArray[i]);
        }
      } else if (value instanceof boolean[]) {
        boolean[] booleanArray = (boolean[]) value;
        for (int i = 0; i < booleanArray.length; i++) {
          maybeAppendComma(i);
          visitBoolean(booleanArray[i]);
        }
      } else if (value instanceof short[]) {
        short[] shortArray = (short[]) value;
        for (int i = 0; i < shortArray.length; i++) {
          maybeAppendComma(i);
          visitShort(shortArray[i]);
        }
      } else if (value instanceof char[]) {
        char[] charArray = (char[]) value;
        for (int i = 0; i < charArray.length; i++) {
          maybeAppendComma(i);
          visitChar(charArray[i]);
        }
      } else if (value instanceof int[]) {
        int[] intArray = (int[]) value;
        for (int i = 0; i < intArray.length; i++) {
          maybeAppendComma(i);
          visitInt(intArray[i]);
        }
      } else if (value instanceof long[]) {
        long[] longArray = (long[]) value;
        for (int i = 0; i < longArray.length; i++) {
          maybeAppendComma(i);
          visitLong(longArray[i]);
        }
      } else if (value instanceof float[]) {
        float[] floatArray = (float[]) value;
        for (int i = 0; i < floatArray.length; i++) {
          maybeAppendComma(i);
          visitFloat(floatArray[i]);
        }
      } else if (value instanceof double[]) {
        double[] doubleArray = (double[]) value;
        for (int i = 0; i < doubleArray.length; i++) {
          maybeAppendComma(i);
          visitDouble(doubleArray[i]);
        }
      }
      stringBuilder.append('}');
    }
    text.add(stringBuilder.toString());
  }

  private void visitInt(final int value) {
    stringBuilder.append(value);
  }

  private void visitLong(final long value) {
    stringBuilder.append(value).append('L');
  }

  private void visitFloat(final float value) {
    stringBuilder.append(value).append('F');
  }

  private void visitDouble(final double value) {
    stringBuilder.append(value).append('D');
  }

  private void visitChar(final char value) {
    stringBuilder.append("(char)").append((int) value);
  }

  private void visitShort(final short value) {
    stringBuilder.append("(short)").append(value);
  }

  private void visitByte(final byte value) {
    stringBuilder.append("(byte)").append(value);
  }

  private void visitBoolean(final boolean value) {
    stringBuilder.append(value);
  }

  private void visitString(final String value) {
    appendString(stringBuilder, value);
  }

  private void visitType(final Type value) {
    stringBuilder.append(value.getClassName()).append(CLASS_SUFFIX);
  }

  @Override
  public void visitEnum(final String name, final String descriptor, final String value) {
    visitAnnotationValue(name);
    appendDescriptor(FIELD_DESCRIPTOR, descriptor);
    stringBuilder.append('.').append(value);
    text.add(stringBuilder.toString());
  }

  @Override
  public EasyTextifer visitAnnotation(final String name, final String descriptor) {
    visitAnnotationValue(name);
    stringBuilder.append('@');
    appendDescriptor(FIELD_DESCRIPTOR, descriptor);
    stringBuilder.append('(');
    text.add(stringBuilder.toString());
    return addNewTextifier(")");
  }

  @Override
  public EasyTextifer visitArray(final String name) {
    visitAnnotationValue(name);
    stringBuilder.append('{');
    text.add(stringBuilder.toString());
    return addNewTextifier("}");
  }

  @Override
  public void visitAnnotationEnd() {
    // Nothing to do.
  }

  private void visitAnnotationValue(final String name) {
    stringBuilder.setLength(0);
    maybeAppendComma(numAnnotationValues++);
    if (name != null) {
      stringBuilder.append(name).append('=');
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Record components
  // -----------------------------------------------------------------------------------------------

  @Override
  public EasyTextifer visitRecordComponentAnnotation(final String descriptor, final boolean visible) {
    return visitAnnotation(descriptor, visible);
  }

  @Override
  public Printer visitRecordComponentTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    return visitTypeAnnotation(typeRef, typePath, descriptor, visible);
  }

  @Override
  public void visitRecordComponentAttribute(final Attribute attribute) {
    visitAttribute(attribute);
  }

  @Override
  public void visitRecordComponentEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Fields
  // -----------------------------------------------------------------------------------------------

  @Override
  public EasyTextifer visitFieldAnnotation(final String descriptor, final boolean visible) {
    return visitAnnotation(descriptor, visible);
  }

  @Override
  public Printer visitFieldTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    return visitTypeAnnotation(typeRef, typePath, descriptor, visible);
  }

  @Override
  public void visitFieldAttribute(final Attribute attribute) {
    visitAttribute(attribute);
  }

  @Override
  public void visitFieldEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Methods
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visitParameter(final String name, final int access) {
  }

  @Override
  public EasyTextifer visitAnnotationDefault() {
    text.add(tab2 + "default=");
    return addNewTextifier("\n");
  }

  @Override
  public EasyTextifer visitMethodAnnotation(final String descriptor, final boolean visible) {
    return visitAnnotation(descriptor, visible);
  }

  @Override
  public Printer visitMethodTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    return visitTypeAnnotation(typeRef, typePath, descriptor, visible);
  }

  @Override
  public EasyTextifer visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
    return this;
  }

  @Override
  public EasyTextifer visitParameterAnnotation(final int parameter, final String descriptor, final boolean visible) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append('@');
    appendDescriptor(FIELD_DESCRIPTOR, descriptor);
    stringBuilder.append('(');
    text.add(stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append(visible ? ") // parameter " : ") // invisible, parameter ").append(parameter).append('\n');
    return addNewTextifier(stringBuilder.toString());
  }

  @Override
  public void visitMethodAttribute(final Attribute attribute) {
    visitAttribute(attribute);
  }

  @Override
  public void visitCode() {
    // Nothing to do.
  }

  @Override
  public void visitFrame(final int type, final int numLocal, final Object[] local, final int numStack, final Object[] stack) {
  }

  @Override
  public void visitInsn(final int opcode) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append(OPCODES[opcode].toLowerCase()).append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append(OPCODES[opcode].toLowerCase()).append(' ').append(opcode == Opcodes.NEWARRAY ? TYPES[operand] : Integer.toString(operand)).append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append(OPCODES[opcode].toLowerCase()).append(' ').append(var).append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append(OPCODES[opcode].toLowerCase()).append(' ');
    appendDescriptor(INTERNAL_NAME, type);
    stringBuilder.append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append(OPCODES[opcode].toLowerCase()).append(' ');
    appendDescriptor(INTERNAL_NAME, owner);
    stringBuilder.append('.').append(name).append(" : ");
    appendDescriptor(FIELD_DESCRIPTOR, descriptor);
    stringBuilder.append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append(OPCODES[opcode].toLowerCase()).append(' ');
    appendDescriptor(INTERNAL_NAME, owner);
    stringBuilder.append('.').append(name).append(' ');
    appendDescriptor(METHOD_DESCRIPTOR, descriptor);
    if (isInterface) {
      stringBuilder.append(" (itf)");
    }
    stringBuilder.append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append("INVOKEDYNAMIC".toLowerCase()).append(' ');
    stringBuilder.append(name);
    appendDescriptor(METHOD_DESCRIPTOR, descriptor);
    stringBuilder.append(" [");
    stringBuilder.append('\n');
    stringBuilder.append(tab3);
    appendHandle(bootstrapMethodHandle);
    stringBuilder.append('\n');
    stringBuilder.append(tab3);
    if (bootstrapMethodArguments.length != 0) {
      stringBuilder.append('\n');
      for (Object value : bootstrapMethodArguments) {
        stringBuilder.append(tab3);
        if (value instanceof String) {
          Printer.appendString(stringBuilder, (String) value);
        } else if (value instanceof Type) {
          Type type = (Type) value;
          if (type.getSort() == Type.METHOD) {
            appendDescriptor(METHOD_DESCRIPTOR, type.getDescriptor());
          } else {
            visitType(type);
          }
        } else if (value instanceof Handle) {
          appendHandle((Handle) value);
        } else {
          stringBuilder.append(value);
        }
        stringBuilder.append(", \n");
      }
      stringBuilder.setLength(stringBuilder.length() - 3);
    }
    stringBuilder.append('\n');
    stringBuilder.append(tab2).append("]\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append(OPCODES[opcode].toLowerCase()).append(' ');
    appendLabel(label);
    stringBuilder.append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitLabel(final Label label) {
    stringBuilder.setLength(0);
    stringBuilder.append(ltab);
    appendLabel(label);
    stringBuilder.append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitLdcInsn(final Object value) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append("LDC ".toLowerCase());
    if (value instanceof String) {
      Printer.appendString(stringBuilder, (String) value);
    } else if (value instanceof Type) {
      stringBuilder.append(((Type) value).getDescriptor()).append(CLASS_SUFFIX);
    } else {
      stringBuilder.append(value);
    }
    stringBuilder.append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitIincInsn(final int var, final int increment) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append("IINC ".toLowerCase()).append(var).append(' ').append(increment).append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append("TABLESWITCH\n".toLowerCase());
    for (int i = 0; i < labels.length; ++i) {
      stringBuilder.append(tab3).append(min + i).append(": ");
      appendLabel(labels[i]);
      stringBuilder.append('\n');
    }
    stringBuilder.append(tab3).append("default: ");
    appendLabel(dflt);
    stringBuilder.append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append("LOOKUPSWITCH\n".toLowerCase());
    for (int i = 0; i < labels.length; ++i) {
      stringBuilder.append(tab3).append(keys[i]).append(": ");
      appendLabel(labels[i]);
      stringBuilder.append('\n');
    }
    stringBuilder.append(tab3).append("default: ");
    appendLabel(dflt);
    stringBuilder.append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append("MULTIANEWARRAY ".toLowerCase());
    appendDescriptor(FIELD_DESCRIPTOR, descriptor);
    stringBuilder.append(' ').append(numDimensions).append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public Printer visitInsnAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    return visitTypeAnnotation(typeRef, typePath, descriptor, visible);
  }

  @Override
  public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append("TRYCATCHBLOCK ".toLowerCase());
    appendLabel(start);
    stringBuilder.append(' ');
    appendLabel(end);
    stringBuilder.append(' ');
    appendLabel(handler);
    stringBuilder.append(' ');
    appendDescriptor(INTERNAL_NAME, type);
    stringBuilder.append('\n');
    text.add(stringBuilder.toString());
  }

  @Override
  public Printer visitTryCatchAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append("TRYCATCHBLOCK @".toLowerCase());
    appendDescriptor(FIELD_DESCRIPTOR, descriptor);
    stringBuilder.append('(');
    text.add(stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append(") : ");
    appendTypeReference(typeRef);
    stringBuilder.append(", ").append(typePath);
    stringBuilder.append(visible ? "\n" : INVISIBLE);
    return addNewTextifier(stringBuilder.toString());
  }

  @Override
  public void visitLocalVariable(final String name, final String descriptor, final String signature, final Label start, final Label end, final int index) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append("LOCALVARIABLE ".toLowerCase()).append(name).append(' ');
    appendDescriptor(FIELD_DESCRIPTOR, descriptor);
    stringBuilder.append(' ');
    appendLabel(start);
    stringBuilder.append(' ');
    appendLabel(end);
    stringBuilder.append(' ').append(index).append('\n');

    if (signature != null) {
      stringBuilder.append(tab2);
      appendDescriptor(FIELD_SIGNATURE, signature);
      stringBuilder.append(tab2);
      appendJavaDeclaration(name, signature);
    }
    text.add(stringBuilder.toString());
  }

  @Override
  public Printer visitLocalVariableAnnotation(final int typeRef, final TypePath typePath, final Label[] start, final Label[] end, final int[] index, final String descriptor, final boolean visible) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab2).append("LOCALVARIABLE @".toLowerCase());
    appendDescriptor(FIELD_DESCRIPTOR, descriptor);
    stringBuilder.append('(');
    text.add(stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append(") : ");
    appendTypeReference(typeRef);
    stringBuilder.append(", ").append(typePath);
    for (int i = 0; i < start.length; ++i) {
      stringBuilder.append(" [ ");
      appendLabel(start[i]);
      stringBuilder.append(" - ");
      appendLabel(end[i]);
      stringBuilder.append(" - ").append(index[i]).append(" ]");
    }
    stringBuilder.append(visible ? "\n" : INVISIBLE);
    return addNewTextifier(stringBuilder.toString());
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {
  }

  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
  }

  @Override
  public void visitMethodEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Common methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Prints a disassembled view of the given annotation.
   *
   * @param descriptor the class descriptor of the annotation class.
   * @param visible    {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values.
   */
  // DontCheck(OverloadMethodsDeclarationOrder): overloads are semantically different.
  public EasyTextifer visitAnnotation(final String descriptor, final boolean visible) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab).append('@');
    appendDescriptor(FIELD_DESCRIPTOR, descriptor);
    stringBuilder.append('(');
    text.add(stringBuilder.toString());
    return addNewTextifier(visible ? ")\n" : ") // invisible\n");
  }

  /**
   * Prints a disassembled view of the given type annotation.
   *
   * @param typeRef    a reference to the annotated type. See {@link TypeReference}.
   * @param typePath   the path to the annotated type argument, wildcard bound, array element type, or static inner type within 'typeRef'. May be {@literal null} if the annotation
   *                   targets 'typeRef' as a whole.
   * @param descriptor the class descriptor of the annotation class.
   * @param visible    {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values.
   */
  public EasyTextifer visitTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab).append('@');
    appendDescriptor(FIELD_DESCRIPTOR, descriptor);
    stringBuilder.append('(');
    text.add(stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append(") : ");
    appendTypeReference(typeRef);
    stringBuilder.append(", ").append(typePath);
    stringBuilder.append(visible ? "\n" : INVISIBLE);
    return addNewTextifier(stringBuilder.toString());
  }

  /**
   * Prints a disassembled view of the given attribute.
   *
   * @param attribute an attribute.
   */
  public void visitAttribute(final Attribute attribute) {
    stringBuilder.setLength(0);
    stringBuilder.append(tab).append("ATTRIBUTE ");
    appendDescriptor(-1, attribute.type);

    if (attribute instanceof TextifierSupport) {
      if (labelNames == null) {
        labelNames = new HashMap<>();
      }
      ((TextifierSupport) attribute).textify(stringBuilder, labelNames);
    } else {
      stringBuilder.append(" : unknown\n");
    }

    text.add(stringBuilder.toString());
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Appends a string representation of the given access flags to {@link #stringBuilder}.
   *
   * @param accessFlags some access flags.
   */
  private void appendAccess(final int accessFlags) {
    if ((accessFlags & Opcodes.ACC_PUBLIC) != 0) {
      stringBuilder.append("public ");
    }
    if ((accessFlags & Opcodes.ACC_PRIVATE) != 0) {
      stringBuilder.append("private ");
    }
    if ((accessFlags & Opcodes.ACC_PROTECTED) != 0) {
      stringBuilder.append("protected ");
    }
    if ((accessFlags & Opcodes.ACC_FINAL) != 0) {
      stringBuilder.append("final ");
    }
    if ((accessFlags & Opcodes.ACC_STATIC) != 0) {
      stringBuilder.append("static ");
    }
    if ((accessFlags & Opcodes.ACC_SYNCHRONIZED) != 0) {
      stringBuilder.append("synchronized ");
    }
    if ((accessFlags & Opcodes.ACC_VOLATILE) != 0) {
      stringBuilder.append("volatile ");
    }
    if ((accessFlags & Opcodes.ACC_TRANSIENT) != 0) {
      stringBuilder.append("transient ");
    }
    if ((accessFlags & Opcodes.ACC_ABSTRACT) != 0) {
      stringBuilder.append("abstract ");
    }
    if ((accessFlags & Opcodes.ACC_STRICT) != 0) {
      stringBuilder.append("strictfp ");
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0) {
      stringBuilder.append("synthetic ");
    }
    if ((accessFlags & Opcodes.ACC_MANDATED) != 0) {
      stringBuilder.append("mandated ");
    }
    if ((accessFlags & Opcodes.ACC_ENUM) != 0) {
      stringBuilder.append("enum ");
    }
  }

  /**
   * Appends the hexadecimal value of the given access flags to {@link #stringBuilder}.
   *
   * @param accessFlags some access flags.
   */
  private void appendRawAccess(final int accessFlags) {
  }

  /**
   * Appends an internal name, a type descriptor or a type signature to {@link #stringBuilder}.
   *
   * @param type  the type of 'value'. Must be one of {@link #INTERNAL_NAME}, {@link #FIELD_DESCRIPTOR}, {@link #FIELD_SIGNATURE}, {@link #METHOD_DESCRIPTOR},
   *              {@link #METHOD_SIGNATURE}, {@link #CLASS_SIGNATURE} or {@link #HANDLE_DESCRIPTOR}.
   * @param value an internal name, type descriptor or a type signature. May be {@literal null}.
   */
  protected void appendDescriptor(final int type, final String value) {
    if (type == CLASS_SIGNATURE || type == FIELD_SIGNATURE || type == METHOD_SIGNATURE) {
    } else {
      stringBuilder.append(value);
    }
  }

  /**
   * Appends the Java generic type declaration corresponding to the given signature.
   *
   * @param name      a class, field or method name.
   * @param signature a class, field or method signature.
   */
  private void appendJavaDeclaration(final String name, final String signature) {
  }

  /**
   * Appends the name of the given label to {@link #stringBuilder}. Constructs a new label name if the given label does not yet have one.
   *
   * @param label a label.
   */
  protected void appendLabel(final Label label) {
    if (labelNames == null) {
      labelNames = new HashMap<>();
    }
    String name = labelNames.get(label);
    if (name == null) {
      name = "L" + labelNames.size();
      labelNames.put(label, name);
    }
    stringBuilder.append(name);
  }

  /**
   * Appends a string representation of the given handle to {@link #stringBuilder}.
   *
   * @param handle a handle.
   */
  protected void appendHandle(final Handle handle) {
    int tag = handle.getTag();
    boolean isMethodHandle = false;
    switch (tag) {
    case Opcodes.H_GETFIELD:
      stringBuilder.append("GETFIELD".toLowerCase());
      break;
    case Opcodes.H_GETSTATIC:
      stringBuilder.append("GETSTATIC".toLowerCase());
      break;
    case Opcodes.H_PUTFIELD:
      stringBuilder.append("PUTFIELD".toLowerCase());
      break;
    case Opcodes.H_PUTSTATIC:
      stringBuilder.append("PUTSTATIC".toLowerCase());
      break;
    case Opcodes.H_INVOKEINTERFACE:
      stringBuilder.append("INVOKEINTERFACE".toLowerCase());
      isMethodHandle = true;
      break;
    case Opcodes.H_INVOKESPECIAL:
      stringBuilder.append("INVOKESPECIAL".toLowerCase());
      isMethodHandle = true;
      break;
    case Opcodes.H_INVOKESTATIC:
      stringBuilder.append("INVOKESTATIC".toLowerCase());
      isMethodHandle = true;
      break;
    case Opcodes.H_INVOKEVIRTUAL:
      stringBuilder.append("INVOKEVIRTUAL".toLowerCase());
      isMethodHandle = true;
      break;
    case Opcodes.H_NEWINVOKESPECIAL:
      stringBuilder.append("NEWINVOKESPECIAL".toLowerCase());
      isMethodHandle = true;
      break;
    default:
      throw new IllegalArgumentException();
    }
    stringBuilder.append('\n');
    stringBuilder.append(tab3);
    appendDescriptor(INTERNAL_NAME, handle.getOwner());
    stringBuilder.append('.');
    stringBuilder.append(handle.getName());
    if (!isMethodHandle) {
      stringBuilder.append('(');
    }
    appendDescriptor(HANDLE_DESCRIPTOR, handle.getDesc());
    if (!isMethodHandle) {
      stringBuilder.append(')');
    }
    if (handle.isInterface()) {
      stringBuilder.append(" itf");
    }
  }

  /**
   * Appends a comma to {@link #stringBuilder} if the given number is strictly positive.
   *
   * @param numValues a number of 'values visited so far', for instance the number of annotation values visited so far in an annotation visitor.
   */
  private void maybeAppendComma(final int numValues) {
    if (numValues > 0) {
      stringBuilder.append(", ");
    }
  }

  /**
   * Appends a string representation of the given type reference to {@link #stringBuilder}.
   *
   * @param typeRef a type reference. See {@link TypeReference}.
   */
  private void appendTypeReference(final int typeRef) {
    TypeReference typeReference = new TypeReference(typeRef);
    switch (typeReference.getSort()) {
    case TypeReference.CLASS_TYPE_PARAMETER:
      stringBuilder.append("CLASS_TYPE_PARAMETER ").append(typeReference.getTypeParameterIndex());
      break;
    case TypeReference.METHOD_TYPE_PARAMETER:
      stringBuilder.append("METHOD_TYPE_PARAMETER ").append(typeReference.getTypeParameterIndex());
      break;
    case TypeReference.CLASS_EXTENDS:
      stringBuilder.append("CLASS_EXTENDS ").append(typeReference.getSuperTypeIndex());
      break;
    case TypeReference.CLASS_TYPE_PARAMETER_BOUND:
      stringBuilder.append("CLASS_TYPE_PARAMETER_BOUND ").append(typeReference.getTypeParameterIndex()).append(", ").append(typeReference.getTypeParameterBoundIndex());
      break;
    case TypeReference.METHOD_TYPE_PARAMETER_BOUND:
      stringBuilder.append("METHOD_TYPE_PARAMETER_BOUND ").append(typeReference.getTypeParameterIndex()).append(", ").append(typeReference.getTypeParameterBoundIndex());
      break;
    case TypeReference.FIELD:
      stringBuilder.append("FIELD");
      break;
    case TypeReference.METHOD_RETURN:
      stringBuilder.append("METHOD_RETURN");
      break;
    case TypeReference.METHOD_RECEIVER:
      stringBuilder.append("METHOD_RECEIVER");
      break;
    case TypeReference.METHOD_FORMAL_PARAMETER:
      stringBuilder.append("METHOD_FORMAL_PARAMETER ").append(typeReference.getFormalParameterIndex());
      break;
    case TypeReference.THROWS:
      stringBuilder.append("THROWS ").append(typeReference.getExceptionIndex());
      break;
    case TypeReference.LOCAL_VARIABLE:
      stringBuilder.append("LOCAL_VARIABLE");
      break;
    case TypeReference.RESOURCE_VARIABLE:
      stringBuilder.append("RESOURCE_VARIABLE");
      break;
    case TypeReference.EXCEPTION_PARAMETER:
      stringBuilder.append("EXCEPTION_PARAMETER ").append(typeReference.getTryCatchBlockIndex());
      break;
    case TypeReference.INSTANCEOF:
      stringBuilder.append("INSTANCEOF");
      break;
    case TypeReference.NEW:
      stringBuilder.append("NEW");
      break;
    case TypeReference.CONSTRUCTOR_REFERENCE:
      stringBuilder.append("CONSTRUCTOR_REFERENCE");
      break;
    case TypeReference.METHOD_REFERENCE:
      stringBuilder.append("METHOD_REFERENCE");
      break;
    case TypeReference.CAST:
      stringBuilder.append("CAST ").append(typeReference.getTypeArgumentIndex());
      break;
    case TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
      stringBuilder.append("CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT ").append(typeReference.getTypeArgumentIndex());
      break;
    case TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT:
      stringBuilder.append("METHOD_INVOCATION_TYPE_ARGUMENT ").append(typeReference.getTypeArgumentIndex());
      break;
    case TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
      stringBuilder.append("CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT ").append(typeReference.getTypeArgumentIndex());
      break;
    case TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT:
      stringBuilder.append("METHOD_REFERENCE_TYPE_ARGUMENT ").append(typeReference.getTypeArgumentIndex());
      break;
    default:
      throw new IllegalArgumentException();
    }
  }

  /**
   * Creates and adds to {@link #text} a new {@link EasyTextifer}, followed by the given string.
   *
   * @param endText the text to add to {@link #text} after the textifier. May be {@literal null}.
   * @return the newly created {@link EasyTextifer}.
   */
  private EasyTextifer addNewTextifier(final String endText) {
    EasyTextifer textifier = createTextifier();
    text.add(textifier.getText());
    if (endText != null) {
      text.add(endText);
    }
    return textifier;
  }

  /**
   * Creates a new {@link EasyTextifer}.
   *
   * @return a new {@link EasyTextifer}.
   */
  protected EasyTextifer createTextifier() {
    return new EasyTextifer(api);
  }
}
