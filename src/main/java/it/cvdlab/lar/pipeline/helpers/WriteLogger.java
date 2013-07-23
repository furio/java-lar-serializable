package it.cvdlab.lar.pipeline.helpers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class WriteLogger {
	private static final String FILE_EXTENSION = ".log";
	
	private WriteLogger() {}
	
	public static void writeLogMatrix(String name, String content){
        try {
            FileWriter fw = new FileWriter(name + "." + System.currentTimeMillis() + FILE_EXTENSION);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            System.err.print("Unable to write to file " + name+ ".");
            e.printStackTrace();
        }
    }	
}
