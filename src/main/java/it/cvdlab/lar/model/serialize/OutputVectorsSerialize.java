package it.cvdlab.lar.model.serialize;

import it.cvdlab.lar.model.OutputVectorsContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

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
    	FileOutputStream fos = null;
    	
    	try {
    		fos = new FileOutputStream(filePath);
    		
    		for(List<Byte> currList: input.getVectorList()) {
    			fos.write( ArrayUtils.toPrimitive(currList.toArray(new Byte[1])) );
    		}
    		
    		// Serialize offset vettori
    		/*
    		ByteBuffer bfWrite = ByteBuffer.allocate(4);
    		
    		for(List<Integer> currList: input.getVectorOffset()) {
    			for(Integer currInt : currList) {
    				bfWrite.putInt(currInt);
    				fos.write( bfWrite.array() );
    				bfWrite.rewind();
    			}
    		}
    		*/
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
