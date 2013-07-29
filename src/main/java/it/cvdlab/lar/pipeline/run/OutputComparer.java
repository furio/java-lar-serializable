package it.cvdlab.lar.pipeline.run;

import it.cvdlab.lar.model.OutputVectorsContainer;
import it.cvdlab.lar.model.serialize.OutputVectorsSerialize;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class OutputComparer {
	private OutputComparer() {}

	private static final String INPUT_FILE_1 = "output_1.json";
	private static final String INPUT_FILE_2 = "output_2.json";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Options cmdLineOptions = new Options();
		cmdLineOptions.addOption("i1", "input-1", true, "input file to compare. Default: " + INPUT_FILE_1);
		cmdLineOptions.addOption("i2", "input-2", true, "input file to compare. Default: " + INPUT_FILE_2);
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
		
		String input1 = INPUT_FILE_1;
		String input2 = INPUT_FILE_2;
		
		if (cmd.hasOption("i1")) {
			input1 = cmd.getOptionValue("i1");
		}
		
		if (cmd.hasOption("i2")) {
			input2 = cmd.getOptionValue("i2");
		}
		
		System.out.println("Comparing files: " + input1 + " and " + input2);
	
		System.out.println("Reading/parsing first file");
		OutputVectorsContainer ov1 = OutputVectorsSerialize.fromFile(input1);
		System.out.println("Reading/parsing second file");
		OutputVectorsContainer ov2 = OutputVectorsSerialize.fromFile(input2);
		System.out.println("Comparing files");
		boolean bEquals = ov1.equals(ov2);
		System.out.println("Two OutputVectors files are equals? " + bEquals);
	}
}
