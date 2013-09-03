package it.cvdlab.lar.pipeline.kernelwrap.experimental;

import it.cvdlab.lar.model.helpers.PointerUtils;
import it.cvdlab.lar.pipeline.helpers.MultipleFind;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bridj.Pointer;

import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.LocalSize;

public class PrefixSumKernel extends RunningKernel {
	private static DeviceFeature runOn = DeviceFeature.GPU;
	
	private static String KERNEL_FILE = "prefixsum.cl";
	private static String KERNEL_FUNCTION = "kernel__scan_block_anylength";
	
	private static String D_TYPE = "T";
	
	private static String PTR_INPUT_KEY = "input";

	public static List<Integer> runPrefixScan(List<Integer> elements) {
		if (elements == null) {
			return null;
		}
		
		
		PrefixSumKernel psk = new PrefixSumKernel(runOn);
		System.out.println("Context: " + psk.initContext());
		System.out.println("Queue: " + psk.initQueue());
		
		psk.createNewPointerInteger(PTR_INPUT_KEY, elements);
		psk.createInputMemoryBuffer(PTR_INPUT_KEY);
		
		Map<String,String> defineMap = Maps.newHashMap();
		defineMap.put(D_TYPE, "int");

		psk.loadkernelSourceFromFile(PrefixSumKernel.class.getResource(KERNEL_FILE));
		psk.initProgram(defineMap, null);
		psk.addFasterMathOptions();
		psk.initKernel(KERNEL_FUNCTION);
		
		int blockSize = elements.size() / (int)psk.getMaxKernelWorkgroupSize();
		int B = blockSize * (int)psk.getMaxKernelWorkgroupSize();
		if ((elements.size() % (int)psk.getMaxKernelWorkgroupSize()) > 0) { blockSize++; };
		int[] localWorkSize = {(int)psk.getMaxKernelWorkgroupSize()};
		int[] globalWorkSize = { MultipleFind.toMultipleOf(elements.size() / (int)psk.getMaxKernelWorkgroupSize(), (int)psk.getMaxKernelWorkgroupSize())};
		
		System.out.println(blockSize);
		System.out.println(B);
		System.out.println(localWorkSize[0]);
		System.out.println(globalWorkSize[0]);
		
		psk.setKernelArgs( new LocalSize(psk.getMaxKernelWorkgroupSize()*(Integer.SIZE/8)),
							psk.getBufferInteger(PTR_INPUT_KEY),
							B, elements.size(), blockSize);
		
		CLEvent evtFinish = psk.enqueueKernel(localWorkSize, globalWorkSize);
		
		Pointer<Integer> ptrOut = psk.getBufferInteger(PTR_INPUT_KEY).read(psk.getQueue(), evtFinish);
		
		List<Integer> lRes = PointerUtils.copyFromPointer(ptrOut);
		
		ptrOut.release();
		psk.releaseAll(evtFinish);
		
		return lRes;
	}

	public PrefixSumKernel(DeviceFeature runOn2) {
		super(runOn2);
	}
	
	@Override
	void preProcessKernelSource(String theSource) {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		System.out.println(runPrefixScan(Ints.asList(binarydataInit(1024,1))));
	}
	
	private static int[] binarydataInit(int length, int initValue) {
		int[] arr = new int[length];
		Arrays.fill(arr, initValue);
		return arr;
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
