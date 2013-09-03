package it.cvdlab.lar.pipeline.kernelwrap.experimental;

import it.cvdlab.lar.model.helpers.PointerUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

import org.bridj.Pointer;

import com.google.common.collect.Maps;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.util.IOUtils;

// LD_PRELOAD = /usr/lib/jvm/java-7-oracle/jre/lib/amd64/libjsig.so

public abstract class RunningKernel {
	private static int KERNEL_MIN_LENGTH = 100;
	private static String KERNEL_KEYWORD = "__kernel";
	private boolean CALL_GC_COLLECT = false;
	
	enum BufferType {
		Integer(Integer.class),
		Float(Float.class),
		Byte(Byte.class);
		
		@SuppressWarnings("rawtypes")
		private Class value;
		
		private BufferType(@SuppressWarnings("rawtypes") Class value) {
			this.value = value;
		}
		
		@SuppressWarnings("rawtypes")
		Class value() {
			return value;
		}
	}
	
	private class TupleBuffer<T> {
		private BufferType type;
		private T buffer;
		
		TupleBuffer(BufferType btType, T mBuffer) {
			type = btType;
			buffer = mBuffer;
		}
		
		BufferType getType() {
			return type;
		}

		T getBuffer() {
			return buffer;
		}		
	}
	
	// Running where
	private DeviceFeature runOn;
	
	// copy to host info
	private boolean copyToDevice;
	
	// To release stuff
	private Map<String,TupleBuffer<CLMem>> buffersRelease;
	@SuppressWarnings("rawtypes")
	private Map<String,TupleBuffer<Pointer>> pointersRelease;
	
	private CLContext context;
	private CLQueue queue;
	private ByteOrder byteOrder;
	private CLProgram program;
	private CLKernel kernel;
	
	CLQueue getQueue() {
		// TODO remove and put memory copy inside this class
		return queue;
	}	

	// stats stuff
	private long availableMemory;
	private long maxAllocation;
	private long maxWorkGroupSize;
	private long maxKernelWorkgroupSize;
	
	// kernel string
	private String kernelSource;
	
	RunningKernel() {
		super();
		//
		buffersRelease = Maps.newHashMap();
		pointersRelease = null;
		setRunOn(DeviceFeature.GPU);
		//
		context = null;
		queue = null;
		byteOrder = null;
		program = null;
		kernel = null;
		//
		kernelSource = null;
	}
	
	RunningKernel(DeviceFeature selectedFeature) {
		this();
		setRunOn(selectedFeature);
	}
	
	long getAvailableMemory() {
		return availableMemory;
	}

	long getMaxAllocation() {
		return maxAllocation;
	}

	long getMaxWorkGroupSize() {
		return maxWorkGroupSize;
	}

	long getMaxKernelWorkgroupSize() {
		return maxKernelWorkgroupSize;
	}	
	
	private void setRunOn(DeviceFeature dv) {
		runOn = dv;
		copyToDevice = (runOn == DeviceFeature.CPU);
	}
	
	boolean initContext() {
		try {
			this.context = JavaCL.createBestContext(runOn);
		} catch(Exception e) {
			e.printStackTrace();
		}
		

		if (this.context == null) {
			clearAllocatedObjects();
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
			return false;
		}
		
		
		this.queue = context.createDefaultQueue();
		if (this.queue == null) {
			clearAllocatedObjects();
			return false;
		}
		
		this.byteOrder = context.getByteOrder();
		
		// init pointers
		this.pointersRelease = Maps.newHashMap();  
		
		return true;
	}

	boolean loadkernelSourceFromFile(URL path) {
		boolean bReturn = false;
		
		try {
			this.kernelSource = IOUtils.readText(path);
			bReturn = true;
		} catch (IOException e) {
			e.printStackTrace();
			bReturn = false;
			this.kernelSource = null;
		}
		
		if (bReturn) {
			bReturn = loadkernelSource();
		}
		
		return bReturn;
	}
	
	boolean loadkernelSourceFromString(String kernelSource) {
		this.kernelSource = kernelSource;
		return loadkernelSource();
	}	
	
	private boolean loadkernelSource() {
		if ( kernelSourceChecks() ) {
			preProcessKernelSource(this.kernelSource);
			return kernelSourceChecks();
		}
		
		return false;
	}
	
	private boolean kernelSourceChecks() {
		if (this.kernelSource == null) {
			return false;
		}

		if (this.kernelSource.length() < KERNEL_MIN_LENGTH) {
			return false;
		}
		
		if ( this.kernelSource.indexOf(KERNEL_KEYWORD) == -1 ) {
			return false;
		}
		
		return true;
	}
	
	abstract void preProcessKernelSource(String theSource);
	
