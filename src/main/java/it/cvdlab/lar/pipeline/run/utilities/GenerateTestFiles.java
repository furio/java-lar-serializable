package it.cvdlab.lar.pipeline.run.utilities;

import it.cvdlab.lar.model.CsrMatrix;
import it.cvdlab.lar.model.InputVectorsContainer;
import it.cvdlab.lar.model.serialize.CsrMatrixSerializable;
import it.cvdlab.lar.model.serialize.InputVectorsSerialize;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
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
		byte[] vettore1 = {1, 1, 1, 0, 1};
		byte[] vettore2 = {0, 1, 0, 0, 0};
		byte[] vettore3 = {0, 0, 1, 0, 0};
		byte[] vettore4 = {0, 0, 0, 1, 0};
		byte[] vettore5 = {1, 1, 1, 0, 1};
		
		List<List<Byte>> outsideList = Lists.newArrayList();
		outsideList.add(Bytes.asList(vettore1));
		outsideList.add(Bytes.asList(vettore2));
		outsideList.add(Bytes.asList(vettore3));
		outsideList.add(Bytes.asList(vettore4));
		outsideList.add(Bytes.asList(vettore5));
		
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
