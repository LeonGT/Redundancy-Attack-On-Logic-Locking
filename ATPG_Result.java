import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.math.*;
import java.time.*;


public class ATPG_Result{

    //Sample Tetramax Summary
    /////////////////////////////////////////////////////
    // Uncollapsed Stuck Fault Summary Report	    //
    // ----------------------------------------------- //
    // fault class                     code   #faults  //
    // ------------------------------  ----  --------- //
    // Detected                         DT          1  //
    // Possibly detected                PT          0  //
    // Undetectable                     UD          0  //
    // ATPG untestable                  AU          0  //
    // Not detected                     ND          0  //
    // ----------------------------------------------- //
    // total faults                                 1  //
    // test coverage                           100.00% //
    // ----------------------------------------------- //
    //            Pattern Summary Report		    //
    // ----------------------------------------------- //
    // #internal patterns                           2  //
    //     #basic_scan patterns                     2  //
    // ----------------------------------------------- //
    /////////////////////////////////////////////////////

    
    //Sample Tetramax Pattern
    ///////////////////////////////////////////////////////////////////////////////////////
    // report_patterns -all								 //
    //  Pattern 0 (basic_scan)							         //
    //  Time 0: force_all_pis =							         //
    //      0000000000 0000000000 0000000000 0000000000 1001001111 1101010011 0000000010 //
    //      11									         //
    //  Time 1: measure_all_pos = 1100110011 0100011000 0001100111 0110	                 //
    //  Pattern 1 (basic_scan)						                 //
    //  Time 0: force_all_pis =						                 //
    //      0000000000 0000000000 0000000000 0000000000 1100100111 1110101001 1000000001 //
    //      01									         //
    //  Time 1: measure_all_pos = 0111011101 0010111101 0011000000 0010		         //
    // exit									         //
    ///////////////////////////////////////////////////////////////////////////////////////

    ArrayList<TPattern> patterns = new ArrayList<TPattern>();
    int numDetected;
    int numUndetected;
    
    public ATPG_Result(List<String> atpgOut, int numPI, int numKI, int numPO){
	
	//Key words for summary extraction
	Pattern ptnDetected = Pattern.compile(".*Detected\\s+DT.*"); //Detected...DT...
	Pattern ptnUndetected = Pattern.compile(".*Undetectable\\s+UD.*"); //Detected...DT...
	Pattern ptnPattern = Pattern.compile(".*Pattern\\s\\d+\\s\\(.*"); //Pattern X (
	
	
	for (int i = 0; i < atpgOut.size(); ++i){
	    String currLine = atpgOut.get(i);

	    //the line specifying the number of detected faults
	    if (ptnDetected.matcher(currLine).matches()){
		numDetected = Tool.extractInt(currLine);
	    }

	    //the line specifying the number of undetected faults
	    if (ptnUndetected.matcher(currLine).matches()){
		numUndetected = Tool.extractInt(currLine);
	    }
	    
	    //find the parts specifying patterns
	    if (ptnPattern.matcher(currLine).matches()){

		//read input condition
		String inputPtn = atpgOut.get(++i); // first line with Time 0
		while (Tool.isNumeric(atpgOut.get(i+1))){
		    inputPtn += atpgOut.get(++i);
		}

		//read output condition
		String outputPtn = atpgOut.get(++i); //first line with Time 1
		while (Tool.isNumeric(atpgOut.get(i+1))){
		    outputPtn += atpgOut.get(++i);
		}

		//clean input and output by removing leading string and spaces

		inputPtn = inputPtn 
		    .substring(inputPtn.indexOf("=")+1, inputPtn.length())
		    .replaceAll("\\s+", "");

		outputPtn = outputPtn
		    .substring(outputPtn.indexOf("=")+1, outputPtn.length())
		    .replaceAll("\\s+", "");

				
		//System.out.format("ATPG_EXTRACT:\n\tInput :%s\n\tOutput:%s\n",
		//		  inputPtn, outputPtn);

		TPattern ptn = new TPattern(inputPtn, outputPtn,
						  numPI, numKI, numPO);
		patterns.add(ptn);
		System.out.println(ptn);
	    }
	}
    }

    class TPattern{
	
	String inValue = ""; // primary input to DUTs
	String keyValueA = ""; //key value to DUT copy A
	String keyValueB = ""; //key value to DUT copy B
	String outValue = ""; // primary output of the oracle
	String auxOutValue = ""; //helper outputs (oracle & final_out)

	TPattern(String in, String out,
		    int numPI, int numKI, int numPO){

	    //check validity
	    if (numPI+numKI*2 != in.length()){
		System.err.println("Input Length Mismatch");
		return;
	    }

	    //Format of in: [--numPI--][--numKI--][--numKI--]
	    //Format of out: [--numPO--][2]
	    //where 2 bits are oracle & final indicators
	    
	    this.inValue = in.substring(0, numPI); //numPI bits
	    this.keyValueA = in.substring(numPI, numPI+numKI); //numKI bits
	    this.keyValueB = in.substring(numPI+numKI, in.length()); //numKI bits
	    this.outValue = out.substring(0, numPO); //numPO bits
	    this.auxOutValue = out.substring(numPO, out.length()); //2 bits
	    
	}

	@Override
	public String toString() {

	    return String.format("ATPG Pattern:" +
				 "\n  inValue:%s\n  keyValueA:%s\n  keyValueB:%s" +
				 "\n  outValue:%s\n  auxOutValue:%s\n",
				 inValue, keyValueA, keyValueB,
				 outValue, auxOutValue);
	    
	}

    }
    


}
