package it.cvdlab.lar.model.serialize;

import it.cvdlab.lar.model.InputVectorsContainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;

public class InputVectorsSerialize {
	private InputVectorsSerialize() {};
    // Jackson
    private static ObjectMapper jacksonMapper = new ObjectMapper();
    
    public static InputVectorsContainer fromFile(String filePath) {
    	InputVectorsContainer output = null;
    	
    	try {
    		output = jacksonMapper.readValue(new File(filePath), InputVectorsContainer.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return output;
    }
    
    public static void toFile(InputVectorsContainer input, String filePath) {
    	try {
			jacksonMapper.writeValue(new File(filePath), input);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static InputVectorsContainer fromBinaryFile(String filePath, int vectorLength) {
    	InputVectorsContainer ivInput = new InputVectorsContainer();
    	File fIn = null;
    	FileInputStream fos = null;
    	int[] coord = new int[3];
    	byte[] resultLine = new byte[vectorLength];
    	
    	List<List<Integer>> offsetArrays = Lists.newArrayList();
    	List<List<Byte>> byteArrays = Lists.newArrayList();
    	
    	
    	
    	try {
    		fIn = new File(filePath);
    		long fileSize = fIn.length();
    		fos = new FileInputStream(fIn);
    		long maxChains = fileSize / ((long)(vectorLength+12));
    		// System.err.println("MaxChains: " + maxChains);
    		
    		ByteBuffer bfWrite = ByteBuffer.allocate(4);
    		for( long i = 0 ; i < maxChains ; ++i) {
    			
    			// Write offsets
    			Arrays.fill(coord, 0);
    				
    			for(int j = 0; j < coord.length; ++j) {
    				fos.read(bfWrite.array());
    				coord[j] = bfWrite.getInt();
    				bfWrite.rewind();
    			}
    				
    			offsetArrays.add( ImmutableList.copyOf( Ints.asList(coord) ) );
    			
    			// Read bytes
    			it.cvdlab.lar.pipeline.helpers.ArrayUtils.fill(resultLine, (byte) 0);
    			fos.read(resultLine);
    			byteArrays.add( ImmutableList.copyOf( Bytes.asList(resultLine) ) );
    			// System.err.println("Read chain: " + byteArrays.size() + "-" + i);
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
    	
    	ivInput.setVectorList(byteArrays);
    	ivInput.setVectorOffset(offsetArrays);
    	
    	return ivInput;
    }    
}
