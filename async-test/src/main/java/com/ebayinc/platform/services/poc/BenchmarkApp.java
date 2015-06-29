package com.ebayinc.platform.services.poc;

import com.ebayinc.platform.services.benchmark.IBenchmarkCollector;

public class BenchmarkApp {

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.err
					.println("Invalid number of arguments. "
							+ "Expects {className} {warmupCount} {noOfThreads} {timesPerThread}");
			return;
		}
		String className = args[0];
		int warmupCount = Integer.parseInt(args[1]);
		int noOfThreads = Integer.parseInt(args[2]);
		int timesPerThread = Integer.parseInt(args[3]);

		Class<?> clazz = Class.forName(className);
		IBenchmarkCollector collector = (IBenchmarkCollector) clazz.newInstance();
		collector.collect(warmupCount, noOfThreads, timesPerThread);
		collector.publish();
	}
}
