//import edu.princeton.cs.algs4.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.Map.*;
import java.util.AbstractMap.*;
import java.util.regex.*;
import java.math.*;
import java.io.Serializable;
import java.util.stream.*;
import java.util.concurrent.*;

//connectivity graph including edges and nodes
public class ConnGraph{

    public static String DirIn = "IN";
    public static String DirOut = "OUT";
    public ConcurrentHashMap<String, ConnNode> nodes = new ConcurrentHashMap<String, ConnNode>();
    public ConcurrentHashMap<String, ConnEdge> edges = new ConcurrentHashMap<String, ConnEdge>();
    public HashMap<HashSet<String>, ConnNode> usedSymbols = new HashMap<HashSet<String>, ConnNode>();
    public HashMap<String, HashSet<ConnNode>> nodeClassList = new HashMap<String, HashSet<ConnNode>>();
    public LinkedList<ConnNode> frontier = new LinkedList<ConnNode>();;
    
    public ConnNode rootNode; // root node of the graph
    public String initConf; //initial configuration symbol

    public static int printCounter = 0;
    
    //Constructor
    public ConnGraph(){}

    //transform with full incoming and outgoing consistency enforced
    public void transformNew(){

	//initialize graph information
	rootNode.inSymbol = new HashSet<String>(Arrays.asList("X"));
	enforceIncomingConsistencyHypo(rootNode);
	frontier.add(rootNode);

	printInText("Initial");
	
	
	//frontier.add(nodes.get("st4"));
	//frontier.add(nodes.get("st6"));
	//frontier.add(nodes.get("st2"));
	//frontier.add(nodes.get("st5"));
	

	while (!frontier.isEmpty()){
	    ConnNode currNode = continueBFS();
	    printInText(currNode.name + " BFS Visited");

	    /*
	    //Debugging interruption
	    if (currNode.name.equals("st5"))
		frontier.add(nodes.get("st2.c1"));

	    else if (currNode.name.equals("st2.c1"))
		frontier.add(nodes.get("st5.c1"));

	    else if (currNode.name.equals("st5.c1"))
		break;
	    */
	}
	
	printInText("Final Result");

    }


    //take one node at the beginning of the queue
    public ConnNode continueBFS(){

	System.out.println("\n=========ACT: Entering BFS=========");
		
	
	//get the first unvisited node in the queue
	ConnNode currNode = frontier.poll();
	currNode.visited = true;
	System.out.println("\nINFO: BFS Handling Node: " + currNode);
	
	//check if the actual incoming symbol is compatible with hypothetical outgoing symbol
	for (ConnEdge outEdge : currNode.outgoingEdges){

	    System.out.println("\nACT: BFS Checking Consistency Edge: " + outEdge);
	    
	    //skip the edges without hypo symbol
	    if (outEdge.hypoSymbol == null) continue;

	    //for the first compatible outedge: update encoding
	    //Also check if the outEdge can be connected to other variants of the toNode (copies)
	    if (checkAndFixValidity(outEdge)){

		System.out.println("COMPATIBLE: " + outEdge);
		setSymbolNode(currNode, Helper.difference(currNode.inSymbol, outEdge.hypoSymbol));
		outEdge.setSymbolEdge(outEdge.hypoSymbol);		       
	    }
	    
	    //check if the outEdge can be connected to other variants of the toNode (copies)
	    else{		
		System.out.println("INCOMPATIBLE: " + outEdge);
		fixConsistency(outEdge); //transform the graph by duplicating the toNode
		
	    }
	}
	    

	System.out.println("\nACT: BFS ensuring node & out symbols for current Node: " + currNode);
	//Make sure node symbol is assigned after this point
	if (currNode.symbol == null)
	    setSymbolNode(currNode, new HashSet<String>(Arrays.asList(currNode.name)));

	
	//Make sure node outSymbol is assigned after this point
	currNode.setOutSymbolNode(Helper.difference(currNode.inSymbol, currNode.symbol));

	
	propagateSymbolConsistency(currNode);

	return currNode;
    }

    
    // [inEdge] --> [fromNode (current BFS)] --> [outEdge] --> [toNode (visited)]
    //fromNode cannot be encoded to generate the required outEdge symbol
    //Goal: divert the outEdge to a new node
    public boolean checkAndFixValidity(ConnEdge currEdge){

	
	ConnNode fromNode = currEdge.fromNode;
	ConnNode toNodeExact = currEdge.toNode;

	//check if the current exact can be exactly satisfied
	if (checkIOSymbolValidity(fromNode.symbol, fromNode.inSymbol, toNodeExact.inSymbol))
	    return true;
	
	//find out all the other nodes of the same "base" as the toNode
	for (ConnNode toNode : nodeClassList.get(Helper.extractBaseName(toNodeExact.name))){

	    if (checkIOSymbolValidity(fromNode.symbol, fromNode.inSymbol, toNode.inSymbol)){

		//reconnect the edge to this toNode
		reconnectEdge(currEdge, toNode);
		
		return true;
	    }
	}
	return false;
    }


    
    // [inEdge] --> [fromNode (current BFS)] --> [outEdge] --> [toNode (visited)]
    //fromNode cannot be encoded to generate the required outEdge symbol
    //Goal: divert the outEdge to a new node
    public void fixConsistency(ConnEdge currEdge){
	
	ConnNode fromNode = currEdge.fromNode;
	ConnNode toNode = currEdge.toNode;
	
	System.out.format("ACT: Split Node: %s\n" , toNode);


	//create a copy node
	ConnNode copyNode = createDuplicateNode(toNode);

	//add the copyNode to frontier;
	//frontier.add(copyNode);

	//reconnect the outEdge (manual edit)
	reconnectEdge(currEdge, copyNode);
    }


    
    //set the symbol for a node and keep track of the mapping
    public void setSymbolNode(ConnNode currNode, HashSet<String> symbol){

	currNode.setSymbol(symbol);
	usedSymbols.put(symbol, currNode);
    }

    
    public void propagateSymbolConsistency(ConnNode currNode){

	for (ConnEdge outEdge : currNode.outgoingEdges){

	    System.out.println("\nACT: BFS Assign Edge: " + outEdge);

	    //Update outgoing configurations to ensure output consistency
	    outEdge.setSymbolEdge(currNode.outSymbol);

	    enforceIncomingConsistencyHypo(outEdge.toNode);
	    
	    //add the new node to frontier
	    if (!frontier.contains(outEdge.toNode) && !outEdge.toNode.visited)
		frontier.add(outEdge.toNode);
	}
	
    }

