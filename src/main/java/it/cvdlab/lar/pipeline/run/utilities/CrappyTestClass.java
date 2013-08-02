package it.cvdlab.lar.pipeline.run.utilities;

import it.cvdlab.lar.pipeline.helpers.cuda.CudaDeviceStats;
import it.cvdlab.lar.pipeline.helpers.cuda.CudaQuery;


public class CrappyTestClass {

	public static void main(String[] args) {
		for(CudaDeviceStats cCurr: CudaQuery.getDevicesInfos()) {
			System.out.println(cCurr);
		}
	}
/*
	public static void main(String[] args) {
		int[] matrixOne = new int[] { 1, 0, 0, 0, 1,
									  0, 1, 0, 0, 0, 
									  0, 0, 0, 0, 1, 
									  0, 0, 1, 1, 0, 
									  1, 0, 0, 0, 1 };
		
		CsrMatrix csrMatrixOne = CsrMatrix.fromFlattenArray(matrixOne, 5);
		System.out.println(csrMatrixOne.toString());
		
		int[] vettori = new int[] { 1, 1, 1, 0, 1,
										0, 1, 0, 0, 0,
										0, 0, 1, 0, 0,
										0, 0, 0, 1, 0,
										0, 0, 0, 0, 1,
										1, 1, 1, 0, 1};
		
		//clMultiply(csrMatrixOne, vettori, 5);
		
		// System.out.println(Integer.SIZE);

		int[] flatResult = ArrayUtils.flatten(BinaryTranslator.fromFlatArray(vettori, 5));
		System.out.println(Arrays.toString(flatResult));
		
		clMultiply(csrMatrixOne, flatResult, 1, 5);
	}
*/
	
}