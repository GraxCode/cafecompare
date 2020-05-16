package me.nov.cafecompare.diff;

import java.util.LinkedList;

import name.fraser.neil.plaintext.DiffMatchPatch;

public class DiffMath {

  public static int codeDiffDist(String from, String to) {
    DiffMatchPatch dmp = new DiffMatchPatch();
    dmp.Diff_Timeout = 0.05f;
    LinkedList<DiffMatchPatch.Diff> diff = dmp.diff_main(from, to);
    return dmp.diff_levenshtein(diff);
  }

  public static float confidencePercent(String from, String to) {
    DiffMatchPatch dmp = new DiffMatchPatch();
    dmp.Diff_Timeout = 0.05f;
    LinkedList<DiffMatchPatch.Diff> diff = dmp.diff_main(from, to);
    return lowerConfidence(dmp.diff_levenshtein(diff), diff.stream().mapToInt(d -> d.text.length()).sum()) * 100;
  }

  /**
   * Lower end of 95% confidence interval of opposite percent
   */
  public static float lowerConfidence(int edits, int length) {
    float match = 1 - (edits / (float) length);
    float confidence = (float) (1.96f * Math.sqrt((match * (1f - match)) / (float) length));
    return match - confidence;
  }
}
