package it.cvdlab.lar.model;

public final class CLEngineConfig {
	// String settings
    private static final String PROPERTY_NNZWEIGHT = CLEngineConfig.class.getPackage().getName()
            + ".nnzWeight";
    private static final String PROPERTY_USECOO = CLEngineConfig.class.getPackage().getName()
            + ".useCOO";
    private static final String PROPERTY_NOCL = CLEngineConfig.class.getPackage().getName()
            + ".noOpenCL";
    private static final String PROPERTY_FORCEGPUCX = CLEngineConfig.class.getPackage().getName()
            + ".forceGPU";    
    private static final String PROPERTY_USEDEVICEMEM = CLEngineConfig.class.getPackage().getName()
            + ".useDeviceMem";
    private static final String PROPERTY_FORCEGC = CLEngineConfig.class.getPackage().getName()
            + ".forceGC";
    private static final String PROPERTY_USESHAREDCL = CLEngineConfig.class.getPackage().getName()
            + ".useSharedCL";
    private static final String PROPERTY_USEIMPLLOCAL = CLEngineConfig.class.getPackage().getName()
            + ".useNoLocalSize";
	
    // 
	private static int NNZ_WEIGHT = 3;
	private static boolean USECOO = false;
	private static boolean NO_OPENCL = false;
	private static boolean FORCE_GPU = true;
	private static boolean USE_DEVICE_MEM = true;
	private static boolean FORCE_GC = false;
	private static boolean SHARED_CL = true;
	private static boolean IMPL_LOCAL = false;
	
	static {
		String nnzWeight = System.getProperty(PROPERTY_NNZWEIGHT);
		String useCOO = System.getProperty(PROPERTY_USECOO);
		String noOpenCL = System.getProperty(PROPERTY_NOCL);
		String forceGPU = System.getProperty(PROPERTY_FORCEGPUCX);
		String deviceMem = System.getProperty(PROPERTY_USEDEVICEMEM);
		String forceGC = System.getProperty(PROPERTY_FORCEGC);
		String sharedCL = System.getProperty(PROPERTY_USESHAREDCL);
		String implLocal = System.getProperty(PROPERTY_USEIMPLLOCAL);
		
		if (nnzWeight != null) {
			try{
				int value = Integer.valueOf(nnzWeight);
				if (value >= 1) {
					System.out.println(PROPERTY_NNZWEIGHT+ ": " + value);
					NNZ_WEIGHT = value;
				}
			} catch(NumberFormatException e) {
				
			}
		}
		
		if (useCOO != null) {
			USECOO = Boolean.valueOf(useCOO);
			System.out.println(PROPERTY_USECOO+ ": " + USECOO);
		}
		
		if (noOpenCL != null) {
			NO_OPENCL = Boolean.valueOf(noOpenCL);
			System.out.println(PROPERTY_NOCL+ ": " + NO_OPENCL);
		}

		if (deviceMem != null) {
			USE_DEVICE_MEM = Boolean.valueOf(deviceMem);
			System.out.println(PROPERTY_USEDEVICEMEM+ ": " + USE_DEVICE_MEM);		
		}
		
		if (forceGPU != null) {
			FORCE_GPU = Boolean.valueOf(forceGPU);
			System.out.println(PROPERTY_FORCEGPUCX+ ": " + FORCE_GPU);
		}
		
		if (forceGC != null) {
			FORCE_GC = Boolean.valueOf(forceGC);
			System.out.println(PROPERTY_FORCEGC+ ": " + FORCE_GC);
		}
		
		if (sharedCL != null) {
			SHARED_CL = Boolean.valueOf(sharedCL);
			System.out.println(PROPERTY_USESHAREDCL+ ": " + SHARED_CL);			
		}
		
		if (implLocal != null) {
			IMPL_LOCAL = Boolean.valueOf(implLocal);
			System.out.println(PROPERTY_USEIMPLLOCAL+ ": " + IMPL_LOCAL);			
		}		
	}

	public static int getNNZ_WEIGHT() {
		return NNZ_WEIGHT;
	}

	public static boolean isUSECOO() {
		return USECOO;
	}

	public static boolean isNO_OPENCL() {
		return NO_OPENCL;
	}

	public static boolean isFORCE_GPU() {
		return FORCE_GPU;
	}

	public static boolean isUSE_DEVICE_MEM() {
		return USE_DEVICE_MEM;
	}

	public static boolean isFORCE_GC() {
		return FORCE_GC;
	}

	public static boolean isSHARED_CL() {
		return SHARED_CL;
	}

	public static boolean isIMPL_LOCAL() {
		return IMPL_LOCAL;
	}
}