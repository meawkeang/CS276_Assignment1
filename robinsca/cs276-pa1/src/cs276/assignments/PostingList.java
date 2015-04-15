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
		Integer termID = left.getTermId();
		List<Integer> leftAr = left.getList();
		List<Integer> rightAr = right.getList();
		int leftSize = leftAr.size();
		int rightSize = rightAr.size();
		int combinedSize = leftSize + rightSize;
		ArrayList<Integer> postings = new ArrayList<Integer>(combinedSize);
		int leftPtr = 0;
		int rightPtr = 0;
		int leftDoc = -1;
		int rightDoc = -1;
		for(int i = 0; i < combinedSize; i++){
			if(leftPtr < leftSize){
				leftDoc = leftAr.get(leftPtr);
			}
			if(rightPtr < rightSize){
				rightDoc = rightAr.get(rightPtr);
			}
			if(leftDoc < rightDoc){
				postings.add(new Integer(leftDoc));
				leftPtr = leftPtr + 1;
				if(leftPtr >= leftSize){
					leftDoc = Integer.MAX_VALUE;
				}
			}
			if(rightDoc > leftDoc){
				postings.add(new Integer(rightDoc));
				rightPtr = rightPtr + 1;
				if(rightPtr >= rightSize){
					rightDoc = Integer.MAX_VALUE;
				}
			}
		}
		PostingList pl = new PostingList(termID,postings);
		return pl;
	}
}
