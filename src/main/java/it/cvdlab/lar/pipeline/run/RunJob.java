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
	private static final String OUTPUT_FILE = DefaultFileNames.OUTPUT_FILE;
	
	@SuppressWarnings("unused")
	private static final Integer[] CRAP_INT_VECTOR = {};
	
	public static void main(String[] args) {
		Options cmdLineOptions = new Options();
		cmdLineOptions.addOption("b", "bordo", true, "input file containing the bordo matrix. Default: " + BORDO3_FILE);
		cmdLineOptions.addOption("s", "selettori", true, "input file containing the chains. Default: " + SELETTORI_FILE);
		cmdLineOptions.addOption("o", "output", true, "output file. Default: " + OUTPUT_FILE);
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
		String output_vettori = OUTPUT_FILE;
		
		if (cmd.hasOption("b")) {
			input_bordo = cmd.getOptionValue("b");
		}
		
		if (cmd.hasOption("s")) {
			input_selettori = cmd.getOptionValue("s");
		}
		
		if (cmd.hasOption("o")) {
			output_vettori = cmd.getOptionValue("o");
		}
		
		System.out.println("Bordo3: " + input_bordo);
		System.out.println("Selettori: " + input_selettori);
		System.out.println("Output: " + output_vettori);
		
		System.out.println("Lettura bordo3");
		CsrMatrix bordo3 = CsrMatrixSerializable.fromFile(input_bordo);
		System.out.println("Lettura q.c.");
		InputVectorsContainer ivc = InputVectorsSerialize.fromFile(input_selettori);
		
		runJob(bordo3, ivc, output_vettori);
	}
	
	private static void runJob(CsrMatrix b3, InputVectorsContainer ivc, String outFile) {
		int vectorLength = ivc.getVectorList().get(0).size();
		
		System.out.println("Conversione a binario delle q.c.");
		int[] flatResult = ArrayUtils.flatten(BinaryTranslator.fromArrays(ivc.getVectorList()));
		
		int bitSetLength = (int)Math.ceil((double)vectorLength / (double)Integer.SIZE);

		System.out.println("Chiamata kernel");
		List<ResultTuple> resultTuples = LARTestBinaryContinuos.clMultiply(b3, flatResult, bitSetLength, vectorLength);
		
		OutputVectorsContainer ov  = OutputVectorsSerialize.fromResultTuple(resultTuples, ivc, b3.getRowCount());
		
		System.out.println("Serializzazione risultati");
		OutputVectorsSerialize.toFile(ov, outFile);
	}
	
	private RunJob() {}
}
