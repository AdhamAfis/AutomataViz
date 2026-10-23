package com.dfavisualizer.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.io.File;
import com.dfavisualizer.ui.dialogs.RegexAlternativesDialog;

public class RegexAlternativesDialogCheck {
 public static void main(String[] args)throws Exception {
  UiTheme.install(); MainFrame[] app={null}; AutomataGraph[] graph={null}; JDialog[] dialog={null};
  SwingUtilities.invokeAndWait(()->{
   app[0]=new MainFrame();app[0].display();
   String source="(b|a+b[ab])*a+bab*";
   LearningReliabilityCheck.find(app[0].getFrame().getContentPane(),JTextField.class).setText(source);
   assert app[0].getVisualizationController().visualizeDfa(source,true,false,true,false);
   graph[0]=(AutomataGraph)app[0].getDfaPanel().getVisualizationComponent();
  });
  try {
   SwingUtilities.invokeLater(()->LearningReliabilityCheck.button(app[0].getGraphControls(),"Regex alternatives").doClick(0));
   long end=System.nanoTime()+20_000_000_000L;boolean[] ready={false};
   while(!ready[0]&&System.nanoTime()<end){
    SwingUtilities.invokeAndWait(()->{
     for(Window w:app[0].getFrame().getOwnedWindows())if(w instanceof JDialog && "Regex alternatives".equals(((JDialog)w).getTitle()))dialog[0]=(JDialog)w;
     if(dialog[0]!=null){String status=((JLabel)ReconnectionInteractionCheck.named(dialog[0].getContentPane(),"alternatives.status")).getText();
      assert !status.startsWith("Could not"):status; ready[0]=status.startsWith("Verified");}
    });Thread.sleep(20);
   }
   assert ready[0]:"Alternatives never completed verification";
   String[] selected={null};
   SwingUtilities.invokeAndWait(()->{
    JTextArea shortest=(JTextArea)ReconnectionInteractionCheck.named(dialog[0].getContentPane(),"alternatives.shortest");
    JTextArea readable=(JTextArea)ReconnectionInteractionCheck.named(dialog[0].getContentPane(),"alternatives.readable");
    assert !shortest.getText().equals(readable.getText()):"Fixture should provide distinct goals";
    assert shortest.getText().length()<=readable.getText().length(); selected[0]=readable.getText();
    JButton copy=(JButton)ReconnectionInteractionCheck.named(dialog[0].getContentPane(),"alternatives.shortest.copy");assert copy.isEnabled();
    try {
     BufferedImage screenshot=new BufferedImage(dialog[0].getWidth(),dialog[0].getHeight(),BufferedImage.TYPE_INT_RGB);
     dialog[0].paint(screenshot.getGraphics()); new File("target/visual-checks").mkdirs();
     ImageIO.write(screenshot,"png",new File("target/visual-checks/regex-alternatives.png"));
    }catch(Exception ex){throw new RuntimeException(ex);}
    ((JButton)ReconnectionInteractionCheck.named(dialog[0].getContentPane(),"alternatives.readable.use")).doClick(0);
   });
   SwingUtilities.invokeAndWait(()->{
    assert !dialog[0].isDisplayable();
    assert selected[0].equals(LearningReliabilityCheck.find(app[0].getFrame().getContentPane(),JTextField.class).getText());
    assert app[0].getDfaPanel().getVisualizationComponent()==graph[0]:"Use expression rebuilt or replaced the graph";
    RegexAlternativesDialog cancelled=new RegexAlternativesDialog(app[0].getFrame(),graph[0],new String[0],value->{throw new AssertionError("Cancelled dialog applied an expression");});
    cancelled.setModal(false);cancelled.setVisible(true);cancelled.dispose();
   });
   Thread.sleep(200);
   JDialog[] lesson={null};
   SwingUtilities.invokeAndWait(()->{
    var steps=new com.dfavisualizer.ui.dialogs.StepByStepDialog(app[0],"a*", "aa");
    for(Window owned:app[0].getFrame().getOwnedWindows())if(owned instanceof JDialog && "Step-by-Step Conversion Process".equals(((JDialog)owned).getTitle()))lesson[0]=(JDialog)owned;
    lesson[0].setModal(false);steps.display();
   });
   long lessonEnd=System.nanoTime()+15_000_000_000L;boolean[] loaded={false};
   while(!loaded[0]&&System.nanoTime()<lessonEnd){SwingUtilities.invokeAndWait(()->loaded[0]=LearningReliabilityCheck.find(lesson[0].getContentPane(),JTabbedPane.class).isEnabledAt(1));Thread.sleep(20);}
   assert loaded[0];
   SwingUtilities.invokeAndWait(()->LearningReliabilityCheck.find(lesson[0].getContentPane(),JTabbedPane.class).setSelectedIndex(1));
   SwingUtilities.invokeLater(()->LearningReliabilityCheck.button(lesson[0].getContentPane(),"Regex alternatives").doClick(0));
   ready[0]=false;end=System.nanoTime()+15_000_000_000L;
   while(!ready[0]&&System.nanoTime()<end){SwingUtilities.invokeAndWait(()->{
    for(Window owned:lesson[0].getOwnedWindows())if(owned instanceof JDialog)dialog[0]=(JDialog)owned;
    if(dialog[0]!=null && dialog[0].getOwner()==lesson[0])ready[0]=((JLabel)ReconnectionInteractionCheck.named(dialog[0].getContentPane(),"alternatives.status")).getText().startsWith("Verified");
   });Thread.sleep(20);}
   assert ready[0]:"NFA lesson alternatives did not finish verification";
   SwingUtilities.invokeAndWait(()->{
    assert ((JTextArea)ReconnectionInteractionCheck.named(dialog[0].getContentPane(),"alternatives.shortest")).getText().equals("a*");
    ((JButton)ReconnectionInteractionCheck.named(dialog[0].getContentPane(),"alternatives.shortest.use")).doClick(0);
    assert ((JTextField)ReconnectionInteractionCheck.named(lesson[0].getContentPane(),"walkthrough.currentExpression")).getText().equals("a*");
    lesson[0].dispose();
   });
  } finally { SwingUtilities.invokeAndWait(app[0].getFrame()::dispose); }
  System.out.println("Regex alternatives dialog checks passed (real action, verified choices, use, graph preservation, close during load)");
 }
}
