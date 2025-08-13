import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.regex.*;
import java.util.*;
import java.util.stream.*;
import java.time.*;

public class Tool{

    //Tester
    public static void main(String[] args) throws Exception{

	List<String> output = exec("tmax -shell run_tmax.tcl", true);

	ATPG_Result atpgRe = new ATPG_Result(output,1,1,1);
	//System.out.println(result);
    }


    //read a file into arrayList
    public static List<String> readFile(String file){
	try{
	    return Files.readAllLines(Paths.get(file));
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return null;
    }

    //replace the line containing "keyword" with the new line entirely
    public static void replaceFileContent(String inFile, String outFile,
				     String keyword, String newline){

	try{
	//Instantiating the Scanner class to read the file
	Scanner sc = new Scanner(new File(inFile));
	//instantiating the StringBuffer class
	StringBuffer buffer = new StringBuffer();
	//Reading lines of the file and appending them to StringBuffer
	while (sc.hasNextLine()) {
	    String currLine = sc.nextLine();
	    if (currLine.contains(keyword)){
		currLine = newline;
	    }
	    buffer.append(currLine+System.lineSeparator());
	}
	String fileContents = buffer.toString();
	sc.close();


	//instantiating the FileWriter class
	FileWriter writer = new FileWriter(outFile);
	writer.append(fileContents);
	writer.flush();
	}
	catch (Exception e){
	    System.out.println("Error when replacing file content.");
	    e.printStackTrace();
	}
	return;
    }

    
    public static boolean isNumeric(String str) {
	//match a number with optional '-' and decimal.
	return str.replaceAll("\\s", "")
	    .matches("-?\\d+(\\.\\d+)?");  
    }
    
    public static List<String> exec(String cmd, Boolean real) throws Exception{

	System.out.println("INFO: Start cmd - \"" + cmd + "\"");
	Instant timeOld = Instant.now();
	
	List<String> ret = new ArrayList<String>();
	try {
	    Process proc = Runtime.getRuntime().exec(cmd);

	    InputStream inputStream = proc.getInputStream();
	    InputStreamReader inputStreamReader =
		new InputStreamReader(inputStream);
	    BufferedReader bufferedReader =
		new BufferedReader(inputStreamReader);
	    String line;
	    while ((line = bufferedReader.readLine()) != null) {
		if(real) System.out.println(line);
		ret.add(line);
	    }
	    proc.waitFor();
	} catch (IOException e) {
	    System.out.println("Error when executing command: " + cmd);
	    e.printStackTrace();
	}

	//Report the execution time
	Instant timeNew = Instant.now();
	long spentTime = Duration
	    .between(timeOld, timeNew).toMillis();
	System.out.println("INFO: command finished in "
			   +  (spentTime/1000) + "s");
	return ret;
    }
    
	
    //https://stackoverflow.com/questions/67090500/java-execute-shell-commands-how-to-read-output-in-real-time
    public static String execReal(String cmd, Boolean real) throws Exception{

	//System.out.println("Executing CMD: " + cmd);
	StringBuilder ret = new StringBuilder();
	try {
	    Process proc = Runtime.getRuntime().exec(cmd);

	    InputStream inputStream = proc.getInputStream();
	    InputStreamReader inputStreamReader =
		new InputStreamReader(inputStream);
	    BufferedReader bufferedReader =
		new BufferedReader(inputStreamReader);
	    String line;
	    while ((line = bufferedReader.readLine()) != null) {
		if(real) System.out.println(line);
		ret.append(line + "\n");
	    }
	    proc.waitFor();
	} catch (IOException e) {
	    System.out.println("Error when executing command: " + cmd);
	    e.printStackTrace();
	}
	return ret.toString();
    }

    

    //extract (key) gates whose index is between start and end-1
    public static ArrayList<Gate> extractRange
	(ArrayList<Gate> original, int start, int end){
	ArrayList<Gate> extracted = new ArrayList<Gate>();
	for (Gate g : original){
	    if (extractInt(g.name) < start ||
		extractInt(g.name) >= end) continue;
	    extracted.add(g);
	}
	return extracted;
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

    //Extract integer from a string
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

    public static String extractFName(String path) {
	return path.substring(path.lastIndexOf("/")+1, path.lastIndexOf("."));
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
