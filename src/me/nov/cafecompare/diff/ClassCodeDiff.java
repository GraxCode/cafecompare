package me.nov.cafecompare.diff;

import java.util.LinkedList;

import name.fraser.neil.plaintext.DiffMatchPatch;

public class ClassCodeDiff {

  public static int codeDiff(String code1, String code2) {
    DiffMatchPatch dmp = new DiffMatchPatch();
    dmp.Diff_Timeout = 0.05f;
    LinkedList<DiffMatchPatch.Diff> diff = dmp.diff_main(code1, code2);
    return dmp.diff_levenshtein(diff);
  }
}