    public void enforceIncomingConsistencyHypo(ConnNode currNode){
	for (ConnEdge inEdge : currNode.incomingEdges)
	    inEdge.setSymbolEdgeHypo(currNode.inSymbol);
    }



    public boolean checkIOSymbolValidity(HashSet<String> currSymbol, HashSet<String> inSymbol, HashSet<String> outSymbol){

	
	if (inSymbol == null || outSymbol == null) return true;

	HashSet<String> diffSymbol = Helper.difference(inSymbol, outSymbol);

	if (currSymbol == null){
	    return (diffSymbol.size() >= 2 && !usedSymbols.containsKey(diffSymbol));
	}
	else{
	    return diffSymbol.equals(currSymbol);
	}
    }

    


    //duplicate node with the appropriate name
    //"Name" -> "Name.copy[n]"
    public ConnNode createDuplicateNode (ConnNode currNode){

	String baseName = Helper.extractBaseName(currNode.name);
	String copyNodeName = baseName + ".c" + (nodeClassList.get(baseName).size());

	//System.out.println("Here: " + copyNodeName);
	ConnNode copyNode = addNode(copyNodeName);

	
	//deep copy the outgoing edges
	for (ConnEdge outEdge : currNode.outgoingEdges){
	    //create a new edge

	    ConnEdge copyEdge = addEdge(copyNode, outEdge.toNode);
	    
	}

	return copyNode;
    }

    
    


	
    //Method: add a new node to the graph
    //Return: whether the node is successfully added, newly
    public ConnNode addNode(String name){
	if (nodes.containsKey(name)){
	    System.out.format("WARNING: Node %s has already existed\n", name);
	    return null;
	}
	else{
	    ConnNode currNode = new ConnNode(name);
	    nodes.put(name, currNode);

	    String baseName = Helper.extractBaseName(currNode.name);
		    
	    //Update node class list
	    if (!nodeClassList.containsKey(baseName)){
		nodeClassList.put(baseName, new HashSet<ConnNode>(Arrays.asList(currNode)));
	    }
	    else{
		nodeClassList.get(Helper.extractBaseName(name)).add(currNode);
	    }
	    
	    return currNode;
	}

	
    }

