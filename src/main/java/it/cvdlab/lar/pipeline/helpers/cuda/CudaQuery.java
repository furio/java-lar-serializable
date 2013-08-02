package it.cvdlab.lar.pipeline.helpers.cuda;

import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_CONCURRENT_KERNELS;
import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_KERNEL_EXEC_TIMEOUT;
import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_X;
import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_Y;
import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_Z;
import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_X;
import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_Y;
import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_Z;
import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_MAX_REGISTERS_PER_BLOCK;
import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_MAX_THREADS_PER_BLOCK;
import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_MULTIPROCESSOR_COUNT;
import static jcuda.driver.CUdevice_attribute.CU_DEVICE_ATTRIBUTE_TEXTURE_ALIGNMENT;
import static jcuda.driver.JCudaDriver.cuDeviceComputeCapability;
import static jcuda.driver.JCudaDriver.cuDeviceGet;
import static jcuda.driver.JCudaDriver.cuDeviceGetAttribute;
import static jcuda.driver.JCudaDriver.cuDeviceGetCount;
import static jcuda.driver.JCudaDriver.cuDeviceGetName;
import static jcuda.driver.JCudaDriver.cuInit;

import java.util.ArrayList;
import java.util.List;

import jcuda.driver.CUdevice;
import jcuda.driver.JCudaDriver;

public class CudaQuery {

	private CudaQuery() {}

	public static List<CudaDeviceStats> getDevicesInfos() {
		try {
	        JCudaDriver.setExceptionsEnabled(true);
	        cuInit(0);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
		List<CudaDeviceStats> lstReturn = new ArrayList<CudaDeviceStats>();
		
        int deviceCountArray[] = { 0 };
        cuDeviceGetCount(deviceCountArray);
        int deviceCount = deviceCountArray[0];

        for (int i = 0; i < deviceCount; i++)
        {
        	CudaDeviceStats cdsAdd = new CudaDeviceStats();
        	
            CUdevice device = new CUdevice();
            cuDeviceGet(device, i);

            // Obtain the device name
            byte deviceName[] = new byte[1024];
            cuDeviceGetName(deviceName, deviceName.length, device);
            cdsAdd.setDeviceName( createString(deviceName) );

            // Obtain the compute capability
            int majorArray[] = { 0 };
            int minorArray[] = { 0 };
            cuDeviceComputeCapability(majorArray, minorArray, device);
            int major = majorArray[0];
            int minor = minorArray[0];
            cdsAdd.setCores( CudaCores.getCores(major, minor) );
            
            // Obtain the device attributes
            int array[] = { 0 };
            
            // 
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_MAX_THREADS_PER_BLOCK, device);
            cdsAdd.setThreadsPerBlock(array[0]);

            // 
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_MAX_THREADS_PER_BLOCK, device);
            cdsAdd.setWarpSize(array[0]);
            
            // 
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_MAX_REGISTERS_PER_BLOCK, device);
            cdsAdd.setRegisters(array[0]);
            
            // 
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_MULTIPROCESSOR_COUNT, device);
            cdsAdd.setMultiprocessors(array[0]);

            // 
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_TEXTURE_ALIGNMENT, device);
            cdsAdd.setAlignment(array[0]);
            
            // 
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_CONCURRENT_KERNELS, device);
            cdsAdd.setConcurrencyKernel(array[0] == 1);
            
            // 
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_KERNEL_EXEC_TIMEOUT, device);
            cdsAdd.setRuntimelimitKernel(array[0] == 1);
            
            //
            int[] blockgrid = new int[CudaCoordinates.length()];
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_X, device);
            blockgrid[CudaCoordinates.X.index()] = array[0];
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_Y, device);
            blockgrid[CudaCoordinates.Y.index()] = array[0];
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_Z, device);
            blockgrid[CudaCoordinates.Z.index()] = array[0];
            cdsAdd.setBlockSize(blockgrid.clone());
            
            //
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_X, device);
            blockgrid[CudaCoordinates.X.index()] = array[0];
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_Y, device);
            blockgrid[CudaCoordinates.Y.index()] = array[0];
            cuDeviceGetAttribute(array, CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_Z, device);
            blockgrid[CudaCoordinates.Z.index()] = array[0];             
            cdsAdd.setGridSize(blockgrid.clone());
            
            
            lstReturn.add(cdsAdd);
        }		
		
		
		return lstReturn;
	}

    private static String createString(byte bytes[])
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++)
        {
            char c = (char)bytes[i];
            if (c == 0)
            {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
