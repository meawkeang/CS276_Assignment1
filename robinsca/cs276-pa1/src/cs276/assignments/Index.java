package cs276.assignments;

import cs276.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Index {

	// Term id -> (position in index file, doc frequency) dictionary
	private static Map<Integer, Pair<Long, Integer>> postingDict 
		= new TreeMap<Integer, Pair<Long, Integer>>();
	// Doc name -> doc id dictionary
	private static Map<String, Integer> docDict
		= new TreeMap<String, Integer>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict
		= new TreeMap<String, Integer>();
	// Block queue
	private static LinkedList<File> blockQueue
		= new LinkedList<File>();

	// Total file counter
	private static int totalFileCount = 0;
	// Document counter
	private static int docIdCounter = 0;
	// Term counter
	private static int wordIdCounter = 0;
	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list to the file 
	 * You should record the file position of this posting list
	 * so that you can read it back during retrieval
	 * 
	 * */
	private static void writePosting(FileChannel fc, PostingList posting)
			throws IOException {
		/*
		 * Your code here
		 */
	}

	private static void merge(FileChannel fc1,FileChannel fc2,
		FileChannel comb,BaseIndex index){

		PostingList left = index.readPosting(fc1);
		PostingList right = index.readPosting(fc2);
		if(left.getTermId() == right.getTermId()){
			PostingList combined = PostingList.combineLists(left,right);
		}
		

		System.out.println(left);
		System.out.println(right);
	}

	private static void mapper(ArrayList<Pair<Integer,Integer>> pairs, String term, int docID){
		if(!termDict.containsKey(term)){
			wordIdCounter = wordIdCounter + 1;
			termDict.put(term,new Integer(wordIdCounter));
		}
		Pair<Integer,Integer> pair = new Pair<Integer,Integer>(termDict.get(term),new Integer(docID));
		pairs.add(pair);
	}

	private static void reducer(ArrayList<Pair<Integer,Integer>> pairs,
		FileChannel fc, BaseIndex index){
		//System.out.println("Unsorted all pairs");
		//System.out.println(pairs);
		Collections.sort(pairs);
		//System.out.println("Sorted all pairs");
		//System.out.println(pairs);
		if(pairs.size() == 0) return;
		//start and i are inclusive pointers
		int start = 0;
		for(int i = 0; i < pairs.size(); i++){
			if(i+1 == pairs.size()){
				//We're finished with the list
				createPosting(pairs,start,i,fc,index);
				return;
			}
			Pair<Integer,Integer> current = pairs.get(i);
			Pair<Integer,Integer> next = pairs.get(i+1);
			if(!((Integer)current.getFirst()).equals((Integer)next.getFirst())){
				//We dont have a match
				createPosting(pairs,start,i,fc,index);
				start = i+1;
			}
		}
	}

	private static void createPosting(ArrayList<Pair<Integer,Integer>> pairs,
		int start,int end,FileChannel fc,BaseIndex index){
		//System.out.println("Posting");
		//System.out.println(pairs.subList(start,end+1));
		List<Pair<Integer,Integer>> pairPostings = pairs.subList(start,end+1);
		ArrayList<Integer> intPostings = new ArrayList<Integer>(pairPostings.size());
		//docIDs are all non-negative
		Integer prevDoc = new Integer(-1);
		for(int i = 0; i < pairPostings.size(); i++){
			Integer doc = (Integer)(pairPostings.get(i)).getSecond();
			if(!doc.equals(prevDoc)){
				intPostings.add(doc);
			}
			prevDoc = doc;
		}
		int termID = (Integer)(pairPostings.get(0)).getFirst();
		PostingList pl = new PostingList(termID,intPostings);
		//System.out.println(pl);
		index.writePosting(fc,pl);
	}

	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 3) {
			System.err
					.println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
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

		/* Get root directory */
		String root = args[1];
		File rootdir = new File(root);
		if (!rootdir.exists() || !rootdir.isDirectory()) {
			System.err.println("Invalid data directory: " + root);
			return;
		}

		/* Get output directory */
		String output = args[2];
		File outdir = new File(output);
		if (outdir.exists() && !outdir.isDirectory()) {
			System.err.println("Invalid output directory: " + output);
			return;
		}

		if (!outdir.exists()) {
			if (!outdir.mkdirs()) {
				System.err.println("Create output directory failure");
				return;
			}
		}

		/* BSBI indexing algorithm */
		File[] dirlist = rootdir.listFiles();

		/* For each block */
		for (File block : dirlist) {
			File blockFile = new File(output, block.getName());
			blockQueue.add(blockFile);

			File blockDir = new File(root, block.getName());
			File[] filelist = blockDir.listFiles();
			
			ArrayList<Pair<Integer,Integer>> pairs = new ArrayList<Pair<Integer,Integer>>();

			/* For each file */
			for (File file : filelist) {
				++totalFileCount;
				String fileName = block.getName() + "/" + file.getName();
				docDict.put(fileName, docIdCounter++);
				
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().split("\\s+");
					for (String token : tokens) {
						/* My code here
						 * This is where we create the create the termID docID pairs
						 * for a block. We also create the term dictionary.
						 */
						mapper(pairs,token,docIdCounter);
					}
				}
				reader.close();
			}

			/* Sort and output */
			if (!blockFile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}
			
			RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");
			/* My code here.
			 * This is where we sort the termID docID pairs, create the postings
			 * and block index and write the index to file. The method will call
			 * writePosting above for sure.
			 */
			FileChannel fc = bfc.getChannel();
			reducer(pairs,fc,index);
			bfc.close();
		}

		/* Required: output total number of files. */
		System.out.println(totalFileCount);

		/* Merge blocks */
		while (true) {
			if (blockQueue.size() <= 1)
				break;

			File b1 = blockQueue.removeFirst();
			File b2 = blockQueue.removeFirst();
			
			File combfile = new File(output, b1.getName() + "+" + b2.getName());
			if (!combfile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}
			RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
			RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
			RandomAccessFile mf = new RandomAccessFile(combfile, "rw");
			 
			/* My code here
			 * This is where we merge all of the blocks with merge sort for
			 * already sorted lists. We also track the freq of terms here
			 */
			merge(bf1.getChannel(),bf2.getChannel(),mf.getChannel(),index);
			bf1.close();
			bf2.close();
			mf.close();
			b1.delete();
			b2.delete();
			blockQueue.add(combfile);
		}

		/* Dump constructed index back into file system */
		File indexFile = blockQueue.removeFirst();
		indexFile.renameTo(new File(output, "corpus.index"));

		BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(
				output, "term.dict")));
		for (String term : termDict.keySet()) {
			termWriter.write(term + "\t" + termDict.get(term) + "\n");
		}
		termWriter.close();

		BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(
				output, "doc.dict")));
		for (String doc : docDict.keySet()) {
			docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
		}
		docWriter.close();

		BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(
				output, "posting.dict")));
		for (Integer termId : postingDict.keySet()) {
			postWriter.write(termId + "\t" + postingDict.get(termId).getFirst()
					+ "\t" + postingDict.get(termId).getSecond() + "\n");
		}
		postWriter.close();
	}

}
