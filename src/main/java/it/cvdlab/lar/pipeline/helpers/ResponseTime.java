package it.cvdlab.lar.pipeline.helpers;

import com.nativelibs4java.opencl.CLEvent.EventCallback;

public class ResponseTime implements EventCallback {
	private long start;
	
	public ResponseTime(long startTime) {
		this.start = startTime;
	}
	
	public void callback(int arg0) {
		System.err.println("Kernel execution in: " + (System.currentTimeMillis() - this.start) + " millis. Return code: " + arg0);
	}
}
