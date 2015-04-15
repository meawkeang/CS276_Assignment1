package cs276.assignments;

import java.nio.channels.FileChannel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BasicIndex implements BaseIndex {

	@Override
	public PostingList readPosting(FileChannel fc) {
		try{
			if(fc.position() >= fc.size()) return null;
			ByteBuffer creds = ByteBuffer.allocate(2*4);
			fc.read(creds);
			int termID = creds.getInt(0);
			int numPostings = creds.getInt(4);
			ArrayList<Integer> postings = new ArrayList<Integer>(numPostings);
			ByteBuffer docs = ByteBuffer.allocate(numPostings*4);
			fc.read(docs);
			for(int i = 0; i < numPostings; i++){
				postings.add(new Integer(docs.getInt(i*4)));
			}
			PostingList pl = new PostingList(termID,postings);
			return pl;
		}catch(IOException e){
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
			ByteBuffer creds = ByteBuffer.allocate(2*4);
			creds.putInt(termID);
			creds.putInt(numPostings);
			fc.write(ByteBuffer.wrap(creds.array()));
			ByteBuffer bf = ByteBuffer.allocate(numPostings*4);
			for(int i = 0; i < numPostings; i++){
				bf.putInt(postings.get(i));
			}
			fc.write(ByteBuffer.wrap(bf.array()));
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
