package it.cvdlab.lar.pipeline.helpers;

import java.util.List;

public class ResultTuple {

	private List<Integer> dataOutput;
	private long elapsedTime;
	
	public ResultTuple(List<Integer> dataOutput, long elapsedTime) {
		super();
		this.dataOutput = dataOutput;
		this.elapsedTime = elapsedTime;
	}
	public List<Integer> getDataOutput() {
		return dataOutput;
	}
	public long getElapsedTime() {
		return elapsedTime;
	}

}
