package com.dfavisualizer.algorithm;
import com.dfavisualizer.model.DFA;
public class GraphExampleRegexCheck {
 public static void main(String[] args){
  DFA graph=new DFA();DFA.State[] q=new DFA.State[4];for(int i=0;i<4;i++)q[i]=new DFA.State("q"+i);
  graph.setStartState(q[3]);graph.addAcceptState(q[0]);
  int[][] targets={{2,0},{0,3},{2,1},{2,3}};
  for(int from=0;from<4;from++){graph.addTransition(q[from],'a',q[targets[from][0]]);graph.addTransition(q[from],'b',q[targets[from][1]]);}
  String expression=new AutomatonToRegex(graph).convert();
  assert expression.length()<=20:"Missed shared b* factor: "+expression;
  AutomatonToRegexCheck.check(graph,expression);
  AutomatonToRegexCheck.check(graph,"b*a+b([ab]b*a+b)*ab*");
  System.out.println("Screenshot graph checks passed: "+expression+" (exact language equivalence)");
 }
}
