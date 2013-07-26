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

import java.util.List;

import com.google.common.collect.Lists;

public class RunJob {
	static final String BORDO3_FILE = "bordo3.json";
	static final String SELETTORI_FILE = "selettori.json";
	static final String OUTPUT_FILE = "output.json";
	@SuppressWarnings("unused")
	private static final Integer[] CRAP_INT_VECTOR = {};
	
	public static void main(String[] args) {
		System.out.println("Lettura bordo3");
		CsrMatrix bordo3 = CsrMatrixSerializable.fromFile(BORDO3_FILE);
		System.out.println("Lettura q.c.");
		InputVectorsContainer ivc = InputVectorsSerialize.fromFile(SELETTORI_FILE);
		
		runJob(bordo3, ivc);
	}
	
	private static void runJob(CsrMatrix b3, InputVectorsContainer ivc) {
		int vectorLength = ivc.getVectorList().get(0).size();
		int vectorsCount = ivc.getVectorList().size();
		
		System.out.println("Conversione a binario delle q.c.");
		int[] flatResult = ArrayUtils.flatten(BinaryTranslator.fromArrays(ivc.getVectorList()));
		
		int bitSetLength = (int)Math.ceil((double)vectorLength / (double)Integer.SIZE);

		System.out.println("Chiamata kernel");
		List<ResultTuple> resultTuples = LARTestBinaryContinuos.clMultiply(b3, flatResult, bitSetLength, vectorLength);
		
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
		OutputVectorsSerialize.toFile(ov, OUTPUT_FILE);
	}
	
	private RunJob() {}
}