	boolean initProgram(Map<String,String> definesMap, List<String> options) {
		if (! kernelSourceChecks() ) {
			return false;
		}
		
		this.program = this.context.createProgram(this.kernelSource);
		
		if (definesMap != null) {
			addDefines(definesMap);
		}
		
		if (options != null) {
			addBuildOptions(options);
		}
		
		return true;
	}
	
	private void addDefines(Map<String,String> definesMap) {
		for(String key: definesMap.keySet()) {
			this.addDefine(key, definesMap.get(key));
		}
	}
	
	private void addDefine(String name, String value) {
		if (this.program != null) {
			this.program.defineMacro(name, value);
		}
	}
	
	private void addBuildOptions(List<String> options) {
		for(String option: options) {
			addBuildOption(option);
		}
	}
	
	private void addBuildOption(String option) {
		if (this.program != null) {
			this.program.addBuildOption(option);
		}
	}
	
	void addFasterMathOptions() {
		if (this.runOn != DeviceFeature.CPU) {
			addBuildOption("-cl-denorms-are-zero");
			addBuildOption("-cl-finite-math-only");			
		}
	}
	
	boolean initKernel(String kernelFunction) {
		if ( this.program == null ) {
			return false;
		}
		
		this.kernel = program.createKernel(kernelFunction);
		getkernelStats();
		
		return true;
	}
	
	void setKernelArgs(Object ... args) {
		if (this.kernel != null) {
			this.kernel.setArgs(args);
		}
	}
	
	boolean initKernel(String kernelFunction, Object ... args) {
		if ( !initKernel(kernelFunction) ) {
			return false;
		}
		
		setKernelArgs(args);
		
		return true;
	}
	
	void resetKernelAndProgram() {
		if ( kernel != null ) {
			kernel.release();
			
			kernel = null;
		}
		
		if ( program != null ) {
			program.release();
			
			program = null;
		}
		
		this.maxKernelWorkgroupSize = this.maxWorkGroupSize;
	}
	
	private void getkernelStats() {
		this.maxKernelWorkgroupSize = this.maxWorkGroupSize;
		Map<CLDevice, Long> prefsLocal = this.kernel.getPreferredWorkGroupSizeMultiple();
		
		for(CLDevice currDev : prefsLocal.keySet()) {
			this.maxKernelWorkgroupSize = Math.min(this.maxKernelWorkgroupSize, prefsLocal.get(currDev));
		}
	}
	
	CLEvent enqueueKernel(int[] locSize, int[] wgSize) {
		if (locSize == null) {
			return this.kernel.enqueueNDRange(queue, wgSize);
		} else {
			return this.kernel.enqueueNDRange(queue, wgSize, locSize);
		}
	}
	
	private <T extends Number> Pointer<T> createNewPointer(long length, Class<T> type) {
		return Pointer.allocateArray(type, length).order(byteOrder);
	}
	
	@SuppressWarnings("unchecked")
	<T> Pointer<T> createNewPointer(long length, BufferType dataType) {
		return createNewPointer(length, dataType.value());
	}
	
