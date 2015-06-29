package com.ebayinc.platform.services.benchmark;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

public class AsyncIOBenchmarkCollector implements IBenchmarkCollector {

	private static final String URL = "http://www.json-generator.com/api/json/get/bIPOGuXFqq?indent=2"/*"http://www.json-generator.com/api/json/get/cvJIUtuoOa?indent=2"*/;
	
	private Client sClient;
	private ConcurrentMap<String, Integer> results = new ConcurrentHashMap<String, Integer>();
	private int timesPerThread;
	private CountDownLatch doneLatch;
	
	public void collect(int warmupCount, int noOfThreads, final int timesPerThread) {
		this.timesPerThread = timesPerThread;
		this.doneLatch = new CountDownLatch(timesPerThread * noOfThreads);
		configure();
		try {
			warmup(warmupCount);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for (int i=0; i<noOfThreads; i++) {
			new Thread(new Runnable() {
				
				public void run() {
					results.put(Thread.currentThread().getName(),0);
					for (int j=0; j<timesPerThread;j++) {
						try {
							invoke();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
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
		for (Entry<String,Integer> entry: results.entrySet()){
			System.out.println(entry.getKey() + "\t\t\t" + entry.getValue()/timesPerThread);
		}
	}

	private void invoke() throws Exception {
		final String requestThreadId = Thread.currentThread().getName();
		final long start = System.currentTimeMillis();
		sClient.target(URL).request().async().get(new InvocationCallback<Response>() {

			public void completed(Response response) {
				//response.readEntity(String.class);
				long span = System.currentTimeMillis() - start;
				int newVal = results.get(requestThreadId);
				newVal += span;
				results.put(requestThreadId, newVal);
				doneLatch.countDown();
				//System.out.println("Request processed by: " + Thread.currentThread().getName());
			}

			public void failed(Throwable throwable) {
				throwable.printStackTrace();
			}
		});
	}
	
	private void configure(){
		sClient = ClientBuilder.newClient();
	}
	
	private void warmup(int count) throws Exception{
		for (int i=0; i<count; i++) {
			sClient.target(URL).request().async().get().get().readEntity(String.class);
		}
		System.out.println("Warmup done...");
	}
}
