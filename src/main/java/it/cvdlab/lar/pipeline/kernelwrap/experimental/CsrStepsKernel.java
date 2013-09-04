package it.cvdlab.lar.pipeline.kernelwrap.experimental;

import it.cvdlab.lar.model.CsrMatrix;
import it.cvdlab.lar.model.helpers.PointerUtils;
import it.cvdlab.lar.pipeline.helpers.MultipleFind;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bridj.Pointer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.LocalSize;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;

public class CsrStepsKernel extends RunningKernel {
	private static DeviceFeature runOn = DeviceFeature.GPU;
	
	private static String[] KERNEL_FILE = {"csr_run_1.cl", "prefixsum.cl", "csr_run_2.cl"};
	private static String[] KERNEL_FUNCTION = {"m_mul_m_count","kernel__scan_block_anylength","m_mul_m"};
	
	private static String D_TYPE = "T";
	private static String D_ROWS_A = "ROWS_A";
	private static String D_ROWS_B = "ROWS_B";
	
	private static String PTR_INPUT_KEY = "input";
	
	private static String PTR_ROWPTR_A = "rowptr_a";
	private static String PTR_ROWPTR_B = "rowptr_b";
	private static String PTR_ROWPTR_C = "rowptr_c";
	private static String PTR_COLDATA_A = "coldata_a";
	private static String PTR_COLDATA_B = "coldata_b";
	private static String PTR_COLDATA_C = "coldata_c";
	private static String PTR_DATA_A = "data_a";
	private static String PTR_DATA_B = "data_b";
	private static String PTR_DATA_C = "data_c";

	public CsrStepsKernel(DeviceFeature runOn2) {
		super(runOn2);
	}

	@Override
	void preProcessKernelSource(String theSource) {
		// TODO Auto-generated method stub
		
	}
	
	public void runCsrStep1(CsrMatrix mtxOne, CsrMatrix mtxTwo) {
		CsrMatrix mtxTwoT = mtxTwo.transpose();
		
		this.createNewPointerInteger(PTR_ROWPTR_A, mtxOne.getRowPointer());
		this.createInputMemoryBuffer(PTR_ROWPTR_A);
		this.createNewPointerInteger(PTR_ROWPTR_B, mtxTwoT.getRowPointer());
		this.createInputMemoryBuffer(PTR_ROWPTR_B);
		this.createNewPointerInteger(PTR_COLDATA_A, mtxOne.getColdata());
		this.createInputMemoryBuffer(PTR_COLDATA_A);
		this.createNewPointerInteger(PTR_COLDATA_B, mtxTwoT.getColdata());
		this.createInputMemoryBuffer(PTR_COLDATA_B);
		
		List<Integer> lstOutput = Lists.newArrayList(Collections.nCopies(mtxOne.getRowPointer().size(), 0));
		this.createNewPointerInteger(PTR_ROWPTR_C, lstOutput);
		this.createInputOutputMemoryBuffer(PTR_ROWPTR_C);
		
		Map<String,String> defineMap = Maps.newHashMap();
		defineMap.put(D_ROWS_A, String.valueOf( mtxOne.getRowCount() ) );
		defineMap.put(D_ROWS_B, String.valueOf( mtxTwoT.getRowCount() ) );
		
		this.loadkernelSourceFromFile(CsrStepsKernel.class.getResource(KERNEL_FILE[0]));
		this.initProgram(defineMap, null);
		this.addFasterMathOptions();
		this.initKernel(KERNEL_FUNCTION[0]);
		
		int rowsWorkGroup = (int)this.getMaxKernelWorkgroupSize();
		int[] localWorkSize = { rowsWorkGroup };
		int[] globalWorkSize = { MultipleFind.toMultipleOf(mtxOne.getRowCount() / rowsWorkGroup, rowsWorkGroup)};
		// TODO: decide number of rows per core
		
		System.out.println("getMaxKernelWorkgroupSize " + this.getMaxKernelWorkgroupSize());
		System.out.println("localWorkSize[0] " + localWorkSize[0]);
		System.out.println("globalWorkSize[0] " + globalWorkSize[0]);
		
		this.setKernelArgs( this.getBufferInteger(PTR_ROWPTR_A),
							this.getBufferInteger(PTR_COLDATA_A),
							this.getBufferInteger(PTR_ROWPTR_B),
							this.getBufferInteger(PTR_COLDATA_B),
							this.getBufferInteger(PTR_ROWPTR_C),
							1);
		
		CLEvent evtFinish = this.enqueueKernel(localWorkSize, globalWorkSize);
		
		// Debug
		Pointer<Integer> ptrOut = this.getBufferInteger(PTR_ROWPTR_C).read(this.getQueue(), evtFinish);
		List<Integer> lRes = PointerUtils.copyFromPointer(ptrOut);
		System.out.println(lRes);
		
		// Release
		// ------
		
		this.resetKernelAndProgram(evtFinish);
		evtFinish.release();
		
		return;
	}

	public List<Integer> runPrefixScan(int elementSize) {
		Map<String,String> defineMap = Maps.newHashMap();
		defineMap.put(D_TYPE, "int");
		
		this.loadkernelSourceFromFile(CsrStepsKernel.class.getResource(KERNEL_FILE[1]));
		this.initProgram(defineMap, null);
		this.addFasterMathOptions();
		this.initKernel(KERNEL_FUNCTION[1]);
		
		int wgSize = (int)this.getMaxKernelWorkgroupSize();
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
		
		this.setKernelArgs( new LocalSize(this.getMaxKernelWorkgroupSize()*(Integer.SIZE/8)),
							this.getBufferInteger(PTR_ROWPTR_C),
							B, elementSize, blockSize);
		
		CLEvent evtFinish = this.enqueueKernel(localWorkSize, globalWorkSize);
		Pointer<Integer> ptrOut = this.getBufferInteger(PTR_ROWPTR_C).read(this.getQueue(), evtFinish);
		List<Integer> lRes = PointerUtils.copyFromPointer(ptrOut);
		
		ptrOut.release();
		this.resetKernelAndProgram(evtFinish);
		evtFinish.release();	
		
		return lRes;
	}
	
	public void runCsrStep2(CsrMatrix mtxOne, CsrMatrix mtxTwo) {
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CsrStepsKernel cskRun = new CsrStepsKernel(runOn);
		System.out.println("Context: " + cskRun.initContext());
		System.out.println("Queue: " + cskRun.initQueue());
		
		cskRun.releaseAll();
	}	
}
