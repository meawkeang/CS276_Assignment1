package cs276.assignments;

import java.nio.channels.FileChannel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BasicIndex implements BaseIndex {

	@Override
	public PostingList readPosting(FileChannel fc) {
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
