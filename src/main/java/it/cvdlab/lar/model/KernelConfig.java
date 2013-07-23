package it.cvdlab.lar.model;


import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.CLPlatform.DeviceFeature;

public final class KernelConfig {
	public static CLContext createContext() {
		CLContext context = null;
		
		try {
			if ( CLEngineConfig.isFORCE_GPU() ) {
				context = JavaCL.createBestContext(DeviceFeature.GPU);
			} else {
				context = JavaCL.createBestContext();
			}
		}  catch (CLException e) {
			context = null;
			System.err.println(e.toString());
        }
		
		System.err.println(context.toString());
		
		return context;
	}
	
	private static final String KERNEL_DENSE_LOCAL = "SpMSpM-Multiply-Naive.cl";
	private static final String KERNEL_DENSE_NOLOCAL = "SpMSpM-Multiply-Naive.nl.cl";
	private static final String KERNEL_COO_LOCAL = "SpMSpM-Multiply-COO.cl";
	private static final String KERNEL_COO_NOLOCAL = "SpMSpM-Multiply-COO.nl.cl";
	private static final String KERNEL_COO_FLAT_LOCAL = "SpMSpM-MultiplyRow-COO.cl";
	private static final String KERNEL_COO_FLAT_NOLOCAL = "SpMSpM-MultiplyRow-COO.nl.cl";	
	private static final String KERNEL_NNZ_LOCAL = "NNZ-Calc.cl";
	private static final String KERNEL_NNZ_NOLOCAL = "NNZ-Calc.nl.cl";
	
	public static final String KERNEL_DENSE() {
		if (CLEngineConfig.isIMPL_LOCAL()) {
			return KERNEL_DENSE_NOLOCAL;
		} else {
			return KERNEL_DENSE_LOCAL;
		}
	}
	
	public static final String KERNEL_COO() {
		if (CLEngineConfig.isIMPL_LOCAL()) {
			return KERNEL_COO_NOLOCAL;
		} else {
			return KERNEL_COO_LOCAL;
		}
	}
	
	public static final String KERNEL_COO_FLAT() {
		if (CLEngineConfig.isIMPL_LOCAL()) {
			return KERNEL_COO_FLAT_NOLOCAL;
		} else {
			return KERNEL_COO_FLAT_LOCAL;
		}
	}
	
	public static final String KERNEL_NNZ() {
		if (CLEngineConfig.isIMPL_LOCAL()) {
			return KERNEL_NNZ_NOLOCAL;
		} else {
			return KERNEL_NNZ_LOCAL;
		}
	}	
	
	public static final String KERNEL_DENSE_FUN_FULL = "spmm_kernel_naive";
	public static final String KERNEL_DENSE_FUN_SHORT = "spmm_binary_kernel_naive";
	public static final String KERNEL_COO_FUN_FULL = "spmm_coo_kernel_naive";
	public static final String KERNEL_COO_FUN_SHORT = "spmm_coo_binary_kernel_naive";
	public static final String KERNEL_NNZ_FUN = "nnz_calc_kernel";
	
	public static final String DEFINE_ROW = "%%AROW%%";
	public static final String DEFINE_COL = "%%BCOL%%";
}
