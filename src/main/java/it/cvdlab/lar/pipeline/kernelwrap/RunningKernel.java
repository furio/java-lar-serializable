package it.cvdlab.lar.pipeline.kernelwrap;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

import org.bridj.Pointer;

import com.google.common.collect.Lists;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;
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
	private List<CLMem> buffersRelease;
	@SuppressWarnings("rawtypes")
	private List<Pointer> pointersRelease;
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
		buffersRelease = Lists.newArrayList();
		pointersRelease = Lists.newArrayList();
		runOn = DeviceFeature.GPU;
		copyToDevice = (runOn == DeviceFeature.CPU);
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
		super();
		runOn = selectedFeature;
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
	
	
	private void clearAllocatedObjects() {
		if (queue != null) {
			queue.flush();
			queue.release();
			
			queue = null;
		}

		if ( kernel != null ) {
			kernel.release();
			
			kernel = null;
		}
		
		if ( program != null ) {
			program.release();
			
			program = null;
		}
		
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
		for (CLMem buffObject : buffersRelease) {
			buffObject.release();
		}
		buffersRelease.clear();
	}

	@SuppressWarnings("rawtypes")
	private void clearAllocatedPTRObjects() {
		System.err.println("Clearing POINTERS");
		for (Pointer buffObject : pointersRelease) {
			buffObject.release();
		}
		pointersRelease.clear();
	}		
}
