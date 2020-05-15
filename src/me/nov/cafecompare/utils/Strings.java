package me.nov.cafecompare.utils;

import java.util.regex.*;

public class Strings {
  public static boolean containsRegex(String input, String search) {
    try {
      return Pattern.compile(search).matcher(input).find();
    } catch (PatternSyntaxException e) {
      return input.toLowerCase().contains(search.toLowerCase());
    }
  }
}
