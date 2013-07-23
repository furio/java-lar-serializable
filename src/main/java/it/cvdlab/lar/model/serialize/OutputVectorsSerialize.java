package it.cvdlab.lar.model.serialize;

import it.cvdlab.lar.model.OutputVectorsContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.codehaus.jackson.map.ObjectMapper;

public class OutputVectorsSerialize {
	private OutputVectorsSerialize() {};
    // Jackson
    private static ObjectMapper jacksonMapper = new ObjectMapper();
    
    public static OutputVectorsContainer fromFile(String filePath) {
    	OutputVectorsContainer output = null;
    	
    	try {
			String jsonMatrix = new String(Files.readAllBytes(Paths.get(filePath)));
			output = jacksonMapper.readValue(jsonMatrix, OutputVectorsContainer.class);
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
}
