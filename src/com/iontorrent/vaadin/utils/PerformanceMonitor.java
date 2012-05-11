package com.iontorrent.vaadin.utils;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;


public class PerformanceMonitor {
	private int availableProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	private long lastSystemTime = 0;
	private long lastProcessCpuTime = 0;
	private DecimalFormat format = new DecimalFormat("#.##");
	
	public String getMemoryInfo() {
		String s = "";
		s += "Available processors (cores): " + Runtime.getRuntime().availableProcessors()+"\n";
		/* Total amount of free memory available to the JVM */
		s += "Free memory (M bytes): " + Runtime.getRuntime().freeMemory()/1000000+"\n";
		/* This will return Long.MAX_VALUE if there is no preset limit */
		long maxMemory = Runtime.getRuntime().maxMemory();
		/* Maximum amount of memory the JVM will attempt to use */
		s+="Maximum memory (M bytes): " + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory/1000000)+"\n";

		/* Total memory currently in use by the JVM */
		s+="Total memory (M bytes): " + Runtime.getRuntime().totalMemory()/1000000;
		return s;
	}
	public int nrProcessors() {
		return Runtime.getRuntime().availableProcessors();
	}
	public double getFreeMb() {
		return Runtime.getRuntime().freeMemory()/1000000;
	}
	public double getTotalMb() {
		return Runtime.getRuntime().totalMemory()/1000000;
	}
	public String getCPUInfo() {
		String s= "CPU usage: ";
		double usage = getCpuUsage();
		if (usage <=0) {
			s += "unknown";
		}
		else s += usage+"%";
		return s;
	}
	public String getCsvHeader() {
		return "free, total, used";
	}
	public String getCsvInfo() {
		long free =Runtime.getRuntime().freeMemory()/1000000;
		long total =Runtime.getRuntime().totalMemory()/1000000;
		String s = free+", "+total+", "+(total-free);
		return s;
	}
	public synchronized double getCpuUsage() {
		return 0;
	}

}