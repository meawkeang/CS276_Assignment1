package cs276.assignments;

import java.util.ArrayList;
import java.util.List;

public class PostingList {

	private int termId;
	/* A list of docIDs (i.e. postings) */
	private List<Integer> postings;

	public PostingList(int termId, List<Integer> list) {
		this.termId = termId;
		this.postings = list;
	}

	public PostingList(int termId) {
		this.termId = termId;
		this.postings = new ArrayList<Integer>();
	}

	public int getTermId() {
		return this.termId;
	}

	public List<Integer> getList() {
		return this.postings;
	}

	public String toString() {
		return "termID: " + this.termId + " " + this.postings;
	}

	//This is a list for combined two postings lists with the same terms
	public static PostingList combineLists(PostingList left, PostingList right){
		//Setup
		Integer termID = left.getTermId();
		List<Integer> leftAr = left.getList();
		List<Integer> rightAr = right.getList();
		int leftSize = leftAr.size();
		int rightSize = rightAr.size();
		//Each docID is unique, this is indeed the final size
		int combinedSize = leftSize + rightSize;
		ArrayList<Integer> finalPostings = new ArrayList<Integer>(combinedSize);
		int leftPtr = 0;
		int rightPtr = 0;
		//Loop
		for(int i = 0; i < combinedSize; i++){
			if(leftPtr >= leftSize){
				finish(finalPostings,rightPtr,rightAr);
				break;
			}
			if(rightPtr >= rightSize){
				finish(finalPostings,leftPtr,leftAr);
				break;
			}
			int leftDoc =  leftAr.get(leftPtr);
			int rightDoc = rightAr.get(rightPtr);
			if(leftDoc < rightDoc){
				finalPostings.add(new Integer(leftDoc));
				leftPtr = leftPtr + 1;
			}else{
				finalPostings.add(new Integer(rightDoc));
				rightPtr = rightPtr + 1;
			}
		}
		//Finished product
		//System.out.println("Came in as");
		//System.out.println(left);
		//System.out.println(right);
		//System.out.println("Leaving as");
		PostingList pl = new PostingList(termID,finalPostings);
		//System.out.println(pl);
		return pl;
	}

	private static void finish(ArrayList<Integer> postings, int index, List<Integer> ar){
		int length = ar.size();
		for(int i = index; i < length; i++){
			postings.add(ar.get(i));
		}
	}

	public static PostingList combineMultipleLists(ArrayList<PostingList> pl){
		//System.out.println(pl);
		if(pl == null || pl.size() == 0) return null;
		while(pl.size() > 1){
			PostingList list1 = pl.remove(0);
			PostingList list2 = pl.remove(0);
			pl.add(combineLists(list1,list2));
		}
		return pl.get(0);
	}

}
