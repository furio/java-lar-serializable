package it.cvdlab.lar.pipeline.kernelwrap;

import it.cvdlab.lar.model.CsrMatrix;
import it.cvdlab.lar.pipeline.helpers.ResultTuple;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

public class LARTestBinaryJava {
	
	public static List<ResultTuple> clMultiply(CsrMatrix matrixA, int[] vector, int vectorSize, int oldVectorSize) {
		int howManyVectors = vector.length / vectorSize;
		System.out.println("Vector count: " + howManyVectors);
		System.out.println("Single vector size: " + vectorSize);
		System.out.println("Row count: " + matrixA.getRowCount());
		
		// Stora i risultati
		List<ResultTuple> lsOutput = Lists.newArrayList();
		
		// program.addBuildOption("-D" + D_OLDVECTORSIZE + "=" + oldVectorSize);
		// program.addBuildOption("-D" + D_INPUTVECTORSIZE + "=" + vectorSize);
		// program.addBuildOption("-D" + D_TOTALVECTORSSIZE + "=" + howManyVectors);		
		int INTEGER_BIT_SIZE = 32;
		int OLDVECTORSIZE = oldVectorSize;
		
		List<Byte> bResult; 
		for(int j = 0; j < howManyVectors; ++j) {
			bResult = Lists.newArrayList();
			int[] localVector = Arrays.copyOfRange(vector, j*vectorSize, j*vectorSize + vectorSize);
			// System.out.println(Arrays.toString(localVector));
			for (int row = 0; row < matrixA.getRowCount(); ++row) {
		  		int dot_prod = 0;
				int row_end = matrixA.getRowPointer().get(row+1);
		   
				for (int i = matrixA.getRowPointer().get(row); i < row_end; ++i) {
					int currCol = matrixA.getColumnIndices().get(i);
					int bitToCheck = currCol - INTEGER_BIT_SIZE*(currCol/INTEGER_BIT_SIZE);
					int tmpRes = ((localVector[ currCol / INTEGER_BIT_SIZE ] & (1 << bitToCheck)) >>> bitToCheck);
					
					dot_prod += tmpRes;
				}
				
				bResult.add( (byte)(dot_prod % 2) );
			}
			
			lsOutput.add(new ResultTuple(bResult, 1, 0));
		}
		
		

		return lsOutput;
	}


}
