package com.dfavisualizer.algorithm;
public class RegexAlternativesCheck {
 public static void main(String[] args) {
  var graph=new RegexToDfaConverter().convertRegexToDfa("b*a+b([ab]b*a+b)*ab*");
  var alternatives=new AutomatonToRegex(graph).alternatives("(b|a+b[ab])*a+bab*", "a*");
  assert alternatives.shortest.length()<=18;
  assert !alternatives.readable.equals(alternatives.shortest):"Readability must consider candidates beyond shortest length";
  AutomatonToRegexCheck.check(graph,alternatives.shortest);
  AutomatonToRegexCheck.check(graph,alternatives.readable);
  assert !alternatives.shortest.equals("a*")&&!alternatives.readable.equals("a*"):"Unverified source was offered";
  var simple=new AutomatonToRegex(new RegexToDfaConverter().convertRegexToDfa("a*")).alternatives();
  assert simple.shortest.equals("a*")&&simple.readable.equals("a*");
  System.out.println("Regex alternatives checks passed (distinct goals, exact equivalence, invalid candidate rejection, identical choices)");
 }
}
