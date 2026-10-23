package com.dfavisualizer.ui;

import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import com.dfavisualizer.model.DFA;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxCellState;

/** The user's exact workflow: build the suffix DFA, then loop b on its accepting state. */
public class SuffixLoopInteractionCheck {
 public static void main(String[] args) throws Exception {
  UiTheme.install(); MainFrame[] app={null};
  String source="(a|b)*aba";
  SwingUtilities.invokeAndWait(()->{
   app[0]=new MainFrame();app[0].display();
   LearningReliabilityCheck.find(app[0].getFrame().getContentPane(),JTextField.class).setText(source);
   assert app[0].getVisualizationController().visualizeDfa(source,true,false,true,false);
  });
  try {
   SwingUtilities.invokeAndWait(()->{
    AutomataGraph graph=(AutomataGraph)app[0].getDfaPanel().getVisualizationComponent();
    DFA model=app[0].getCurrentDfa();
    DFA.State accepting=model.getAcceptStates().iterator().next();
    mxCell edge=graph.transition(accepting.getName(),model.getTransitionTarget(accepting,'b').getName(),'b');
    graph.getGraph().setSelectionCell(edge);
    mxCellState edgeState=graph.getGraph().getView().getState(edge);
    mxPoint tip=edgeState.getAbsolutePoint(edgeState.getAbsolutePointCount()-1);
    mxCellState target=graph.getGraph().getView().getState(graph.state(accepting.getName()));
    Component canvas=graph.getGraphControl();int x=(int)Math.round(tip.getX()),y=(int)Math.round(tip.getY()),tx=(int)target.getCenterX(),ty=(int)target.getCenterY();
    canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,1,InputEvent.BUTTON1_DOWN_MASK,x,y,1,false,MouseEvent.BUTTON1));
    canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_DRAGGED,2,InputEvent.BUTTON1_DOWN_MASK,tx,ty,0,false,MouseEvent.NOBUTTON));
    canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_RELEASED,3,0,tx,ty,1,false,MouseEvent.BUTTON1));
    assert model.getTransitionTarget(accepting,'b').equals(accepting):"End-state b drag did not form a loop";
   });
   long deadline=System.nanoTime()+15_000_000_000L;String[] result={source};
   while(result[0].equals(source)&&System.nanoTime()<deadline){SwingUtilities.invokeAndWait(()->result[0]=LearningReliabilityCheck.find(app[0].getFrame().getContentPane(),JTextField.class).getText());Thread.sleep(20);}
   assert !result[0].equals(source):"Regex field was never updated";
   assert result[0].length()<=20:"UI bypasses the shorter graph expression: "+result[0];
   java.util.regex.Pattern pattern=java.util.regex.Pattern.compile(result[0]);
   SwingUtilities.invokeAndWait(()->{
    DFA model=app[0].getCurrentDfa();
    for(int length=0;length<=10;length++)for(int bits=0;bits<(1<<length);bits++){
     StringBuilder word=new StringBuilder();for(int i=0;i<length;i++)word.append((bits&(1<<i))==0?'a':'b');
     DFA.State state=model.getStartState();for(int i=0;i<word.length();i++){state=model.getTransitionTarget(state,word.charAt(i));if(state==null)break;}
     assert pattern.matcher(word).matches()==model.getAcceptStates().contains(state):"Generated field disagrees with graph: "+word;
    }
   });
   assert pattern.matcher("aba").matches()&&pattern.matcher("ababb").matches()&&!pattern.matcher("ababa").matches();
   System.out.println("Exact suffix-loop UI workflow passed. Raw regex field: `"+result[0]+"`");
  }finally{SwingUtilities.invokeAndWait(app[0].getFrame()::dispose);}
 }
}
