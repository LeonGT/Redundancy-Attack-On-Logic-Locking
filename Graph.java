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

public class Graph implements Serializable{ 

    //Basic attributes, built at initialization
    LinkedHashMap<String, Node> nodes = new LinkedHashMap<String, Node>(); //Maintain order
    String rootName = "none";
    String resetCondition = "none";
    int numInput = 0;
    int numOutput = 0;
    int numEdge = 0;
    int numNode = 0;
    int partSeed = 0;
    
    //Advanced attributes, calculated later
    int numMetisEdge; // in prepMetis
    int numFF; // in prepRandomencoding
    int numEdgeTPart; //in prepPartitionsize
    int numEdgeDPart; //in prepPartitionsize

    Graph(String inFile) throws Exception{
	this(inFile, 0);
    }
    
    Graph(String inFile, int encodingSeed) throws Exception{

	//(KISS2 Example)
	//.i 8
	//.o 19
	//.p 251
	//.s 48
	//.r 000000
	//0-11---- 000000 000000 0100011010010010111

	//construct directed graph
	BufferedReader buf = null;
	try{

	    //figure out the number of FFs
	    buf = new BufferedReader(new FileReader(inFile));

	    //read the meta data
	    for(String line; (line = buf.readLine()) != null;){
		
		//skip empty lines
		if (line.isEmpty() || line.contains("#")) continue;

		//end of the file
		if (line.contains(".e")) break;
		
		//.i: input
		if (line.contains(".i"))
		    numInput = Integer.parseInt(line.split(" ")[1]);

		//.o: output
		else if (line.contains(".o"))
		    numOutput = Integer.parseInt(line.split(" ")[1]);
		
		//.p: edge
		else if (line.contains(".p"))
		    numEdge = Integer.parseInt(line.split(" ")[1]);
	
		//.s: state
		else if (line.contains(".s"))
		    numNode = Integer.parseInt(line.split(" ")[1]);
		
		//.r: initial state
		else if (line.contains(".r"))
		    rootName = line.split(" ")[1];

		//.z: custom reset signal
		else if (line.contains(".z"))
		    resetCondition = line.split(" ")[1];

		//All states have the same behavior (global reset), ifnore
		else if (line.contains("*"))
		    continue;

		//read the rest of the transition lines
		else {
		    //input fromState toState output
		    String[] lineSep = line.split("\\s+");

		    if (lineSep.length != 4) {
			System.err.println("Invalid format on line : " + line);
			throw new IOException("Invalid KISS format");
		    }
		    String input = lineSep[0];
		    String fromName = lineSep[1];
		    String toName = lineSep[2];
		    String output = lineSep[3];

		    //if the from state is specified with X
		    if (fromName.contains("x")){
			addXEdgeByLine(input, fromName, toName, output);
		    }
		    else{
			addEdgeByLine(input, fromName, toName, output);
		    }
		}
	    }   
	}
	catch (Exception e){
	    e.printStackTrace();
	    return;
	}
	finally{
	    buf.close();
	}

	
	//Clean up some formatting issue in KISS2
	//.p numEdge not specified in some files

	//.s numNode not specified
	if (numNode == 0) {
	    numNode = nodes.size();
	}

	//rootname not specified - set to first
	if (rootName.equals("none")) {
	    for (Node currNode : getAllNodes()){
		rootName = currNode.name;
		break;
	    }
	}
	
	prepMetis();
	prepRandomEncoding(encodingSeed);
	prepPartitionInfo();

	System.out.format("MISC: input %d; output %d; edge %d; node %d; root %s\n",
			  numInput, numOutput, numEdge, numNode, rootName);
    }
    
    /*    
    //Transform all self loops into loop of 2
    void transform1_SelfLoop() throws Exception{

	//check each node individually
	for (Node curr : getAllNodes()){

	    //If there is a self loop on n
	    if (curr.outgoings.contains(curr)){
		
		

		//Duplicate the node
		String newNodeName = curr + "_2";
		Node newNode = new Node(curr + "_2");
		nodes.add(newNodeName, newNode);
		
		//Update the set of incoming nodes for current
		curr.incomings.remove(curr);
		curr.incomings.add(newNode);
		
		//Update the set of outgoing nodes for current
		curr.outgoings.remove(curr);
		curr.outgoings.add(newNode);
		
		//Update the set of incoming nodes for duplicate

		
		//Update the set of outgoing nodes for duplicate
		
		//for (Transition tr=)
	    }

	    
	}
	return;
    }	
    */	
    
