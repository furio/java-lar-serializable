package it.cvdlab.lar.pipeline.kernelwrap;

import it.cvdlab.lar.model.helpers.PointerUtils;
import it.cvdlab.lar.pipeline.helpers.MultipleFind;
import it.cvdlab.lar.pipeline.helpers.ResponseTime;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bridj.Pointer;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.LocalSize;
import com.nativelibs4java.util.IOUtils;

public class PrefixSumKernelOld {
	private static DeviceFeature runOn = DeviceFeature.GPU;
	
	private static String KERNEL_FILE = "prefixsum.cl";
	private static String KERNEL_FUNCTION = "kernel__scan_block_anylength";
	
	private static String D_TYPE = "T";
	
	private static int ELEMENTS_NUMBER = 1024;
	private static long TOTAL_MEMORY = 0;
	private static long MAX_ALLOCATION = 0;

	public static List<Integer> runPrefixScan(List<Integer> elements) {
		if (elements == null) {
			return null;
		}
		
		System.out.println(elements);
		
		List<CLMem> buffersRelease = Lists.newArrayList();
		@SuppressWarnings("rawtypes")
		List<Pointer> pointersRelease = Lists.newArrayList();

		System.err.println("Kernel context");
		CLContext context = JavaCL.createBestContext(runOn);

		if (context == null) {
			clearAllocatedCLObjects(buffersRelease);
			clearAllocatedPTRObjects(pointersRelease);

			return null;
		}

		// WorkGroupSize
		System.err.println("wgsize");
		long maxWorkGroupSize = Long.MAX_VALUE;
		for (CLDevice currDev : context.getDevices()) {
			maxWorkGroupSize = Math.min(maxWorkGroupSize, currDev.getMaxWorkGroupSize());
			TOTAL_MEMORY += currDev.getGlobalMemSize();
			MAX_ALLOCATION = currDev.getMaxMemAllocSize();
		}
		System.out.println("Max Alloc Size: " + MAX_ALLOCATION);
		System.out.println("Max Wg Size: " + maxWorkGroupSize);
		System.out.println("TotalMemory: " + TOTAL_MEMORY);

		CLQueue queue = context.createDefaultQueue();
		ByteOrder byteOrder = context.getByteOrder();

		// Native memory
		Pointer<Integer> vector_data = null;

		System.err.println("PTR Buffers");
		// Allocate
		vector_data = Pointer.allocateInts(elements.size()).order(byteOrder);
		pointersRelease.add(vector_data);
		PointerUtils.copyToPointer(elements, vector_data);

		// CLBuffers
		CLBuffer<Integer> cl_vector_data = null;

		System.err.println("CL Buffers");
		try {
			cl_vector_data = context.createBuffer(Usage.InputOutput, vector_data, (runOn == DeviceFeature.GPU));
			buffersRelease.add(cl_vector_data);
		} catch (CLException e) {
			queue.flush();
			queue.release();
			clearAllocatedCLObjects(buffersRelease);
			clearAllocatedPTRObjects(pointersRelease);
			context.release();

			System.err.println(e.toString());
			return null;
		}
		System.err.println("Buffer element size (byte): " + cl_vector_data.getElementSize());

		System.err.println("Kernel source");
		// Read the program sources and compile them :
		String kernelSource = null;
		try {
			kernelSource = IOUtils.readText(PrefixSumKernelOld.class.getResource(KERNEL_FILE));
		} catch (IOException e) {
			queue.flush();
			queue.release();
			clearAllocatedCLObjects(buffersRelease);
			clearAllocatedPTRObjects(pointersRelease);
			context.release();

			System.err.println(e.toString());
			return null;
		}
		// System.out.println(kernelSource);

		System.err.println("Create program");
		CLProgram program = context.createProgram(kernelSource);
		// System.err.println("\t options: " + "-D " + KERNEL_DEFINE_VSIZE + "=" + vectorSize);
		
		// Static input parameters
		program.defineMacro(D_TYPE, "int");
		
		if (runOn != DeviceFeature.CPU) {
			// Remove unused stuff for this kernel
			program.addBuildOption("-cl-denorms-are-zero");
			program.addBuildOption("-cl-finite-math-only");			
		}


		// Get and call the kernel :
		System.err.println("Create kernel");
		CLKernel multiplyMatrixKernel = null;
		multiplyMatrixKernel = program.createKernel(KERNEL_FUNCTION);
		
		long multipleWorkGroup = 32;
		Map<CLDevice, Long> prefsLocal = multiplyMatrixKernel.getPreferredWorkGroupSizeMultiple();
		for(CLDevice currDev : prefsLocal.keySet()) {
			System.out.println("Dev: " + currDev.getName() + " -- Multiple: " + prefsLocal.get(currDev));
			multipleWorkGroup = prefsLocal.get(currDev);
		}
		
		int blockSize = elements.size() / (int)multipleWorkGroup;
		int B = blockSize * (int)multipleWorkGroup;
		if ((elements.size() % (int)multipleWorkGroup) > 0) { blockSize++; };
		int[] localWorkSize = {(int)multipleWorkGroup };
		int[] globalWorkSize = { MultipleFind.toMultipleOf(elements.size() / (int)multipleWorkGroup, (int)multipleWorkGroup)};		
		
		System.err.println("WgSize: " + globalWorkSize[0] + " - LocalSize: " + ((localWorkSize == null) ? 0 : localWorkSize[0]));
		
		System.err.println("Adding local cache");
		multiplyMatrixKernel.setArgs(
				new LocalSize(multipleWorkGroup*(Integer.SIZE/8)),
				cl_vector_data,
				B,
				elements.size(),
				blockSize
		);
		
		CLEvent addEvt = null;
		long kernelTime = System.currentTimeMillis();
		if (true && (runOn == DeviceFeature.CPU)) {
			System.err.println("EnqueueND Range - wgSize");
			addEvt = multiplyMatrixKernel.enqueueNDRange(queue, globalWorkSize);
		} else {
			System.err.println("EnqueueND Range - wgSize+locSize");
			addEvt = multiplyMatrixKernel
					.enqueueNDRange(queue, globalWorkSize, localWorkSize);
		}
		ResponseTime rtCount = new ResponseTime(kernelTime);
		addEvt.setCompletionCallback(rtCount);
		// Pointer<Float> matrixDataOut =
		// Pointer.allocateFloats(matrixA.getRowCount()*matrixBToTranspose.getColCount()).order(byteOrder);
		// cl_output_data.read(queue, matrixDataOut, true, addEvt);

		Pointer<Integer> lstOut = cl_vector_data.read(queue, addEvt);
		pointersRelease.add(lstOut);
		List<Integer> listMatrixOut = PointerUtils.copyFromPointer(lstOut);
		
		addEvt.release();
		queue.flush();
		queue.release();
		multiplyMatrixKernel.release();
		program.release();
		clearAllocatedCLObjects(buffersRelease);
		clearAllocatedPTRObjects(pointersRelease);
		context.release();
		
		
		return listMatrixOut;
	}
	
	public static void main(String[] args) {
		System.out.println(runPrefixScan(Ints.asList(binarydataInit(ELEMENTS_NUMBER,1))));
	}
	
	private static int[] binarydataInit(int length, int initValue) {
		int[] arr = new int[length];
		Arrays.fill(arr, initValue);
		return arr;
	}
	
	private static void clearAllocatedCLObjects(List<CLMem> listOfObjects) {
		System.err.println("Clearing CLMEM");
		for (CLMem buffObject : listOfObjects) {
			buffObject.release();
		}
		listOfObjects.clear();
	}

	@SuppressWarnings("rawtypes")
	private static void clearAllocatedPTRObjects(List<Pointer> listOfObjects) {
		System.err.println("Clearing POINTERS");
		for (Pointer buffObject : listOfObjects) {
			buffObject.release();
		}
		listOfObjects.clear();
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
