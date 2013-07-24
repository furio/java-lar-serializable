package it.cvdlab.lar.pipeline.kernelwrap;

import it.cvdlab.lar.model.CsrMatrix;
import it.cvdlab.lar.model.helpers.CLEngineConfig;
import it.cvdlab.lar.model.helpers.PointerUtils;
import it.cvdlab.lar.pipeline.helpers.ResponseTime;
import it.cvdlab.lar.pipeline.helpers.ResultTuple;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;

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

public class LARTestBinaryContinuos {
	private static DeviceFeature runOn = DeviceFeature.CPU;
	private static String KERNEL_FILE = "larnewfullbinary_modulo_offset.cl";
	private static String KERNEL_FUNCTION = "many_vec_mul_bitwise_binary"; // "many_vec_mul_local_bitwise"
	
	private static String D_ROWSIZE = "ROWSIZE";
	private static String D_OLDVECTORSIZE = "OLDVECTORSIZE";
	private static String D_INPUTVECTORSIZE = "INPUTVECTORSIZE";
	private static String D_TOTALVECTORSSIZE = "TOTALVECTORSSIZE";
	
	private static long TOTAL_MEMORY = 0;
	private static long RESULT_VECTOR_SIZE = 0;

	public static List<ResultTuple> clMultiply(CsrMatrix matrixA, int[] vector, int vectorSize, int oldVectorSize) {
		int howManyVectors = vector.length / vectorSize;
		int howManyResultVectors = 0;
		System.out.println("Vector count: " + howManyVectors);
		System.out.println("Single vector size: " + vectorSize);
		System.out.println("Row count: " + matrixA.getRowCount());
		
		// Stora i risultati
		List<ResultTuple> lsOutput = Lists.newArrayList();
		
		// Vettore risultato è grosso
		RESULT_VECTOR_SIZE = matrixA.getRowCount() * (Integer.SIZE / 8);

		// Lista di CL buffer da deallocare
		List<CLMem> buffersRelease = Lists.newArrayList();
		@SuppressWarnings("rawtypes")
		List<Pointer> pointersRelease = Lists.newArrayList();

		long startTime = System.currentTimeMillis();

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
		}
		System.out.println("Max Wg Size: " + maxWorkGroupSize);

		CLQueue queue = context.createDefaultQueue();
		ByteOrder byteOrder = context.getByteOrder();

		// Native memory
		Pointer<Integer> vector_data = null;
		Pointer<Integer> matA_rowptr, matA_colindices;

		System.err.println("PTR Buffers");
		// Allocate
		matA_rowptr = Pointer.allocateInts(matrixA.getRowptr().size()).order(byteOrder);
		pointersRelease.add(matA_rowptr);
		matA_colindices = Pointer.allocateInts(matrixA.getColdata().size()).order(byteOrder);
		pointersRelease.add(matA_colindices);
		vector_data = Pointer.allocateInts(vector.length).order(byteOrder);
		pointersRelease.add(vector_data);

		PointerUtils.copyToPointer(matrixA.getRowptr(), matA_rowptr);
		PointerUtils.copyToPointer(matrixA.getColdata(), matA_colindices);
		PointerUtils.copyToPointer(Ints.asList(vector), vector_data);

		// CLBuffers
		CLBuffer<Integer> cl_matA_rowptr = null, cl_matA_colindices = null , cl_vector_data = null;
		CLBuffer<Integer> cl_output_data = null;

		System.err.println("CL Buffers");
		try {
			cl_matA_rowptr = context.createBuffer(Usage.Input, matA_rowptr,
					CLEngineConfig.isUSE_DEVICE_MEM());
			TOTAL_MEMORY -= cl_matA_rowptr.getByteCount();
			buffersRelease.add(cl_matA_rowptr);
			cl_matA_colindices = context.createBuffer(Usage.Input,
					matA_colindices, CLEngineConfig.isUSE_DEVICE_MEM());
			TOTAL_MEMORY -= cl_matA_colindices.getByteCount();
			buffersRelease.add(cl_matA_colindices);

			cl_vector_data = context.createBuffer(Usage.Input, vector_data,
					CLEngineConfig.isUSE_DEVICE_MEM());
			TOTAL_MEMORY -= cl_vector_data.getByteCount();
			buffersRelease.add(cl_vector_data);

			// Output buffer
			howManyResultVectors = (int) (TOTAL_MEMORY/RESULT_VECTOR_SIZE);
			System.out.println("Computable vectors: " + howManyResultVectors);
			cl_output_data = context.createIntBuffer(Usage.Output, matrixA.getRowCount() * howManyResultVectors);
			buffersRelease.add(cl_output_data);
		} catch (CLException e) {
			queue.flush();
			queue.release();
			clearAllocatedCLObjects(buffersRelease);
			clearAllocatedPTRObjects(pointersRelease);
			context.release();

			System.err.println(e.toString());
			return null;
		}

