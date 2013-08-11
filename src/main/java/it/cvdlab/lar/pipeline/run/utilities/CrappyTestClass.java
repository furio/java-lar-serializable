package it.cvdlab.lar.pipeline.run.utilities;

import it.cvdlab.lar.model.CsrMatrix;
import it.cvdlab.lar.model.InputVectorsContainer;
import it.cvdlab.lar.model.OutputVectorsContainer;
import it.cvdlab.lar.model.serialize.OutputVectorsSerialize;
import it.cvdlab.lar.pipeline.helpers.ArrayUtils;
import it.cvdlab.lar.pipeline.helpers.BinaryTranslator;
import it.cvdlab.lar.pipeline.helpers.ResultTuple;
import it.cvdlab.lar.pipeline.kernelwrap.LARTestBinary;
import it.cvdlab.lar.pipeline.kernelwrap.LARTestBinaryJava;

import java.util.Arrays;
import java.util.List;



public class CrappyTestClass {

	public static void main(String[] args) {
		int[] matrixOne = new int[] { 1, 0, 0, 0, 1,
									  0, 1, 0, 0, 0, 
									  0, 0, 0, 0, 1, 
									  0, 0, 1, 1, 0, 
									  1, 0, 0, 0, 1,
									  1, 0, 0, 0, 1};
		
		CsrMatrix csrMatrixOne = CsrMatrix.fromFlattenArray(matrixOne, 5);
		System.out.println(csrMatrixOne.toString());
		
		int[] vettori = new int[] { 1, 1, 1, 0, 1,
										0, 1, 0, 0, 0,
										0, 0, 1, 0, 0,
										0, 0, 0, 1, 0,
										0, 0, 0, 0, 1,
										1, 1, 1, 0, 1};

		int vectorLength = 5;
		
		System.out.println("Conversione a binario delle q.c.");
		int[] flatResult = ArrayUtils.flatten(BinaryTranslator.fromFlatArray(vettori, 5));
		System.out.println(Arrays.toString(flatResult));
		
		int bitSetLength = (int)Math.ceil((double)vectorLength / (double)Integer.SIZE);

		System.out.println("Chiamata kernel");
		System.out.println("New vLength: " + bitSetLength);
		System.out.println("Old vLength: " + vectorLength);
		List<ResultTuple> resultTuples = LARTestBinary.clMultiply(csrMatrixOne, flatResult, bitSetLength, vectorLength);
		
		OutputVectorsContainer ov  = OutputVectorsSerialize.fromResultTuple(resultTuples, new InputVectorsContainer(), csrMatrixOne.getRowCount());
		
		System.out.println("Serializzazione risultati");
		for (List<Byte> bt : ov.getVectorList()) {
			System.out.println(bt);
		}
		
		System.out.println("Chiamata kernel JAVA");
		resultTuples = LARTestBinaryJava.clMultiply(csrMatrixOne, flatResult, bitSetLength, vectorLength);
		
		ov  = OutputVectorsSerialize.fromResultTuple(resultTuples, new InputVectorsContainer(), csrMatrixOne.getRowCount());
		for (List<Byte> bt : ov.getVectorList()) {
			System.out.println(bt);
		}		
	}
	
}