    //report the graph connectivity in a list
    void reportGraphInText() throws Exception{

	System.out.println("INFO: Print the connectivity pattern of the graph");
	for (Node n : getAllNodes()){
	    System.out.print(n.name + ": ");

	    //get a list of names of outgoing nodes
	    ArrayList<String> outgoingNodeNames = new ArrayList<String>();
	    for (Node outN : n.outgoings){
		outgoingNodeNames.add(outN.name);
	    }

	    String nextNodesText = outgoingNodeNames.toString();
	    System.out.println("{" + nextNodesText + "}");
	}	
    }

    //void convertGraph()
    
    void addXEdgeByLine(String input, String fromName, String toName, String output)  throws Exception{

	//Example: 000011110 0000xxxx000xxxx 000000001000000 01xxxx11110
	
        for (String fromNameExp : Tool.expandOnX(fromName)){
	    addEdgeByLine(input, fromNameExp, toName, output);
	}
    }

    
    void addEdgeByLine(String input, String fromName, String toName, String output)  throws Exception{

	numEdge++;
	Node fromNode;
	Node toNode;
		
	//retrieve or create nodes
	if (nodes.containsKey(fromName)){
	    fromNode = nodes.get(fromName);
	}
	else{
	    fromNode = new Node(fromName);
	    nodes.put(fromName, fromNode);
	}
	
	
	if (nodes.containsKey(toName)){
	    toNode = nodes.get(toName);
	}
	else{
	    toNode = new Node(toName);
	    nodes.put(toName, toNode);
	}
		
	//make the connection
	//check if the output is unique and it's not a self loop
	fromNode.addOutgoingNode(toNode);
	toNode.addIncomingNode(fromNode);
	fromNode.addTransition(fromName, toName, input, output);
	
	
    }
    
    void prepPartitionInfo(){
	
	int counterD = 0;
	int counterT = 0;

	//assign partition to each transition and count
	for (Node.Transition tran : getAllTransitions()){

	    //ignore all reset edges
	    if (tran.compatibleInput(resetCondition))
		continue;
	    
	    if (nodes.get(tran.toNode).group == 'D'){
		tran.conf = 'D';
		counterD++;
	    }
	    else{
		tran.conf = 'T';
		counterT++;
	    }
	}

	numEdgeDPart = counterD;
	numEdgeTPart = counterT;
    }

    void prepPartitionInfoEnt(){
	
	int counterD = 0;
	int counterT = 0;

	//assign partition to each transition and count
	for (Node.Transition tran : getAllTransitions()){
	    
	    if (nodes.get(tran.fromNode).group == 'D'){
		tran.conf = 'D';
		counterD++;
	    }
	    else{
		tran.conf = 'T';
		counterT++;
	    }
	}

	numEdgeDPart = counterD;
	numEdgeTPart = counterT;
    }
    
    //Get the set of cut transitions
    ArrayList<Node.Transition> getCut(){
	ArrayList<Node.Transition> cutTrans = new ArrayList<Node.Transition>();
	
	for (Node currNode : getAllNodes()){
	    for (Node.Transition tran : currNode.transitions) {
		Node arriveNode = nodes.get(tran.toNode); 
		if (!arriveNode.name.equals(rootName) && (arriveNode.group != currNode.group)){
		    cutTrans.add(tran);
		}
	    }
	}
	return cutTrans;
    }

    //report the cut and expansion ratio after partitioning
    Report reportPartition(boolean print){
	return new Report(numEdge, numEdgeDPart, numEdgeTPart, getCut().size(), print);
    }

    ArrayList<Node> getAllNodes(){
	/*
	Set nodesSet = nodes.entrySet();
	Iterator iterator = nodesSet.iterator();
	ArrayList<Node> allNodes = new ArrayList<Node>();
	
	while(iterator.hasNext()){
	    Map.Entry curr = (Map.Entry)iterator.next();
	    Node currNode = (Node)curr.getValue();
	    allNodes.add(currNode);
	}
	return allNodes;
	*/
	return new ArrayList<>(nodes.values());
    }

