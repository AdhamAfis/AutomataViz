package com.dfavisualizer.ui;
import javax.swing.SwingUtilities;
import com.dfavisualizer.algorithm.RegexParser;
import com.dfavisualizer.model.*;
import com.mxgraph.model.mxCell;
public class ReconnectionCheck {
 public static void main(String[] args)throws Exception {UiTheme.install();SwingUtilities.invokeAndWait(()->{
  DFA dfa=new DFA();DFA.State a=new DFA.State("q0"),b=new DFA.State("q1"),c=new DFA.State("q2");
  dfa.setStartState(a);dfa.addAcceptState(b);dfa.addState(c);dfa.addTransition(a,'a',b);dfa.addTransition(a,'b',b);dfa.addTransition(c,'a',c);
  AutomataGraph view=(AutomataGraph)new DfaVisualizer().visualizeDfa(dfa);
  mxCell edge=view.transition("q0","q1",'a');
  assert !view.getGraph().isCellEditable(edge);
  view.getGraph().connectCell(edge,view.state("q2"),false);
  assert dfa.getTransitionTarget(a,'a').equals(c) && dfa.getTransitionTarget(a,'b').equals(c):"Reconnecting a grouped arrow does not update simulation";
  assert view.transition("q0","q2",'a')==edge;
  assert view.getGraph().getEdgeValidationError(edge,view.state("q2"),view.state("q1"))!=null:"Conflicting DFA source accepted";
  view.getGraph().connectCell(edge,view.state("q0"),false);
  assert edge.getSource()==edge.getTarget() && dfa.getTransitionTarget(a,'a').equals(a):"Self-loop reconnect failed";
  view.getGraph().connectCell(edge,null,false);
  assert edge.getTarget()==view.state("q0"):"Dangling arrow accepted";
  NFA nfa=new RegexParser().parse("a|b");AutomataGraph nv=(AutomataGraph)new NfaVisualizer().visualizeNfa(nfa);
  mxCell ne=null;for(Object item:nv.getGraph().getChildEdges(nv.getGraph().getDefaultParent())){mxCell x=(mxCell)item;if(x.getValue() instanceof AutomataGraph.Transition && x.getSource()!=null){ne=x;break;}}
  int from=Integer.parseInt(ne.getSource().getValue().toString().substring(1));
  int to=nfa.getStartState();nv.getGraph().connectCell(ne,nv.state("q"+to),false);
  assert nfa.getTransitions().entrySet().stream().anyMatch(e->e.getKey().getState()==from && e.getValue().contains(to)):"NFA model not updated";
 });System.out.println("Reconnection checks passed (DFA/NFA models, grouped symbols, loops, conflicts, dangling edges)");}
}
