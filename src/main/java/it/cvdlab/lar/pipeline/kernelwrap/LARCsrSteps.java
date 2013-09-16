package it.cvdlab.lar.pipeline.kernelwrap;

import it.cvdlab.lar.model.CsrMatrix;
import it.cvdlab.lar.model.helpers.CLEngineConfig;
import it.cvdlab.lar.model.helpers.PointerUtils;
import it.cvdlab.lar.pipeline.helpers.MultipleFind;
import it.cvdlab.lar.pipeline.helpers.TransformNumberList;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bridj.Pointer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

public class LARCsrSteps {
	private static DeviceFeature runOn = DeviceFeature.GPU;
	
	private static String[] KERNEL_FILE = {"csr_run_1.cl", "prefixsum.cl", "csr_run_2.cl"};
	private static String[] KERNEL_FUNCTION = {"m_mul_m_count","kernel__scan_block_anylength","m_mul_m"};
	
	private static int ROWS_PER_CORE = 4000;
	
	private static String D_TYPE = "T";
	private static String D_ROWS_A = "ROWS_A";
	private static String D_ROWS_B = "ROWS_B";
	
	private static String PTR_ROWPTR_A = "rowptr_a";
	private static String PTR_ROWPTR_B = "rowptr_b";
	private static String PTR_ROWPTR_C = "rowptr_c";
	private static String PTR_COLDATA_A = "coldata_a";
	private static String PTR_COLDATA_B = "coldata_b";
	private static String PTR_COLDATA_C = "coldata_c";
	private static String PTR_DATA_A = "data_a";
	private static String PTR_DATA_B = "data_b";
	private static String PTR_DATA_C = "data_c";
	
	private static String PTR_INPUT_KEY = PTR_ROWPTR_C;
	
	private static Map<String,CLMem> mapOfBuffers;
	@SuppressWarnings("rawtypes")
	private static Map<String,Pointer> mapOfPointers;
	
	private static void clearAllocatedCLObjects() {
		System.err.println("Clearing CLMEM");
		for(String key: mapOfBuffers.keySet()) {
			mapOfBuffers.get(key).release();
		}
		mapOfBuffers.clear();
	}

	private static void clearAllocatedPTRObjects() {
		System.err.println("Clearing POINTERS");
		for(String key: mapOfPointers.keySet()) {
			mapOfPointers.get(key).release();
		}
		mapOfPointers.clear();
	}
	
	static {
		mapOfBuffers = Maps.newHashMap();
		mapOfPointers = Maps.newHashMap();
	}
	
	private LARCsrSteps() { 
		init();
		this.program = Lists.newArrayList();
		this.kernel = Lists.newArrayList();
	}
	
	void clearAllocatedObjects() {
		if (queue != null) {
			queue.flush();
			queue.release();
			
			queue = null;
		}
		
		clearAllocatedCLObjects();
		clearAllocatedPTRObjects();
		
		System.err.println("Clear kernel & program");
		clearKernelProgram();
		
		System.err.println("Clear context");
		if (context != null) {
			context.release();
			
			context = null;
		}		
	}
	
	private void clearKernelProgram() {
		for(int i = this.kernel.size() - 1; i <= 0; --i) {
			this.kernel.get(i).release();
			this.program.get(i).release();
		}

		this.kernel.clear();
		this.program.clear();
	}
	
	private CLContext context;
	private CLQueue queue;
	private ByteOrder byteOrder;
	private List<CLProgram> program;
	private List<CLKernel> kernel;
	//
	private long availableMemory;
	private long maxAllocation;
	private long maxWorkGroupSize;
	private long maxKernelWorkgroupSize;
	
	private void init() {
		initContext();
	}
	
	boolean initContext() {
		try {
			this.context = JavaCL.createBestContext(runOn);
		} catch(Exception e) {
			e.printStackTrace();
		}
		

		if (this.context == null) {
			clearAllocatedObjects();
			System.err.println("No context");
			return false;
		}
		
		initStatsForContext();
		
		return true;
	}
	
	private void initStatsForContext() {
		this.maxWorkGroupSize = Long.MAX_VALUE;
		this.maxAllocation = Long.MAX_VALUE;
		this.availableMemory = 0;
		for (CLDevice currDev : context.getDevices()) {
			this.maxWorkGroupSize = Math.min(this.maxWorkGroupSize, currDev.getMaxWorkGroupSize());
			this.availableMemory += currDev.getGlobalMemSize();
			this.maxAllocation = Math.min(this.maxAllocation, currDev.getMaxMemAllocSize() );
		}
	}
	
