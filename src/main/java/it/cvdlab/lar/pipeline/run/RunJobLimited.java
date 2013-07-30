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
import it.cvdlab.lar.pipeline.kernelwrap.LARTestBinary;
import it.cvdlab.lar.pipeline.kernelwrap.LARTestBinaryJava;
import it.cvdlab.lar.pipeline.run.utilities.DefaultFileNames;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Lists;

public class RunJobLimited {
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
		String output_selettori = SELETTORI_FILE;
		
		if (cmd.hasOption("b")) {
			input_bordo = cmd.getOptionValue("b");
		}
		
		if (cmd.hasOption("s")) {
			input_selettori = cmd.getOptionValue("s");
		}
		
		if (cmd.hasOption("o")) {
			output_selettori = cmd.getOptionValue("o");
		}		
		
		System.out.println("Lettura bordo3");
		CsrMatrix bordo3 = CsrMatrixSerializable.fromFile(input_bordo);
		System.out.println("Lettura q.c.");
		InputVectorsContainer ivc = InputVectorsSerialize.fromFile(input_selettori);
		
		runJob(bordo3, ivc, output_selettori);
	}
	
	private static void runJob(CsrMatrix b3, InputVectorsContainer ivc, String outFile) {
		int vectorLength = ivc.getVectorList().get(0).size();
		int vectorsCount = ivc.getVectorList().size();
		
		System.out.println("Conversione a binario delle q.c.");
		int[] flatResult = ArrayUtils.flatten(BinaryTranslator.fromArrays(ivc.getVectorList()));
		
		int bitSetLength = (int)Math.ceil((double)vectorLength / (double)Integer.SIZE);

		System.out.println("Chiamata kernel");
		System.out.println("New vLength: " + bitSetLength);
		System.out.println("Old vLength: " + vectorLength);
		// List<ResultTuple> resultTuples = LARTestBinaryJava.clMultiply(b3, flatResult, bitSetLength, vectorLength);
		List<ResultTuple> resultTuples = LARTestBinary.clMultiply(b3, flatResult, bitSetLength, vectorLength);
		
		OutputVectorsContainer ov = new OutputVectorsContainer();
		ov.setVectorOffset(ivc.getVectorOffset());
		List<List<Byte>> resultsAnnidated = Lists.newArrayListWithCapacity(vectorsCount);
		
		System.out.println("Conversione risultati");
		List<Byte> result;
		long totalElapsed = 0;
		for(ResultTuple rtCurr: resultTuples) {
			result = rtCurr.getDataOutput();
			totalElapsed += rtCurr.getElapsedTime();
			
			for(int i = 0; i < rtCurr.getVectorsQty(); i++) {
				List<Byte> currList = Lists.newArrayListWithCapacity(b3.getRowCount());
				for(int j = 0; j < b3.getRowCount(); j++) {
					currList.add( result.get(i*b3.getRowCount() + j) );
				}
				resultsAnnidated.add(i, currList);
			}
		}

		ov.setVectorList(resultsAnnidated);
		ov.setVectorStats( Lists.newArrayList(new Long(totalElapsed), new Long(0)) );
		
		System.out.println("Serializzazione risultati");
		OutputVectorsSerialize.toFile(ov, outFile);
	}
	
	private RunJobLimited() {}
}
