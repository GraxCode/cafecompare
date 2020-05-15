package me.nov.cafecompare.decompiler;

import me.nov.cafecompare.io.Conversion;

public class ASMifierBridge implements IDecompilerBridge {

  @Override
  public void setAggressive(boolean aggressive) {
  }

  @Override
  public String decompile(String name, byte[] bytes) {
    return Conversion.textify(Conversion.toNode(bytes));
  }
}