	boolean initQueue() {
		if (this.context == null) {
			System.err.println("No context");
			return false;
		}
		
		this.queue = context.createDefaultQueue();
		if (this.queue == null) {
			clearAllocatedObjects();
			System.err.println("No queue");
			return false;
		}
		
		this.byteOrder = context.getByteOrder();
		
		return true;
	}
	
	private void getkernelStats(CLKernel currKernel) {
		this.maxKernelWorkgroupSize = this.maxWorkGroupSize;
		Map<CLDevice, Long> prefsLocal = currKernel.getPreferredWorkGroupSizeMultiple();
		
		for(CLDevice currDev : prefsLocal.keySet()) {
			this.maxKernelWorkgroupSize = Math.min(this.maxKernelWorkgroupSize, prefsLocal.get(currDev));
		}
	}
	
	CLEvent enqueueKernel(CLKernel currKernel, int[] locSize, int[] wgSize) {
		if (locSize == null) {
			return currKernel.enqueueNDRange(queue, wgSize);
		} else {
			return currKernel.enqueueNDRange(queue, wgSize, locSize);
		}
	}	
	
	@SuppressWarnings("unchecked")
	public boolean runCsrStep1(CsrMatrix mtxOne, CsrMatrix mtxTwo) {
		CsrMatrix mtxTwoT = mtxTwo.transpose();
		List<Integer> lstOutput = Lists.newArrayList(Collections.nCopies(mtxOne.getRowptr().size(), 0));
		
		mapOfPointers.put(PTR_ROWPTR_A, Pointer.allocateInts(mtxOne.getRowptr().size()).order(byteOrder));
		mapOfPointers.put(PTR_ROWPTR_B, Pointer.allocateInts(mtxTwoT.getRowptr().size()).order(byteOrder));
		mapOfPointers.put(PTR_ROWPTR_C, Pointer.allocateInts(lstOutput.size()).order(byteOrder));
		mapOfPointers.put(PTR_COLDATA_A, Pointer.allocateInts(mtxOne.getColdata().size()).order(byteOrder));
		mapOfPointers.put(PTR_COLDATA_B, Pointer.allocateInts(mtxTwoT.getColdata().size()).order(byteOrder));
		
		PointerUtils.copyToPointer(mtxOne.getRowptr(), mapOfPointers.get(PTR_ROWPTR_A));
		PointerUtils.copyToPointer(mtxTwoT.getRowptr(), mapOfPointers.get(PTR_ROWPTR_B));
		PointerUtils.copyToPointer(lstOutput, mapOfPointers.get(PTR_ROWPTR_C));
		PointerUtils.copyToPointer(mtxOne.getColdata(), mapOfPointers.get(PTR_COLDATA_A));
		PointerUtils.copyToPointer(mtxTwoT.getColdata(), mapOfPointers.get(PTR_COLDATA_B));
		
		// Memory
		try {
			mapOfBuffers.put(PTR_ROWPTR_A, context.createBuffer(Usage.Input, (Pointer<Integer>) mapOfPointers.get(PTR_ROWPTR_A),
					CLEngineConfig.isUSE_DEVICE_MEM()));
			mapOfBuffers.put(PTR_ROWPTR_B, context.createBuffer(Usage.Input, (Pointer<Integer>) mapOfPointers.get(PTR_ROWPTR_B),
					CLEngineConfig.isUSE_DEVICE_MEM()));
			mapOfBuffers.put(PTR_ROWPTR_C, context.createBuffer(Usage.InputOutput, (Pointer<Integer>) mapOfPointers.get(PTR_ROWPTR_C),
					CLEngineConfig.isUSE_DEVICE_MEM()));
			mapOfBuffers.put(PTR_COLDATA_A, context.createBuffer(Usage.Input, (Pointer<Integer>) mapOfPointers.get(PTR_COLDATA_A),
					CLEngineConfig.isUSE_DEVICE_MEM()));
			mapOfBuffers.put(PTR_COLDATA_B, context.createBuffer(Usage.Input, (Pointer<Integer>) mapOfPointers.get(PTR_COLDATA_B),
					CLEngineConfig.isUSE_DEVICE_MEM()));
		} catch (CLException e) {
			clearAllocatedObjects();

			System.err.println(e.toString());
			return false;
		}
		
		System.err.println("Kernel source: " + KERNEL_FILE[0]);
		// Read the program sources and compile them :
		String kernelSource = null;
		try {
			kernelSource = IOUtils.readText(LARCsrSteps.class.getResource(KERNEL_FILE[0]));
		} catch (IOException e) {
			clearAllocatedObjects();

			System.err.println(e.toString());
			return false;
		}
		// System.out.println(kernelSource);

		System.err.println("Create program");
		program.add( context.createProgram(kernelSource) );
		// System.err.println("\t options: " + "-D " + KERNEL_DEFINE_VSIZE + "=" + vectorSize);
		
		// Static input parameters
		program.get(0).defineMacro(D_ROWS_A, mtxOne.getRowCount());
		program.get(0).defineMacro(D_ROWS_B, mtxTwoT.getRowCount());
		
		if (runOn != DeviceFeature.CPU) {
			// Remove unused stuff for this kernel
			program.get(0).addBuildOption("-cl-denorms-are-zero");
			program.get(0).addBuildOption("-cl-finite-math-only");			
		}
		
		System.out.println("ROWS_A " + mtxOne.getRowCount());
		System.out.println("ROWS_B " + mtxTwoT.getRowCount());
		
		kernel.add( program.get(0).createKernel(KERNEL_FUNCTION[0]) );
		this.getkernelStats(kernel.get(0));
		
		// TODO: decide number of rows per core
		int rowsWorkGroup = (int)this.maxKernelWorkgroupSize;
		int rowsDivisor = mtxOne.getRowCount() * rowsWorkGroup / ROWS_PER_CORE;
		int howManyWGs = 1;
		if (rowsDivisor != 0) {
			howManyWGs = mtxOne.getRowCount() / rowsDivisor;
			if ((mtxOne.getRowCount() % rowsDivisor) != 0) {
				howManyWGs++;
			}			
		}
		
		int[] localWorkSize = { rowsWorkGroup };
		int[] globalWorkSize = { howManyWGs*rowsWorkGroup };
		
		System.out.println("getMaxKernelWorkgroupSize " + this.maxKernelWorkgroupSize);
		System.out.println("localWorkSize[0] " + localWorkSize[0]);
		System.out.println("globalWorkSize[0] " + globalWorkSize[0]);
		
		kernel.get(0).setArgs( (CLBuffer<Integer>) mapOfBuffers.get(PTR_ROWPTR_A),
								(CLBuffer<Integer>) mapOfBuffers.get(PTR_COLDATA_A),
								(CLBuffer<Integer>) mapOfBuffers.get(PTR_ROWPTR_B),
								(CLBuffer<Integer>) mapOfBuffers.get(PTR_COLDATA_B),
								(CLBuffer<Integer>) mapOfBuffers.get(PTR_ROWPTR_C),
							ROWS_PER_CORE);
		
		CLEvent evtFinish = this.enqueueKernel(kernel.get(0), localWorkSize, globalWorkSize);
		
		// Debug
		Pointer<Integer> ptrOut = ((CLBuffer<Integer>) mapOfBuffers.get(PTR_ROWPTR_C)).read(queue, evtFinish);
		List<Integer> lRes = PointerUtils.copyFromPointer(ptrOut);
		System.out.println(lRes);
		ptrOut.release();
		evtFinish.release();
		
		return true;
	}	
	
	
	@SuppressWarnings("unchecked")
	public List<Integer> runPrefixScan(int elementSize) {
		System.err.println("Kernel source");
		// Read the program sources and compile them :
		String kernelSource = null;
		try {
			kernelSource = IOUtils.readText(LARCsrSteps.class.getResource(KERNEL_FILE[1]));
		} catch (IOException e) {
			clearAllocatedObjects();

			System.err.println(e.toString());
			return null;
		}
		
		System.err.println("Create program");
		program.add( context.createProgram(kernelSource) );
		// System.err.println("\t options: " + "-D " + KERNEL_DEFINE_VSIZE + "=" + vectorSize);
		
		// Static input parameters
		program.get(1).defineMacro(D_TYPE,"int");
		
		if (runOn != DeviceFeature.CPU) {
			// Remove unused stuff for this kernel
			program.get(1).addBuildOption("-cl-denorms-are-zero");
			program.get(1).addBuildOption("-cl-finite-math-only");			
		}
		
		kernel.add( program.get(1).createKernel(KERNEL_FUNCTION[1]) );
		this.getkernelStats(kernel.get(1));
		
		//
		
		int wgSize = (int)this.maxKernelWorkgroupSize;
		int blockSize = elementSize / wgSize;
		int B = blockSize * wgSize;
		if ((elementSize % wgSize) > 0) { blockSize++; };
		int[] localWorkSize = {wgSize};
		int[] globalWorkSize = { MultipleFind.toMultipleOf(elementSize / wgSize, wgSize)};
		
		System.out.println("getMaxKernelWorkgroupSize " + wgSize);
		System.out.println("blockSize " + blockSize);
		System.out.println("B " + B);
		System.out.println("localWorkSize[0] " + localWorkSize[0]);
		System.out.println("globalWorkSize[0] " + globalWorkSize[0]);
		
		kernel.get(1).setArgs( new LocalSize(this.maxKernelWorkgroupSize*(Integer.SIZE/8)),
							mapOfBuffers.get(PTR_INPUT_KEY),
							B, elementSize, blockSize);
		
		CLEvent evtFinish = this.enqueueKernel(kernel.get(1), localWorkSize, globalWorkSize);
		Pointer<Integer> ptrOut = ((CLBuffer<Integer>) mapOfBuffers.get(PTR_INPUT_KEY)).read(this.queue, evtFinish);
		List<Integer> lRes = PointerUtils.copyFromPointer(ptrOut);		
		ptrOut.release();
		evtFinish.release();	
		
		return lRes;
	}
		
