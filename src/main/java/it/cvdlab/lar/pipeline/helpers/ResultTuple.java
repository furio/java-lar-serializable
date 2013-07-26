package it.cvdlab.lar.pipeline.helpers;

import java.util.List;

public class ResultTuple {

	private List<Byte> dataOutput;
	private long elapsedTime;
	
	public ResultTuple(List<Byte> dataOutput, long elapsedTime) {
		super();
		this.dataOutput = dataOutput;
		this.elapsedTime = elapsedTime;
	}
	public List<Byte> getDataOutput() {
		return dataOutput;
	}
	public long getElapsedTime() {
		return elapsedTime;
	}

}
