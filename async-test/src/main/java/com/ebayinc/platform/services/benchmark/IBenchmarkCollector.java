package com.ebayinc.platform.services.benchmark;

public interface IBenchmarkCollector {

	public void collect(int warmupCount, int noOfThreads, int timesPerThread);
	
	public void publish();
}