		System.err.println("Kernel source");
		// Read the program sources and compile them :
		String kernelSource = null;
		try {
			kernelSource = IOUtils.readText(LARTestBinaryContinuos.class.getResource(KERNEL_FILE));
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
		program.addBuildOption("-D" + D_ROWSIZE + "=" + matrixA.getRowCount());
		program.addBuildOption("-D" + D_OLDVECTORSIZE + "=" + oldVectorSize);
		program.addBuildOption("-D" + D_INPUTVECTORSIZE + "=" + vectorSize);
		program.addBuildOption("-D" + D_TOTALVECTORSSIZE + "=" + howManyVectors);
		
		if (runOn != DeviceFeature.CPU) {
			// Remove unused stuff for this kernel
			program.addBuildOption("-cl-denorms-are-zero");
			program.addBuildOption("-cl-finite-math-only");			
		}

		// Get and call the kernel :
		System.err.println("Create kernel");
		CLKernel multiplyMatrixKernel = null;
		
		int vectorsToCompute = howManyVectors;
		for(int i = 0; i < howManyVectors; i += howManyResultVectors, vectorsToCompute -= howManyResultVectors) {
			multiplyMatrixKernel = program.createKernel(KERNEL_FUNCTION);
			
			if (KERNEL_FUNCTION.indexOf("local") != -1) {
				System.err.println("Adding local cache");
				multiplyMatrixKernel.setArgs(cl_matA_rowptr, cl_matA_colindices,
					cl_vector_data, cl_output_data, i,
					new LocalSize(vectorSize*(Integer.SIZE/8)));
			} else {
				System.err.println("No local cache");
				multiplyMatrixKernel.setArgs(cl_matA_rowptr, cl_matA_colindices,
					cl_vector_data, cl_output_data, i);
			}
			
			int[] wgSize;
			int[] locSize;
			int multipleGroupSize = Math.min(howManyResultVectors, vectorsToCompute);
			
			// Math.max(multipleWorkGroup, howManyVectors) ==> prendi il multiplo o i vettori
			if (true && (runOn == DeviceFeature.CPU)) {
				wgSize = new int[]{ multipleGroupSize };
				locSize = null;
			} else {
				wgSize = new int[]{ (int) (multipleGroupSize * maxWorkGroupSize) }; 
				locSize = new int[]{ (int) maxWorkGroupSize };			
			}
			
			System.err.println("WgSize: " + wgSize[0] + " - LocalSize: " + ((locSize == null) ? 0 : locSize[0]));			
			
			// queue.finish();
			CLEvent addEvt = null;
			long kernelTime = System.currentTimeMillis();
			if (true && (runOn == DeviceFeature.CPU)) {
				System.err.println("EnqueueND Range - wgSize");
				addEvt = multiplyMatrixKernel.enqueueNDRange(queue, wgSize);
			} else {
				System.err.println("EnqueueND Range - wgSize+locSize");
				addEvt = multiplyMatrixKernel
						.enqueueNDRange(queue, wgSize, locSize);
			}
			ResponseTime rtCount = new ResponseTime(kernelTime);
			addEvt.setCompletionCallback(rtCount);

			Pointer<Integer> matrixDataOut = cl_output_data.read(queue, addEvt);
			if (i == 0) {
				pointersRelease.add(matrixDataOut);
			}
			
			// Pointer<Float> matrixDataOut =
			// Pointer.allocateFloats(matrixA.getRowCount()*matrixBToTranspose.getColCount()).order(byteOrder);
			// cl_output_data.read(queue, matrixDataOut, true, addEvt);

			List<Integer> listMatrixOut = PointerUtils.copyFromPointerInteger(matrixDataOut);
			
			addEvt.release();
			multiplyMatrixKernel.release();
			
			lsOutput.add(new ResultTuple(listMatrixOut, rtCount.elapsedTime()));
		}

		queue.flush();
		queue.release();
		
		program.release();
		clearAllocatedCLObjects(buffersRelease);
		clearAllocatedPTRObjects(pointersRelease);
		context.release();

		// System.out.println(listMatrixOut);
		System.err.println("Calculated in: "
				+ (System.currentTimeMillis() - startTime) + " millis");

		// System.out.println(listMatrixOut);

		return lsOutput;
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

/*
	public static void main(String[] args) {
		int[] matrixOne = new int[] { 1, 0, 0, 0, 1,
									  0, 1, 0, 0, 0, 
									  0, 0, 0, 0, 1, 
									  0, 0, 1, 1, 0, 
									  1, 0, 0, 0, 1 };
		
		CsrMatrix csrMatrixOne = CsrMatrix.fromFlattenArray(matrixOne, 5);
		System.out.println(csrMatrixOne.toString());
		
		int[] vettori = new int[] { 1, 1, 1, 0, 1,
										0, 1, 0, 0, 0,
										0, 0, 1, 0, 0,
										0, 0, 0, 1, 0,
										0, 0, 0, 0, 1,
										1, 1, 1, 0, 1};
		
		//clMultiply(csrMatrixOne, vettori, 5);
		
		// System.out.println(Integer.SIZE);

		int[] flatResult = ArrayUtils.flatten(BinaryTranslator.fromFlatArray(vettori, 5));
		System.out.println(Arrays.toString(flatResult));
		
		clMultiply(csrMatrixOne, flatResult, 1, 5);
	}
*/
	

}
