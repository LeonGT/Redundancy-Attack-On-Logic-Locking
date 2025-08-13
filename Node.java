//import edu.princeton.cs.algs4.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.Map.*;
import java.util.AbstractMap.*;
import java.util.regex.*;
import java.math.*;
import java.io.Serializable;

public class Node implements Comparable<Node>,Serializable{
    public String name; // original name from the KISS2 file
    public int seqName; // sequentially assigned counter name for metis representation
    public String encoding; // binary encoding
    public char group;  // 'D' or 'T'
    public HashSet<Node> incomings;
    public HashSet<Node> outgoings;
    public HashSet<Transition> transitions; // all outgoing transitions
    public boolean visited;
    
    public Node(String name){
	this.name = name;
	this.seqName = 0;
	this.encoding = null;
	this.group = 'D';
	this.incomings = new HashSet<Node>();
	this.outgoings = new HashSet<Node>();
	this.transitions = new HashSet<Transition>();
	this.visited = false;
    }

    public boolean addIncomingNode(Node another){

	if (!incomings.contains(another)){
	    incomings.add(another);
	    return true;
	}
	else{
	    return false;
	}
    }

    public boolean addOutgoingNode(Node another){
	if (!outgoings.contains(another)){
	    outgoings.add(another);
	    return true;
	}
	else{
	    return false;
	}
    }

    public void addTransition(String fromNode, String toNode, String input, String output){
	Transition tran = new Transition(fromNode, toNode, input, output);
	transitions.add(tran);
    }
    
    public void removeInput(Node another){
	incomings.remove(another);
    }

    public void removeOutput(Node another){
	outgoings.remove(another);
    }    

    
    @Override
    public String toString(){
	//Example: Node s2 (0011, T)
	return String.format("Node %s (%s, %c)", name, encoding, group);
    }

    @Override
    public int compareTo(Node another){
	return this.name.compareTo(another.name);
    }
    
    class Transition implements Serializable{
	String fromNode;
	String toNode;
	String input;
	String output;
	char conf;

	public Transition(String fromNode, String toNode, String input, String output){
	    this.fromNode = fromNode;
	    this.toNode = toNode;
	    this.input = input.replace("-", "?");
	    this.output = output.replace("-", "x");
	    this.conf = 'D';
	}

	//check if the input condition is covered by another, e.g. 1100- is covered by 1----
	public boolean compatibleInput (String anotherInput) {

	    if (anotherInput.equals("none")) return false;
	    
	    int specifiedBitAnotherIndex = Math.max(anotherInput.indexOf('0'), anotherInput.indexOf('1'));
	    
	    char cthis = input.charAt(specifiedBitAnotherIndex);
	    char canother = anotherInput.charAt(specifiedBitAnotherIndex);
		
	    //if canother is a care bit and cthis doesn't match
	    if (cthis != canother) {
		return false;
	    }
	    else{	    	    
		return true;
	    }
	}
	
	@Override
	public boolean equals(Object ob) {
	    if (ob == this)  return true;	    
	    if (ob == null || ob.getClass() != getClass())  return false;
	    
	    Transition p = (Transition) ob;
	    //return p.toNode.equals(this.toNode);
	    return p.toString().equals(this.toString());
	}

	@Override
	public String toString(){
	    return String.format("Transition: from %s,to %s, input %s, output %s", fromNode, toNode, input, output);
	}
    }
}
