package it.cvdlab.lar.model.serialize;

import it.cvdlab.lar.model.InputVectorsContainer;
import it.cvdlab.lar.model.OutputVectorsContainer;
import it.cvdlab.lar.pipeline.helpers.ResultTuple;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.collect.Lists;

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
    
	public static OutputVectorsContainer fromResultTuple(List<ResultTuple> resultTuples, InputVectorsContainer ivc, int vectorLength) {
		OutputVectorsContainer ov = new OutputVectorsContainer();
		ov.setVectorOffset(ivc.getVectorOffset());
		
		List<List<Byte>> resultsAnnidated = Lists.newArrayList();
		
		// System.out.println("Conversione risultati di " + ivc.getVectorList().size() + " vettori");
		List<Byte> result;
		long totalElapsed = 0;
		for(ResultTuple rtCurr: resultTuples) {
			result = rtCurr.getDataOutput();
			totalElapsed += rtCurr.getElapsedTime();
			System.out.println("-- Converting " + rtCurr.getVectorsQty() + " vectors");
			for(int i = 0; i < rtCurr.getVectorsQty(); i++) {
				List<Byte> currList = Lists.newArrayListWithCapacity(vectorLength);
				for(int j = 0; j < vectorLength; j++) {
					currList.add( result.get(i*vectorLength + j) );
				}
				resultsAnnidated.add(currList);
			}
		}

		ov.setVectorList(resultsAnnidated);
		ov.setVectorStats( Lists.newArrayList(new Long(totalElapsed), new Long(0)) );
		
		return ov;
    }
}
