package it.cvdlab.lar.model.serialize;

import it.cvdlab.lar.model.CsrMatrix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.codehaus.jackson.map.ObjectMapper;

public class CsrMatrixSerializable {
	private CsrMatrixSerializable() {};
	
    // Jackson
    private static ObjectMapper jacksonMapper = new ObjectMapper();
    
    public static CsrMatrix fromFile(String filePath) {
    	CsrMatrix output = null;
    	
    	try {
			String jsonMatrix = new String(Files.readAllBytes(Paths.get(filePath)));
			output = jacksonMapper.readValue(jsonMatrix, CsrMatrix.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return output;
    }
    
    public static void toFile(CsrMatrix input, String filePath) {
    	try {
			jacksonMapper.writeValue(new File(filePath), input);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }    
}