	@SuppressWarnings("unchecked")
	public CsrMatrix runCsrStep2(CsrMatrix mtxOne, CsrMatrix mtxTwo, List<Integer> lRes) {
		CsrMatrix mtxTwoT = mtxTwo.transpose();
		int nnzCount = lRes.get( lRes.size() - 1);
		
		mapOfPointers.put(PTR_DATA_A, Pointer.allocateInts(mtxOne.getData().size()).order(byteOrder));
		mapOfPointers.put(PTR_DATA_B, Pointer.allocateInts(mtxTwoT.getData().size()).order(byteOrder));
		
		PointerUtils.copyToPointer(TransformNumberList.toInteger( mtxOne.getData() ), (Pointer<Integer>) mapOfPointers.get(PTR_DATA_A));
		PointerUtils.copyToPointer(TransformNumberList.toInteger( mtxTwoT.getData() ),(Pointer<Integer>) mapOfPointers.get(PTR_DATA_B));
		
		// Memory
		try {
			mapOfBuffers.put(PTR_DATA_A, context.createBuffer(Usage.Input, (Pointer<Integer>) mapOfPointers.get(PTR_DATA_A),
					CLEngineConfig.isUSE_DEVICE_MEM()));
			mapOfBuffers.put(PTR_DATA_B, context.createBuffer(Usage.Input, (Pointer<Integer>) mapOfPointers.get(PTR_DATA_B),
					CLEngineConfig.isUSE_DEVICE_MEM()));
			
			mapOfBuffers.put(PTR_COLDATA_C, context.createBuffer(Usage.Output, Integer.class, nnzCount));
			mapOfBuffers.put(PTR_DATA_C, context.createBuffer(Usage.Output, Integer.class, nnzCount));
		} catch (CLException e) {
			clearAllocatedObjects();

			System.err.println(e.toString());
			return null;
		}
		
		System.err.println("Kernel source");
		// Read the program sources and compile them :
		String kernelSource = null;
		try {
			kernelSource = IOUtils.readText(LARCsrSteps.class.getResource(KERNEL_FILE[2]));
		} catch (IOException e) {
			clearAllocatedObjects();

			System.err.println(e.toString());
			return null;
		}
		// System.out.println(kernelSource);

		System.err.println("Create program");
		program.add( context.createProgram(kernelSource) );
		// System.err.println("\t options: " + "-D " + KERNEL_DEFINE_VSIZE + "=" + vectorSize);
		
		// Static input parameters
		program.get(2).defineMacro(D_ROWS_A, mtxOne.getRowCount());
		program.get(2).defineMacro(D_ROWS_B, mtxTwoT.getRowCount());
		
		if (runOn != DeviceFeature.CPU) {
			// Remove unused stuff for this kernel
			program.get(2).addBuildOption("-cl-denorms-are-zero");
			program.get(2).addBuildOption("-cl-finite-math-only");			
		}
		
		System.out.println("ROWS_A " + mtxOne.getRowCount());
		System.out.println("ROWS_B " + mtxTwoT.getRowCount());
		
		kernel.add( program.get(2).createKernel(KERNEL_FUNCTION[2]) );
		this.getkernelStats(kernel.get(2));
		
		// TODO: decide number of rows per core
		int rowsWorkGroup = (int)this.maxKernelWorkgroupSize;
		int rowsDivisor = mtxOne.getRowCount() * rowsWorkGroup / ROWS_PER_CORE;
		int howManyWGs = 1;
		if (rowsDivisor != 0) {
			howManyWGs = mtxOne.getRowCount() / rowsDivisor;
			if ((mtxOne.getRowCount() % rowsDivisor) != 0) {
				howManyWGs++;
			}			
		}
		
		int[] localWorkSize = { rowsWorkGroup };
		int[] globalWorkSize = { howManyWGs*rowsWorkGroup };
		
		System.out.println("getMaxKernelWorkgroupSize " + this.maxKernelWorkgroupSize);
		System.out.println("localWorkSize[0] " + localWorkSize[0]);
		System.out.println("globalWorkSize[0] " + globalWorkSize[0]);
		
		kernel.get(0).setArgs( (CLBuffer<Integer>) mapOfBuffers.get(PTR_ROWPTR_A),
				(CLBuffer<Integer>) mapOfBuffers.get(PTR_COLDATA_A),
				(CLBuffer<Integer>) mapOfBuffers.get(PTR_DATA_A),
				(CLBuffer<Integer>) mapOfBuffers.get(PTR_ROWPTR_B),
				(CLBuffer<Integer>) mapOfBuffers.get(PTR_COLDATA_B),
				(CLBuffer<Integer>) mapOfBuffers.get(PTR_DATA_B),
				(CLBuffer<Integer>) mapOfBuffers.get(PTR_ROWPTR_C),
				(CLBuffer<Integer>) mapOfBuffers.get(PTR_COLDATA_C),
				(CLBuffer<Integer>) mapOfBuffers.get(PTR_DATA_C),
			ROWS_PER_CORE);

		CLEvent evtFinish = this.enqueueKernel(kernel.get(2), localWorkSize, globalWorkSize);
		
		// Debug
		Pointer<Integer> ptrColOut = ((CLBuffer<Integer>) mapOfBuffers.get(PTR_COLDATA_C)).read(this.queue, evtFinish);
		List<Integer> lColRes = PointerUtils.copyFromPointer(ptrColOut);
		System.out.println(lColRes);

		Pointer<Integer> ptrDataOut = ((CLBuffer<Integer>) mapOfBuffers.get(PTR_DATA_C)).read(this.queue);
		List<Integer> lDataRes = PointerUtils.copyFromPointer(ptrDataOut);
		System.out.println(lDataRes);
		
		// Release
		// ------
		ptrColOut.release();
		ptrDataOut.release();
		evtFinish.release();
		
		return new CsrMatrix(lRes, lColRes, TransformNumberList.toFloat(lDataRes), mtxOne.getRowshape(), mtxTwoT.getRowshape());		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		CsrMatrix mtxA = CsrMatrix.fromFlattenArray(matrixTest, 5);
		CsrMatrix mtxB = mtxA.transpose();
		
		LARCsrSteps lcsr = new LARCsrSteps();
		lcsr.initQueue();
		lcsr.runCsrStep1(mtxA, mtxB);
		List<Integer> prefixSumRes = lcsr.runPrefixScan(mtxA.getRowPointer().size()); 
		System.out.println( prefixSumRes );		
		
		lcsr.clearAllocatedObjects();
		/*
		 * 		CsrMatrix mtxA = CsrMatrix.fromFlattenArray(matrixTest, 5);
		CsrMatrix mtxB = mtxA.transpose();
		
		CsrStepsKernel cskRun = new CsrStepsKernel(runOn);
		System.out.println("Context: " + cskRun.initContext());
		System.out.println("Queue: " + cskRun.initQueue());
		cskRun.runCsrStep1(mtxA, mtxB);
		List<Integer> prefixSumRes = cskRun.runPrefixScan(mtxA.getRowPointer().size()); 
		System.out.println( prefixSumRes );
		CsrMatrix resultCL = cskRun.runCsrStep2(mtxA, mtxB, prefixSumRes);
		System.out.println(resultCL);
		
		CsrMatrix result = mtxA.multiply(mtxB);
		System.out.println(result);
		
		
		cskRun.releaseAll();
		*/
		
		System.gc();
	}
	
	public static float[] matrixTest = { 0F, 0F, 0F, 2F, 5F,
		0F, 2F, 7F, 8F, 0F,
		1F, 3F, 0F, 0F, 0F,
		4F, 5F, 0F, 0F, 4F,
		0F, 7F, 7F, 0F, 0F };	

}
