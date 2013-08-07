package it.cvdlab.lar.pipeline.kernelwrap;

import java.util.List;

import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;

public class PrefixSumKernel extends RunningKernel {
	private static DeviceFeature runOn = DeviceFeature.GPU;
	
	private static String KERNEL_FILE = "prefixsum.cl";
	private static String KERNEL_FUNCTION = "kernel__scan_block_anylength";
	
	private static String D_TYPE = "T";
	


	public static List<Integer> runPrefixScan(List<Integer> elements) {
		PrefixSumKernel psk = new PrefixSumKernel(runOn);
		psk.initContext();
		psk.initQueue();
		
		
		
		
		return null;
	}

	public PrefixSumKernel(DeviceFeature runOn2) {
		super(runOn2);
	}
	
	@Override
	void preProcessKernelSource(String theSource) {
		// TODO Auto-generated method stub
		
	}
}

/*
 * _datasetSize = datasetSize; sizeof dell'array in input
 * _workgroupSize clGetKernelWorkGroupInfo(kernel__scan, _context->clDevice, CL_KERNEL_WORK_GROUP_SIZE, sizeof(size_t), &_workgroupSize, 0);
 * _valueSize = sizeof(element);
 * 
 * 
 * 	cl_int clStatus;

	int blockSize = _datasetSize / _workgroupSize;
	int B = blockSize * _workgroupSize;
	if ((_datasetSize % _workgroupSize) > 0) { blockSize++; };
	size_t localWorkSize = {_workgroupSize};
	size_t globalWorkSize = {toMultipleOf(_datasetSize / blockSize, _workgroupSize)};

	clStatus  = clSetKernelArg(kernel__scan, 0, _workgroupSize * _valueSize, 0);
	clStatus |= clSetKernelArg(kernel__scan, 1, sizeof(cl_mem), &_clBuffer_values);
	clStatus |= clSetKernelArg(kernel__scan, 2, sizeof(int), &B);
	clStatus |= clSetKernelArg(kernel__scan, 3, sizeof(int), &_datasetSize);
	clStatus |= clSetKernelArg(kernel__scan, 4, sizeof(int), &blockSize);

	clStatus |= clEnqueueNDRangeKernel(_context->clQueue, kernel__scan, 1, NULL, &globalWorkSize, &localWorkSize, 0, NULL, NULL);
	checkCLStatus(clStatus);
	
	
	*/
