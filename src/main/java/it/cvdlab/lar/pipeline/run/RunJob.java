package it.cvdlab.lar.pipeline.run;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import it.cvdlab.lar.model.CsrMatrix;
import it.cvdlab.lar.model.InputVectorsContainer;
import it.cvdlab.lar.model.OutputVectorsContainer;
import it.cvdlab.lar.model.serialize.CsrMatrixSerializable;
import it.cvdlab.lar.model.serialize.InputVectorsSerialize;
import it.cvdlab.lar.model.serialize.OutputVectorsSerialize;
import it.cvdlab.lar.pipeline.helpers.ArrayUtils;
import it.cvdlab.lar.pipeline.helpers.BinaryTranslator;
import it.cvdlab.lar.pipeline.kernelwrap.LARTestBinary;

public class RunJob {
	static final String BORDO3_FILE = "bordo3.json";
	static final String SELETTORI_FILE = "selettori.json";
	static final String OUTPUT_FILE = "output.json";
	private static final Integer[] CRAP_VECTOR = {};
	
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

		List<Integer> result = LARTestBinary.clMultiply(b3, flatResult, bitSetLength, vectorLength);
		
		OutputVectorsContainer ov = new OutputVectorsContainer();
		ov.setVectorOffset(ivc.getVectorOffset());
		
		List<List<Integer>> resultsAnnidated = Lists.newArrayListWithCapacity(vectorsCount);
		
		for(int i = 0; i < vectorsCount; i++) {
			List<Integer> currList = Lists.newArrayListWithCapacity(b3.getRowCount());
			for(int j = 0; j < b3.getRowCount(); j++) {
				int temp = result.get(i*b3.getRowCount() + j);
				currList.add(temp);
			}
			resultsAnnidated.add(i, currList);
		}
		
		ov.setVectorList(resultsAnnidated);
		ov.setVectorStats(Lists.asList(0, 0, CRAP_VECTOR ));
		
		OutputVectorsSerialize.toFile(ov, OUTPUT_FILE);
	}
	
	private RunJob() {}
}
