package com.dfavisualizer.algorithm;
public class RegexOptimizationCheck {
 public static void main(String[] args) {
  for(String suffix:new String[]{"abab","aba","aabb","abba"}) {
   String input="(a|b)*"+suffix;
   var dfa=new RegexToDfaConverter().convertRegexToDfa(input);
   String optimized=new AutomatonToRegex(dfa).convert();
   assert optimized.length()<=input.length():"Suffix language should remain compact without a source hint: "+input+" -> "+optimized;
  }
  System.out.println("Regex optimization checks passed (compact suffix languages without source hints)");
 }
}
