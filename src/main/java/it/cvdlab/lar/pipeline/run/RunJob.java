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

import java.util.List;

import com.google.common.collect.Lists;

public class RunJob {
	static final String BORDO3_FILE = "bordo3.json";
	static final String SELETTORI_FILE = "selettori.json";
	static final String OUTPUT_FILE = "output.json";
	@SuppressWarnings("unused")
	private static final Integer[] CRAP_INT_VECTOR = {};
	
	public static void main(String[] args) {
		CsrMatrix bordo3 = CsrMatrixSerializable.fromFile(BORDO3_FILE);
		InputVectorsContainer ivc = InputVectorsSerialize.fromFile(SELETTORI_FILE);
		
		runJob(bordo3, ivc);
	}
	
	private static void runJob(CsrMatrix b3, InputVectorsContainer ivc) {
		int vectorLength = ivc.getVectorList().get(0).size();
		int vectorsCount = ivc.getVectorList().size();
		
		int[] flatResult = ArrayUtils.flatten(BinaryTranslator.fromArrays(ivc.getVectorList()));
		
		int bitSetLength = (int)Math.ceil((double)vectorLength / (double)Integer.SIZE);

		List<ResultTuple> resultTuples = LARTestBinary.clMultiply(b3, flatResult, bitSetLength, vectorLength);
		
		OutputVectorsContainer ov = new OutputVectorsContainer();
		ov.setVectorOffset(ivc.getVectorOffset());
		List<List<Integer>> resultsAnnidated = Lists.newArrayListWithCapacity(vectorsCount);
		
		
		List<Integer> result;
		long totalElapsed = 0;
		for(ResultTuple rtCurr: resultTuples) {
			result = rtCurr.getDataOutput();
			totalElapsed += rtCurr.getElapsedTime();
			
			for(int i = 0; i < vectorsCount; i++) {
				List<Integer> currList = Lists.newArrayListWithCapacity(b3.getRowCount());
				for(int j = 0; j < b3.getRowCount(); j++) {
					int temp = result.get(i*b3.getRowCount() + j);
					currList.add(temp);
				}
				resultsAnnidated.add(i, currList);
			}
		}

		ov.setVectorList(resultsAnnidated);
		ov.setVectorStats( Lists.newArrayList(new Long(totalElapsed), new Long(0)) );
		
		OutputVectorsSerialize.toFile(ov, OUTPUT_FILE);
	}
	
	private RunJob() {}
}
