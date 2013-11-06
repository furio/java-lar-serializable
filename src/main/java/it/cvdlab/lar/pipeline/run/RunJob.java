package it.cvdlab.lar.pipeline.run;

import it.cvdlab.lar.model.CsrMatrix;
import it.cvdlab.lar.model.InputVectorsContainer;
import it.cvdlab.lar.model.OutputVectorsContainer;
import it.cvdlab.lar.model.serialize.CsrMatrixSerializable;
import it.cvdlab.lar.model.serialize.InputVectorsSerialize;
import it.cvdlab.lar.model.serialize.OutputVectorsSerialize;
import it.cvdlab.lar.pipeline.helpers.ArrayUtils;
import it.cvdlab.lar.pipeline.helpers.BinaryTranslator;
import it.cvdlab.lar.pipeline.helpers.ResultTuple;
import it.cvdlab.lar.pipeline.kernelwrap.LARTestBinaryContinuos;
import it.cvdlab.lar.pipeline.run.utilities.DefaultFileNames;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class RunJob {
	private static final String BORDO3_FILE = DefaultFileNames.BORDO3_FILE;
	private static final String SELETTORI_FILE = DefaultFileNames.SELETTORI_FILE;
	private static final String SELETTORI_BIN_FILE = DefaultFileNames.SELETTORI_BIN_FILE;
	private static final String OUTPUT_FILE = DefaultFileNames.OUTPUT_FILE;
	private static final boolean USE_BINARY = false;
	
	@SuppressWarnings("unused")
	private static final Integer[] CRAP_INT_VECTOR = {};
	
	public static void main(String[] args) {
		Options cmdLineOptions = new Options();
		cmdLineOptions.addOption("b", "bordo", true, "input file containing the bordo matrix. Default: " + BORDO3_FILE);
		cmdLineOptions.addOption("s", "selettori", true, "input file containing the chains. Default: " + SELETTORI_FILE);
		cmdLineOptions.addOption("v", "selettori-binari", true, "input file containing the chains. Default: " + SELETTORI_BIN_FILE);
		// cmdLineOptions.addOption("w", "selettori-dim", true, "dimesion of the chain [mandatory for binary].");
		cmdLineOptions.addOption("o", "output", true, "output file. Default: " + OUTPUT_FILE);
		cmdLineOptions.addOption("y", "binaryoutput", false, "output file in binary. Default: " + USE_BINARY);
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
			formatter.printHelp( "RunJob", cmdLineOptions );
			return;
		}		
		
		String input_bordo = BORDO3_FILE;
		String input_selettori = SELETTORI_FILE;
		// int input_selettori_dim = 0;
		boolean input_selettori_binari = false;
		String output_vettori = OUTPUT_FILE;
		boolean use_binary = USE_BINARY;
		
		if (cmd.hasOption("b")) {
			input_bordo = cmd.getOptionValue("b");
		}
		
		if (cmd.hasOption("v")) {
			input_selettori_binari = true;
			// input_selettori_dim = Integer.parseInt(cmd.getOptionValue("w"));
			input_selettori = cmd.getOptionValue("v");
		} else {
			if (cmd.hasOption("s")) {
				input_selettori = cmd.getOptionValue("s");
			}			
		}

		
		if (cmd.hasOption("o")) {
			output_vettori = cmd.getOptionValue("o");
		}
		
		if (cmd.hasOption("y")) {
			use_binary = true;
			if (!output_vettori.endsWith(DefaultFileNames.BIN_EXT))
				output_vettori = output_vettori + DefaultFileNames.BIN_EXT;
		}
		
		System.out.println("Bordo3: " + input_bordo);
		System.out.println("Selettori: " + input_selettori);
		System.out.println("Output: " + output_vettori);
		System.out.println("Binary output: " + use_binary);
		
		
		try{
			System.out.println("Lettura bordo3");
			CsrMatrix bordo3 = CsrMatrixSerializable.fromFile(input_bordo);
			
			InputVectorsContainer ivc = null;
			if (input_selettori_binari) {
				System.out.println("Lettura q.c. binary (" + bordo3.getColCount() +" + 12 per chain)");
				ivc = InputVectorsSerialize.fromBinaryFile(input_selettori, bordo3.getColCount());
			} else {
				System.out.println("Lettura q.c. json");
				ivc = InputVectorsSerialize.fromFile(input_selettori);
			}
			System.out.println("Starting job");
			runJob(bordo3, ivc, output_vettori, use_binary);
		} catch(Exception e) {
			System.out.println("Exception while running: " + e.toString());
			System.exit(2);
		}
	}
	
	private static void runJob(CsrMatrix b3, InputVectorsContainer ivc, String outFile, boolean binaryOutput) {
		int vectorLength = ivc.getVectorList().get(0).size();
		
		System.out.println("Conversione a binario delle q.c.");
		// int[] flatResult = ArrayUtils.flatten(BinaryTranslator.fromArrays(ivc.getVectorList()));
		int[] flatResult = ArrayUtils.flatten(BinaryTranslator.fromByteArrays(ivc.getVectorList()));
		
		int bitSetLength = (int)Math.ceil((double)vectorLength / (double)Integer.SIZE);

		System.out.println("Chiamata kernel");
		List<ResultTuple> resultTuples = LARTestBinaryContinuos.clMultiply(b3, flatResult, bitSetLength, vectorLength);
		
		OutputVectorsContainer ov = OutputVectorsSerialize.fromResultTuple(resultTuples, ivc, b3.getRowCount());
		
		System.out.println("Serializzazione risultati");
		if (binaryOutput) {
			OutputVectorsSerialize.toBinaryFile(ov, outFile, true);
		} else {
			OutputVectorsSerialize.toFile(ov, outFile);
		}
	}
	
	private RunJob() {}
}
