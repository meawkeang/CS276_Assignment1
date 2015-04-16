package cs276.assignments;

import java.nio.channels.FileChannel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class VBIndex implements BaseIndex {

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
			ByteBuffer bf = ByteBuffer.allocate(2*4 + numPostings*4);
			bf.clear();
			bf.putInt(termID);
			bf.putInt(numPostings);
			for(int i = 0; i < numPostings; i++){
				bf.putInt(postings.get(i));
			}
			bf.flip();
			fc.write(bf);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	private byte[] vbEncodeNumber(int number){
		ArrayList<Byte> finalByteList = new ArrayList<Byte>();
		while(true){
			finalByteList.add(0,new Byte((byte)(number%128)));
			if(number < 128) break;
			number = number/128;
		}
		Byte lastByte = finalByteList.get(finalByteList.size()-1);
		byte terminatedByte = (byte)((int)lastByte.byteValue() + (int)128);
		finalByteList.set(finalByteList.size()-1,new Byte(terminatedByte));
		//Copt byte arraylist to byte array
		byte[] byteAr = new byte[finalByteList.size()];
		for(int i = 0; i < finalByteList.size(); i++){
			byteAr[i] = (finalByteList.get(i)).byteValue();
		}
		return byteAr;
	}


}
