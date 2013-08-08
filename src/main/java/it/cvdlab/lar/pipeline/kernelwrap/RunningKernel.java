package it.cvdlab.lar.pipeline.kernelwrap;

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

public abstract class RunningKernel {
	private static int KERNEL_MIN_LENGTH = 100;
	private static String KERNEL_KEYWORD = "__kernel";
	private boolean CALL_GC_COLLECT = false;
	
	// Running where
	private DeviceFeature runOn;
	
	// copy to host info
	private boolean copyToDevice;
	
	// To release stuff
	private Map<String,CLMem> buffersRelease;
	private PointerMap pointersRelease;
	
	private CLContext context;
	private CLQueue queue;
	private ByteOrder byteOrder;
	private CLProgram program;
	private CLKernel kernel;

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
		this.context = JavaCL.createBestContext(runOn);

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
		this.pointersRelease= new PointerMap();  
		
		return true;
	}

	boolean loadkernelSourceFromFile(URL path) {
		boolean bReturn = false;
		
		try {
			this.kernelSource = IOUtils.readText(path);
			bReturn = true;
		} catch (IOException e) {

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
		addDefines(definesMap);
		addBuildOptions(options);
		
		return true;
	}
	
	private void addDefines(Map<String,String> definesMap) {
		for(String key: definesMap.keySet()) {
			this.addDefine(key, definesMap.get(key));
		}
	}
	
	private void addDefine(String name, String value) {
		if (this.program != null) {
			this.program.addBuildOption("-D" + name + "=" + value);
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
	
	boolean initKernel(String kernelFunction, Object ... args) {
		if ( this.program == null ) {
			return false;
		}
		
		this.kernel = program.createKernel(kernelFunction);
		this.kernel.setArgs(args);
		getkernelStats();
		
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
		Map<CLDevice, Long> prefsLocal = this.kernel.getWorkGroupSize();
		
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
	
	
	// TODO: pointer and buffer alloc
	Pointer<Integer> createNewPointerInteger(String key, long length) {
		Pointer<Integer> currPtr = Pointer.allocateInts(length).order(byteOrder);
		if (pointersRelease.setPointerInteger(key, currPtr) != null) {
			currPtr.release();
			return null;
		}
		
		return currPtr;
	}
	
	Pointer<Float> createNewPointerFloat(String key, long length) {
		Pointer<Float> currPtr = Pointer.allocateFloats(length).order(byteOrder);
		if (pointersRelease.setPointerFloat(key, currPtr) != null) {
			currPtr.release();
			return null;
		}
		
		return currPtr;
	}
	
	Pointer<Byte> createNewPointerByte(String key, long length) {
		Pointer<Byte> currPtr = Pointer.allocateBytes(length).order(byteOrder);
		if (pointersRelease.setPointerByte(key, currPtr) != null) {
			currPtr.release();
			return null;
		}
		
		return currPtr;
	}
	
	Pointer<Integer> getPointerInteger(String key) {
		return this.pointersRelease.getPointerInteger(key);
	}
	
	Pointer<Float> getPointerFloat(String key) {
		return this.pointersRelease.getPointerFloat(key);
	}
	
	Pointer<Byte> getPointerByte(String key) {
		return this.pointersRelease.getPointerByte(key);
	}		
	
	CLMem createInputMemoryBuffer(String pointerKey) {
		@SuppressWarnings("rawtypes")
		Pointer currPtr = getPointerInteger(pointerKey);
		
		if (currPtr != null) {
			return this.buffersRelease.put(pointerKey, setupInputIntegerBuffer(getPointerInteger(pointerKey)) );
		}
		
		currPtr = getPointerFloat(pointerKey);
		if (currPtr != null) {
			return this.buffersRelease.put(pointerKey, setupInputFloatBuffer(getPointerFloat(pointerKey)) );
		}
		
		currPtr = getPointerByte(pointerKey);
		if (currPtr != null) {
			return this.buffersRelease.put(pointerKey, setupInputByteBuffer(getPointerByte(pointerKey)) );
		}		
		
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	CLBuffer<Integer>  createOutputMemoryBufferInteger(String key, long len) {
		if (this.buffersRelease.containsKey(key)) {
			return null;
		}
		
		return (CLBuffer<Integer>) this.buffersRelease.put(key, setupOutputIntegerBuffer(len) );
	}
	
	@SuppressWarnings("unchecked")
	CLBuffer<Float> createOutputMemoryBufferFloat(String key, long len) {
		if (this.buffersRelease.containsKey(key)) {
			return null;
		}
		
		return (CLBuffer<Float>) this.buffersRelease.put(key, setupOutputFloatBuffer(len) );
	}
	
	@SuppressWarnings("unchecked")
	CLBuffer<Byte> createOutputMemoryBufferByte(String key, long len) {
		if (this.buffersRelease.containsKey(key)) {
			return null;
		}
		
		return (CLBuffer<Byte>) this.buffersRelease.put(key, setupOutputByteBuffer(len) );
	}	
	
	@SuppressWarnings("unchecked")
	CLBuffer<Integer> getBufferInteger(String key) {
		return (CLBuffer<Integer>) this.buffersRelease.get(key);
	}
	
	@SuppressWarnings("unchecked")
	CLBuffer<Float> getBufferFloat(String key) {
		return (CLBuffer<Float>) this.buffersRelease.get(key);
	}
	
	@SuppressWarnings("unchecked")
	CLBuffer<Byte> getBufferByte(String key) {
		return (CLBuffer<Byte>) this.buffersRelease.get(key);
	}	
	
	private CLBuffer<Integer> setupInputIntegerBuffer(Pointer<Integer> origin) {
		return context.createBuffer(Usage.Input, origin, copyToDevice);
	}
	
	private CLBuffer<Float> setupInputFloatBuffer(Pointer<Float> origin) {
		return context.createBuffer(Usage.Input, origin, copyToDevice);
	}
	
	private CLBuffer<Byte> setupInputByteBuffer(Pointer<Byte> origin) {
		return context.createBuffer(Usage.Input, origin, copyToDevice);
	}
	
	private CLBuffer<Integer> setupOutputIntegerBuffer(long length) {
		return context.createIntBuffer(Usage.Output, length);
	}
	
	private CLBuffer<Float> setupOutputFloatBuffer(long length) {
		return context.createFloatBuffer(Usage.Output, length);
	}
	
	private CLBuffer<Byte> setupOutputByteBuffer(long length) {
		return context.createByteBuffer(Usage.Output, length);
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
			this.buffersRelease.get(buffObject).release();
		}
		this.buffersRelease.clear();
	}

	private void clearAllocatedPTRObjects() {
		System.err.println("Clearing POINTERS");
		this.pointersRelease.clearAllocatedPTRObjects();
	}		
}

class PointerMap {
	private Map<String, Pointer<Integer>> integerMap;
	private Map<String, Pointer<Float>> floatMap;
	private Map<String, Pointer<Byte>> byteMap;
	
	
	PointerMap() {
		super();
		integerMap = Maps.newHashMap();
		floatMap = Maps.newHashMap();
		byteMap = Maps.newHashMap();
	}
	
	private boolean checkKeyUniqueness(String key) {
		return !(this.integerMap.containsKey(key) || this.floatMap.containsKey(key) || this.byteMap.containsKey(key));
	}
	
	Pointer<Integer> setPointerInteger(String key, Pointer<Integer> ptr) {
		return checkKeyUniqueness(key) ? this.integerMap.put(key,ptr) : null;
	}
	
	Pointer<Float> setPointerFloat(String key, Pointer<Float> ptr) {
		return checkKeyUniqueness(key) ? this.floatMap.put(key,ptr) : null;
	}
	
	Pointer<Byte> setPointerByte(String key, Pointer<Byte> ptr) {
		return checkKeyUniqueness(key) ? this.byteMap.put(key,ptr) : null;
	}	
	
	Pointer<Integer> getPointerInteger(String key) {
		return this.integerMap.get(key);
	}
	
	Pointer<Float> getPointerFloat(String key) {
		return this.floatMap.get(key);
	}
	
	Pointer<Byte> getPointerByte(String key) {
		return this.byteMap.get(key);
	}	
	
	void clearAllocatedPTRObjects() {
		for(String buffObject: integerMap.keySet()) {
			integerMap.get(buffObject).release();
		}
		
		for(String buffObject: floatMap.keySet()) {
			integerMap.get(buffObject).release();
		}
		
		for(String buffObject: byteMap.keySet()) {
			integerMap.get(buffObject).release();
		}		
		
		integerMap.clear();
		floatMap.clear();
		byteMap.clear();
	}
}