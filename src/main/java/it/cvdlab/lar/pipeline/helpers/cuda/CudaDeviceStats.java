package it.cvdlab.lar.pipeline.helpers.cuda;

import java.util.Arrays;

public class CudaDeviceStats {
	private String deviceName;
	private int cores;
	private int multiprocessors;
	private boolean concurrencyKernel;
	private boolean runtimelimitKernel;
	private int threadsPerBlock;
	private int[] blockSize;
	private int[] gridSize;
	private int warpSize;
	private int alignment;
	private int registers;
	
	CudaDeviceStats() {}
	
	public String getDeviceName() {
		return deviceName;
	}
	
	void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	
	public int getCores() {
		return cores;
	}
	
	void setCores(int cores) {
		this.cores = cores;
	}
	
	public int getMultiprocessors() {
		return multiprocessors;
	}
	
	void setMultiprocessors(int multiprocessors) {
		this.multiprocessors = multiprocessors;
	}
	
	public int getTotalCores() {
		return multiprocessors * cores;
	}	

	public boolean isConcurrencyKernel() {
		return concurrencyKernel;
	}

	void setConcurrencyKernel(boolean concurrencyKernel) {
		this.concurrencyKernel = concurrencyKernel;
	}

	public boolean isRuntimelimitKernel() {
		return runtimelimitKernel;
	}

	void setRuntimelimitKernel(boolean runtimelimitKernel) {
		this.runtimelimitKernel = runtimelimitKernel;
	}

	public int getThreadsPerBlock() {
		return threadsPerBlock;
	}

	void setThreadsPerBlock(int threadsPerBlock) {
		this.threadsPerBlock = threadsPerBlock;
	}
	
	public int getBlockSize(CudaCoordinates fetch) {
		return blockSize[fetch.index()];
	}

	int[] getBlockSize() {
		return blockSize;
	}

	void setBlockSize(int[] blockSize) {
		this.blockSize = blockSize;
	}

	public int getGridSize(CudaCoordinates fetch) {
		return gridSize[fetch.index()];
	}
	
	int[] getGridSize() {
		return gridSize;
	}

	void setGridSize(int[] gridSize) {
		this.gridSize = gridSize;
	}

	public int getWarpSize() {
		return warpSize;
	}

	void setWarpSize(int warpSize) {
		this.warpSize = warpSize;
	}

	public int getAlignment() {
		return alignment;
	}

	void setAlignment(int alignment) {
		this.alignment = alignment;
	}
	
	public int getRegisters() {
		return registers;
	}

	void setRegisters(int registers) {
		this.registers = registers;
	}	

	@Override
	public String toString() {
		return "CudaDeviceStats [deviceName=" + deviceName + ", cores=" + cores
				+ ", multiprocessors=" + multiprocessors
				+ ", concurrencyKernel=" + concurrencyKernel
				+ ", runtimelimitKernel=" + runtimelimitKernel
				+ ", threadsPerBlock=" + threadsPerBlock + ", blockSize="
				+ Arrays.toString(blockSize) + ", gridSize="
				+ Arrays.toString(gridSize) + ", warpSize=" + warpSize
				+ ", alignment=" + alignment
				+ ", registers=" + registers + "]";
	}
}
