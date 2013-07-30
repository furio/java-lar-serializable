package it.cvdlab.lar.pipeline.run.utilities;

import it.cvdlab.lar.model.OutputVectorsContainer;
import it.cvdlab.lar.model.serialize.OutputVectorsSerialize;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class BinaryOutputVectorWriter {
	private BinaryOutputVectorWriter() {}
	private static final String INPUT_FILE = DefaultFileNames.OUTPUT_FILE;
	private static final String OUTPUT_FILE_EXT = ".bin";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Options cmdLineOptions = new Options();
		cmdLineOptions.addOption("i", "input", true, "input file to convert. Default: " + INPUT_FILE);
		cmdLineOptions.addOption("h", "help", false, "print help");
		
		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		
		try {
			cmd = parser.parse( cmdLineOptions, args);
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		}
		
		if (cmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "OutputComparer", cmdLineOptions );
			return;
		}
		
		String input = INPUT_FILE;
		
		if (cmd.hasOption("i")) {
			input = cmd.getOptionValue("i");
		}
		
		System.out.println("Reading/parsing file: " + input);
		OutputVectorsContainer ovInput = OutputVectorsSerialize.fromFile(input);		

		System.out.println("Writing binary file: " + input + OUTPUT_FILE_EXT);
		OutputVectorsSerialize.toBinaryFile(ovInput, input + OUTPUT_FILE_EXT);
	}

}
