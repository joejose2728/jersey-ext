package com.ebayinc.platform.services.benchmark;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.glassfish.jersey.client.ClientConfig;

import com.ebayinc.platform.jaxrs.ahc.connector.AsyncHttpClientConnectorProvider;

public class AsyncNIOBenchmarkCollector implements IBenchmarkCollector {

	private static final String URL = "http://www.json-generator.com/api/json/get/bIPOGuXFqq?indent=2" /*"http://www.json-generator.com/api/json/get/cvJIUtuoOa?indent=2"*/;
	
	private Client sClient;
	private ConcurrentMap<String, Long> results = new ConcurrentHashMap<String, Long>();
	private int timesPerThread;
	private CountDownLatch doneLatch;
		
	public void collect(int warmupCount, int noOfThreads, final int timesPerThread) {
		this.timesPerThread = timesPerThread;
		this.doneLatch = new CountDownLatch(noOfThreads);
		configure();
		try {
			warmup(warmupCount);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for (int i=0; i<noOfThreads; i++) {
			new Thread(new Runnable() {
				CountDownLatch latch = new CountDownLatch(timesPerThread);
				AtomicLong timespan = new AtomicLong();
				
				public void run() {
					String requestThreadId = Thread.currentThread().getName();
					results.put(requestThreadId,0l);
					for (int j=0; j<timesPerThread;j++) {
						try {
							invoke(latch, timespan);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					try {
						latch.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					results.put(requestThreadId, timespan.get());
					doneLatch.countDown();
				}
			}).start();
		}
		
		try {
			doneLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void publish() {
		System.out.println("ThreadId\t\t\tAvg time per request(ms)");
		System.out.println("========\t\t\t========================");
		for (Entry<String,Long> entry: results.entrySet()){
			System.out.println(entry.getKey() + "\t\t\t" + entry.getValue()/timesPerThread);
		}
	}

	private void invoke(final CountDownLatch latch, final AtomicLong timespan) throws Exception {
		final long start = System.currentTimeMillis();
		sClient.target(URL).request().async().get(new InvocationCallback<Response>() {

			public void completed(Response response) {
				//response.readEntity(String.class);
				long span = System.currentTimeMillis() - start;
				timespan.addAndGet(span);
				latch.countDown();
				/*System.out.println("Request processed by: " + Thread.currentThread().getName());*/
			}

			public void failed(Throwable throwable) {
				throwable.printStackTrace();
			}
		});
	}
	
	private void configure(){
		ClientConfig config = new ClientConfig();
		config.connectorProvider(new AsyncHttpClientConnectorProvider(new DefaultAsyncHttpClient()));
		sClient = ClientBuilder.newClient(config);
	}
	
	private void warmup(int count) throws Exception{
		for (int i=0; i<count; i++) {
			sClient.target(URL).request().async().get().get().readEntity(String.class);
		}
		System.out.println("Warmup done...");
	}
}
