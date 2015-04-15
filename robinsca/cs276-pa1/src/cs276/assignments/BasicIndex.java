package cs276.assignments;

import java.nio.channels.FileChannel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BasicIndex implements BaseIndex {

	@Override
	public PostingList readPosting(FileChannel fc) {
		ByteBuffer termBuf = ByteBuffer.allocate(4);
		ByteBuffer numPostingsBuf = ByteBuffer.allocate(4);
		try{
			if(fc.position() >= fc.size()) return null;
			fc.read(termBuf);
			fc.read(numPostingsBuf);
			int termID = termBuf.getInt(0);
			int numPostings = numPostingsBuf.getInt(0);
			ArrayList<Integer> postings = new ArrayList<Integer>(numPostings);
			for(int i = 0; i < numPostings; i++){
				ByteBuffer docID = ByteBuffer.allocate(4);
				fc.read(docID);
				postings.add(new Integer(docID.getInt(0)));
			}
			PostingList pl = new PostingList(termID,postings);
			return pl;
		}catch(/*IO*/Exception e){
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void writePosting(FileChannel fc, PostingList p) {
		//System.out.println(p);
		Integer termID = p.getTermId();
		List<Integer> postings = p.getList();
		int numPostings = postings.size();
		try{
			fc.write(ByteBuffer.wrap(ByteBuffer.allocate(4).putInt(termID).array()));
			fc.write(ByteBuffer.wrap(ByteBuffer.allocate(4).putInt(numPostings).array()));
			for(int i = 0; i < numPostings; i++){
				fc.write(ByteBuffer.wrap(ByteBuffer.allocate(4).putInt(postings.get(i)).array()));
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
