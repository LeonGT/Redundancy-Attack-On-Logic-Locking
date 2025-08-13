//import edu.princeton.cs.algs4.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.Map.*;
import java.util.AbstractMap.*;
import java.util.regex.*;
import java.math.*;

public class FM{

    public Graph g;
    ArrayList<Node> nodeList;
    public HashSet<Node> allA, allB;
    public HashSet<Node> unmoved;
    public int nodeCount;
    public int largerCount;
    public int cost;
    public int bal;
    public int partSeed;
    
    public FM(Graph graph, int balance, int seed){

	//set up global parameters
	allA = new HashSet<Node>();
        allB = new HashSet<Node>();
        unmoved = new HashSet<Node>();
	nodeCount = graph.numNode;
	partSeed = seed;
	bal = balance;
	
	largerCount = (int) Math.floor(nodeCount * 0.5 * (1 + (double)balance/100)) + 1;
	nodeList = new ArrayList<>(graph.nodes.values());

	//generate a random initial partition
        Collections.shuffle(nodeList, new Random(seed));
        int counter = 0;
        for (Node currNode : nodeList){
	    (counter++ < (nodeCount/2)? allA:allB).add(currNode);
	}

	cost = getCutCost();
	System.out.format("SUMMARY: Initial FM Partition - {%d} nodes into {%d, %d} at cost %d\n",
			  nodeCount, allA.size(), allB.size(), cost);

	int passCounter = 1;
	System.out.format("INFO: Pass %d ", passCounter);		    
	int newCost = doAllMoves();
	System.out.format("SUMMARY: After-Pass FM Partition - {%d} nodes into {%d, %d} at cost %d\n",
			  nodeCount, allA.size(), allB.size(), newCost);
	
	while (newCost < cost){
	    cost = newCost;
	    System.out.format("INFO: Pass %d\n", ++passCounter);		    
	    newCost = doAllMoves();

	    System.out.format("SUMMARY: After-Pass FM Partition - {%d} nodes into {%d, %d} at cost %d\n",
			      nodeCount, allA.size(), allB.size(), newCost);
	
	}

	//update the graph object

	HashSet<Node> smallerList = allA.size()<allB.size() ? allA : allB;
	HashSet<Node> largerList = allA.size()<allB.size() ? allB : allA;
	for (Node node : smallerList){
	    node.group = 'T';
	}
	for (Node node : largerList){
	    node.group = 'D';
	}	
	
	System.out.format("SUMMARY: Final FM Partition - {%d} nodes into {%d, %d} at cost %d\n",
			  nodeCount, allA.size(), allB.size(), getCutCost());
	
    }

    public int doAllMoves(){
	
	unmoved.addAll(allA);
	unmoved.addAll(allB);
        LinkedList<Node> moved = new LinkedList<>();
	int minCost = cost;
	int minIndex = 0;
	
	//move all nodes
	for (int i = 1; i <= nodeCount; ++i){

	    System.out.format("INFO: Move %d/%d\n", i, nodeCount);	    
	    Node bestMove = findBestSingleMove(); // find best single move

	    //no move can make the partition balanced
	    moveNode(bestMove);
	    moved.push(bestMove);
	    unmoved.remove(bestMove);

	    //check if the cost is any better
	    int newCost = getCutCost();
	    if (newCost < minCost){
		minCost = newCost;
		minIndex = i;
	    }
	}

	System.out.format("INFO: BEST CUMULATIVE %d/%d\n", minIndex, nodeCount);

	//rewind a bit more
	while (moved.size() > minIndex){
	    Node singleMove = moved.pop();
	    System.out.format("INFO: Rewind Move %d/%d on node %s\n", moved.size()+1, nodeCount, singleMove.seqName);
	    moveNode(singleMove);
        }

	return getCutCost();
    }

    public Node findBestSingleMove(){

	int bestGain = Integer.MIN_VALUE;
	Node bestMove = null;

	//find the best node move
	//System.out.println("DEBUG: UNMOVED size " + unmoved.size());
	for (Node node : unmoved){
	    int gain = getNodeGain(node);
	    if (gain > bestGain){
		bestGain = gain;
		bestMove = node;
	    }	    
	}

        System.out.format("INFO: Best Single Move %s with gain %d - {%d, %d} at cost %d\n",
			  bestMove.seqName, bestGain, allA.size(), allB.size(), getCutCost());

	return bestMove;
    }

    //how much moving a node would gain
    int getNodeGain(Node node){

	//initial cost
	int initCost = getCutCost();
	moveNode(node);
	int newCost = getCutCost();
	boolean stillBalanced = Math.max(allA.size(), allB.size()) <= largerCount;
	moveNode(node);

	//System.out.format("DEBUG: %s with init %d and new %d and gain %d\n",
	//			  node.seqName, initCost, newCost, initCost - newCost);

	//reject any move that would violate balance constraint
	if (stillBalanced)
	    return initCost - newCost;	
	else
	    return Integer.MIN_VALUE;
    }

    void moveNode(Node node){
	if (allA.contains(node)){
	    allA.remove(node);
	    allB.add(node);
	}
	else {
	    allB.remove(node);
	    allA.add(node);
	}
    }
    
    int getCutCost(){

	int tempCost = 0;

	//entrance states in A
	for (Node currNode : allA){
	    for (Node inNode : currNode.incomings){
		if (allB.contains(inNode)){
		    tempCost++;
		    break;
		}
	    }
	}

	//entrance states in B
	for (Node currNode : allB){
	    for (Node inNode : currNode.incomings){
		if (allA.contains(inNode)){
		    tempCost++;
		    break;
		}
	    }
	}
	return tempCost;
    }

    ReportFM getReport(){

	return new ReportFM(bal, partSeed, allA.size(), allB.size(), cost);

    }
    
    class ReportFM implements Comparable<ReportFM>{

	int balance;
	int initSeed;
	int DNode;
	int TNode;
	int totalNode;
	int cutSize;
	int smallerNode;
	int largerNode;
        double rRatio;

	
	public ReportFM(int balance, int initSeed, int DNode, int TNode, int cutSize){
	    
	    //STORE
	    this.balance = balance;
	    this.initSeed = initSeed;
	    this.DNode = DNode;
	    this.TNode = TNode;
	    this.cutSize = cutSize;

	    this.totalNode = DNode + TNode;
	    this.smallerNode = Math.min(DNode, TNode);
	    this.largerNode = Math.max(DNode, TNode);
	    this.rRatio = smallerNode/(double)cutSize;
	}

	@Override
	public int compareTo(ReportFM anotherReport) {

	    if (this.rRatio < anotherReport.rRatio){
		return -1;
	    }
	    else if (this.rRatio < anotherReport.rRatio){
		return (this.smallerNode < anotherReport.smallerNode ? -1 : 1);
	    }
	    else{
		return 1;
	    }
	}

	@Override
	public String toString(){
	    return String.format("SUMMARY: Seed: %d, rRatio: %.2f, smallerNode, %d", partSeed, rRatio, smallerNode);
	    
	}
    }
    
}