    public void reconnectEdge(ConnEdge currEdge, ConnNode newToNode){

	System.out.format("ACT: Reconnect Edge %s to %s\n" , currEdge, newToNode);
	ConnNode fromNode = currEdge.fromNode;
	ConnNode toNode = currEdge.toNode;

	//disconnect the edge from the toNode
	currEdge.toNode.incomingEdges.remove(currEdge);
	
	//update the edge information
	currEdge.toNode = newToNode;
	currEdge.hypoSymbol = newToNode.inSymbol; //which is null
	currEdge.name = currEdge.fromNode.name + "_" + currEdge.toNode.name;
	
	//connect the edge to copyNode
	newToNode.incomingEdges.add(currEdge);
	
    }
			      
    //add edge by node names
    public ConnEdge addEdge(String fromNodeName, String toNodeName){

	String edgeName = fromNodeName + "_" + toNodeName;
	//check if the edge already exists
	if (edges.containsKey(edgeName)){
	    System.out.format("WARNING: Edge %s has already existed\n", edgeName);
	    return null;
	}
	else{

	    //Make sure both nodes exist
	    if (!nodes.containsKey(fromNodeName))
		addNode(fromNodeName);
	    if (!nodes.containsKey(toNodeName))
		addNode(toNodeName);

	    //retrieve the two nodes
	    ConnNode fromNode = nodes.get(fromNodeName);
	    ConnNode toNode = nodes.get(toNodeName);

	    return addEdge(fromNode, toNode);
	}
    }

    //add edge by nodes
    public ConnEdge addEdge(ConnNode fromNode, ConnNode toNode){

	String edgeName = fromNode.name + "_" + toNode.name;
	//check if the edge already exists
	if (edges.containsKey(edgeName)){
	    System.out.format("WARNING: Edge %s has already existed\n", edgeName);
	    return null;
	}
	else{

	    //build and add new edge
	    ConnEdge currEdge = new ConnEdge(fromNode, toNode);
	    edges.put(currEdge.name, currEdge);

	    System.out.println("INFO: Add Edge " + currEdge);

	    //a self loop is only counted as an outgoing edge
	    fromNode.outgoingEdges.add(currEdge);
	    toNode.incomingEdges.add(currEdge);
	    currEdge.setSymbolEdgeHypo(toNode.inSymbol);

	    /*
	    if (!fromNode.equals(toNode)){
		toNode.incomingEdges.add(currEdge);
		currEdge.setSymbolEdgeHypo(toNode.inSymbol);
	    }
	    */
	    
	    return currEdge;
	}
    }
    
    public void printInText(String msg){

	System.out.println();
	//start report
	System.out.print(new String(new char[20]).replace('\0', '='));
	System.out.format("REPORT GRAPH %d %s", ++printCounter, msg);
	System.out.println(new String(new char[20]).replace('\0', '='));

	//Summary
	System.out.format("= Root: %s\n" , rootNode.toString());
	System.out.format("= Number of nodes: %d\n" , nodes.size());
	System.out.format("= Number of edges: %d\n" , edges.size());
	System.out.format("= Frontier queue: %s\n" ,
			  "[" + frontier.stream().map(ConnNode::getName)
			  .collect(Collectors.joining(", ")) + "]");
	//Node List
	for (ConnNode currNode : nodes.values()){
	    System.out.println(currNode.toString());

	    for (ConnEdge outgoingEdge : currNode.outgoingEdges){
		System.out.println("\t" + outgoingEdge.toString());
	    }
	}	
	
	//end report
	System.out.println("\n");
	//System.out.print(new String(new char[20]).replace('\0', '-'));
	//System.out.print("REPORT GRAPH");
	//System.out.println(new String(new char[20]).replace('\0', '-'));

    }
}

class ConnNode{

    String name;
    HashSet<ConnEdge> incomingEdges;
    HashSet<ConnEdge> outgoingEdges;
    HashSet<String> inSymbol = null;
    HashSet<String> outSymbol = null; 
    HashSet<String> symbol = null;
    boolean visited;
    
    ConnNode(String name){
	this.name = name;
	this.incomingEdges = new HashSet<ConnEdge>();
	this.outgoingEdges = new HashSet<ConnEdge>();
	this.visited = false;
    }

