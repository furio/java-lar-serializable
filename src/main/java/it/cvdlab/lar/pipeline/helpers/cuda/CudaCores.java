package it.cvdlab.lar.pipeline.helpers.cuda;

import java.util.HashMap;
import java.util.Map;

public class CudaCores {
    private static Map<Integer,Integer> coresMap;
    
    static{
    	coresMap = new HashMap<Integer, Integer>();
    	
    	coresMap.put(0x10, 8);
    	coresMap.put(0x11, 8);
    	coresMap.put(0x12, 8);
    	coresMap.put(0x13, 8);
    	coresMap.put(0x20, 32);
    	coresMap.put(0x21, 48);
    	coresMap.put(0x30, 192);
    	coresMap.put(0x35, 192);
    }
    
    static int getCores(int major, int minor) {
    	int cudaVersion = ((major << 4) + minor);
    	
    	if (coresMap.containsKey(cudaVersion)) {
    		return coresMap.get(cudaVersion);
    	}
    	
    	return -1;
    }
}
