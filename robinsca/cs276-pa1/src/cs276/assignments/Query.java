package cs276.assignments;

import java.util.Collections;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import cs276.util.Freq;

public class Query {

	// Term id -> position in index file
	private static Map<Integer, Long> posDict = new HashMap<Integer, Long>();
	// Term id -> document frequency
	private static Map<Integer, Integer> freqDict = new HashMap<Integer, Integer>();
	// Doc id -> doc name dictionary
	private static Map<Integer, String> docDict = new HashMap<Integer, String>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict = new HashMap<String, Integer>();
	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list with a given termID from the file 
	 * You should seek to the file position of this specific
	 * posting list and read it back.
	 * */
	private static PostingList readPosting(FileChannel fc, int termId)
			throws IOException {
		long pos = posDict.get(termId);
		PostingList pl = index.readPosting(fc.position(pos));
		//System.out.println(pl);
		return pl;
	}

	private static void answer(String query, FileChannel fc) throws IOException {
		String[] tokens = query.trim().split("\\s+");
		ArrayList<Freq> ar = new ArrayList<Freq>(tokens.length);
		for(int i = 0; i < tokens.length; i++){
			if(!termDict.containsKey(tokens[i])){
				//Posting list of 0, automatically no returned docs
				System.out.println("no results found");
				return;
			}
			int termID = termDict.get(tokens[i]);
			int freq = freqDict.get(termID);
			ar.add(new Freq(new Integer(termID),new Integer(freq)));
		}
		Collections.sort(ar);
		//System.out.println(ar);
		processQuery(ar,fc);
	}

	private static void processQuery(ArrayList<Freq> ar, FileChannel fc) throws IOException {
		List<Integer> docList = readPosting(fc,(ar.get(0)).getTermId()).getList();
		for(int i = 1; i < ar.size(); i++){
			List<Integer> nextList = readPosting(fc,(ar.get(i)).getTermId()).getList();
			docList = intersectList(docList, nextList);
			if(docList.size() == 0) break;
		}
		printDocList(docList);
	}

	private static List<Integer> intersectList(List<Integer> shorterList,
		List<Integer> longerList){
		ArrayList<Integer> finalList = new ArrayList<Integer>(shorterList.size());
		int leftPtr = 0;
		int rightPtr = 0;
		while(true){
			if(leftPtr >= shorterList.size() || rightPtr >= longerList.size()){
				return finalList;
			}
			if(shorterList.get(leftPtr).equals(longerList.get(rightPtr))){
				finalList.add(new Integer(shorterList.get(leftPtr)));
				leftPtr = leftPtr + 1;
				rightPtr = rightPtr + 1;
			}else{
				if(shorterList.get(leftPtr).intValue() < longerList.get(rightPtr).intValue()){
					leftPtr = leftPtr + 1;
				}else{
					rightPtr = rightPtr + 1;
				}
			}
		}
	}

	private static void printDocList(List<Integer> docList){
		if(docList.size() == 0){
			System.out.println("no results found");
			return;
		}
		ArrayList<String> list = new ArrayList<String>(docList.size());
		for(int i = 0; i < docList.size(); i++){
			list.add(docDict.get(docList.get(i)));
		}
		//System.out.println(list);
		Collections.sort(list);
		for(int i = 0; i < list.size(); i++){
			System.out.println(list.get(i));
		}
	}

	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 2) {
			System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
			return;
		}

		/* Get index */
		String className = "cs276.assignments." + args[0] + "Index";
		try {
			Class<?> indexClass = Class.forName(className);
			index = (BaseIndex) indexClass.newInstance();
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get index directory */
		String input = args[1];
		File inputdir = new File(input);
		if (!inputdir.exists() || !inputdir.isDirectory()) {
			System.err.println("Invalid index directory: " + input);
			return;
		}

		/* Index file */
		RandomAccessFile indexFile = new RandomAccessFile(new File(input,
				"corpus.index"), "r");

		String line = null;
		/* Term dictionary */
		BufferedReader termReader = new BufferedReader(new FileReader(new File(
				input, "term.dict")));
		while ((line = termReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			termDict.put(tokens[0], Integer.parseInt(tokens[1]));
		}
		termReader.close();

		/* Doc dictionary */
		BufferedReader docReader = new BufferedReader(new FileReader(new File(
				input, "doc.dict")));
		while ((line = docReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
		}
		docReader.close();

		/* Posting dictionary */
		BufferedReader postReader = new BufferedReader(new FileReader(new File(
				input, "posting.dict")));
		while ((line = postReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
			freqDict.put(Integer.parseInt(tokens[0]),
					Integer.parseInt(tokens[2]));
		}
		postReader.close();

		/* Processing queries */
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		/* For each query */
		while ((line = br.readLine()) != null) {
			answer(line,indexFile.getChannel());
		}
		br.close();
		indexFile.close();
	}
}