    void setSymbol(HashSet<String> newSymbol){
	if (symbol == null){
	    symbol = newSymbol;
	    System.out.format("INFO: Set Node Symbol %s\n", this);
	}
	else if (symbol.equals(newSymbol))
	    System.out.format("INFO: Set Node Symbol (Repeat) %s\n", this);
	else
	    System.out.format("ERROR: Invalid update - Node %s already has a node symbol\n", name);
    }

    
    void setOutSymbolNode(HashSet<String> newSymbol){
	if (outSymbol == null){
	    outSymbol  = newSymbol;
	    System.out.format("INFO: Set Node outSymbol %s\n", this);
	}
	else if (outSymbol.equals(newSymbol))
	    System.out.format("INFO: Set Node outSymbol (Repeat) %s\n", this);
	else
	    System.out.format("ERROR: Invalid update - Node %s already has a outSymbol\n", name);
    }


    void setInSymbolNode(HashSet<String> newSymbol){
	if (inSymbol == null){
	    inSymbol  = newSymbol;
	    System.out.format("INFO: Set Node inSymbol %s\n", this);
	}
	else if (inSymbol.equals(newSymbol))
	    System.out.format("INFO: Set Node inSymbol (Repeat) %s\n", this);
	else
	    System.out.format("ERROR: Invalid update - Node %s already has an inSymbol\n", name);
    }
    
    @Override
    public String toString(){
	return String.format(">>> Node %s (%s, InSymb: %s, OutSymb: %s, Symb: %s)",
			     name,
			     visited? "Visited" : "Unvisited",
			     Helper.convertSetToString(inSymbol),
			     Helper.convertSetToString(outSymbol),
			     Helper.convertSetToString(symbol));
    }

    public String getName(){
	return name;
    }
    
}


class ConnEdge{

    int weight;
    String name;
    ConnNode fromNode;
    ConnNode toNode;
    HashSet<String> hypoSymbol = null; // hypothetical symbol (may be needed)
    HashSet<String> symbol = null;
    
    ConnEdge(ConnNode fromNode, ConnNode toNode){
	this.fromNode = fromNode;
	this.toNode = toNode;
	this.name = fromNode.name + "_" + toNode.name;
	this.weight = 1;
    }

    void setSymbolEdgeHypo(HashSet<String> newSymbol){

	hypoSymbol = newSymbol;
	System.out.println("INFO: Set Edge Hypo Symbol: " + this);
    }


    void setSymbolEdge(HashSet<String> newSymbol){
	if (symbol == null){

	    symbol = newSymbol;
	    hypoSymbol = newSymbol;
	    fromNode.setOutSymbolNode(newSymbol);
	    toNode.setInSymbolNode(newSymbol);

	    System.out.format("INFO: Set Edge Symbol: %s\n", this);
	}
	else if (symbol.equals(newSymbol))
	    System.out.format("INFO: Set Edge Symbol (Repeat) %s\n", this);
	else
	    System.out.format("ERROR: Invalid update - Edge %s already has a symbol\n", name);
    }

    void setSymbolEdgeConsistency(HashSet<String> newSymbol){
	if (symbol == null){

	    symbol = newSymbol;
	    hypoSymbol = newSymbol;

	    fromNode.outSymbol = newSymbol;
	    toNode.inSymbol = newSymbol;
	    
	    
	    System.out.println("INFO: Set Edge Symbol: " + this);
	}
	else
	    System.out.format("ERROR: Edge %s already has a symbol\n", name);
    }


    @Override
    public boolean equals(Object another){

	if (! (another instanceof ConnEdge)) return false;

	return this.name.equals(((ConnEdge)another).name);
    }


    @Override
    public String toString(){
	return String.format(">>> Edge %s (Symb: %s, HypoSymb: %s)", name,
			     Helper.convertSetToString(symbol),
			     Helper.convertSetToString(hypoSymbol));
    }
    
    public String getName(){
	return name;
    }
    
}


class Helper{


    //get the difference of two hashsets
    //https://stackoverflow.com/questions/8064570/
    //what-is-the-best-way-get-the-symmetric-difference-between-two-sets-in-java
    public static HashSet<String> difference(HashSet<String> s1, HashSet<String> s2){

	//union
	HashSet<String> symmetricDiff =
	    (s1 == null) ? new HashSet<String>() : new HashSet<String>(s1);
	symmetricDiff.addAll(s2);

	//intersection
	HashSet<String> temp =
	    (s1 == null) ? new HashSet<String>() : new HashSet<String>(s1);
	temp.retainAll(s2);

	symmetricDiff.removeAll(temp);
	return symmetricDiff;
    }


