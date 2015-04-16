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


	private static void sanityCheck(File indexFile) throws IOException {
		System.out.println("SanityCheck...");
		RandomAccessFile mf = new RandomAccessFile(indexFile, "r");
		FileChannel fc = mf.getChannel();
		PostingList pl = index.readPosting(fc);
		while(pl != null){
			System.out.println(pl);
			pl = index.readPosting(fc);
		}
	}
	
	/* 
	 * Write a posting list to the file 
	 * You should record the file position of this posting list
	 * so that you can read it back during retrieval
	 * 
	 * 
	 */
	private static void writePosting(FileChannel fc, PostingList posting)
			throws IOException {
		//System.out.println(posting);
		List<Integer> list = posting.getList();
		Pair<Long,Integer> posFreq = new Pair<Long,Integer>(new Long(fc.position()), new Integer(list.size()));
		postingDict.put(new Integer(posting.getTermId()),posFreq);
		index.writePosting(fc,posting);
	}

	private static void merge(ArrayList<FileChannel> kFileChannels,
		FileChannel comb) throws IOException{

		//Read the first postings list from each FileChannel
		int kSize = kFileChannels.size();
		ArrayList<PostingList> kPostings = new ArrayList<PostingList>(kSize);
		for(int i = 0; i < kSize; i++){
			PostingList pl = index.readPosting(kFileChannels.get(i));
			kPostings.add(pl);
		}
		ArrayList<Integer> shortestIds = new ArrayList<Integer>(kSize);
		ArrayList<Pair<Integer,Integer>> validIds = new ArrayList<Pair<Integer,Integer>>(kSize);
		//System.out.println("Merging...");
		while(true){
			//All null, all lists are exhausted.
			boolean done = true;
			for(int i = 0; i < kSize; i++){
				PostingList pl = kPostings.get(i);
				if(pl != null){
					done = false;
					validIds.add(new Pair<Integer,Integer>(new Integer(pl.getTermId()),new Integer(i)));
				}
			}
			if(done) return;
			//Order the terms from least to greatest
			Collections.sort(validIds);
			//System.out.println(validIds);
			//Take the shorter termID, if equal then combine
			Pair<Integer,Integer> sh = validIds.get(0);
			int shortestTerm = (Integer)sh.getFirst();
			shortestIds.add((Integer)sh.getSecond());
			int validSize = validIds.size();
			for(int i = 1; i < validSize; i++){
				Pair<Integer,Integer> next = validIds.get(i);
				int nextTerm = (Integer)next.getFirst();
				if(nextTerm == shortestTerm){
					//The terms are equal, we're going to have to merge these
					shortestIds.add((Integer)next.getSecond());
				}else{
					break;
				}
			}
			int shortSize = shortestIds.size();
			if(shortSize == 1){
				//No terms are equal, write and advance the shortest term
				int shortId = shortestIds.get(0);
				writePosting(comb,kPostings.get(shortId));
				PostingList advance = index.readPosting(kFileChannels.get(shortId));
				kPostings.set(shortId,advance);
			}else{
				//Multiple terms are equal, we need to merge them
				ArrayList<PostingList> shortList = new ArrayList<PostingList>(shortSize);
				for(int i = 0; i < shortSize; i++){
					int shortestIndex = shortestIds.get(i);
					shortList.add(kPostings.get(shortestIndex));
					PostingList advance = index.readPosting(kFileChannels.get(shortestIndex));
					kPostings.set(shortestIndex,advance);
				}
				PostingList combinedPosting = PostingList.combineMultipleLists(shortList);
				writePosting(comb,combinedPosting);
			}
			shortestIds.clear();
			validIds.clear();
			//System.out.println(left);
			//System.out.println(right);
		}
	}

	private static void mapper(ArrayList<Pair<Integer,Integer>> pairs, String term, int docID){
		if(!termDict.containsKey(term)){
			wordIdCounter = wordIdCounter + 1;
			termDict.put(term,new Integer(wordIdCounter));
		}
		Pair<Integer,Integer> pair = new Pair<Integer,Integer>(termDict.get(term),new Integer(docID));
		pairs.add(pair);
		//System.out.println(pair);
	}

	private static void reducer(ArrayList<Pair<Integer,Integer>> pairs,
		FileChannel fc){
		//System.out.println("Unsorted all pairs");
		//System.out.println(pairs);
		Collections.sort(pairs);
		//System.out.println("Sorted all pairs");
		//System.out.println(pairs);
		int pairArSize = pairs.size();
		if(pairArSize == 0) return;
		//start and i are inclusive pointers
		int start = 0;
		for(int i = 0; i < pairArSize; i++){
			if(i+1 == pairArSize){
				//We're finished with the list
				createPosting(pairs,start,i,fc);
				return;
			}
			Pair<Integer,Integer> current = pairs.get(i);
			Pair<Integer,Integer> next = pairs.get(i+1);
			if(!((Integer)current.getFirst()).equals((Integer)next.getFirst())){
				//We dont have a match
				createPosting(pairs,start,i,fc);
				start = i+1;
			}
		}
	}

	private static void createPosting(ArrayList<Pair<Integer,Integer>> pairs,
		int start,int end,FileChannel fc){
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
		index.writePosting(fc,pl);
		//System.out.println(pl);
	}

	public static void main(String[] args) throws IOException {
		/* Show timing or not */
		boolean verbose = false;
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
			
			int avgTokensPerDoc = 200;
			int avgDocsPerBlock = 10000;
			int avgPairs = avgTokensPerDoc*avgDocsPerBlock;
			ArrayList<Pair<Integer,Integer>> pairs = new ArrayList<Pair<Integer,Integer>>(avgPairs);

			/* For each file */
			long begin = System.currentTimeMillis();
			for (File file : filelist) {
				++totalFileCount;
				String fileName = block.getName() + "/" + file.getName();
				docIdCounter = docIdCounter + 1;
				docDict.put(fileName, docIdCounter);
				
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
			long end = System.currentTimeMillis();
			if(verbose){
				System.out.println("Mapper phase took " + ((end - begin)/1000) + " seconds");
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
			begin = System.currentTimeMillis();
			reducer(pairs,fc);
			end = System.currentTimeMillis();
			if(verbose){
				System.out.println("Reducer phase took " + ((end - begin)/1000) + " seconds");
			}
			bfc.close();
		}

		/* Required: output total number of files. */
		System.out.println(totalFileCount);

		/* Merge blocks */
		long begin = System.currentTimeMillis();
		while (true) {
			if (blockQueue.size() <= 1)
				break;
			int size = blockQueue.size();
			ArrayList<File> fileList = new ArrayList<File>(size);
			String combName = "";
			for(int i = 0; i < size; i++){
				File fl = blockQueue.removeFirst();
				fileList.add(fl);
				if(i != size - 1){
					combName = combName + fl.getName() + "+";
				}else{
					combName = combName + fl.getName();
				}
			}
			File combfile = new File(output,combName);
			if (!combfile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}
			ArrayList<RandomAccessFile> raList = new ArrayList<RandomAccessFile>(size);
			ArrayList<FileChannel> kInputs = new ArrayList<FileChannel>(size);
			for(int i = 0; i < size; i++){
				RandomAccessFile ra = new RandomAccessFile(fileList.get(i),"r");
				raList.add(ra);
				kInputs.add(ra.getChannel());
			}
			RandomAccessFile mf = new RandomAccessFile(combfile, "rw");
			 
			/* My code here
			 * This is where we merge all of the blocks with merge sort for
			 * already sorted lists. We also track the freq of terms here
			 */
			merge(kInputs,mf.getChannel());
			for(int i = 0; i < size; i++){
				(raList.get(i)).close();
				(fileList.get(i)).delete();
			}
			mf.close();
			blockQueue.add(combfile);
		}
		long end = System.currentTimeMillis();
		if(verbose){
			System.out.println("Merge phase took " + ((end - begin)/1000) + " seconds");
		}

		/* Dump constructed index back into file system */
		File indexFile = blockQueue.removeFirst();
		//sanityCheck(indexFile);
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
