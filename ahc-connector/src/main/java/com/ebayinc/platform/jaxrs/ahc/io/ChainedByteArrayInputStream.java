package com.ebayinc.platform.jaxrs.ahc.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

public class ChainedByteArrayInputStream extends InputStream {

	private LinkedList<ByteArrayInputStream> streams = new LinkedList<>();

	private ByteArrayInputStream head;
	private int count;
	private int pos;

	@Override
	public int read() throws IOException {
		if (head == null) {
			head = streams.poll();
			if (head == null) {	// streams has no element; end of chained stream
				return -1;
			}
		} 

		if (pos >= count) {
			return -1;
		}
	
		int nextByte = (head.available() > 0) ? head.read() : ((head = streams.poll()) != null) ? head.read() : -1; 
		pos++;
		return nextByte;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (head == null) {
			head = streams.poll();
			if (head == null) {	// streams has no element; end of chained stream
				return -1;
			}
		} 

		int avail = count - pos;
		if (avail < len) {
			len = avail;
		}

		if (len <= 0) {
			return -1;
		}

		int toRead = len;
		int bytesRead = 0;
		int newOffset = off + bytesRead;
		while (toRead != 0) {
			bytesRead = head.read(b, newOffset, toRead);
			if (bytesRead != -1) {
				newOffset += bytesRead;
				toRead -= bytesRead;
			} else {
				head = streams.poll();
			}
		}
		
		pos += len;
		return len;
	}

	@Override
	public int available() throws IOException {
		return count - pos;
	}

	public void chain(ByteArrayInputStream byteArrayIS) {
		if (byteArrayIS.available() > 0) { //add to chain only if bytes available to read
			this.streams.add(byteArrayIS);
			this.count += byteArrayIS.available();
		}
	}

}
