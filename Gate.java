import java.util.*;
import java.io.Serializable;


public class Gate implements Comparable<Gate>, Serializable{

    //Type Constants
    public static String AND = "AND";
    public static String NAND = "NAND";
    public static String OR = "OR";
    public static String NOR = "NOR";
    public static String XOR = "XOR";
    public static String XNOR = "XNOR";
    public static String NOT = "NOT";
    public static String INPUT = "INPUT";
    public static String OUTPUT = "OUTPUT";
    public static String UNKNOWN = "UNKNOWN";
    public static String BUFFER = "BUFF";
    public static String LUT = "LUT";
    public static String MUX = "MUX";

    //Property Constants
    public static String PRO_FFIN = "FF_IN";
    public static String PRO_FFOUT = "FF_OUT";
    public static String PRO_NONE = "NONE";

    
    public String type;
    public String name;
    public boolean out;
    public boolean kin;
    public String property;
    public LinkedList<Gate> inputs;
    public LinkedList<Gate> outputs;
    public boolean visited = false;
    public boolean reachFromKinOnly = false;
    public int depth = 100;

    public Gate(String name){
	this.type = UNKNOWN;
	this.name = name;
	this.out = false;
	this.kin = false;
	this.property = PRO_NONE;
	this.inputs = new LinkedList<Gate>();
	this.outputs = new LinkedList<Gate>();
    }
	
    public Gate(String type, String name, boolean out, boolean kin){
	this.type = type;
	this.name = name;
	this.out = out;
	this.kin = kin;
	this.property = PRO_NONE;
	this.inputs = new LinkedList<Gate>();
	this.outputs = new LinkedList<Gate>();
    }

    public void addInputs(HashSet<Gate> inputs){
	this.inputs.addAll(inputs);		
    }

    public void addInput(Gate input){
	this.inputs.add(input);
    }

    public void clearInputs(){
	this.inputs.clear();
    }

    public void clearInput(Gate input){
	this.inputs.remove(input);
    }

    public void addOutputs(LinkedList<Gate> outputs){
	this.outputs.addAll(outputs);
    }

    public void addOutput(Gate output){
	this.outputs.add(output);
    }

    public void clearOutputs(){
	this.outputs.clear();
    }

    public void clearOutput(Gate output){
	this.outputs.remove(output);
    }
	
    @Override
    public int compareTo(Gate another) {
	return this.toString().compareTo(another.toString());
    }

    @Override
    public String toString() {

	//NAME = TYPE(IN1, IN2)
	String build = "";
	ArrayList<Gate> inputs = new ArrayList<>(this.inputs);
	build += this.name + " = " + this.type + "(";

	if (!this.type.equals(Gate.INPUT)){
						
	    //write the first input gate
	    build += inputs.get(0).name;
				
	    for (int i = 1; i < inputs.size(); ++i){
		build += ", " + inputs.get(i).name;
	    }
	}
	build += ")";
	return build;
		
    }

}
