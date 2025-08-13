	//import edu.princeton.cs.algs4.*;
	import java.io.*;
	import java.nio.*;
	import java.nio.file.*;
	import java.util.*;
	import java.util.Map.*;
	import java.util.AbstractMap.*;
	import java.util.regex.*;
	import java.math.*;
	import java.net.URI;
	import java.time.*;


	public class Netlist_Handler{

		public static void main(String[] args) throws Exception{

		//Sample run
		//java Main fullSteps KISS2/acm-sigda-mcnc/s8.kiss2 s8.metis s8.sav s8.part s8.v s8_obf.v

		Instant before = Instant.now(); //record the time before execution
		String command = null;
		
		//wait for user's input
		if (args.length == 0){

			//guide user to enter certain commands
			System.out.println("Enter a command to continue: ");
			try{
			Scanner in = new Scanner(System.in);
			command = in.nextLine();
			in.close();
			}
			catch (NoSuchElementException e){ //catch Ctrl+D
			System.out.println("User pressed EXIT");
			return;
			}
		}
		
			command = args[0];
		
		switch(command){

		case "parse": //inBenchFile
			parse(args[1]);
			break;
			
		case "supportingAnalysis": //inBenchFile, circuitBinary, 
			supportingAnalysis(args[1], args[2], args[3]);
			break;
			
		case "reverseFSM": //inBenchFile
			reverseFSM(args[1]);
			break;
			
		case "attackSingleRepeat": //inBenchFile, circuitBinary, 
			attackSingleRepeat(args[1]);
			break;
		
		case "encSecureXOR":
			encSecureXOR(args[1], Integer.parseInt(args[2]));
			break;
			
		case "encSecureNew":
			encSecureNew(args[1], Integer.parseInt(args[2]), args[3]);
			break;
			
		case "findSupport":
			findSupport(args[1], args[2]);
			break;

		case "buildMeter":
			buildMeter(args[1]);
			break;
			
		case "keyComparison":
			keyComparison(args[1]);
			break;

		case "encRandomXOR":
			encRandomXOR(args[1], args[2], Integer.parseInt(args[3]),
				Integer.parseInt(args[4]));
			break;
		default:
			System.out.println("Unrecognized command, EXIT");
			return;			
			
		}
		
		Instant after = Instant.now(); //record the time after execution
		long spentTime = Duration.between(before, after).toMillis();
		System.out.println("Finished - Total Execution Time: " +
				(spentTime/1000) + "s and " + (spentTime % 1000) + "mS");
		}

		//COMMAND STORAGE
		//Extract final output
		//cat result.txt | grep -F "[FINAL] ./Benchmark"
		
		public static void parse(String inFile){
		Circuit circuit = new Circuit(inFile);
		circuit.printTo("dut.bench");
		circuit.printToV("dut.v");
		//circuit.printTo("temp1.bench");
		//circuit.printToV("temp1.v");

		//String fName = Tool.extractFName(inFile);
		//System.out.println(fName);
		//circuit.printToV("test.v");
		/*
		for (String gate : circuit.findNear("keyinput52", 15)){
			if (gate.contains("keyinput")) System.out.println("Nearby KG: " + gate);
		}
		*/
			//findSecureXORGate(inFile, 10);
		}

		
		public static void encRandomXOR(String inFile, String outFile, int seed, int numKey){
		Circuit circuit = new Circuit(inFile);
		circuit.printTo(Tool.extractFName(outFile) + "_orig.bench");
		circuit.encRandomXOR(seed, numKey);
		circuit.printTo(outFile);
		circuit.printTo(Tool.extractFName(outFile) + ".bench");
		
		}

		
		public static void buildMeter(String inBenchFile) throws Exception{

		Circuit origCircuit = new Circuit(inBenchFile);
		Circuit meterCircuit = new Circuit(inBenchFile);
		//clean all gates
		meterCircuit.gates = new HashMap<String, Gate>();
		
		for (Gate g : meterCircuit.getGateList()){
			
		}
		
		
		}

		//inFile has two lines: line 1 with the original key
		//line 2 with the recovered key
		public static void keyComparison (String inFile) throws Exception{

		Scanner s = new Scanner(new File(inFile));
		ArrayList<String> list = new ArrayList<String>();
		while (s.hasNext()){
			list.add(s.next());
		}
		s.close();

		String originalKey = list.get(0);
		String recoverKey = list.get(1);
		int correctCounter = 0;
		int incorrectCounter = 0;
		int total = recoverKey.length();
		
		for (int i = 0; i < total; ++i){
			char c1 = originalKey.charAt(i);
			char c2 = recoverKey.charAt(i);

			if (c2 != 'x' && c2 == c1)
			correctCounter++;
			else if (c2 != 'x' && c2 != c1)
			incorrectCounter++;
		}

		System.out.println("Correct: " + correctCounter);
		System.out.println("Inorrect: " + incorrectCounter);
		
		}

		
		//outFile1: gates with pure key gate supporting inputs
		//outFile2: gates with some non-key supporting inputs
		public static void supportingAnalysis(String inBenchFile, String outFile1, String outFile2) throws Exception{
		Circuit circuit = new Circuit(inBenchFile);
		//	circuit.printTo(outBenchFile, null);
		//circuit.printToV(outBenchFile);
		circuit.analyzeReachability();

		ArrayList<Gate> isFromKin = circuit.findFaninKinOnlyGates();
		ArrayList<Gate> isNotFromKin = circuit.getGateList();
			isNotFromKin.removeAll(isFromKin);

		PrintWriter writer1;
		PrintWriter writer2;
		try{
			writer1 = new PrintWriter(outFile1);
			writer2 = new PrintWriter(outFile2);
		}
		catch (IOException e){
			System.out.println("Output File cannot be written");
			return;
		}
		
		System.out.println("Gates with fan-in cone purely from key inputs");
		for (Gate g : isFromKin){
			//System.out.println(g.name);
			writer1.println(g.name);
		}

		System.out.println("Gates with fan-in cone NOT from key inputs");
		for (Gate g : isNotFromKin){
			//System.out.println(g.name);
			writer2.println(g.name);
		}

		writer1.close();
		writer2.close();
		}

		public static void findSupport(String inBenchFile, String gate) throws Exception{
		Circuit circuit = new Circuit(inBenchFile);
		System.out.println(circuit.findSupport(gate));
		
		
		}

		
		public static ArrayList<Gate[]> findSecureXORGate(String inBenchFile, int max){

		//find all gate and branch locations
		//If gate: Gate[] has size 1: current gate
		//If branch: Gate[] has size 2: current gate, branch to gate
		Circuit circuit = new Circuit(inBenchFile);
		int counter = 0;

		ArrayList<Gate[]> gatesAndBranches = circuit.getInternalGateBranchList();
		ArrayList<Gate> gates = circuit.getInternalGateList();
		for (Gate g : gates){
			gatesAndBranches.add(new Gate[]{g});
		}
		
		Collections.shuffle(gatesAndBranches, new Random(1)); //shuffle gates
		
		ArrayList<Gate[]> candidates = new ArrayList<Gate[]>();

		String[] ignoreKeywordsArr = findIgnoreKeywords(circuit);
		
		
		for (Gate[] gateSpec : gatesAndBranches){
				
			Circuit setCircuit = new Circuit(inBenchFile);
			char[] initKey = new char[] {'X'};
			char[] keyAss = null;

			//add the key gate
			//if gate
			if (gateSpec.length == 1){
			setCircuit.addXOR(0, gateSpec[0].name);
			}
			//if branch
			else{
			setCircuit.addXORBranch(0, gateSpec[0].name, gateSpec[1].name);
			}

			//attack the single key gate
			try{
			keyAss = attackSingle(setCircuit, initKey, -1);
			}
			catch (Exception e){}

			
			if (keyAss[0] == 'X'){
			candidates.add(gateSpec);
			counter++;
			System.out.println("[INFO] Found " + counter + " individual secure locations");
			if (counter == max) break;
			}
		}

		//print for debug
		for(Gate[] gArr : candidates){

			System.out.print("Candidate: ");
			for (Gate g : gArr){
			System.out.print(g.name + ", ");
			}
			System.out.println("");
		}

		return candidates;
		}

		
		public static void encSecureNew(String inBenchFile, int numKeyGate, String kgType) throws Exception{

		Instant timeStart = Instant.now(); //record the time before execution
		Circuit circuit = new Circuit(inBenchFile);	
		//int numGate = circuit.gates.size() - circuit.getInputList().size(); //Number of logical gates
		//int numKeyGate = Math.min(128, Math.max(1, numGate * overheadLevel / 100)); //insert at least 1 key gate
		System.out.format("[INFO] Goal: Insert %d Key Gates to %s\n", numKeyGate, inBenchFile);
		
		LinkedList<Gate[]> gatesAndBranches = null;

			if (kgType.equals("XOR")){
			gatesAndBranches = new LinkedList<Gate[]>(circuit.getInternalGateBranchList());
			ArrayList<Gate> gates = circuit.getInternalGateList();
			for (Gate g : gates){
			gatesAndBranches.add(new Gate[]{g});
			}
		}
		else if (kgType.equals("MUX")){

			gatesAndBranches = new LinkedList<Gate[]>();
			for (Gate gBranch : circuit.getInternalGateList()){

			if (gBranch.outputs.size() < 1) continue; // alt must have other branches
			
			for (Gate[] gateArr : circuit.getInternalGateBranchList()){
				Gate g1 = gateArr[0];
				Gate g2 = gateArr[1];
				Gate[] cand = new Gate[]{g1, g2, gBranch};
				gatesAndBranches.add(cand);
			}
			}
			
		}

		Collections.shuffle(gatesAndBranches, new Random(1)); //shuffle gates

		int keyCounter = 0;
		int consecutiveNoImproveCounter = 0;
		
		//circuit that keeps adding key gates
		Circuit updateCircuit = new Circuit(inBenchFile);
		
		while (keyCounter < numKeyGate){

			consecutiveNoImproveCounter ++;

			if (consecutiveNoImproveCounter > 40){
			System.out.println("[FINAL] Result ABOSRT: CANNOT FIND INDIVIDUALLY GOOD LOCATION");
			return;
			}
			
			System.out.println("[INFO] Trying to insert K" + keyCounter + "/" + numKeyGate);
			
			//generate a trial circuit
			updateCircuit.printTo("temp.bench");
			Circuit setCircuit = new Circuit("temp.bench");
			//initialize key array
			
			//generate a fresh circuit
			Gate[] cand = gatesAndBranches.removeFirst();

			//inset one more XOR
			if (kgType.equals("XOR")){
			if (cand.length == 1){
				setCircuit.addXOR(keyCounter, cand[0].name);
			}
			//if branch
			else{
				setCircuit.addXORBranch(keyCounter, cand[0].name, cand[1].name);
			}
			}

			//insert one more MUX
			else if (kgType.equals("MUX")){
			if (!setCircuit.addMUXBranchNew(keyCounter, cand[0].name, cand[1].name, cand[2].name))
				continue;
			}

			//attack the single key gate
			ArrayList<Gate> kiList = setCircuit.getKiList();
			char[] keyAss = new char[kiList.size()];
			Arrays.fill(keyAss, 'X');
			
			//try singleton
			char[] keyAssResult = attackSingle(setCircuit, keyAss, keyCounter);
			if (keyAssResult[keyCounter] != 'X') continue;
			
			//try doubleton
			ArrayList<Integer> doubleResult = doubleResult = attackDouble(setCircuit, keyAss, keyCounter);

			//more relaxed threshold
			if (doubleResult.get(0) + doubleResult.get(1) != 0) continue;
			

			//more stringent threshold
			//if (doubleResult.get(2) != 0) continue;

			
			keyCounter++;
			consecutiveNoImproveCounter = 0;
			System.out.println("[INFO] Found " + keyCounter + " individual secure locations");

			//store the trial circuit
			updateCircuit = setCircuit;		
			
		}
		
		String fName = Tool.extractFName(inBenchFile);
		String encPath = "./Redundancy-Secure/" + fName + "_enc" + kgType + ".bench";
		updateCircuit.printTo(encPath);

		Instant timeEnd= Instant.now(); //record the time before execution
		long spentTime = Duration.between(timeStart,timeEnd).toMillis();
			System.out.format("[FINAL] Result Defense %d KG finished with runtime %s",
				numKeyGate, String.valueOf(spentTime/1000));

		
		attackSingleRepeat(encPath);
		}
		
		//Overhead level is the percentage (5 -> 5%)
		public static void encSecureXOR(String inBenchFile, int overheadLevel) throws Exception{

		Circuit circuit = new Circuit(inBenchFile);	
		int numGate = circuit.gates.size();
		int numKeyGate = Math.max(1, numGate * overheadLevel / 100); //insert at least 1 key gate
		System.out.format("[INFO] Goal: Insert %d Key Gates to %s\n", numKeyGate, inBenchFile);
		
			ArrayList<Gate[]> candidates = findSecureXORGate(inBenchFile, numKeyGate * 2);
		
		if (candidates.size() != numKeyGate *2){
			System.out.format("[WARNING]: Limited flexibility of individually secure locations (%d/%d)\n",
					candidates.size(), numKeyGate);
		}

		int counter = 0;

		//circuit that keeps receiving key gates
		Circuit updateCircuit = new Circuit(inBenchFile);
		
		for (Gate[] gateSpec : candidates){

			updateCircuit.printTo("temp.bench");
			Circuit setCircuit = new Circuit("temp.bench");
			
			if (gateSpec.length == 1){
			setCircuit.addXOR(counter, gateSpec[0].name);
			counter++;
			}
			//if branch
			else{
			setCircuit.addXORBranch(counter, gateSpec[0].name, gateSpec[1].name);
			counter++;
			}

			updateCircuit = setCircuit;
			if (counter == numKeyGate) break;
		}

		updateCircuit.printTo("temp.bench");
		attackSingleRepeat("temp.bench");
		
		}
		
		public static ArrayList<Integer> attackDouble(Circuit circuit, char[] keyAss, int targetKeyIndex) throws Exception{

		String[] ignoreKeywordsArr = findIgnoreKeywords(circuit);
		ArrayList<Integer> result = new ArrayList<Integer>();
		ArrayList<Gate> kiList = circuit.getKiList();
		int numKey = kiList.size();
		
		//handle each key input and set to both values
		char[] keyAssStart = keyAss.clone();
		char[] keyAssRet = keyAss.clone();
		
		//extract the faults in the original circuits
		System.out.println("[INFO] Finding redundant faults (double)...");
		String dcOutput = Tool.execReal("dc_shell -f ./run_dc_redundant.tcl", false);

		//Error handling
		if (dcOutput.contains("Error")) return null;
		
		generateTmaxScript("run_tmax_redundant.tcl", "run_tmax_redundant_set.tcl",
				keyAssStart, null);
		String tmaxOutput = Tool.execReal("tmax -shell ./run_tmax_redundant_set.tcl", false);
		HashSet<String> origRedun = extractFaultTmax(tmaxOutput);
		removeIgnoreKeword(origRedun, ignoreKeywordsArr);

		char[] correctKeyArr = circuit.correctKey.toCharArray();
		int countCorrect = 0;
		int countIncorrect = 0;
		int countRedundantInAllCombinations = 0;
		
		ArrayList<Integer> unsetKeyBits = new ArrayList<Integer>();
		for (int i = 0; i < numKey; ++i){
			if (keyAss[i] == 'X') unsetKeyBits.add(i);
		}
			
		//for each key bit
		for (int ii = 0; ii < unsetKeyBits.size(); ++ii){
			
			int i = unsetKeyBits.get(ii);

			//System.out.println("[Debug1] i=" + i + ",ii=" + ii);

			//if there is a particular target, only care that target
			if (targetKeyIndex != -1 && targetKeyIndex != i){
			//System.out.println("[Debug2] skip");
			continue;
			}

			//if there is no target, ignore maximum ii (no pair)
			if (targetKeyIndex == -1 && ii == unsetKeyBits.size()-1){
			//System.out.println("[Debug3] skip");
			continue;
			}

			//find which key bits are close by
			ArrayList<String> nearbyGates = circuit.findNear(Circuit.kin + i, 10);
			HashSet<String> nearbyKeyGates = new HashSet<String>();
			
			for (String gate : nearbyGates)
			if (gate.contains(Circuit.kin)) nearbyKeyGates.add(gate);

			int startIndex = ii+1;
			int endIndex = unsetKeyBits.size()-1;

			//if there is a particular target, look for other key gates with smaller index
			if (targetKeyIndex != -1){
			startIndex = 0;
			endIndex = ii-1;
			}
			
			
			//for every other key bit that is close by
			for (int jj=startIndex; jj <= endIndex; ++jj){

			int j = unsetKeyBits.get(jj);
			if (nearbyKeyGates.contains(Circuit.kin + j)){

				System.out.format("[INFO] Attacking Pair: K%d and K%d\n",
						i, j);

				int[] red = new int [4];
				//trial=0 -> try equivalent, trial=1 -> try invert
				for (int trial : new int[]{0,1,2,3}){
				
				char[] keyAssTry = keyAssStart.clone();
				if (trial == 2 || trial == 3){
					keyAssTry[i] = '1';
				}
				else{
					keyAssTry[i] = '0';
				}

				
				if (trial == 1 || trial == 3){
					keyAssTry[j] = '1';
				}
				else{
					keyAssTry[j] = '0';
				}
				
				generateTmaxScript("run_tmax_redundant.tcl", "run_tmax_redundant_set.tcl",
						keyAssTry, null);

				// System.out.format("[INFO] Trying K%d and K%d %s ", i, j,
				// 		      (trial==0)? "equivalent" : "complement");
				// String eqLine = String.format("add_pi_equivalences {%s%s%s}",
				// 				  Circuit.kin+i,
				// 				  (trial==0)? " " : " -invert ",
				// 				  Circuit.kin+j);
				// generateTmaxScript("run_tmax_redundant.tcl", "run_tmax_redundant_set.tcl",
				// 		       keyAssStart, eqLine);

				//find redundant faults under the key assignment
				tmaxOutput = Tool.execReal("tmax -shell ./run_tmax_redundant_set.tcl", false);
				HashSet<String> newRedun = extractFaultTmax(tmaxOutput);
				newRedun.removeAll(origRedun);
				removeIgnoreKeword(newRedun, ignoreKeywordsArr);
				System.out.println("New Redundant: " + newRedun);

				countRedundantInAllCombinations += newRedun.size();
				red[trial] = newRedun.size();
				}

				
				//if 0&3 > 1&2
				if (red[1] > red[0] && red[1] > red[3] && red[2] > red[0] && red[2] > red[3]){
				
				String resultPrint = String.format("[FINAL] Equivalent Pair: K%d and K%d\n", i, j);
				System.out.println(resultPrint);
				
				
				if (correctKeyArr[i] == correctKeyArr[j])
					countCorrect++;
				else
					countIncorrect++;
				}
				else if (red[1] < red[0] && red[1] < red[3] && red[2] < red[0] && red[2] < red[3]){
				
				String resultPrint = String.format("[FINAL] Complement Pair: K%d and K%d\n", i, j);
				System.out.println(resultPrint);
				
				if (correctKeyArr[i] != correctKeyArr[j])
					countCorrect++;
				else
					countIncorrect++;
				}
				
			}		
			}
		}
		
			result.add(countCorrect);
		result.add(countIncorrect);
		result.add(countRedundantInAllCombinations);
		
		return result;
		}

		public static String[] findIgnoreKeywords(Circuit circuit){

		//Analyze ignore keywords
		ArrayList<String> ignoreKeywords = new ArrayList<String>();
		circuit.analyzeReachability();
		for (Gate g : circuit.findFaninKinOnlyGates()){
			ignoreKeywords.add(g.name);
		}
		String[] ignoreKeywordsArr = Arrays.copyOf(ignoreKeywords.toArray(),
							ignoreKeywords.toArray().length,
							String[].class);
		System.out.println("Ignore keywords: " + String.join(",", ignoreKeywordsArr));

		return ignoreKeywordsArr;
		}

		public static char[] attackSingleRepeat(String inBenchFile) throws Exception{

		//Output format
		//Bench 
		ArrayList<String> finalOutput = new ArrayList<String>();
		finalOutput.add(inBenchFile);
		
		Instant timeStart = Instant.now(); //record the time before execution
		
		System.out.println("[FINAL] Attacking " + inBenchFile);
		
		//parse the circuit to make it compatible with Atalanta	
		Circuit circuit = new Circuit(inBenchFile);
		//	circuit.printToV("./TFolder/netlist.v");
		ArrayList<Gate> kiList = circuit.getKiList();
		finalOutput.add(String.valueOf(kiList.size()));
		
		//initialize key array
		char[] keyAss = new char[kiList.size()];
		Arrays.fill(keyAss, 'X');

		
		while (true){

			char[] keyAssNew = attackSingle(circuit, keyAss, -1);

			System.out.println("Key Assignment: " + new String(keyAssNew));
			//stop if no progress has been made
			if (Arrays.equals(keyAss, keyAssNew)) break;
			keyAss = keyAssNew;
			if (! new String(keyAss).contains("X")) break;
		}
		
		
		//time of the singleton attack
		Instant timeSingle = Instant.now(); //record the time after execution
		long spentTime = Duration.between(timeStart,timeSingle).toMillis();
		finalOutput.add(String.valueOf(spentTime/1000));


		//report number of broken key bits
		int match = 0;
		int notMatch = 0;
		char[] correctKeyArr = circuit.correctKey.toCharArray();
		for (int i = 0; i < correctKeyArr.length; ++i){
			if (keyAss[i] != 'X')
			if (keyAss[i] == correctKeyArr[i]) match++;
			else notMatch++;
		}
		finalOutput.add(String.valueOf(match));
		finalOutput.add(String.valueOf(notMatch));
			
		
		
		//time of the doubleton attack
		ArrayList<Integer> doubleResult = attackDouble(circuit, keyAss, -1);
		Instant timeDouble = Instant.now(); //record the time after execution
		spentTime = Duration.between(timeSingle, timeDouble).toMillis();
		finalOutput.add(String.valueOf(spentTime/1000));
		finalOutput.add(String.valueOf(doubleResult.get(0)));
		finalOutput.add(String.valueOf(doubleResult.get(1)));
		
		System.out.println("[FINAL] Result Attack:" + finalOutput.toString()
				.replace("[", "")
				.replace("]", "")
				.replace(" ", ""));
		return keyAss;
		}


		//circuit: circuit to attack
		//keyAss: intial key assignment (partially specified)
		//ignoreKeywordsArr: keywords to ignore
		//targetKeyIndex: only key to run the singleton attack on
		public static char[] attackSingle(Circuit circuit, char[] keyAss, int targetKeyIndex) throws Exception{

		circuit.printToV("temp.v");
		String [] ignoreKeywordsArr = findIgnoreKeywords(circuit);
		
		//handle each key input and set to both values
		char[] keyAssStart = keyAss.clone();
		char[] keyAssRet = keyAss.clone();
		
		
		//extract the faults in the original circuits
		System.out.println("[INFO] Finding redundant faults (single) ...");
		String dcOutput = Tool.execReal("dc_shell -f ./run_dc_redundant.tcl", false);

		//Error handling
		if (dcOutput.contains("Error")) return null;
		
		generateTmaxScript("run_tmax_redundant.tcl", "run_tmax_redundant_set.tcl",
				keyAssStart, null);
		String tmaxOutput = Tool.execReal("tmax -shell ./run_tmax_redundant_set.tcl", false);
		HashSet<String> origRedun = extractFaultTmax(tmaxOutput);
		removeIgnoreKeword(origRedun, ignoreKeywordsArr);
		
		System.out.println("Original Redundant: " + origRedun);
		
		int[] red = new int [2];

		ArrayList<Gate> kiList = circuit.getKiList();
		int numKey = kiList.size();

		//inclusive index range
		int startIndex = 0;
		int endIndex = numKey-1;
		
		if (targetKeyIndex != -1){
			startIndex = targetKeyIndex;
			endIndex = targetKeyIndex;
		}
		
		for (int kIndex = startIndex; kIndex <= endIndex; ++kIndex){

			System.out.println("[INFO] Examining K" + kIndex);
			//skip key bits that have already been resolved
			if (keyAss[kIndex] != 'X') continue;
			
			for (int value : new int[]{0,1}){

			char[] keyAssTry = keyAssStart.clone();
			keyAssTry[kIndex] = (char)(value + '0');
			generateTmaxScript("run_tmax_redundant.tcl", "run_tmax_redundant_set.tcl",
					keyAssTry, null);
				
			//find redundant faults under the key assignment
			tmaxOutput = Tool.execReal("tmax -shell ./run_tmax_redundant_set.tcl", false);
			HashSet<String> newRedun = extractFaultTmax(tmaxOutput);
			newRedun.removeAll(origRedun);
			removeIgnoreKeword(newRedun, ignoreKeywordsArr);
			System.out.println("New Redundant when K" + kIndex + " is set to " + value +
					": " + newRedun);
			
			red[value] = newRedun.size();
			}

			if (red[0] > red[1])
			keyAssRet[kIndex] = '1';
			else if (red[1] > red[0])
			keyAssRet[kIndex] = '0';

			System.out.println("Result: Determining K" + kIndex + " to be " + keyAssRet[kIndex]);
		}

		System.out.println("[INFO] Single Attack Done with result: \n" + new String(keyAssRet));
		System.out.println("Compared to correct key: \n" + circuit.correctKey);
		System.out.println("[IMPORT] Recovered Key After An Iteration: " + new String(keyAssRet));


		int match = 0;
		int notMatch = 0;
		char[] correctKeyArr = circuit.correctKey.toCharArray();
		for (int i = 0; i < correctKeyArr.length; ++i){
			if (keyAssRet[i] != 'X')
			if (keyAssRet[i] == correctKeyArr[i]) match++;
			else notMatch++;
		}
		
		System.out.format("[RES] Total/Correct/Incorrect: %d/%d/%d\n"
				,keyAssRet.length, match, notMatch);
		
		return keyAssRet;
		}

		public static void generateTmaxScript(String origScript, String newScript,
						char[] keyAss, String additional){

		String keyAssignmentLines = "";
		if (keyAss != null)
			for (int i=0; i < keyAss.length; ++i){
			if (keyAss[i] != 'X'){
				String keyInName = Circuit.kin + i;
				int value = keyAss[i] - '0';		
				keyAssignmentLines += String.format("add_pi_constraints %d %s\n", value, keyInName);
			}
			}

		if (additional != null) keyAssignmentLines += additional + "\n";
		Tool.replaceFileContent(origScript, newScript,
					"####REPLACE",
					keyAssignmentLines);
		}
		
		public static HashSet<String> extractFaultTmax(String tmaxOutput){
		
		String[] tmaxArr = tmaxOutput.split("\\r?\\n");
		HashSet<String> redList = new HashSet<String>();
		
		for (int i = 0; i < tmaxArr.length; ++i){
			
			String currLine = tmaxArr[i];

			//section that prints the set of untestable faults
			if (currLine.contains("-class UD")){

			String nextLine = tmaxArr[++i];

			//read until "report_faults -class UD"
			while (i < tmaxArr.length - 1){

				//reach the end of current section or end of file
				if (nextLine.contains("-class") || nextLine.contains("exit")
				|| nextLine.contains("Warning") || nextLine.contains("#")) break;

				//if (!Arrays.stream(ignoreKeywords).anyMatch(nextLine::contains)){
				String[] splited = nextLine.split("\\s+");

				//ignore equivalent faults ("--" in the middle field)
				if (!splited[2].equals("--"))
				redList.add(splited[1] + "_" + splited[3]);
				//System.out.println("REDUNDANT: " + splited[1] + "_" + splited[3]);
				//}
				nextLine = tmaxArr[++i];
			}
			break;
			}
		}

		return redList;
		}

		public static void removeIgnoreKeword(HashSet<String> set, String[] ignoreKeywords){
		Iterator<String> iterator = set.iterator();
		while (iterator.hasNext()) {
			String element = iterator.next();
			if (Arrays.stream(ignoreKeywords).anyMatch(element::contains)) {
			iterator.remove();
			}
		}
		}

		
		public static void reverseFSM(String inBenchFile) throws Exception{

		//parse the netlist containing FF
		Circuit circuit = new Circuit(inBenchFile);
		//Tool.saveObjBinary(circuit, outBinaryFile);

		//store the flattened netlist without FF
		String flattenCircuitName = Tool.extractFName(inBenchFile) + "_flatten.bench";
		circuit.printTo(flattenCircuitName);//, "SEQ");

		//modify the netlist to include the trick logic
		ArrayList<String> ffOutList = circuit.getFFOutNameList();

		System.out.println(ffOutList);
		File f = new File(flattenCircuitName);
		RandomAccessFile raf = new RandomAccessFile(f, "rw");

		//Seek to end of file
		long pointer = f.length();
		raf.seek(pointer);
		System.out.println("Pointer: "+pointer);

		
		//generate all binary strings of size KGs.size (000...000 - 111...111)
		//Length is the number of FFs
		int size = ffOutList.size();
		boolean[][] bools = Tool.enumerate(size);


		//generate faultList.txt
		Tool.execReal("echo \"MET /0\" > faultList.txt", false);
		
		//ATALANTA Diagnostics COMMAND
		String atalantaCommand = "atalanta -A -f faultList.txt " + flattenCircuitName;
		
		//MAIN ITERATION
		//Modify the netlist

		for (int i = 0; i < bools.length; ++i){

			System.out.println("I is: " + i);
			raf.seek(pointer); //move to the end of flattened netlist
			raf.setLength(pointer); //remove everything after that
			
			//prepare set key_value pairs
			boolean[] currBool = bools[i];
			StringBuilder miterLine = new StringBuilder();

			
			for (int j = 0; j < currBool.length; ++j){
			String outBit = ffOutList.get(j);

			//inverted FF output bit if the bool is false
			//Also prepare for the miter line
			if (!currBool[j]){
				raf.write(String.format("%s_INV = NOT(%s)\n", outBit, outBit).getBytes());
				miterLine.append(outBit + "_INV, ");
			}
			else{
				miterLine.append(outBit + ", ");
			}
			
			}

			//construct the final miter gate
			miterLine.setLength(miterLine.length()-2); // remove last extra comma
			raf.write("OUTPUT(MITER)\n".getBytes());
			raf.write(String.format("MITER = AND(%s)\n", miterLine.toString()).getBytes());

			//run ATALANTA
			Tool.execReal(atalantaCommand, false);

			//parse Atalanta test vector
			String testResultName = Tool.extractFName(inBenchFile) + "_flatten.test";
			
			BufferedReader br = new BufferedReader(new FileReader(testResultName));
			String line;

			//skip the header
			//MITER /1
			//1: xxxx10x xx1x0
			while (!br.readLine().contains("MITER /0")) {}
			
			while ((line = br.readLine()) != null) {
			String[] splitted = line.split("\\s+");
			String inputCond = splitted[2];
			String inFFCond = inputCond.substring(0, size);
			String inPICond = inputCond.substring(size, inputCond.length());
			String outputCond = splitted[3];
			String outFFCond = outputCond.substring(0, size);
			String outPICond = outputCond.substring(size, outputCond.length()-1);
			
			//input, curr_state, next_state, output
			System.out.format("%s %s %s %s\n", inPICond, inFFCond, outFFCond, outPICond);
			}
					
		}
		
		raf.close();
		}



	}

