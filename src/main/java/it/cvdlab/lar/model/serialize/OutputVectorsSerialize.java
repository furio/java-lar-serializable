package it.cvdlab.lar.model.serialize;

import it.cvdlab.lar.model.OutputVectorsContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.map.ObjectMapper;

public class OutputVectorsSerialize {
	private OutputVectorsSerialize() {};
    // Jackson
    private static ObjectMapper jacksonMapper = new ObjectMapper();
    
    public static OutputVectorsContainer fromFile(String filePath) {
    	OutputVectorsContainer output = null;
    	
    	try {
			output = jacksonMapper.readValue(new File(filePath), OutputVectorsContainer.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return output;
    }
    
    public static void toFile(OutputVectorsContainer input, String filePath) {
    	try {
			jacksonMapper.writeValue(new File(filePath), input);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static void toBinaryFile(OutputVectorsContainer input, String filePath) {
    	toBinaryFile(input, filePath, false);
    }
    
    public static void toBinaryFile(OutputVectorsContainer input, String filePath, boolean serializeOffsets) {
    	FileOutputStream fos = null;
    	
    	try {
    		fos = new FileOutputStream(filePath);
    		
    		ByteBuffer bfWrite = ByteBuffer.allocate(4);
    		for(int i = 0; i < input.getVectorList().size(); ++i) {
    			
    			// Write offsets
    			if (serializeOffsets == true) {
        			for(Integer currInt : input.getVectorOffset().get(i)) {
        				bfWrite.putInt(currInt);
        				fos.write( bfWrite.array() );
        				bfWrite.rewind();
        			}
    			}
    			
    			// Write bytes
        		fos.write( ArrayUtils.toPrimitive(input.getVectorList().get(i).toArray(new Byte[1])) );    			
    		}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }     
}