    public static String convertSetToString(HashSet<String> inSet){
	
	return (inSet == null) ? "[]" :
	    "[" + inSet.stream().map(Object::toString)
	    .collect(Collectors.joining(", ")) + "]";
    }

    //"Name.copy[0]" -> "Name"
    public static String extractBaseName(String name){

	if (name.contains("."))
	    return name.substring(0, name.indexOf('.'));
	else
	    return name;
    }
}



/*
    
  public void consistencyFixFull(){

  ConnNode problemNode;
	
  //iteratively enforce all consistency until no more change can be made
  while ((problemNode = consistencyCheck()) != null){
  consistencyFix(problemNode);
  break;
  }

  printInText("Consistency Enforcement Done" );
	
  }


  //incoming and outgoing symbols at the node are incompatible
  public void consistencyFix(ConnNode problemNode){
	

  //find and fix one problematic edge
  ConnEdge problemEdge;
  for (ConnEdge edge : problemNode.outgoingEdges){
  //if (edge.)
  }
	
  Iterator<ConnEdge> outEdgeIter = problemNode.outgoingEdges.iterator();

  while (outEdgeIter.hasNext()){
  ConnEdge outEdge = outEdgeIter.next();
  ConnNode nextNode = outEdge.toNode;

  System.out.format("ACT: Split %s due to inconsistene edge %s\n",
  nextNode.name, outEdge.name);
		
  ConnNode copyNode = createDuplicateNode(nextNode);
	    
  //remove the edge
  }
		
  }

	

    
  public ConnNode consistencyCheck(){

  for (ConnNode currNode : nodes.values()){

  HashSet<String> inSymbol = currNode.inSymbol;
  HashSet<String> outSymbol = currNode.outSymbol;

  //skip nodes without full symbol sets
  if (inSymbol == null || outSymbol == null) continue;


  //if they can be made compatible through state encoding
  if (checkIOValidity(inSymbol, outSymbol)){
		    
  HashSet<String> diffSymbol = Helper.difference(inSymbol, outSymbol);
  currNode.setSymbol(diffSymbol);
  }
  else{
  System.out.println("INFO: Inconsistency at " + currNode);
  return currNode;		
  }
  }

  return null;
  }
    

	

  public void consistencyPropagateFull(){

  //iteratively enforce all consistency until no more change can be made
  while (consistencyPropagate(DirIn) || consistencyPropagate(DirOut)){}

  printInText("Consistency Enforcement Done" );
	
  }
    
  //ensure all edges on the "direction" of all nodes are consistent
  //direction = DirIn / DirOut
  public boolean consistencyPropagate(String direction){

  //check if the direction is valid
  if (!direction.equals(DirIn) && !direction.equals(DirOut)){
  System.out.println("ERROR: Direction is invalid: " + direction);
  return true;
  }
	    
	
  boolean changeIndicator = false;

	
  for (ConnNode currNode : nodes.values()){

  HashSet<String> activeSymbol =
  (direction.equals(DirIn)) ? currNode.inSymbol : currNode.outSymbol;

  if (activeSymbol != null){

  for (ConnEdge edge : ((direction.equals(DirIn)) ?
  currNode.incomingEdges : currNode.outgoingEdges)){

  //System.out.println("TEST: Checking Edge " + edge);
  if (edge.symbol == null){
  edge.setSymbolEdge(activeSymbol);
  //System.out.println("Set Symbol: " + edge);
  changeIndicator = true;
  }
  }
  }
  }

  return changeIndicator;
  }

*/


/*
//split if the node's incoming and outgoing symbols cannot be satisfied by state encoding
public void splitNodeInconsistentIO(ConnNode currNode, ConnEdge outEdge){

//split the next node
ConnNode nextNode = outEdge.toNode;
System.out.format("ACT: Split %s due to inconsistene edge %s\n",
nextNode.name, outEdge.name);
		
ConnNode copyNode = createDuplicateNode(nextNode);

ConnEdge copyEdge = addEdge(currNode, copyNode); // this edge has not symbol yet
copyNode.incomingEdges.add(copyEdge);
	
}

    // [fromNode (current BFS)] --> [currEdge] --> [toNode (visited)]
    public void removeEdge(ConnEdge currEdge){

	System.out.println("INFO: Remove Edge " + currEdge);
	currEdge.fromNode.outgoingEdges.remove(currEdge);
	currEdge.toNode.incomingEdges.remove(currEdge);
	edges.remove(currEdge.name);
    }
*/