	@SuppressWarnings({ "rawtypes" })
	private <T extends Number> Pointer<T> setPointer(String key, Pointer<T> pointerData, BufferType btType) {
		if (this.pointersRelease.containsKey(key)) {
			return null;
		}
		
		this.pointersRelease.put(key, new TupleBuffer<Pointer>(btType, pointerData));
		return pointerData; 
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Pointer createNewPointer(String key, long length, BufferType bType) {
		Pointer currPtr = createNewPointer(length, bType.value() );
		if (setPointer(key, currPtr, bType) == null) {
			currPtr.release();
			return null;
		}
		
		return currPtr;
	}

	Pointer<Integer> createNewPointerInteger(String key, List<Integer> data) {
		Pointer<Integer> currPtr = createNewPointerInteger(key, data.size());
		if (currPtr == null) {
			return null;
		}
		
		PointerUtils.copyToPointer(data, currPtr);
		return currPtr;
	}
	
	@SuppressWarnings("unchecked")
	Pointer<Integer> createNewPointerInteger(String key, long length) {
		return createNewPointer(key, length, BufferType.Integer);
	}
	
	Pointer<Float> createNewPointerFloat(String key, List<Float> data) {
		Pointer<Float> currPtr = createNewPointerFloat(key, data.size());
		if (currPtr == null) {
			return null;
		}
		
		PointerUtils.copyToPointer(data, currPtr);
		return currPtr;
	}	
	
	@SuppressWarnings("unchecked")
	Pointer<Float> createNewPointerFloat(String key, long length) {
		return createNewPointer(key, length, BufferType.Float);
	}
	
	Pointer<Byte> createNewPointerByte(String key, List<Byte> data) {
		Pointer<Byte> currPtr = createNewPointerByte(key, data.size());
		if (currPtr == null) {
			return null;
		}
		
		PointerUtils.copyToPointer(data, currPtr);
		return currPtr;
	}		
	
	@SuppressWarnings("unchecked")
	Pointer<Byte> createNewPointerByte(String key, long length) {
		return createNewPointer(key, length, BufferType.Byte);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Number> Pointer<T> getPointer(String key) {
		return (Pointer<T>) this.pointersRelease.get(key).getBuffer();
	}
	
	Pointer<Integer> getPointerInteger(String key) {
		return getPointer(key);
	}
	
	Pointer<Float> getPointerFloat(String key) {
		return getPointer(key);
	}
	
	Pointer<Byte> getPointerByte(String key) {
		return getPointer(key);
	}		
	
	private <T extends Number> CLBuffer<T> setupInputBuffer(Pointer<T> origin) {
		return context.createBuffer(Usage.InputOutput, origin, copyToDevice);
	}
	
	private <T extends Number> CLBuffer<T> setupOutputBuffer(long length, Class<T> inputType) {
		return context.createBuffer(Usage.Output, inputType, length);
	}
	
	@SuppressWarnings("rawtypes")
	CLMem createInputMemoryBuffer(String pointerKey) {
		TupleBuffer<Pointer> ptrCurr = this.pointersRelease.get(pointerKey);
		if (ptrCurr == null) {
			return null;
		}
		
		switch(ptrCurr.getType()) {
			case Integer:
				this.buffersRelease.put(pointerKey, new TupleBuffer<CLMem>(ptrCurr.getType(), setupInputBuffer(getPointerInteger(pointerKey))));
				
			case Float:
				this.buffersRelease.put(pointerKey, new TupleBuffer<CLMem>(ptrCurr.getType(), setupInputBuffer(getPointerFloat(pointerKey)))).getBuffer();
				
			case Byte:
				this.buffersRelease.put(pointerKey, new TupleBuffer<CLMem>(ptrCurr.getType(), setupInputBuffer(getPointerByte(pointerKey)))).getBuffer();				
		}

		return this.buffersRelease.get(pointerKey).getBuffer();
	}
	
	@SuppressWarnings("unchecked")
	private CLMem createOutputMemoryBuffer(String key, long len, BufferType bType) {
		if (this.buffersRelease.containsKey(key)) {
			return null;
		}
		
		this.buffersRelease.put(key,new TupleBuffer<CLMem>( bType, setupOutputBuffer(len, bType.value()) ));
		return this.buffersRelease.get(key).getBuffer();
	}
	
	@SuppressWarnings("unchecked")
	CLBuffer<Integer>  createOutputMemoryBufferInteger(String key, long len) {
		return (CLBuffer<Integer>) createOutputMemoryBuffer(key, len, BufferType.Integer);
	}
	
	@SuppressWarnings("unchecked")
	CLBuffer<Float> createOutputMemoryBufferFloat(String key, long len) {
		return (CLBuffer<Float>) createOutputMemoryBuffer(key, len, BufferType.Float);
	}
	
	@SuppressWarnings("unchecked")
	CLBuffer<Byte> createOutputMemoryBufferByte(String key, long len) {
		return (CLBuffer<Byte>) createOutputMemoryBuffer(key, len, BufferType.Byte);
	}	
	
	@SuppressWarnings("unchecked")
	private <T extends Number> CLBuffer<T> getBuffer(String key) {
		return (CLBuffer<T>) this.buffersRelease.get(key).getBuffer();
	}
	
	CLBuffer<Integer> getBufferInteger(String key) {
		return getBuffer(key);
	}
	
	CLBuffer<Float> getBufferFloat(String key) {
		return getBuffer(key);
	}
	
	CLBuffer<Byte> getBufferByte(String key) {
		return getBuffer(key);
	}
	
	void releaseAll(CLEvent ... events) {
		if (events != null) {
			for(CLEvent event : events) {
				event.release();
			}			
		}
		
		releaseAll();
	}
	
	void releaseAll() {
		clearAllocatedObjects();
	}	
	
	private void clearAllocatedObjects() {
		if (queue != null) {
			queue.flush();
			queue.release();
			
			queue = null;
		}

		resetKernelAndProgram();
		
		clearAllocatedCLObjects();
		clearAllocatedPTRObjects();
		
		if (context != null) {
			context.release();
			
			context = null;
		}
		
		if ( CALL_GC_COLLECT ) {
			System.gc();
		}
	}
	
	private void clearAllocatedCLObjects() {
		System.err.println("Clearing CLMEM");
		for(String buffObject: this.buffersRelease.keySet()) {
			this.buffersRelease.get(buffObject).getBuffer().release();
		}
		this.buffersRelease.clear();
	}

	private void clearAllocatedPTRObjects() {
		System.err.println("Clearing POINTERS");
		for(String buffObject: this.pointersRelease.keySet()) {
			this.pointersRelease.get(buffObject).getBuffer().release();
		}
		this.pointersRelease.clear();
	}		
}