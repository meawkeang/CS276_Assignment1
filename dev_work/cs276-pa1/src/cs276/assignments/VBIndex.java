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
			int numBytes = creds.getInt(4);
			ByteBuffer docs = ByteBuffer.allocate(numBytes);
			fc.read(docs);
			ArrayList<Integer> postings = vbDecodeStream(numBytes,docs);
			PostingList pl = new PostingList(termID,postings);
			return pl;
		}catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void writePosting(FileChannel fc, PostingList p) {
		Integer termID = p.getTermId();
		List<Integer> postings = p.getList();
		ArrayList<Byte> byteList = vbEncodeStream(postings);
		try{
			ByteBuffer bf = ByteBuffer.allocate(2*4 + byteList.size());
			bf.clear();
			bf.putInt(termID);
			bf.putInt(new Integer(byteList.size()));
			bf.put(arListToByteAr(byteList));
			bf.flip();
			fc.write(bf);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	private ArrayList<Integer> vbDecodeStream(int numBytes, ByteBuffer docs){
		ArrayList<Integer> postings = new ArrayList<Integer>();
		byte[] byteArray = docs.array();
		int number = 0;
		int prevNumber = 0;
		for(int i = 0; i < numBytes; i++){
			byte b = byteArray[i];
			if(b < 0){
				number = number*128 + prevNumber + (b+128);
				postings.add(new Integer(number));
				prevNumber = number;
				number = 0;
			}else{
				number = number*128 + b;
			}
		}
		return postings;
	}

	private ArrayList<Byte> vbEncodeStream(List<Integer> docIds){
		//System.out.println(docIds);
		int initialDoc = (docIds.get(0)).intValue();
		ArrayList<Byte> byteAr = vbEncodeNumber(initialDoc);
		int prevDoc = initialDoc;
		for(int i = 1; i < docIds.size(); i++){
			int nextDoc = docIds.get(i);
			byteAr.addAll(vbEncodeNumber(nextDoc-prevDoc));
			prevDoc = nextDoc;
		}
		return byteAr;
	}

	private ArrayList<Byte> vbEncodeNumber(int number){
		ArrayList<Byte> finalByteList = new ArrayList<Byte>();
		while(true){
			finalByteList.add(0,new Byte((byte)(number%128)));
			if(number < 128) break;
			number = number/128;
		}
		Byte lastByte = finalByteList.get(finalByteList.size()-1);
		byte terminatedByte = (byte)((int)lastByte.byteValue() + (int)128);
		finalByteList.set(finalByteList.size()-1,new Byte(terminatedByte));
		return finalByteList;
	}

	private byte[] arListToByteAr(ArrayList<Byte> byteList){
		//Copy byte arraylist to byte array
		byte[] byteAr = new byte[byteList.size()];
		for(int i = 0; i < byteList.size(); i++){
			byteAr[i] = (byteList.get(i)).byteValue();
		}
		return byteAr;
	}


}
