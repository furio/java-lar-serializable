package it.cvdlab.lar.pipeline.run.utilities;

import it.cvdlab.lar.model.CsrMatrix;
import it.cvdlab.lar.model.InputVectorsContainer;
import it.cvdlab.lar.model.serialize.CsrMatrixSerializable;
import it.cvdlab.lar.model.serialize.InputVectorsSerialize;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

public class GenerateTestFiles {
	private GenerateTestFiles() {}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int[] matrixOne = new int[] { 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0,
				1, 0, 0, 1, 1, 0, 1, 0, 0, 0, 1 };

		CsrMatrix csrMatrixOne = CsrMatrix.fromFlattenArray(matrixOne, 5);
		CsrMatrixSerializable.toFile(csrMatrixOne, DefaultFileNames.BORDO3_FILE);
		
		int[] offset = {0,0,0};
		int[] vettore1 = {1, 1, 1, 0, 1};
		int[] vettore2 = {0, 1, 0, 0, 0};
		int[] vettore3 = {0, 0, 1, 0, 0};
		int[] vettore4 = {0, 0, 0, 1, 0};
		int[] vettore5 = {1, 1, 1, 0, 1};
		
		List<List<Integer>> outsideList = Lists.newArrayList();
		outsideList.add(Ints.asList(vettore1));
		outsideList.add(Ints.asList(vettore2));
		outsideList.add(Ints.asList(vettore3));
		outsideList.add(Ints.asList(vettore4));
		outsideList.add(Ints.asList(vettore5));
		
		List<List<Integer>> offsetList = Lists.newArrayList();
		offsetList.add(Ints.asList(offset));
		offsetList.add(Ints.asList(offset));
		offsetList.add(Ints.asList(offset));
		offsetList.add(Ints.asList(offset));
		offsetList.add(Ints.asList(offset));
		
		InputVectorsContainer iv = new InputVectorsContainer();
		iv.setVectorList(outsideList);
		iv.setVectorOffset(offsetList);
		
		InputVectorsSerialize.toFile(iv, DefaultFileNames.SELETTORI_FILE);
	}

}
