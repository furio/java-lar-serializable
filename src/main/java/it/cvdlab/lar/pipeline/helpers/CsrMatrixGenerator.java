package it.cvdlab.lar.pipeline.helpers;

import it.cvdlab.lar.model.CsrMatrix;

import java.util.List;
import java.util.Random;
import java.util.SortedSet;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class CsrMatrixGenerator {
	private CsrMatrixGenerator() {};
	
	//
	private static final int MAX_COLS = 128000;
	
	//
	private static final int DEFAULT_SIZE_MB = 20;
	private static final int DEFAULT_NNZ_PER_ROW = 7;
	private static final boolean DEFAULT_HALF_NNZ_PER_ROW_DIAGONAL = true;
	
	private static Random RND_GENERATOR = new Random();
	
	public static CsrMatrix generateMatrix() {
		return generateMatrix(DEFAULT_SIZE_MB, DEFAULT_NNZ_PER_ROW, DEFAULT_HALF_NNZ_PER_ROW_DIAGONAL);
	}
	
	public static CsrMatrix generateMatrix(int csrsize_mb) {
		return generateMatrix(csrsize_mb, DEFAULT_NNZ_PER_ROW, DEFAULT_HALF_NNZ_PER_ROW_DIAGONAL);
	}
	
	public static CsrMatrix generateMatrix(int csrsize_mb, int nnz_per_row) {
		return generateMatrix(csrsize_mb, nnz_per_row, DEFAULT_HALF_NNZ_PER_ROW_DIAGONAL);
	}
	
	public static CsrMatrix generateMatrix(int csrsize_mb, int nnz_per_row, boolean half_nnz_diagonal) {
		return createMatrix(csrsize_mb, nnz_per_row, half_nnz_diagonal);
	}
	
	private static CsrMatrix createMatrix(int csrsize_mb, int nnz_per_row, boolean half_nnz_diagonal) {
//		System.out.println(csrsize_mb + "-" + nnz_per_row + "-" + half_nnz_diagonal);
		csrsize_mb = csrsize_mb * 1024 * 1024;
		
		// numero righe = (size/type_size - 1) / (DEFAULT_NNZ_PER_ROW+1)
		int numRows = ((csrsize_mb / (Integer.SIZE / 8)) - 1) / (nnz_per_row + 1);
		int numCols = MAX_COLS - 1;
		int nnzCount = numRows * nnz_per_row;
		
		int nnz_on_diagonal = (half_nnz_diagonal) ? (nnz_per_row / 2) : 0;
		int nnz_rnd = nnz_per_row - nnz_on_diagonal;
		
//		System.out.println(numRows + "-" + numCols + "-" + nnzCount);
//		System.out.println(nnz_on_diagonal + "-" + nnz_rnd);
		
		List<Integer> rowPtr = Lists.newArrayList();
		List<Integer> colIdx = Lists.newArrayList();
		
		SortedSet<Integer> currCol = Sets.newTreeSet();
		
		int startColIdx;
		int endColIdx;
		
		// init rowptr
		rowPtr.add(0);
		for(int i = 0; i < numRows; ++i) {
			currCol.clear();
			
			for(int j = 0; j < nnz_on_diagonal; ++j) {
				currCol.add(i+j);
			}
			
			if (nnz_on_diagonal > 0) {
				// check where is the biggest space
				if ( ( currCol.first() - 0 ) >=
						( numCols - currCol.last() ) ) {
					// Space on right
					startColIdx = 0;
					endColIdx = currCol.first();
				} else {
					// Space on left
					startColIdx = currCol.last() + 1;
					endColIdx = numCols;					
				}
			} else {
				startColIdx = 0;
				endColIdx = numCols;
			}
			
			for( ; currCol.size() < nnz_per_row; ) {
				currCol.add( getRandom( startColIdx, endColIdx ) );
			}
			
			colIdx.addAll(currCol);
			rowPtr.add( (i+1) * nnz_per_row );
		}
		
		return new CsrMatrix(rowPtr, colIdx, numRows, numCols);
	}
	
	private static int getRandom(int min, int max) {
		return RND_GENERATOR.nextInt(max - min) + min;
	}
}
