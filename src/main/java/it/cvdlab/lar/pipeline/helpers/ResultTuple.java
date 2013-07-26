package it.cvdlab.lar.pipeline.helpers;

import java.util.List;

public class ResultTuple {

	private List<Byte> dataOutput;
	private int vectorsQty;
	private long elapsedTime;
	
	public ResultTuple(List<Byte> dataOutput, int vQty, long elapsedTime) {
		super();
		this.dataOutput = dataOutput;
		this.vectorsQty = vQty;
		this.elapsedTime = elapsedTime;
	}
	public int getVectorsQty() {
		return vectorsQty;
	}
	public List<Byte> getDataOutput() {
		return dataOutput;
	}
	public long getElapsedTime() {
		return elapsedTime;
	}

}
