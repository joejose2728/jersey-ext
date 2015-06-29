package com.ebayinc.platform.jaxrs.ahc.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ChainedByteArrayInputStreamTest {
	
	private ChainedByteArrayInputStream cbais;
	
	@Before
	public void setUp(){
		cbais = new ChainedByteArrayInputStream();
		cbais.chain(new ByteArrayInputStream(new String("Hello").getBytes()));
		cbais.chain(new ByteArrayInputStream(new String("-World").getBytes()));
	}
	
	@Test
	public void testReadIntoByteArray() throws IOException{
		byte [] b = new byte[7];
		int i = cbais.read(b);
		Assert.assertEquals(7, i);
		Assert.assertEquals("Hello-W", new String(b));
		Assert.assertEquals(4,cbais.available());
	}
	
	@Test
	public void testRead() throws IOException {
		int bb = 0;
		int cnt = -1; //start with -1 to account for the additional cnt++ during last iteration
		while (bb != -1) {
			bb = cbais.read();
			cnt ++;
		}
		Assert.assertEquals(11, cnt);
	}
}
