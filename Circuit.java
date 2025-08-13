	import java.io.*;
	import java.nio.*;
	import java.util.*;
	import java.util.Map.*;
	import java.util.AbstractMap.*;
	import java.util.regex.*;
	import java.math.*;
	import java.io.Serializable;

	public class Circuit implements Serializable{

		public static String kin = "locking_key_";
		//public static String kin = "KIn";
		public String name;
		public HashMap<String, Gate> gates;
		public HashMap<String, String> ffMapping;
		public ArrayList<String> outFFList = new ArrayList<>();
		public String correctKey = "";
		
		//constructor with file
		public Circuit(String filename){

		this.name = filename;
		gates = new HashMap<String, Gate>();
		ffMapping = new HashMap<String, String>();
		
		//array to stroe info
		LinkedHashSet<String> inLines = new LinkedHashSet<>();
		LinkedHashSet<String> kinLines = new LinkedHashSet<>();
		LinkedHashSet<String> outLines = new LinkedHashSet<>();
		LinkedHashSet<String> dffLines = new LinkedHashSet<>();
		LinkedHashSet<String> gateLines = new LinkedHashSet<>();
					
		//read the file

		BufferedReader buf = null;
		try{
			buf = new BufferedReader(new FileReader(filename));

			
			//read file
			for(String line; (line = buf.readLine()) != null;){

			//ignore white space
			line = line.replaceAll("\\s+","");

			if(line.isEmpty() || line.charAt(0) == '#'){
				if (line.contains("key=")) correctKey = line.substring(line.indexOf("=")+1);
				//ignore comments or empty lines
			}
			
			else if (line.startsWith("INPUT(")){

				if (line.contains(kin)){
				kinLines.add(line);
				}
				else{
				inLines.add(line);
				}
			}

			//output line
			else if (line.startsWith("OUTPUT(")){
				outLines.add(line);
			}

			//DFF line
			else if (line.startsWith("DFF(")){
				dffLines.add(line);
			}

			//gate line
			else if (line.contains("=")){
				if (line.contains("= OUTPUT(")) {
					line = line.replace("= OUTPUT(", "= BUFF(");
				}
				gateLines.add(line);
			}
			}
			buf.close();
		}
		catch(IOException e){
			e.printStackTrace();
			System.out.println("Input File cannot be opened");
			return;
		}
		

		//start building gates
		//STEP1: create gates with name. (from inputs and gates)
		for (String line: gateLines){
			//name is element before = sign

			if (line.contains("g1002")) {
				System.out.println(line);
			}
			String gname = line.substring(0, line.indexOf("="));
			gates.put(gname, new Gate(gname));
		}

		for (String line: inLines){
			String gname = extract(line);
			gates.put(gname, new Gate(Gate.INPUT, gname, false, false));
		}

		for (String line: kinLines){
			String gname = extract(line);
			gates.put(gname, new Gate(Gate.INPUT, gname, false, true));
		}

		//handle dff as inputs
		for (String line: dffLines){
			String makeIn = line.substring(0, line.indexOf("="));
			String makeOut = extract(line);
			gates.put(makeIn, new Gate(Gate.INPUT, makeIn, false, false));
			gates.get(makeIn).property = Gate.PRO_FFIN;
			ffMapping.put(makeIn, makeOut); //store the mapping so that the print out is correct
		}
			
		
		//STEP2: complete gates with parameters (IOs)
		for (String line: gateLines){

			String gName = line.substring(0, line.indexOf("="));
			String type = line.substring(line.indexOf("=") + 1,
						line.indexOf("("));
			String inputsStr = extract(line);
			String[] inputs = inputsStr.split("\\s*,\\s*");

			Gate curr = gates.get(gName);
			curr.type = type.toUpperCase(); //update type

			//connect input/output relations
			for(String inname: inputsStr.split("\\s*,\\s*")){
				Gate ingate = gates.get(inname);
				curr.addInput(ingate);
				ingate.addOutput(curr);
			}

			/*
			if (type.equals("MUX")){
			addMUXDefined(gName, inputs[0], inputs[1], inputs[2]);		
			}
			else{
			Gate curr = gates.get(gName);
			curr.type = type.toUpperCase(); //update type

			//connect input/output relations
			for(String inname: inputsStr.split("\\s*,\\s*")){
			Gate ingate = gates.get(inname);
			curr.addInput(ingate);
			ingate.addOutput(curr);
			}
			}
			*/
		}

		//STEP3: Mark output flags
		for (String line: outLines){
			String gname = extract(line);
			Gate curr = gates.get(gname);
			curr.out = true;
		}

		
		//handle dff as outputs
		for (String line: dffLines){
			String makeOut = extract(line);
			Gate curr = gates.get(makeOut);
			System.out.println(curr);
			curr.out = true;
			curr.property = Gate.PRO_FFOUT;
		}
		
		//remove floating lines and clean the netlist
		removeBuffer();
		removeFloatingLines();
		//renameGates();
		//addCon();

		System.out.println("[INFO] Parsed Successfully: " + filename);
		}
		
		//analyze which gates have fan-in cone from only the key inputs
		public void analyzeReachability(){

		clearVisited();
		LinkedList<Gate> queue = new LinkedList<>();

		/*
		for (Gate kinGate : getKiList()){
			queue.add(kinGate);
		}
		queue.add(gates.get("CON"));
		queue.add(gates.get("CON0"));
		queue.add(gates.get("CON1"));
		queue.add(gates.get("CONINV"));
		*/

		for (Gate g : getKiList()){
			queue.add(g);
		}
		
		while (queue.size() != 0){
			Gate curr = queue.poll();
			boolean allInReachFromKinOnly = true;

			for (Gate inGate : curr.inputs){
			if (!inGate.reachFromKinOnly){
				allInReachFromKinOnly = false;
				break;
			}
			}

			if (allInReachFromKinOnly){
			curr.reachFromKinOnly = true;
			for (Gate outGate : curr.outputs){
				if (!queue.contains(outGate))
				queue.add(outGate);
			}
			}
		}	
		}

		public ArrayList<Gate> findFaninKinOnlyGates(){
		ArrayList<Gate> isFromKin = new ArrayList<Gate>();
		for (Gate g : getGateList()){
			if (g.reachFromKinOnly)
			isFromKin.add(g);
		}
		return isFromKin;
		}

		
		//find the tree from gate "source" to "direction" for many "level"
		public String getGatesEncoding(Gate source, boolean direction, int level){

		if (level == 0) return "";
		if (source.type.equals(Gate.INPUT)) return source.type;
		if (source.type.equals(Gate.OUTPUT)) return source.type;

		String ret = source.type + "(";
		
		ArrayList<Gate> nextLevelGates;
		
		//direction = false -> left, input direction
		if (direction == false){
			nextLevelGates = new ArrayList<Gate>(source.inputs);
		}
		else{
			nextLevelGates = new ArrayList<Gate>(source.outputs);
		}

		ArrayList<String> downstreamEncoding = new ArrayList<String>();
		for (Gate nextLevelGate : nextLevelGates) {

			String encoding = getGatesEncoding(nextLevelGate, direction, level-1);
			downstreamEncoding.add(encoding);
		}
		Collections.sort(downstreamEncoding);

		String commaseparatedlist = downstreamEncoding.toString();
	
		commaseparatedlist
			= commaseparatedlist.replace("[", "")
			.replace("]", "")
			.replace(" ", "");

		ret += commaseparatedlist;
		

		ret += ")";
		return ret;
		}

		public ArrayList<Gate> getLUTList(){
			
		ArrayList<Gate> ret = new ArrayList<>();
		for (Gate gate : gates.values()){
			if (gate.type.equals(Gate.LUT)){
			ret.add(gate);
			}
		}
		return ret;
		
		}


		
		//find structurally nearby gates
		public ArrayList<String> findNear(String gName, int level){

		clearVisited();
		ArrayList<String> ret = new ArrayList<>();
		LinkedList<Gate> queue = new LinkedList<>();
		Gate first = gates.get(gName);
		first.visited = true;
		first.depth = 0;
		queue.add(first);

		while (queue.size() != 0){
			Gate curr = queue.poll();
			//terminate until depth
			if (curr.depth > level) break;
			//System.out.println("Level " + curr.depth + ": " + curr.name);

			//terminate when finding 5 nearest keyinputs
			if (curr.name.contains(kin)){
			ret.add(curr.name);
			if (ret.size() >= 6) break;
			}
			
			//storess all input and output gates to the list
			HashSet<Gate> IO = new HashSet<>();
			IO.addAll(curr.inputs);
			IO.addAll(curr.outputs);
			
			Iterator<Gate> itr = IO.iterator();

			while(itr.hasNext()){

			Gate next = itr.next();

			if (!next.visited){
				next.visited = true;
				next.depth = curr.depth + 1;
				queue.add(next);
			}
			}
		}

		return ret;
		}

	
		//list of gates (including inputs and outputs)
		public ArrayList<Gate> getGateList(){
		ArrayList<Gate> ret = new ArrayList<>();
		for (Gate gate : gates.values()){
			ret.add(gate);
		}

		Collections.sort(ret);
		return ret;
		}

		public ArrayList<Gate> getInputList(){

		ArrayList<Gate> ret = new ArrayList<>();
		for (Gate gate : gates.values()){
			if (gate.type.equals(Gate.INPUT)){
			ret.add(gate);
			}
		}

		Collections.sort(ret);
		return ret;
		}
		
		//list of internal gates (not input or output)
		public ArrayList<Gate> getInternalGateList(){
		ArrayList<Gate> ret = new ArrayList<>();
		for (Gate gate : gates.values()){
			if (!gate.type.equals(Gate.INPUT) && !gate.out && !gate.name.contains("CON")){
			ret.add(gate);
			}
		}

		Collections.sort(ret);
		return ret;
		}

		//list of internal gates (not input or output)
		public ArrayList<Gate[]> getInternalGateBranchList(){
		ArrayList<Gate> gates = getInternalGateList();
		ArrayList<Gate[]> ret = new ArrayList<>();
		for (Gate gate: gates){
				
			//gates with multiple fanouts
			if (gate.outputs.size() > 1){
			for (Gate child : gate.outputs){
				Gate[] pair = new Gate[2];
				pair[0] = gate;
				pair[1] = child;
				ret.add(pair);
			}
			}
		}

		Collections.sort(ret, new Comparator<Gate[]>() {
			public int compare(Gate[] o1, Gate[] o2) {

				String s1 = o1[0].name + " " + o1[1].name;
				String s2 = o2[0].name + " " + o2[1].name;
				return s1.compareTo(s2);
			}
			});


		return ret;
		}
		
		public ArrayList<String> getFFOutNameList(){

		return outFFList;
		}
		
		//list of key inputs
		public ArrayList<Gate> getKiList(){
		return getKiList(0, 2048);
		}

		
		public ArrayList<Gate> getKiList(int start, int end){
			
		ArrayList<Gate> ret = new ArrayList<>();
		for (Gate gate : gates.values()){
			//System.out.println("Gate: " + gate.name + ": " + gate.kin);
			if (gate.kin){
			if (Tool.extractInt(gate.name) < start || Tool.extractInt(gate.name) >= end) continue;
			ret.add(gate);
			}
		}

		Collections.sort(ret, new Comparator<Gate>() {
			public int compare(Gate o1, Gate o2) {
				return extractInt(o1.name) - extractInt(o2.name);
			}

			int extractInt(String s) {
				String num = s.replaceAll("\\D", "");
				// return 0 if no digits found
				return num.isEmpty() ? 0 : Integer.parseInt(num);
			}
			});
		return ret;
		}

		public ArrayList<Gate> getOutList(){
		
		ArrayList<Gate> ret = new ArrayList<>();
		for (Gate gate : gates.values()){
			if (gate.out){
			ret.add(gate);
			}
		}
		return ret;	
		}

		//g1 becomes g1 XOR g2
		public void modifyXOROut(String g1Name, String g2Name){

		Gate g1 = gates.get(g1Name);
		Gate g2 = gates.get(g2Name);
		Gate g1New = new Gate(Gate.XOR, g1Name+"extra", true, false);
		gates.put(g1Name+"extra", g1New);
		g1.out = false;
		g1.outputs.add(g1New);
		g2.outputs.add(g1New);
		g1New.inputs.add(g1);
		g1New.inputs.add(g2);
		}
		
		//list of key inputs
		public ArrayList<Gate> decipherMUX(){

		ArrayList<Gate> kis = getKiList();


		for (Gate ki : kis){

			//for MUX key gate, find out which is inverting side
			Gate noninvG = null;
			Gate invG = null;
			for (Gate outg : ki.outputs){
			if (outg.type.equals(Gate.NOT)){
				//the only output of the inverter
				for (Gate trueOutg : outg.outputs){
				invG = trueOutg;
				}
			}
			else{
				noninvG = outg;
			}
			}

			//System.out.println("Gate " + ki.name + "|| INV: " + invG.name + "|| NONINV: " + noninvG.name);

			//find the alternative line to the invG and noninvG
			Gate noninvIN = null;
			Gate invIN = null;

			for (Gate g : invG.inputs){
			if (!g.name.equals(ki.name)) invIN = g;
			}
			for (Gate g : noninvG.inputs){
			if (!g.name.equals(ki.name)) noninvIN = g;
			}
			
			System.out.print("Gate " + ki.name + " >>>>> INV Branch " + (invIN.outputs.size() - 1));
			System.out.println(" >>>>> NONINV Branch " + (noninvIN.outputs.size() - 1)); 
			
			
		}

		return null;
		}

		
		//randomly add key gates
		public ArrayList<String> encRandomXOR(int seed, int numKey){

		ArrayList<String> ret = new ArrayList<>();

		
		System.out.printf("INFO: Randomly insert %d key gates with seed being %d\n", numKey, seed);
		Random rand = new Random(seed);
		ArrayList<Gate> candidates= getInternalGateList();

		for (int i = 0; i < numKey; i++){

			//randomly find a location to insert one key gate
			String prevName = candidates.get(rand.nextInt(candidates.size())).name;
			ret.add(prevName);
			addXOR(i, prevName);
		}

		System.out.println("INFO: Key gate insertion finished");
		//addCon();
		return ret;
		}

		//check if there is a forward path from gate1 to gate2
		public boolean searchPath(Gate g1, Gate g2){
		return searchPath(g1.name, g2.name);
		}
		
		//check if there is a forward path from gate1 to gate2
		public boolean searchPath(String g1, String g2){
		Queue<Gate> queue = new LinkedList<>();
		ArrayList<Gate> gatesList = getGateList();
			
		for (Gate g : gatesList){
			g.visited = false;
		}
		
		Gate gate1 = gates.get(g1);
		Gate gate2 = gates.get(g2);
		queue.add(gate1);
		gate1.visited = true;
			
		while (!queue.isEmpty()){
			Gate g = queue.remove();

			LinkedList<Gate> outputs = g.outputs;
			for (Gate out : outputs){
			//System.out.println("Examining: " + out.name);
			if (!out.visited){
				queue.add(out);
				out.visited = true;
				if (out == gate2){
				return true;
				}
			}				
			}

		}
		return false;
		}

		//find gates in the supporting input cone
		public HashSet<Gate> findPred(Gate g){

		HashSet<Gate> pred = new HashSet<Gate>();

		if (g.type.equals(Gate.INPUT)){
			return pred;
		}
		
		for (Gate ing : g.inputs){
			pred.add(ing);
			pred.addAll(findPred(ing));
		}

		return pred;
		}

		//find the fanout outputs from a gate
		public HashSet<Gate> findSucc(Gate g){

		HashSet<Gate> succ = new HashSet<Gate>();

		if (g.out){
			return succ;
		}
		
		for (Gate outg : g.outputs){
			succ.add(outg);
			succ.addAll(findSucc(outg));
		}

		return succ;
		}

		
		//check if there is a forward path from gate1 to gate2
		public boolean searchLoop(String g1){
		Queue<Gate> queue = new LinkedList<>();
		ArrayList<Gate> gatesList = getGateList();
			
		for (Gate g : gatesList){
			g.visited = false;
		}
		
		Gate gate1 = gates.get(g1);
		queue.add(gate1);
			
		while (!queue.isEmpty()){
			Gate g = queue.remove();
			LinkedList<Gate> outputs = g.outputs;
			//System.out.print("Gate " + g.name + " has outputs: ");
			for (Gate out : outputs){

			//System.out.print(" " + out.name);
			//System.out.println("Examining: " + out.name);
			if (!out.visited){
				queue.add(out);
				out.visited = true;
				if (out == gate1){
				return true;
				}
			}				
			}
			//System.out.println();

		}
		return false;
		}

		
		//randomly add mux key gates
		public ArrayList<String[]> encRandomMUX(int seed, int numKey, boolean withAnd){

		System.out.printf("Randomly insert %d MUX ky gates with seed being %d\n", numKey, seed);
		Random rand = new Random(seed);
		ArrayList<Gate> candidates = getInternalGateList();
		ArrayList<String[]> ret = new ArrayList<>();
			
		
		//ArrayList<String> hist1 = new ArrayList<>();
		//ArrayList<String> hist2 = new ArrayList<>();

		Collections.shuffle(candidates, new Random(seed));

		int success = 0;
		int counter = 0;

		//System.out.println(candidates);
		while (success != numKey){
			String prevName = candidates.get(counter++).name;
			String altName = candidates.get(counter++).name;

			System.out.println("ADDING MUX TO " + prevName +
					" with alternative as " + altName);

			//avoidfeedback loop
			if (searchPath(prevName, altName)){
			System.out.println("Loop between " + prevName + " and " + altName);
			continue;
			}

			String[] pair = new String[2];
			pair[0] = prevName;
			pair[1] = altName;
			ret.add(pair);
			
			addMUX(++success, prevName, altName, withAnd);

			/*
			//double check that there is no loop
			for (Gate gate : getGateList()){
			if (searchLoop(gate.name)){
			System.out.println("LOOP AT " + gate.name);
			}
			}
			*/
		}

		//addCon();
		return ret;
		}

		//Element: <prevName0, prevName1, altName>
		public ArrayList<ArrayList<String>> encRandomMUXMixed(int seed, int numKey, boolean withAnd){
		
		Random rand = new Random(seed);
		ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();

		//set up a list of candidates for true lines and false lines
		ArrayList<Gate[]> candidateBranch = getInternalGateBranchList();
		ArrayList<Gate> candidateStem = getInternalGateList();
		ArrayList<Gate> alts = getInternalGateList();

		Collections.shuffle(candidateBranch, new Random(seed));
		Collections.shuffle(candidateStem, new Random(seed+1));
		Collections.shuffle(alts, new Random(seed));

		Iterator itrBranch = candidateBranch.iterator();
		Iterator itrStem = candidateStem.iterator();
		Iterator itrAlt = alts.iterator();
		
		int counter = 0;
		Random randSelect = new Random(seed);
		HashSet<String> moded = new HashSet<>(); //set of gates already modified, to avoid duplicate insertion
		
		while (counter != numKey){

			int sizeBranch = candidateBranch.size();
			int sizeStem = candidateStem.size();
			int nextTrue = randSelect.nextInt(sizeBranch + sizeStem);

			//select candidateBranch
			if (nextTrue < sizeBranch){

			Gate[] branch = (Gate[])itrBranch.next();
			String prevName0 = branch[0].name;
			String prevName1 = branch[1].name;
			String altName = ((Gate)itrAlt.next()).name;
			
			System.out.println("ADDING MUX TO " + prevName0 + "->" +
					prevName1 + " with alternative as " +
					altName);
			
			//avoid identical true and false lines
			if(prevName0.equals(altName) || prevName1.equals(altName)){
				System.out.println("SKIP: Alternative equals original");
				continue;
			}

			//avoidfeedback loop
			if (searchPath(prevName1, altName)){
				System.out.println("SKIP: Loop between " + prevName1 +
						" and " + altName);
				continue;
			}

			//avoid duplicate MUX
			if (moded.contains(prevName0) || moded.contains(prevName1) || moded.contains(altName)){
				continue;
			}
			
			addMUXBranch(++counter, prevName0, prevName1, altName, withAnd);
			moded.add(prevName0);
			moded.add(prevName1);
			moded.add(altName);
			ret.add(new ArrayList<String>(Arrays.asList(prevName0, prevName1, altName)));
			}

			//select candidatestem
			else{

			String prevName = ((Gate)itrStem.next()).name;
			String altName = ((Gate)itrAlt.next()).name;

			System.out.println("ADDING MUX TO " + prevName +
					" with alternative as " + altName);
			
			
			//avoid identical true and false lines
			if(prevName.equals(altName)){
				System.out.println("SKIP: Alternative equals original");
				continue;
			}
					
			//avoidfeedback loop
			if (searchPath(prevName, altName)){
				System.out.println("SKIP: Loop between " + prevName + " and " + altName);
				continue;
			}

			//avoid duplicate MUX
			if (moded.contains(prevName) || moded.contains(altName)){
				continue;
			}

			addMUX(++counter, prevName, altName, withAnd);
			moded.add(prevName);
			moded.add(altName);
			ret.add(new ArrayList<String>(Arrays.asList(prevName, altName)));
			}
		}

		return ret;
		}
		
		
		//randomly add mux key gates at branches
		public ArrayList<String[]> encRandomMUXBranch(int seed, int numKey, boolean withAnd){
		
		System.out.printf("Randomly insert %d Branch MUX key gates with seed being %d\n", numKey, seed);
		Random rand = new Random(seed);
		ArrayList<Gate[]> candidates = getInternalGateBranchList();
		ArrayList<Gate> alts = getInternalGateList();
		Collections.shuffle(candidates, new Random(seed));
		Collections.shuffle(alts, new Random(seed));
		
		int success = 0;
		int counter = 0;
		int counterAlt = 0;
		
		
		ArrayList<String[]> ret = new ArrayList<>();
		
		while (success != numKey){

			String prevName0 = candidates.get(counter)[0].name;
			String prevName1 = candidates.get(counter++)[1].name;
			String altName = "";
			
			//prefer alternative to have more than 1 output
			while (true){
			if (alts.get(counterAlt++).outputs.size() > 1){
				altName = alts.get(counterAlt++).name;
				break;
			}
			}
			
			
			//String altName = alts.get(counterAlt++).name;

			System.out.println("ADDING MUX TO " + prevName0 + "->" +
					prevName1 + " with alternative as " +
					altName);

			
			//avoidfeedback loop
			if (searchPath(prevName1, altName)){
			System.out.println("Loop between " + prevName1 +
					" and " + altName);
			continue;
			}

			if(prevName0.equals(altName) || prevName1.equals(altName)){
			System.out.println("Alternative equals original");
			continue;
			}

			String[] pair = new String[3];
			pair[0] = prevName0;
			pair[1] = prevName1;
			pair[2] = altName;
			ret.add(pair);

			addMUXBranch(++success, prevName0, prevName1, altName, withAnd);
			
			
			/*
			//double check that there is no loop
			for (Gate gate : getGateList()){
			if (searchLoop(gate.name)){
			System.out.println("LOOP AT " + gate.name);
			}
			}
			*/
		}

		//addCon();
		return ret;
		}

		//randomly add mux key gates at branches
		public ArrayList<String[]> findValidMUXBranch(int maxRT){
		
		ArrayList<Gate[]> candidates = getInternalGateBranchList();
		ArrayList<Gate> alts = getInternalGateList();	
		ArrayList<String[]> ret = new ArrayList<>();

		Collections.shuffle(candidates, new Random(1));
		Collections.shuffle(alts, new Random(1));

		int index = 0;
		
		while (ret.size() < maxRT * maxRT){

			Gate[] branch = candidates.get(index % candidates.size());
			Gate alt = alts.get(index % alts.size());
			index++;
			
			String prevName0 = branch[0].name;
			String prevName1 = branch[1].name;
			String altName = alt.name;

			System.out.println("TRYING MUX TO " + prevName0 + "->" +
					prevName1 + " with alternative as " +
					altName);
			
			if(prevName0.equals(altName) || prevName1.equals(altName)){
			System.out.println("Alternative equals original");
			continue;
			}

			//avoidfeedback loop
			if (searchPath(prevName1, altName)){
			System.out.println("Loop between " + prevName1 +
					" and " + altName);
			continue;
			}

			String[] pair = new String[3];
			pair[0] = prevName0;
			pair[1] = prevName1;
			pair[2] = altName;
			ret.add(pair);
		}
		return ret;
		}


		
		public ArrayList<BigInteger> pair(ArrayList<BigInteger> arr1, ArrayList<BigInteger> arr2){

		ArrayList<BigInteger> ret = new ArrayList<>();

		//pair inputs and outputs codes
		for (BigInteger in : arr1){
			for (BigInteger out : arr2){
			BigInteger newb = in.add(out);
			ret.add(newb);
			}
		}
		return ret;
		}

		public ArrayList<BigInteger> encode(String line){

		//extract the gate from the line (gate or branch)
		if (!line.contains("->")){
			String gName = line.substring(0, line.indexOf(" "));
			System.out.println(convert(gName));
			return convert(gName);
		}
		else{
			String g1Name = line.substring(0, line.indexOf("-"));
			String g2Name = line.substring(line.indexOf(">")+1, line.indexOf(" "));

			ArrayList<BigInteger> ret = new ArrayList<>();

			System.out.println(pair(convert(g1Name), convert(g2Name)));
			return pair(convert(g1Name), convert(g2Name));
		}
		}


		public ArrayList<Gate> findMUXIn(Gate g4){

		if (!g4.name.contains("KG")){
			return new ArrayList<Gate>(Arrays.asList(g4));
		}
		
		Gate g2 = gates.get(g4.name.replace("-4","-2"));
		Gate g3 = gates.get(g4.name.replace("-4","-3"));

		ArrayList<Gate> ret = new ArrayList<>();
		
		for (Gate in : g2.inputs){
			if (!in.name.contains(kin)) ret.addAll(findMUXIn(in));
		}
		for (Gate in : g3.inputs){
			if (!in.name.contains(kin)) ret.addAll(findMUXIn(in));
		}
		
		return ret;
		}

		public Gate findMUXOut(Gate g23){

		//works only when MUX is inserted at a branch
		for (Gate g4 : g23.outputs){
			for (Gate next : g4.outputs){
			return next;
			}
		}
		return null;
		}

		//encode the inputs and outputs type of a gate
		public ArrayList<BigInteger> convert(String gName){

		
		Gate gate = gates.get(gName);
		ArrayList<BigInteger> encodeIn = new ArrayList<>();
		ArrayList<BigInteger> encodeOut = new ArrayList<>();

		encodeIn.add(new BigInteger(gate.type.getBytes()));
		encodeOut.add(new BigInteger(gate.type.getBytes()));
		
		for (Gate in : gate.inputs){

			if (in.name.contains("KG")){
			ArrayList<Gate> possible = findMUXIn(in); 

			ArrayList<BigInteger> encodeInNew = new ArrayList<>();

			//pair them up
			for (BigInteger encIn1 : encodeIn){
				for (Gate encIn2 : possible){
				
				BigInteger newint = new BigInteger(encIn2.type.getBytes());
				encodeInNew.add(encIn1.add(newint));
				}
			}
			encodeIn = encodeInNew;
			}
			else{
			//add the value to each possible encodeIn value
			BigInteger encode = new BigInteger(in.type.getBytes());;
			ArrayList<BigInteger> encodeInNew = new ArrayList<>();
			
			for (BigInteger encIn : encodeIn){
				encodeInNew.add(encIn.add(encode));

			}
			encodeIn = encodeInNew;
			}
		}
		
		for (Gate out : gate.outputs){
			//System.out.println("outType " + out.type);

			BigInteger encode = new BigInteger(out.type.getBytes());;
			if (out.name.contains("KG")) encode = new BigInteger(findMUXOut(out).type.getBytes());;

			ArrayList<BigInteger> encodeOutNew = new ArrayList<>();
			
			for (BigInteger encOut : encodeOut){
			encodeOutNew.add(encOut.add(encode));
			}
			encodeOut = encodeOutNew;

		}
		
		ArrayList<BigInteger> ret = new ArrayList<>();

		//pair inputs and outputs codes
		for (BigInteger in : encodeIn){
			for (BigInteger out : encodeOut){
			BigInteger newb = in.add(out);
			ret.add(newb);
			}
		}
		return ret;
		}


		public void setKGPIO(String kgName){

		Gate curr = gates.get(kgName);

		Gate newIn = new Gate(Gate.INPUT, kgName + "-PI", false, false);
		gates.put(kgName + "-PI", newIn);
		
		for (Gate og : curr.outputs){
			curr.clearOutput(og);
			og.clearInput(curr);
			newIn.addOutput(og);
			og.addInput(newIn);
		}
		

		Gate newBlock = new Gate(Gate.AND, kgName + "-BLOCK", true, false);
		gates.put(kgName + "-BLOCK", newBlock);
		newBlock.addInput(curr);
		newBlock.addInput(gates.get("CON0"));
		gates.get("CON0").addOutput(newBlock);
		curr.addOutput(newBlock);
		
		}
		
		//add key gates according to the arrayList
		public void determEnc(ArrayList<String> selected){

		for (int i = 0; i < selected.size(); i++){
			addXOR(i+1, selected.get(i));
		}		
		return;
		}

		//add key gates according to the arrayList
		public void determEnc(String name, int index){

		addXOR(index, name);
		return;
		}

		public void setKiAll(int value){

		for(Gate ki : getKiList()){
			setKi(ki.name, value);
		}	
		}
		
		public void addXORBranch(int index, String prevName, String postName){

		System.out.format("[INFO] Adding XOR KG%d to branch %s->%s\n", index, prevName, postName);
		String kgName = "KG" + index;
		String kiName = kin + index;
		Gate prev = gates.get(prevName);
		Gate post = gates.get(postName);
		Gate kg = new Gate(Gate.XOR, kgName, false, false);
		Gate ki = new Gate(Gate.INPUT, kiName, false, true);

		gates.put(kgName, kg);
		gates.put(kiName, ki);
			
		//make appropriate connections
		ki.addOutput(kg);
		prev.clearOutput(post);
		prev.addOutput(kg);
		post.clearInput(prev);
		post.addInput(kg);
		kg.addInput(ki);
		kg.addInput(prev);
		
		correctKey += "0";
		}
		
		//add a single key gate
		public void addXOR(int index, String prevName){

		System.out.format("[INFO] Adding XOR KG%d to stem %s\n", index, prevName);
		String kgName = "KG" + index;
		String kiName = kin + index;
		Gate prev = gates.get(prevName);
		Gate kg = new Gate(Gate.XOR, kgName, false, false);
		Gate ki = new Gate(Gate.INPUT, kiName, false, true);

		gates.put(kgName, kg);
		gates.put(kiName, ki);

			
		//make appropriate connections
		ki.addOutput(kg);
		kg.addInput(ki);
		kg.addInput(prev);
		LinkedList<Gate> nexts = prev.outputs;
		for (Gate next: nexts){
			next.clearInput(prev);
			next.addInput(kg);
		}
		prev.clearOutputs();
		prev.addOutput(kg);
		kg.addOutputs(nexts);
		correctKey += "0";
		}

		//G199gat$enc = mux(keyinput0, G154gat, G199gat)
		public boolean addMUXBranchNew(int index, String prevName, String branchName, String altName){

		String kgName = "KG" + index;
		String kiName = kin + index;


		//avoidfeedback loop
		if (searchPath(branchName, altName)){
			System.out.println("SKIP: Loop between " + prevName + " and " + altName);
			return false;
		}

		
		//existing gates
		Gate prev = gates.get(prevName); //first input to MUX
		Gate branch = gates.get(branchName); // branch
		Gate alt = gates.get(altName); //second input to MUX

		if (prev.type.equals(Gate.MUX) || branch.type.equals(Gate.MUX) || alt.type.equals(Gate.MUX)){
			System.out.println("SKIP: back-to-back MUX");
			return false;
		}
		
		//new gates
		Gate kg = new Gate(Gate.MUX, kgName, false, false);
		Gate ki = new Gate(Gate.INPUT, kiName, false, true);
		gates.put(kgName, kg);
		gates.put(kiName, ki);

		ki.addOutput(kg);
		kg.addInput(ki);
		kg.addInput(prev);
		kg.addInput(alt); //kg selects between prev and altg
		prev.clearOutput(branch);
		prev.addOutput(kg);
		branch.clearInput(prev);
		branch.addInput(kg);
		alt.addOutput(kg);

		correctKey += "0";
		
		return true;
		}

		
					
		public boolean addMUXBranch(int index, String prevName, String branchName, String altName, boolean withAnd){

		
		String kgName = "KG" + index;
		String kiName = kin + index;
		Gate prev = gates.get(prevName); //first input to MUX
		Gate branch = gates.get(branchName); // branch
		Gate alt = gates.get(altName); //second input to MUX
			
		if (!prev.outputs.contains(branch)){
			System.out.println("BRANCH DOES NOT EXIST: " + prevName + "->" + branchName);
			return false;
		}

		//avoidfeedback loop
		if (searchPath(branchName, altName)){
			System.out.println("SKIP: Loop between " + prevName + " and " + altName);
			return false;
		}

		
		//4 gate MUX design
		Gate kg1 = new Gate(Gate.NOT, kgName+"-1", false, false);
		Gate kg2 = new Gate(Gate.NAND, kgName+"-2", false, false);
		Gate kg3 = new Gate(Gate.NAND, kgName+"-3", false, false);
		Gate kg4 = new Gate(Gate.NAND, kgName+"-4", false, false);
		Gate ki = new Gate(Gate.INPUT, kiName, false, true);

		gates.put(kgName+"-1", kg1);
		gates.put(kgName+"-2", kg2);
		gates.put(kgName+"-3", kg3);
		gates.put(kgName+"-4", kg4);
		gates.put(kiName, ki);

		//make appropriate connections
		ki.addOutput(kg1);
		ki.addOutput(kg3);
		kg1.addOutput(kg2);
		kg2.addOutput(kg4);
		kg3.addOutput(kg4);

		kg1.addInput(ki);
		kg2.addInput(prev);
		kg2.addInput(kg1);
		kg3.addInput(ki);
		kg3.addInput(alt);
		kg4.addInput(kg2);
		kg4.addInput(kg3);
		kg4.addOutput(branch);
			
		prev.clearOutput(branch);
		prev.addOutput(kg2);
		branch.clearInput(prev);
		branch.addInput(kg4);
		alt.addOutput(kg3);

		
		//"AND prev and alt" is the second input to the MUX
		if (withAnd){
			Gate kg5 = new Gate(Gate.AND, kgName+"-5", false, false);
			gates.put(kgName+"-5", kg5);
			alt.clearOutput(kg3);
			alt.addOutput(kg5);
			kg5.addInput(alt);
			kg5.addInput(prev);
			kg3.clearInput(alt);
			kg3.addInput(kg5);
			prev.addOutput(kg5);
			kg5.addOutput(kg3);

		}

		return true;
		}

		public void addMUXDefined(String gName, String line0Name, String line1Name, String selName){

		Gate line0 = gates.get(line0Name); //first input to MUX
		Gate line1 = gates.get(line1Name); //second input to MUX
		Gate sel = gates.get(selName); //select input
		
		Gate mux1 = new Gate(Gate.NOT, gName+"-1", false, false);
		Gate mux2 = new Gate(Gate.NAND, gName+"-2", false, false);
		Gate mux3 = new Gate(Gate.NAND, gName+"-3", false, false);
		Gate mux4 = new Gate(Gate.NAND, gName, false, false);
		
		gates.put(gName+"-1", mux1);
		gates.put(gName+"-2", mux2);
		gates.put(gName+"-3", mux3);
		gates.put(gName, mux4);

		
		//make appropriate connections
		sel.addOutput(mux1);
		sel.addOutput(mux3);
			mux1.addOutput(mux2);
		mux2.addOutput(mux4);
		mux3.addOutput(mux4);

		
		mux1.addInput(sel);
		mux2.addInput(line0);
		mux2.addInput(mux1);
		mux3.addInput(line1);
		mux3.addInput(sel);
		mux4.addInput(mux2);
		mux4.addInput(mux3);
		

		}
		
		public void addMUX(int index, String prevName, String altName, boolean withAnd){
				
		String kgName = "KG" + index;
		String kiName = kin + index;
		Gate prev = gates.get(prevName); //first input to MUX
		Gate alt = gates.get(altName); //second input to MUX

		//4 gate MUX design
		Gate kg1 = new Gate(Gate.NOT, kgName+"-1", false, false);
		Gate kg2 = new Gate(Gate.NAND, kgName+"-2", false, false);
		Gate kg3 = new Gate(Gate.NAND, kgName+"-3", false, false);
		Gate kg4 = new Gate(Gate.NAND, kgName+"-4", false, false);
		Gate ki = new Gate(Gate.INPUT, kiName, false, true);

		gates.put(kgName+"-1", kg1);
		gates.put(kgName+"-2", kg2);
		gates.put(kgName+"-3", kg3);
		gates.put(kgName+"-4", kg4);
		gates.put(kiName, ki);

			
			
		//make appropriate connections
		ki.addOutput(kg1);
		ki.addOutput(kg3);
		kg1.addOutput(kg2);
		kg2.addOutput(kg4);
		kg3.addOutput(kg4);

		kg1.addInput(ki);
		kg2.addInput(prev);
		kg2.addInput(kg1);
		kg3.addInput(ki);
		kg3.addInput(alt);
		kg4.addInput(kg2);
		kg4.addInput(kg3);
		
		
		LinkedList<Gate> nexts = new LinkedList<Gate>();
		for (Gate next: prev.outputs){
			nexts.add(next);
			next.clearInput(prev);
			next.addInput(kg4);
		}

		prev.clearOutputs();
		prev.addOutput(kg2);
		alt.addOutput(kg3);
		kg4.addOutputs(nexts);

			
		//"AND prev and alt" is the second input to the MUX
		if (withAnd){
			Gate kg5 = new Gate(Gate.AND, kgName+"-5", false, false);
			gates.put(kgName+"-5", kg5);
			alt.clearOutput(kg3);
			alt.addOutput(kg5);
			kg5.addInput(alt);
			kg5.addInput(prev);
			kg3.clearInput(alt);
			kg3.addInput(kg5);
			prev.addOutput(kg5);
			kg5.addOutput(kg3);
		}
		}

		//add constant 0 and 1 to the circuit
		public void addCon(){
		
		if (gates.get("CON") != null){
			//System.out.println("Constants already exist");
			return;
		}

		Gate CON = new Gate(Gate.INPUT, "CON", false, false);
		Gate CONINV = new Gate(Gate.NOT, "CONINV", false, false);
		Gate CON0 = new Gate(Gate.AND, "CON0", true, false);
		Gate CON1 = new Gate(Gate.OR, "CON1", true, false);

		gates.put("CON", CON);
		gates.put("CONINV", CONINV);
		gates.put("CON0", CON0);
		gates.put("CON1", CON1);
			
		CON.addOutput(CONINV);
		CONINV.addOutput(CON0);
		CONINV.addOutput(CON1);
		CON.addOutput(CON0);
		CON.addOutput(CON1);
					
		CONINV.addInput(CON);
		CON0.addInput(CON);
		CON0.addInput(CONINV);
		CON1.addInput(CON);
		CON1.addInput(CONINV);
		}

		public void setKi(String kiName, int value){

		/*
		if (gates.get(kiName).outputs.iterator().next().type.equals(Gate.XOR)){
		//System.out.println("Set XOR");
		setKiXOR(kiName, value);
		}
		else{
		//System.out.println("Set MUX");
		addCon();
		setKiGeneral(kiName, value);
		}
		*/
		addCon();
		setKiGeneral(kiName, value);

		
		}

		//add constants and connect the key inptus to the constants.
		public void setKiGeneral(String kiName, int value){
		Gate ki = gates.get(kiName);	
		if (ki == null){
			
			System.out.println("Setting " + kiName + " to be " + value + " FAILS");
			return;
		}

		//remove the KIn gate
		gates.remove(kiName);

		//check the key gate type 
		for (Gate gate: ki.outputs){

			if (value == 0){
			gate.clearInput(ki);
			Gate CON0 = gates.get("CON0");
			gate.addInput(CON0);
			CON0.addOutput(gate);
			}

			//convert to buffer
			else{
			gate.clearInput(ki);
			Gate CON1 = gates.get("CON1");
			gate.addInput(CON1);
			CON1.addOutput(gate);
			}
		}


		}

		public void setKiXOR(String kiName, int value){

		Gate ki = gates.get(kiName);
		if (ki == null){
			System.out.println("Setting " + kiName + " to be " + value + " FAILS");
			return;
		}

		//remove the KIn gate
		gates.remove(kiName);

		//check the key gate type 
		for (Gate gate: ki.outputs){

			// 1 means invert, 0 means remove
			int decision = value;
			if (gate.type.equals(Gate.XNOR)){
			decision = (decision + 1) % 2; //if XNOR, opposite decision
			}

			//convert to NOT
			if (decision == 1){
			gate.clearInput(ki);
			gate.type = Gate.NOT;
			gate.name = "NOT" + gate.name.substring(gate.name.indexOf("G") + 1);
			}

			//convert to buffer
			else{
			gate.clearInput(ki);
			gate.type = Gate.BUFFER;
			gate.name = "BUFF" + gate.name.substring(gate.name.indexOf("G") + 1);
			}
		}
		}

		public void clearVisited(){
		for (Gate g : getGateList()){
			g.visited = false;
			g.depth = 2000;
			g.reachFromKinOnly = false;
		}

		}

		public void printO(){
		for (Gate o : getGateList()){
			if (o.out){
			ArrayList<String> et = findSupport(o.name);
			System.out.println("Out " + o.name + ": " + et.size() + ": " + et);
			}
		}
		}

		public void printFan(){

		int max = 0;
		for (Gate o : getGateList()){
			System.out.println("Fanouts: " + o.outputs.size() + " " + o.name);
			max = Math.max(max, o.outputs.size());
		}
		System.out.println(max);
		}
		
		
		public ArrayList<String> findSupport(String gName){

		clearVisited();
		ArrayList<String> ret = new ArrayList<>();
		LinkedList<Gate> queue = new LinkedList<>();

		Gate first = gates.get(gName);
		first.visited = true;
		first.depth = 0;
		queue.add(first);

		while (queue.size() != 0){
			Gate curr = queue.poll();
			//	    System.out.println("Handling " + curr.name);
		
			//System.out.println("Level " + curr.depth + ": " + curr.type);
			
			if (curr.type.equals(Gate.INPUT)){
			ret.add(curr.name);	
			}
					
			//store all input and output gates to the list
			HashSet<Gate> IO = new HashSet<>();
			IO.addAll(curr.inputs);
				
			Iterator<Gate> itr = IO.iterator();

			while(itr.hasNext()){
			Gate next = itr.next();
			if (!next.visited){
				next.visited = true;
				next.depth = Math.min(next.depth, curr.depth + 1);
				queue.add(next);
			}
			}
		}

		return ret;
		}

		public ArrayList<String> findOUT(String gName){

		clearVisited();
		ArrayList<String> ret = new ArrayList<>();
		LinkedList<Gate> queue = new LinkedList<>();

		Gate first = gates.get(gName);
		first.visited = true;
		first.depth = 0;
		queue.add(first);

		while (queue.size() != 0){
			Gate curr = queue.poll();

			//System.out.println("Level " + curr.depth + ": " + curr.type);
			
			if (curr.out){
			ret.add(curr.name);	
			}
					
			//store all input and output gates to the list
			HashSet<Gate> IO = new HashSet<>();
			IO.addAll(curr.outputs);
				
			Iterator<Gate> itr = IO.iterator();

			while(itr.hasNext()){
			Gate next = itr.next();
			if (!next.visited){
				next.visited = true;
				next.depth = Math.min(next.depth, curr.depth + 1);
				queue.add(next);
			}
			}
		}

		return ret;
		}


		public void removeBuffer(){

		//remove all buffers
		for (Gate gate: getGateList()){
			if (gate.type.equals(Gate.BUFFER)){

			//extract the input of the buffer
			Gate inGate = gate.inputs.iterator().next();

			//reconnect output gates
			for (Gate outGate : gate.outputs){
				outGate.inputs.remove(gate);
				outGate.inputs.add(inGate);
				inGate.outputs.add(outGate);			     
			}
			inGate.outputs.remove(gate);

			
			//if output gate is a PO pin
			if (gate.out) {
				inGate.out = true;
			}
			gates.remove(gate.name);
			}
		}
		}
		
		
		public void removeFloatingLines(){

		clearVisited();
		
		//remove direct connection between input and output
		for (Gate gate: getGateList()){
			if (gate.type.equals(Gate.INPUT) && gate.outputs.size() == 0){
			gates.remove(gate.name);
			}
		}

		
		//Find all output gates
		ArrayList<Gate> outList = new ArrayList<>();
		for (Gate gate: getGateList()){
			if (gate.out){
			outList.add(gate);
			}
		}

		//Search backwards
		LinkedList<Gate> queue = new LinkedList<>();
		
		for (Gate outGate : outList){
			queue.add(outGate);
			outGate.visited = true;
		}

		while (queue.size() != 0){
			Gate curr = queue.poll();

			//store all input and output gates to the list
			HashSet<Gate> IO = new HashSet<>();
			IO.addAll(curr.inputs);
				
			Iterator<Gate> itr = IO.iterator();

			while(itr.hasNext()){
			Gate next = itr.next();
			if (!next.visited){
				next.visited = true;
				next.depth = Math.min(next.depth, curr.depth + 1);
				queue.add(next);
			}
			}
		}

		//remove those gates that cannot be reached from an output
		for (Gate gate: getGateList()){
			if (!gate.visited){
			//System.out.println("[INFO] Gate " + gate.name
			//+ " is unreachable and thus removed");

			//remove the floating gate
			for (Gate inGate : gate.inputs){
				if (inGate != null)
				inGate.outputs.remove(gate);
			}

			for (Gate outGate : gate.outputs){
				if (outGate != null)
				outGate.inputs.remove(gate);
			}
			
			gates.remove(gate.name);
			}
		}	

		}

		public void renameGates(){
		
		HashMap<String, Gate> newGates = new HashMap<String, Gate>();

		int renameIndex = 0;
		
		for (Gate gate : getGateList()){

			if (gate.name.contains(kin)) {
			String oldName = gate.name;
			String newName = oldName + "_";
			gate.name = newName;
			newGates.put(newName, gate);	   
			}
			else{
			String oldName = gate.name;
			String newName = "G" + (++renameIndex) + "_";
			gate.name = newName;
			newGates.put(newName, gate);	    
			}
		}

		gates = newGates;
		}
		
		//print the circuit to file
		public void printTo(String file){
		
		ArrayList<Gate> inList = new ArrayList<>();
		ArrayList<Gate> ffinList = new ArrayList<>();	
		ArrayList<Gate> kinList = new ArrayList<>();
		ArrayList<Gate> outList = new ArrayList<>();
		ArrayList<Gate> ffoutList = new ArrayList<>();
		ArrayList<Gate> gateList = new ArrayList<>();

		//Temp strings to make sure FF IOs are printed in order
		StringBuilder ffinStr = new StringBuilder();
		StringBuilder ffoutStr = new StringBuilder();
		
		for (Gate gate: getGateList()){

			if (gate.type.equals(Gate.INPUT)){
			if (gate.name.contains(kin)){
				kinList.add(gate);
			}
			else{
				if (gate.property.equals(Gate.PRO_FFIN))
				ffinList.add(gate);
				else
				inList.add(gate);
			}
			}
			else{
			gateList.add(gate);
			if (gate.out){
				if (gate.property.equals(Gate.PRO_FFOUT))
				ffoutList.add(gate);
				else
				outList.add(gate);
			}
			}
		}

		//print info	
		PrintWriter writer;

		try{
			writer = new PrintWriter(file);
		}
		catch (IOException e){
			System.out.println("Output File cannot be written: " + file);
			return;
		}
		writer.println("# key=" + correctKey);
		writer.println("# " + name);
		writer.println("# " + (inList.size()+ffinList.size()) + " none key inputs");
		writer.println("# " + kinList.size() + " key inputs");
		writer.println("# " + (outList.size()+ffoutList.size()) + " outputs");
		writer.println("# " + gateList.size() + " gates");
		writer.println();

		Collections.sort(gateList);
		Collections.sort(inList);
		Collections.sort(ffinList);
		Collections.sort(kinList);
		Collections.sort(outList);
		Collections.sort(ffoutList);

		
		//write FF inputs & prepare corresponding FF outputs
		if (ffinList.size() != 0){
			writer.println("#################### FF Psuedo Inputs");
			for (Gate gate: ffinList){
			String outCorr = ffMapping.get(gate.name);
			Gate outGate= gates.get(outCorr); //Corresponding ffout gate
			ffinStr.append("INPUT(" + gate.name + ")\n");
			ffoutStr.append("OUTPUT(" + outGate.name + ")\n");
			outFFList.add(outGate.name);
			}
		}
			
		writer.println(ffinStr.toString());

		//write inputs
		writer.println("#################### Inputs");
		for (Gate gate: inList)
			writer.println("INPUT(" + gate.name + ")");
		writer.println();
		
		//write key inputs
		if(kinList.size() != 0){
			writer.println("#################### Key Inputs");
			for (Gate gate: kinList)
			writer.println("INPUT(" + gate.name + ")");
			writer.println();
		}

		//write FF outputs
		if (ffoutList.size() != 0){
			writer.println("#################### FF Pseudo Outputs");
			writer.println(ffoutStr.toString());
		}
		
		//write outputs
		writer.println("#################### Outputs");
		for (Gate gate: outList)
			writer.println("OUTPUT(" + gate.name + ")");
		
		writer.println();

		//write gates
		for (Gate gate: gateList){
			String name = gate.name;
			String type = gate.type;
			ArrayList<Gate> inputs = new ArrayList<>(gate.inputs);
				
			writer.write(name + " = " + type + "(");

			//write the first input gate
			writer.write(inputs.get(0).name);

			for (int i = 1; i < inputs.size(); ++i){
			writer.write(", " + inputs.get(i).name);
			}
			writer.write(")\n");
		}
		writer.close();
		}	

		//print in the format of Verilog
		public void printToV(String file){

		ArrayList<String> inList = new ArrayList<>();
		ArrayList<String> kinList = new ArrayList<>();
		ArrayList<String> outList = new ArrayList<>();
		ArrayList<Gate> gateList = new ArrayList<>();
		ArrayList<String> wireList = new ArrayList<>();
		boolean hasMUX = false;
		
		for (Gate gate: getGateList()){

			//check if there is any MUX gates -> add module definition at the end
			if (gate.type.equals(Gate.MUX)){
			hasMUX = true;
			}

			
			//store inputs
			if (gate.type.equals(Gate.INPUT)){
			if (gate.name.contains(kin)){
				kinList.add(gate.name);
			}
			else{
				inList.add(gate.name);
			}
			}

			//store gates and whether they are internal wires and outputs
			else{
			gateList.add(gate);
			if (gate.out){
				outList.add(gate.name);
			}
			else{
				wireList.add(gate.name);
			}
			}
		}

		//sort all collections
		Collections.sort(gateList);
		Collections.sort(inList);
		Collections.sort(outList);
		Collections.sort(wireList);
		//sort key inputs by numbers
		Collections.sort(kinList, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return Integer.valueOf(Tool.extractInt(o1)).compareTo(Integer.valueOf(Tool.extractInt(o2)));
			}
			});
		

		//print info	
		PrintWriter writer;

		try{
			writer = new PrintWriter(file);
		}
		catch (IOException e){
			System.out.println("Output File cannot be written");
			return;
		}

		//prepare strings
		String keyinputs = String.join(",", kinList);
		String inputs = String.join(",", inList);
		String outputs = String.join(",", outList);
		String wires = String.join(",", wireList);
		
		
		String foreName = name.substring(0, name.indexOf("."));
		writer.println("// Standard Header");
		writer.println("// " + name);
		writer.println("// " + kinList.size() + " key inputs");
		writer.println("// " + inList.size() + " total inputs");
		writer.println("// " + outList.size() + " outputs");
		writer.println("// " + gateList.size() + " gates");
		writer.println("\n// ATPG Attack Header");
		writer.println("// NAME: dut");
		if(kinList.size() != 0)
			writer.println("// KEYINPUT: " + keyinputs);
		writer.println("// INPUT: " + inputs);
		writer.println("// OUTPUT: " + outputs);
		writer.println("// CORRECT KEY: " + correctKey);
		writer.println();
		
		
		//writer.println("module " + foreName + " (" + inputs + "," + outputs + ");\n");

		//skip the key inputs if there is none
		if(kinList.size() != 0){
			writer.format("module dut (%s,\n%s,\n%s);\n\n", inputs, keyinputs, outputs);
			writer.println("input " + keyinputs + ";\n");
		}
		else{
			writer.format("module dut (%s,\n%s);\n\n", inputs, outputs);
		}
		writer.println("input " + inputs + ";\n");
		writer.println("output " + outputs + ";\n");
		writer.println("wire " + wires + ";\n");

		int index = 0;

		for (Gate g : gateList){

			ArrayList<String> ginList = new ArrayList<>();
			for (Gate ginn: g.inputs){
			ginList.add(ginn.name);
			}
			String gin = String.join(", ", ginList);
			String type = g.type.toLowerCase();
			if (type.equals("buff")) type = "buf";
			writer.println(type + " GATE" + ++index + " (" + g.name + ", " + gin + ");");
		}

		writer.println("\nendmodule");


		//print MUX
		if (hasMUX){
			String s = String.join("\n",
					"\n\nmodule mux(Y, S, D0, D1);\n",
					"output Y;",
					"input S, D0, D1;",
					"wire T1, T2, Sbar;",
					"and (T1, D1, S), (T2, D0, Sbar);",
					"not (Sbar, S);",
					"or (Y, T1, T2);\n",
					"endmodule"
					);
			writer.println(s);
		}
		
		writer.close();
		}

		
		//extract everything in parentheses
		public String extract(String str){
			
		Pattern prnPattern = Pattern.compile("\\((.*?)\\)");
		Matcher matcher = prnPattern.matcher(str);
		if(matcher.find()){
			String out = matcher.group(1);
			return out;
		}
		return "";
		}	
	}
