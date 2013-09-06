package it.cvdlab.lar.pipeline.kernelwrap.experimental;

import it.cvdlab.lar.model.CsrMatrix;
import it.cvdlab.lar.model.helpers.PointerUtils;
import it.cvdlab.lar.pipeline.helpers.MultipleFind;
import it.cvdlab.lar.pipeline.helpers.TransformNumberList;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bridj.Pointer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.opencl.LocalSize;

public class CsrStepsKernel extends RunningKernel {
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
		
		System.out.println("ROWS_A " + mtxOne.getRowCount());
		System.out.println("ROWS_B " + mtxTwoT.getRowCount());
		
		this.loadkernelSourceFromFile(CsrStepsKernel.class.getResource(KERNEL_FILE[0]));
		this.initProgram(defineMap, null);
		this.addFasterMathOptions();
		this.initKernel(KERNEL_FUNCTION[0]);
		
		// TODO: decide number of rows per core
		int rowsWorkGroup = (int)this.getMaxKernelWorkgroupSize();
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
		
		System.out.println("getMaxKernelWorkgroupSize " + this.getMaxKernelWorkgroupSize());
		System.out.println("localWorkSize[0] " + localWorkSize[0]);
		System.out.println("globalWorkSize[0] " + globalWorkSize[0]);
		
		this.setKernelArgs( this.getBufferInteger(PTR_ROWPTR_A),
							this.getBufferInteger(PTR_COLDATA_A),
							this.getBufferInteger(PTR_ROWPTR_B),
							this.getBufferInteger(PTR_COLDATA_B),
							this.getBufferInteger(PTR_ROWPTR_C),
							ROWS_PER_CORE);
		
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
							this.getBufferInteger(PTR_INPUT_KEY),
							B, elementSize, blockSize);
		
		CLEvent evtFinish = this.enqueueKernel(localWorkSize, globalWorkSize);
		Pointer<Integer> ptrOut = this.getBufferInteger(PTR_INPUT_KEY).read(this.getQueue(), evtFinish);
		List<Integer> lRes = PointerUtils.copyFromPointer(ptrOut);
		
		ptrOut.release();
		this.resetKernelAndProgram(evtFinish);
		evtFinish.release();	
		
		return lRes;
	}
	
	public CsrMatrix runCsrStep2(CsrMatrix mtxOne, CsrMatrix mtxTwo, List<Integer> lRes) {
		CsrMatrix mtxTwoT = mtxTwo.transpose();
		int nnzCount = lRes.get( lRes.size() - 1);
		
		this.createNewPointerInteger(PTR_DATA_A, TransformNumberList.toInteger( mtxOne.getData() ));
		this.createInputMemoryBuffer(PTR_DATA_A);
		this.createNewPointerInteger(PTR_DATA_B, TransformNumberList.toInteger( mtxTwoT.getData() ));
		this.createInputMemoryBuffer(PTR_DATA_B);
		
		this.createOutputMemoryBufferInteger(PTR_COLDATA_C, nnzCount);
		this.createOutputMemoryBufferInteger(PTR_DATA_C, nnzCount);
		
		Map<String,String> defineMap = Maps.newHashMap();
		defineMap.put(D_ROWS_A, String.valueOf( mtxOne.getRowCount() ) );
		defineMap.put(D_ROWS_B, String.valueOf( mtxTwoT.getRowCount() ) );
		
		this.loadkernelSourceFromFile(CsrStepsKernel.class.getResource(KERNEL_FILE[2]));
		this.initProgram(defineMap, null);
		this.addFasterMathOptions();
		this.initKernel(KERNEL_FUNCTION[2]);
		
		// TODO: decide number of rows per core
		int rowsWorkGroup = (int)this.getMaxKernelWorkgroupSize();
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
		
		
		System.out.println("getMaxKernelWorkgroupSize " + this.getMaxKernelWorkgroupSize());
		System.out.println("localWorkSize[0] " + localWorkSize[0]);
		System.out.println("globalWorkSize[0] " + globalWorkSize[0]);
		
		this.setKernelArgs( this.getBufferInteger(PTR_ROWPTR_A),
							this.getBufferInteger(PTR_COLDATA_A),
							this.getBufferInteger(PTR_DATA_A),
							this.getBufferInteger(PTR_ROWPTR_B),
							this.getBufferInteger(PTR_COLDATA_B),
							this.getBufferInteger(PTR_DATA_B),
							this.getBufferInteger(PTR_ROWPTR_C),
							this.getBufferInteger(PTR_COLDATA_C),
							this.getBufferInteger(PTR_DATA_C),
							ROWS_PER_CORE);
		
		CLEvent evtFinish = this.enqueueKernel(localWorkSize, globalWorkSize);
		
		// Debug
		Pointer<Integer> ptrColOut = this.getBufferInteger(PTR_COLDATA_C).read(this.getQueue(), evtFinish);
		List<Integer> lColRes = PointerUtils.copyFromPointer(ptrColOut);
		System.out.println(lColRes);

		Pointer<Integer> ptrDataOut = this.getBufferInteger(PTR_DATA_C).read(this.getQueue());
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
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		CsrMatrix mtxA = CsrMatrix.fromFlattenArray(matrixTest, 5);
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
	}
	
	public static float[] matrixTest = { 0F, 0F, 0F, 2F, 5F,
										0F, 2F, 7F, 8F, 0F,
										1F, 3F, 0F, 0F, 0F,
										4F, 5F, 0F, 0F, 4F,
										0F, 7F, 7F, 0F, 0F };
}
