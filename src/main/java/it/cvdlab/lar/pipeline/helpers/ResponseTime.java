package it.cvdlab.lar.pipeline.helpers;

import com.nativelibs4java.opencl.CLEvent.EventCallback;

public class ResponseTime implements EventCallback {
	private long start;
	private long end;
	
	public ResponseTime(long startTime) {
		this.start = startTime;
	}
	
	public void callback(int arg0) {
		this.end = System.currentTimeMillis();
		System.err.println("Kernel execution in: " + (this.end - this.start) + " millis. Return code: " + arg0);
	}
	
	public long elapsedTime() {
		return this.end - this.start;
	}
}
