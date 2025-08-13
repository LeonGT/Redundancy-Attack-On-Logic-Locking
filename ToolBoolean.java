import java.io.*;
import java.nio.*;
import java.util.regex.*;
import java.util.*;
import java.util.stream.*;

public class ToolBoolean{

    public static String execCmd(String cmd) throws java.io.IOException {
	java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
	return s.hasNext() ? s.next() : "";
    }
    
    public static String exec2(String command) throws Exception{
	Runtime rt = Runtime.getRuntime();
	String[] commands = {command};
	Process proc = rt.exec(commands);

	BufferedReader stdInput = new BufferedReader(new 
						     InputStreamReader(proc.getInputStream()));

	BufferedReader stdError = new BufferedReader(new 
						     InputStreamReader(proc.getErrorStream()));

	// Read the output from the command
	System.out.println("Here is the standard output of the command:\n");
	String s = null;
	while ((s = stdInput.readLine()) != null) {
	    System.out.println(s);
	}

	// Read any errors from the attempted command
	System.out.println("Here is the standard error of the command (if any):\n");
	while ((s = stdError.readLine()) != null) {
	    System.out.println(s);
	}
	
	return "";
    }
    
    //print realtime outputs additionally to terminal
    public static String execReal(String cmd, boolean print){

	StringBuilder ret = new StringBuilder();
	
	try {
	    Process proc = Runtime.getRuntime().exec(cmd);
	    String line;
	    
	    BufferedReader bufferedReaderErr =
		new BufferedReader(new InputStreamReader(proc.getErrorStream()));
	    BufferedReader bufferedReaderOut =
		new BufferedReader(new InputStreamReader(proc.getInputStream()));



	    while ((line = bufferedReaderErr.readLine()) != null) {
		// it prints all at once after command has been executed.
		if(print) System.out.println(line);
	    }
	    
	    
	    while ((line = bufferedReaderOut.readLine()) != null) {
		// it prints all at once after command has been executed.
		if (print) System.out.println(line);
		ret.append(line + "\n");
	    }

	    proc.waitFor();
	}
	catch (Exception e){
	    System.out.println("Unable to execute command: " + cmd);
	    e.printStackTrace();
	    return null;
	}

	return ret.toString();
    }

    //extract (key) gates whose index is between start and end-1
    public static ArrayList<Gate> extractRange(ArrayList<Gate> original, int start, int end){
	ArrayList<Gate> extracted = new ArrayList<Gate>();
	for (Gate g : original){
	    if (extractInt(g.name) < start || extractInt(g.name) >= end) continue;
	    extracted.add(g);
	}
	return extracted;
    }


    public static ArrayList<String> extractFaults(String inFile, String outFile, String name){

	//use ATALANTA to generate fault list
	String ATAGF = "atalanta -D 1 -b 1 " + inFile;
	String testFile = inFile.substring(0, inFile.indexOf(".")) + ".test";
	execReal(ATAGF, true);
	
	Scanner in = null;
	ArrayList<String> faults = new ArrayList<>();
	int counter = 0;
	
	//convert the trivial test set into fault list format
	try{
	    PrintWriter writer = new PrintWriter(outFile);
	    in = new Scanner(new File(testFile));
	    while(in.hasNextLine()){
		String line = in.nextLine();

		//find lines with fault name
		if (line.contains("/") && !line.contains("CON") && !line.contains("KIn")){
		    //add if name is "" or line contains name
		    if (name.equals("") || (!name.equals("") && line.contains(name))){
			faults.add(line);
			writer.write(line + "\n");
			counter++;
		    }
		}
	    }

	    writer.close();
	    System.out.println("Total number of faults: " + counter);
	}
	catch (FileNotFoundException e){
	    System.out.println("Cannot open input file");
	}
	finally{
	    return faults;
	}
	
    }

    //https://stackoverflow.com/questions/27110563
    ///how-can-i-check-if-a-string-has-a-substring-from-a-list
    public static boolean containsKWD(String myString, List<String> keywords){
	for(String keyword : keywords){
	    if(myString.contains(keyword)){
		return true;
	    }
	}
	return false;
    }

    //extract int
    public static int extractInt(String s) {

	String num = s.replaceAll("\\D", "");
	// return 0 if no digits found
	return num.isEmpty() ? 0 : Integer.parseInt(num);
    }


    //save graph object to binary file
    public static void saveObjBinary(Object obj, String binary) {
	
	//save the graph object
	try {
            FileOutputStream f = new FileOutputStream(new File(binary));
            ObjectOutputStream o = new ObjectOutputStream(f);

            // Write objects to file
            o.writeObject(obj);

            o.close();
            f.close();

        } catch (Exception e) {
	    e.printStackTrace();
	    return;
        }
    }
    
    //read graph object from binary file
    public static Object readObjBinary(String binary){

        Object obj = null;
	//save the graph object
	try {
            FileInputStream fi = new FileInputStream(new File(binary));
            ObjectInputStream oi = new ObjectInputStream(fi);

            // Read objects
	    obj = oi.readObject();

            oi.close();
            fi.close();

        } catch (Exception e) {
	    e.printStackTrace();
	    return null;
        }

	return obj;
    }

    public static String extractFileNameFromPath(String path) {
        return path.substring(path.lastIndexOf("/")+1, path.indexOf("."));
    }


    //From https://stackoverflow.com/questions/65973024/generate-all-possible-string-combinations-by-replacing-the-hidden-number-sig
    //convert a string containing X into binary ones. e.g. 1X1 -> 111 & 101
    public static String[] expandOnX(String str) {
	// an array of substrings around a 'number sign'
	String[] arr = str.split("x", -1);
	// an array of possible combinations
	return IntStream
            // iterate over array indices
            .range(0, arr.length)
            // append each substring with possible
            // combinations, except the last one
            // return Stream<String[]>
            .mapToObj(i -> i < arr.length - 1 ?
		      new String[]{arr[i] + "0", arr[i] + "1"} :
		      new String[]{arr[i]})
            // reduce stream of arrays to a single array
            // by sequentially multiplying array pairs
            .reduce((arr1, arr2) -> Arrays.stream(arr1)
                    .flatMap(str1 -> Arrays.stream(arr2)
			     .map(str2 -> str1 + str2))
                    .toArray(String[]::new))
            .orElse(null);
    }


    //generate all binary string of a given width
    public static boolean[][] enumerate(int size){
	
	boolean[][] bools = new boolean[(int)Math.pow(2, size)][size];
		
	for(int i = 0; i < bools.length; i++){
	    for(int j = 0; j < bools[i].length; j++){
		int val = bools.length * j + i;
		int sh = (1 & (val >>> j));
		bools[i][bools[i].length - 1 - j] = (sh != 0);
	    }
	}

	return bools;
	
    }
    
}
