package it.cvdlab.lar.model.serialize;

import it.cvdlab.lar.model.InputVectorsContainer;

import java.io.File;
import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;

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
}
