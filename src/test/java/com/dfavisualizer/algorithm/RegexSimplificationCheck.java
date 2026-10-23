package com.dfavisualizer.algorithm;
public class RegexSimplificationCheck {
 public static void main(String[] args) {
  String[][] cases={{"a*","a*"},{"a?","a?"},{"ab|ac","a[bc]"},{"ba|ca","[bc]a"},{"[a-z]","[a-z]"},{"(a*)*","a*"},{"aa*","a+"},{"a*a*","a*"},{"(a?)+","a*"}};
  for(String[] example:cases){String result=new AutomatonToRegex(new RegexParser().parse(example[0])).convert();assert example[1].equals(result):example[0]+" should simplify to "+example[1]+", got "+result;}
  System.out.println("Regex simplification checks passed (ranges, factoring, optional, plus, nested/repeated stars)");
 }
}
