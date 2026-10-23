package com.dfavisualizer.algorithm;
public class PreferredRegexCheck {
 public static void main(String[] args) {
  String shortExpression="(a|b)*abab";
  String different="((a+b(ab*a+b)*)?b)*a+b(ab*a+b)*ab*";
  var graph=new RegexToDfaConverter().convertRegexToDfa(shortExpression);
  assert new AutomatonToRegex(graph).convert(shortExpression).length() <= shortExpression.length():"Equivalent short source should be retained";
  assert !different.equals(new AutomatonToRegex(graph).convert(different)):"Different language must never replace the graph regex";
  var changed=new RegexToDfaConverter().convertRegexToDfa(different);
  assert !shortExpression.equals(new AutomatonToRegex(changed).convert(shortExpression)):"Must not conceal a changed language using the old source";
  assert "a".equals(new AutomatonToRegex(new RegexToDfaConverter().convertRegexToDfa("a")).convert("((a))")):"Keep simplifications when shorter than the source";
  var looped=new RegexToDfaConverter().convertRegexToDfa("(a|b)*aba");
  var accepting=looped.getAcceptStates().iterator().next();
  var oldTarget=looped.getTransitionTarget(accepting,'b');
  looped.addTransition(accepting,'b',accepting);
  String loopExpression=new AutomatonToRegex(looped).convert("(a|b)*aba","(a|b)*abab","(a|b)*abab*");
  assert java.util.regex.Pattern.matches(loopExpression,"aba");
  assert java.util.regex.Pattern.matches(loopExpression,"ababb");
  assert !java.util.regex.Pattern.matches(loopExpression,"ababa");
  looped.addTransition(accepting,'b',oldTarget);
  assert new AutomatonToRegex(looped).convert("(a|b)*aba").length() <= "(a|b)*aba".length():"Restoring an edge should recover the short source";
  System.out.println("Preferred regex checks passed (exact equivalence, original suffix pattern, changed-language rejection, shorter simplification)");
 }
}