    ArrayList<Node.Transition> getAllTransitions(){

	ArrayList<Node.Transition> allTrans = new ArrayList<Node.Transition>();
	for (Node node : getAllNodes()){
	    for (Node.Transition tran : node.transitions){
		allTrans.add(tran);
	    }
	}

	return allTrans;
    }
    
    //a sequential list of false[D] and true[T] according to the seqName of nodes
    void assignPartition(boolean[] partitionResult){

	Node rootNode = nodes.get(rootName);
	int rootIndex = rootNode.seqName - 1;
	boolean rootIsD = partitionResult[rootIndex]? false:true;
	
	for (Node currNode : getAllNodes()){
	    int currIndex = currNode.seqName - 1; //seqName starts from 1 and index starts from 0
	    if (rootIsD) {
		currNode.group = partitionResult[currIndex]? 'T':'D';
	    }
	    else{
		//flip the D/T assignment is root is not D
		currNode.group = partitionResult[currIndex]? 'D':'T';
	    }
	    //System.out.format("Node %s is assigned partition %c\n", currNode.name, currNode.partition);
	}


	prepPartitionInfo();
	//reportPartition(false);
    }


    //assign random encoding{
    void prepRandomEncoding(int seed){
		
	//calculate the minimum number of flip flops needed
	numFF = 1;
	int maxRepresentable = 2;
	while (maxRepresentable < numNode){
	    numFF++;
	    maxRepresentable <<= 1;	    
	}

	System.out.println("MISC: Random encoding seed is " + seed);
	//System.out.println("maxRepresentable: " + maxRepresentable);
	//System.out.println("numNode: " + numNode);

	//randomly generate a list of encodings
	List<Integer> ints = IntStream.rangeClosed(1, maxRepresentable-1)
	    .boxed().collect(Collectors.toList());
	Collections.shuffle(ints, new Random(seed));
	int index = 0;

	//works for up to 32 bits
	for (Node node : getAllNodes()){

	    //convert the integer to N bit binary string
	    int encodingNum;
	    
	    if (node.name.equals(rootName))
		encodingNum = 0; // root is always encoded all 0s
	    else
		encodingNum = ints.get(index++);
	    
	    node.encoding = String.format("%1$" + numFF + "s",
					  Integer.toBinaryString(encodingNum)).replace(' ', '0');
	}
    }
    
    //assign a sequential name for each node and calculate numEdge for METIS
    void prepMetis() {

	numMetisEdge = 0;
	int counter = 0;
	
	for (Node currNode : getAllNodes()){
	    currNode.seqName = ++counter;
	}

	//Count how many pairs of nodes are connected
	for (Node currNode : getAllNodes()){
	    HashSet<Node> outMetis = new HashSet<Node>();

	    for (Node.Transition tran : currNode.transitions){

		
		//ignore self loop
		if (tran.fromNode.equals(tran.toNode)) continue;

		//ignore reset lines
		if (tran.compatibleInput(resetCondition)) continue;

		Node arriveNode = nodes.get(tran.toNode);

		//To prevent S1->S2 ans S2->S1 are counted twice, if two nodes are interconnected
		//count only the lower index to higher index transition
		if (!arriveNode.outgoings.contains(currNode) || arriveNode.seqName > currNode.seqName)
		    outMetis.add(arriveNode);
	    }
	    numMetisEdge += outMetis.size();
	}	
	    
	return;
    }

    
    void printToKiss(String outFile) throws Exception{
	
	PrintWriter writer = null;
	
	try{
	    writer = new PrintWriter(outFile);
	    writer.println(".i " + numInput);
	    writer.println(".o " + numOutput);
	    writer.println(".p " + numEdge);
	    writer.println(".s " + numNode);

	    for (Node node:getAllNodes()){

		//Example: M'b[currentstate][input]: {nextstate, outsig} = N'b[nextstate][output]
		for (Node.Transition transition : node.transitions){
		    writer.format("%s %s %s %s\n", transition.input, transition.fromNode,
				  transition.toNode, transition.output);
		}
	    }
	}
	catch (Exception e){
	    e.printStackTrace();
	    return;
	}
	finally {
	    writer.close();
	}
	
	return;
    }
	    
