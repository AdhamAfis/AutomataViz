package com.dfavisualizer.ui;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import com.dfavisualizer.model.*;
import com.dfavisualizer.ui.components.*;
import com.dfavisualizer.ui.dialogs.StepByStepDialog;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxCellState;
public class ReconnectionInteractionCheck {
 public static void main(String[] args)throws Exception {
  UiTheme.install();MainFrame[] app={null};
  SwingUtilities.invokeAndWait(()->{app[0]=new MainFrame();app[0].display();LearningReliabilityCheck.find(app[0].getFrame().getContentPane(),JTextField.class).setText("a|b");assert app[0].getVisualizationController().visualizeDfa("a|b",false,false,true,true);});
  try {
   SwingUtilities.invokeAndWait(()->{
    AutomataGraph view=(AutomataGraph)app[0].getDfaPanel().getVisualizationComponent();
    DFA dfa=app[0].getCurrentDfa();String from=dfa.getStartState().getName(),to=dfa.getTransitionTarget(dfa.getStartState(),'a').getName();
    mxCell edge=view.transition(from,to,'a');
    view.getGraph().setSelectionCell(edge);
    mxCellState drawn=view.getGraph().getView().getState(edge);
    mxPoint endpoint=drawn.getAbsolutePoint(drawn.getAbsolutePointCount()-1);
    mxCellState target=view.getGraph().getView().getState(view.state(from));
    Component canvas=view.getGraphControl();
    int x=(int)Math.round(endpoint.getX()),y=(int)Math.round(endpoint.getY()),tx=(int)target.getCenterX(),ty=(int)target.getCenterY();
    canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,1,InputEvent.BUTTON1_DOWN_MASK,x,y,1,false,MouseEvent.BUTTON1));
    canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_DRAGGED,2,InputEvent.BUTTON1_DOWN_MASK,tx,ty,0,false,MouseEvent.NOBUTTON));
    canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_RELEASED,3,0,tx,ty,1,false,MouseEvent.BUTTON1));
    assert dfa.getTransitionTarget(dfa.getStartState(),'a').equals(dfa.getStartState()):"Dragging the arrowhead did not reconnect";
   });
   long sourceDeadline=System.nanoTime()+15_000_000_000L;
   boolean[] sourceReady={false};
   while(!sourceReady[0]&&System.nanoTime()<sourceDeadline){SwingUtilities.invokeAndWait(()->sourceReady[0]=!LearningReliabilityCheck.find(app[0].getFrame().getContentPane(),JTextField.class).getText().equals("a|b"));Thread.sleep(20);}
   assert sourceReady[0]:"Generated expression did not arrive";
   SwingUtilities.invokeAndWait(()->{
    String generated=LearningReliabilityCheck.find(app[0].getFrame().getContentPane(),JTextField.class).getText();
    assert !java.util.regex.Pattern.matches(generated,"a") && java.util.regex.Pattern.matches(generated,"ab") : "Regex input did not follow the reconnected transition: " + generated;
    assert app[0].getCurrentDfa()==app[0].getNonMinimizedDfa();
    assert app[0].getStatusPanel().getStatusArea().getText().contains("Custom automaton");
   });
   JDialog[] dialog={null};
   SwingUtilities.invokeAndWait(()->{StepByStepDialog steps=new StepByStepDialog(app[0],"a|b","a");for(Window w:app[0].getFrame().getOwnedWindows())if(w instanceof JDialog)dialog[0]=(JDialog)w;dialog[0].setModal(false);steps.display();});
   long end=System.nanoTime()+15_000_000_000L;boolean[] ready={false};
   while(!ready[0]&&System.nanoTime()<end){SwingUtilities.invokeAndWait(()->ready[0]=LearningReliabilityCheck.find(dialog[0].getContentPane(),JTabbedPane.class).isEnabledAt(1));Thread.sleep(20);}
   assert ready[0];
   SwingUtilities.invokeAndWait(()->{
    JTabbedPane tabs=LearningReliabilityCheck.find(dialog[0].getContentPane(),JTabbedPane.class);tabs.setSelectedIndex(1);
    NfaLessonPanel lesson=LearningReliabilityCheck.find((Container)tabs.getSelectedComponent(),NfaLessonPanel.class);
    AutomataGraph graph=(AutomataGraph)lesson.getVisualizationComponent();
    for(Object object:graph.getGraph().getChildEdges(graph.getGraph().getDefaultParent())){
     mxCell edge=(mxCell)object;if(edge.getSource()!=null&&edge.getValue() instanceof AutomataGraph.Transition){
      graph.getGraph().connectCell(edge,edge.getSource(),false);break;
     }
    }
    assert !tabs.isEnabledAt(2)&&!tabs.isEnabledAt(3):"Lesson keeps stale conversion claims after reconnect";
    assert LearningReliabilityCheck.button(dialog[0].getContentPane(),"Restore expression").isVisible();
    assert lesson.consumed()==0:"Graph edit retained old trace";
   });
   long lessonDeadline=System.nanoTime()+15_000_000_000L;boolean[] lessonUpdated={false};
   while(!lessonUpdated[0]&&System.nanoTime()<lessonDeadline){SwingUtilities.invokeAndWait(()->lessonUpdated[0]=((JLabel)named(dialog[0].getContentPane(),"walkthrough.phase")).getText().contains("expression updated"));Thread.sleep(20);}
   assert lessonUpdated[0]:"Learn did not update its expression";
   SwingUtilities.invokeAndWait(()->{
    String generated=((JTextField)named(dialog[0].getContentPane(),"walkthrough.currentExpression")).getText();
    assert generated.equals(LearningReliabilityCheck.find(app[0].getFrame().getContentPane(),JTextField.class).getText());
    DFA model=app[0].getCurrentDfa();
    for(String input:new String[]{"","a","b","aa","ab","ba"}){
     DFA.State state=model.getStartState();for(char c:input.toCharArray()){state=model.getTransitionTarget(state,c);if(state==null)break;}
     assert java.util.regex.Pattern.matches(generated,input)==model.getAcceptStates().contains(state):"Learn regex disagrees with graph";
    }
    LearningReliabilityCheck.button(dialog[0].getContentPane(),"Restore expression").doClick(0);
   });
   SwingUtilities.invokeAndWait(()->assertOriginal(app[0]));
  }finally{SwingUtilities.invokeAndWait(app[0].getFrame()::dispose);}
  System.out.println("Reconnection interaction checks passed (mouse endpoint drag, main model/view sync, lesson reset/provenance)");
 }
 static void assertOriginal(MainFrame app){assert "a|b".equals(LearningReliabilityCheck.find(app.getFrame().getContentPane(),JTextField.class).getText()):"Restore did not restore the source expression";}
 static Component named(Container parent,String name){for(Component c:parent.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container){Component found=named((Container)c,name);if(found!=null)return found;}}return null;}

}
