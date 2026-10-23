package com.dfavisualizer.algorithm;
import java.util.*;
import java.util.regex.Pattern;
import com.dfavisualizer.model.*;
public class AutomatonToRegexCheck {
 public static void main(String[] args) {
  for(String source:new String[]{"a|b","a*","ab|ac","ba|ca","a*a*","(a?)+","(ab|ac)?(ab|ac)*","(ab|ba)*","a?b+","()","[ ]","\\n","(?!)","[^ -~]","[.+*?]","[a-z]+@[a-z]+[.][a-z]+","[\\[\\]\\&\\\\]+"}) {
   NFA nfa=new RegexParser().parse(source);
   check(new SubsetConstruction().convertNfaToDfa(nfa),new AutomatonToRegex(nfa).convert());
  }
  String looped="((a+b(ab*a+b)*)?b)*a+b(ab*a+b)*ab*";
  DFA edited=new RegexToDfaConverter().convertRegexToDfa(looped);
  String shorter=new AutomatonToRegex(edited).convert();
  assert shorter.length()<looped.length():"User's looped-state example did not improve";
  check(edited,shorter);
  Random random=new Random(73);
  for(int sample=0;sample<40;sample++) {
   DFA dfa=new DFA();DFA.State[] states=new DFA.State[4];
   for(int i=0;i<4;i++){states[i]=new DFA.State("q"+i);dfa.addState(states[i]);if(random.nextBoolean())dfa.addAcceptState(states[i]);}
   dfa.setStartState(states[0]);
   for(DFA.State state:states)for(char symbol:new char[]{'a','b'})if(random.nextBoolean())dfa.addTransition(state,symbol,states[random.nextInt(4)]);
   check(dfa,new AutomatonToRegex(dfa).convert());
  }
  DFA empty=new DFA();empty.setStartState(new DFA.State("q0"));check(empty,new AutomatonToRegex(empty).convert());
  empty.addAcceptState(empty.getStartState());check(empty,new AutomatonToRegex(empty).convert());
  System.out.println("Automaton-to-regex checks passed (40 seeded DFAs, epsilon NFAs, loops, empty/epsilon languages, escaping, parser round trips)");
 }
 static void check(DFA dfa,String expression) {
  Pattern pattern=Pattern.compile(expression);
  DFA roundtrip=new SubsetConstruction().convertNfaToDfa(new RegexParser().parse(expression));
  Set<Character> alphabet=new HashSet<>(dfa.getAlphabet());alphabet.addAll(roundtrip.getAlphabet());
  Set<List<DFA.State>> seen=new HashSet<>();Queue<List<DFA.State>> queue=new ArrayDeque<>();
  List<DFA.State> initial=Arrays.asList(dfa.getStartState(),roundtrip.getStartState());seen.add(initial);queue.add(initial);
  while(!queue.isEmpty()){
   List<DFA.State> pair=queue.remove();
   assert dfa.getAcceptStates().contains(pair.get(0))==roundtrip.getAcceptStates().contains(pair.get(1)):"Round-trip language changed: "+expression;
   for(char symbol:alphabet){
    List<DFA.State> next=Arrays.asList(pair.get(0)==null?null:dfa.getTransitionTarget(pair.get(0),symbol),pair.get(1)==null?null:roundtrip.getTransitionTarget(pair.get(1),symbol));
    if(seen.add(next))queue.add(next);
   }
  }
  List<String> words=new ArrayList<>(Arrays.asList("",".","+","*","?","[","]","&","\\","adham@gmail.com","a@b.cd"," ","é","\n"));
  for(int length=1;length<=5;length++)for(int bits=0;bits<(1<<length);bits++){StringBuilder word=new StringBuilder();for(int i=0;i<length;i++)word.append((bits&(1<<i))==0?'a':'b');words.add(word.toString());}
  for(String word:words){boolean expected=accepts(dfa,word);assert pattern.matcher(word).matches()==expected:expression+" input="+word;assert accepts(roundtrip,word)==expected:"App parser disagrees with generated regex: "+expression;}
 }
 private static boolean accepts(DFA dfa,String input){DFA.State state=dfa.getStartState();for(char c:input.toCharArray()){state=dfa.getTransitionTarget(state,c);if(state==null)return false;}return dfa.getAcceptStates().contains(state);}
}