    void printToMetis(String outFile) throws Exception{
	    
	//METIS Example
	//39(numNode) 96(numEdge)
	//[list of connected nodes of the first node]
	
	PrintWriter writer = null;
	
	try{
	    writer = new PrintWriter(outFile);
	    
	    //header: [numNode] [numEdge] [weight config parameter (1==edge weight)]
	    writer.println(numNode + " " + (numMetisEdge) + " 1");

	    //comment line representing the mapping
	    for (Node currNode : getAllNodes()){
		writer.format("%%Node %s is mapped to %d\n", currNode.name, currNode.seqName);
	    }

	    //transitions
	    int counter = 0;
	    for (Node currNode : getAllNodes()){

		HashSet<Node> allIO = new HashSet<Node>();

                //System.out.printf("Node %s, incoming %s, outgoing %s\n", currNode.name, currNode.incomings, currNode.outgoings);
		allIO.addAll(currNode.outgoings);
		allIO.addAll(currNode.incomings);
		
		//inputs and outputs nodes
		for (Node arriveNode : allIO){
		    
		    //ignore self loop
		    if (currNode.seqName != arriveNode.seqName){

			//count the number of transitions between currNode and connNode
			int weight = 0;

			//count the number of transitions from currNode to arriveNode
			//ignore the transitions going into the root Node
			for (Node.Transition tran : currNode.transitions) {
			    if (tran.toNode.equals(arriveNode.name) && !tran.compatibleInput(resetCondition)) weight++;
			}

			//count the number of transitions from arriveNode to currNode
			//ignore the transitions going into the root Node
			for (Node.Transition tran : arriveNode.transitions) {
			    if (tran.toNode.equals(currNode.name) && !tran.compatibleInput(resetCondition)) weight++;
			}

			//print in the format of arrive node followed by weight
			if (weight != 0)
			    writer.print(arriveNode.seqName + " " + weight + " ");
		    }
		}
		writer.println();
	    }	
	        

	}
	catch (Exception e){
	    e.printStackTrace();
	    return;
	}
	finally {
	    writer.close();
	}
	
	return;
    }

    void printToVerilog(String outFile, boolean obfed) throws Exception{

	PrintWriter writer;
	
	try{
	    writer = new PrintWriter(outFile);
	}
	catch (IOException e){
	    System.out.println("Output File cannot be written");
	    return;
	}

	//=========================== define module and input
	//If outputing obfed FSM, additional D/T Select signal
	if (!obfed)
	    writer.format("module fsm_nsl(reset, clk, in, out);\n");
	else {
	    writer.format("module fsm_nsl(reset, clk, in, out, tffin);\n");
	    writer.format("    input tffin;\n");
	}   

	writer.format("    input reset, clk;\n");
	writer.format("    input [%d:0] in;\n", numInput-1);
	if (obfed)
	    writer.format("    reg conf;\n");
	writer.format("    output reg [%d:0] out;\n", numOutput-1);
	writer.format("    reg [%d:0] state, nextstate;\n\n", numFF-1);


	//=========================== initialization
	writer.format("    initial begin\n");
	writer.format("        out = %d'b0;\n", numOutput);
	writer.format("        state = %d'b0;\n", numFF);
	writer.format("        nextstate = %d'b0;\n", numFF);
	if (obfed)
	    writer.format("        conf = 1'b0;\n");
	writer.format("    end\n\n");
	
	//=========================== reset and update next state
	writer.format("    //STATE UPDATE LOGIC\n");
	writer.format("    always @(posedge clk) begin\n");
	if (obfed)
	    writer.format("        conf <= conf ^ tffin;\n");
	writer.format("        if (reset == 1)\n");
	writer.format("            state <= %d'b%s;\n", numFF, nodes.get(rootName).encoding);
	
	//If obfed, use the dtselect to flip the state
	if (!obfed){
	    writer.format("        else\n");
	    writer.format("            state <= nextstate;\n\n");
	}
	else{
	    writer.format("        else if (conf == 1)\n");		
	    writer.format("            state <= state ^ nextstate;\n\n");
	    writer.format("        else\n");
	    writer.format("            state <= nextstate;\n");
	}
	writer.format("    end\n\n");

	/*
	//T-FF in CCU
	writer.format("    //CCU LOGIC\n");
	if (obfed) {
	    writer.format("    always @(negedge clk)\n");
	    int specifiedIndex;
	    writer.format("        if (reset%s)\n", resetCondition.equals("none")?
			  "" : String.format(" || in[%d]==%c",
					     specifiedIndex = Math.max(resetCondition.indexOf('0'),
									   resetCondition.indexOf('1')),
					     resetCondition.charAt(specifiedIndex)
					     )); //reset signal or the inherent reset condition
	    writer.format("            dtselect = 1'b%d;\n", (nodes.get(rootName).group == 'T')? 1:0);
	    writer.format("        else\n");
	    writer.format("            dtselect = dtselect ^ tffin;\n\n");
	}
	*/
	
	//=========================== transitions
	writer.format("    //NEXT STATE LOGIC\n");
	//writer.println("    always@(state or in) begin");
	writer.println("    always@(*) begin");
	writer.println("        casez({state, in})\n");

	int inputTotal = numFF + numInput;
	int outputTotal = numFF + numOutput;

	//print the encoding of each state in the KISS2 file
	writer.println("            //Encoding information");
	for (Node node:getAllNodes()){
	    writer.format("            //%s\n", node.toString());
	}
	writer.println("\n            //Format - in: [state]_[input]; out:[nextstate]_[output]");

	for (Node node:getAllNodes()){

	    //Example: M'b[currentstate][input]: {nextstate, outsig} = N'b[nextstate][output]
	    for (Node.Transition transition : node.transitions){
		String originNode = node.encoding;
		String arriveNode = nodes.get(transition.toNode).encoding;

		//if non-obfuscated
		if (!obfed || transition.conf == 'D'){
		    writer.format("            %d'b%s: begin nextstate = %d'b%s; out = %d'b%s; end\n",
				  inputTotal,
				  originNode + "_" +  transition.input,
				  numFF,
				  arriveNode,
				  numOutput,
				  transition.output
				  );
		}
		else{
		    writer.format("            %d'b%s: begin nextstate = %d'b%s ^ %d'b%s; out = %d'b%s; end\n",
				  // input width
				  inputTotal,
				  // state condition + input condition
				  originNode + "_" +  transition.input,
				  //flip the arrive node if "obfed" flag is true and the arriving state is a T state
				  //(!obfed || transition.conf == 'D') ? "": "~",
				  numFF,
				  arriveNode,
				  numFF,
				  originNode,
				  numOutput,
				  transition.output
				  );
		}
	    }
	}

	
	writer.format("            default: begin nextstate = %d'b%s; out = %d'b%s; end\n",
		      numFF,
		      new String(new char[numFF]).replace('\0', 'x'),
		      numOutput,
		      new String(new char[numOutput]).replace('\0', 'x')
		      );
	
	
	writer.println();
	writer.println("        endcase");
	writer.println("    end");
	writer.println("endmodule");


	writer.close();


    }

    
    class Report{
	int numEdge;
	int numEdgeDPart;
	int numEdgeTPart;
	int numCut;
	
	int smallerEdge;
	int numNonResetEdge;
	double expansionRatio;	
	
	public Report(int numEdge, int numEdgeDPart, int numEdgeTPart, int numCut, boolean print){
	    
	    //STORE
	    this.numEdge = numEdge;
	    this.numCut = numCut;
	    this.numEdgeDPart = numEdgeDPart;
	    this.numEdgeTPart = numEdgeTPart;

	    this.smallerEdge = Math.min(numEdgeDPart, numEdgeTPart);
	    this.numNonResetEdge = numEdgeDPart + numEdgeTPart;
	    this.expansionRatio = (double)smallerEdge/numCut;

	    if (print){
		//PRINT
		System.out.println("\nNumber of cut is " + numCut);
		//System.out.println("Number of Edge is " + numEdge);
		System.out.println("Number of Non-reset Edge is " + numNonResetEdge);
		System.out.format("Number of Edge in smaller partition is %d (%.2f)\n",
				  smallerEdge, (double)smallerEdge/numNonResetEdge);
		System.out.format("Expansion Ratio is %.2f\n\n", expansionRatio);
	    }
	}
    }


}
