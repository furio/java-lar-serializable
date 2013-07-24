package it.cvdlab.lar.pipeline.helpers;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

public class BinaryTranslator {
	private BinaryTranslator() {}

	// source.length % vectorLength = 0
	public static int[][] fromFlatArray(int[] source, int vectorLength) throws IndexOutOfBoundsException {
		if ((source.length % vectorLength) != 0) {
			throw new IndexOutOfBoundsException("Source length isn't a multiple of vectorLength");
		}
		
		int[][] chainTo = new int[source.length/vectorLength][vectorLength];
		
		for(int i = 0, j = 0; i < source.length; i += vectorLength, j++) {
			chainTo[j] = Arrays.copyOfRange(source, i, i+vectorLength);
		}
		
		return fromArrays(chainTo);
	}	

	public static int[][] fromArrays(List<List<Integer>> source) {
		int[][] returnArray = new int[source.size()][];
		
		for(int i = 0; i < source.size(); ++i) {
			returnArray[i] = fromArray(ArrayUtils.toPrimitive( source.get(i).toArray(new Integer[1]) ));
		}
		
		return returnArray;
	}
	
	public static int[][] fromArrays(int[][] source) {
		int[][] returnArray = new int[source.length][];
		
		for(int i = 0; i < source.length; ++i) {
			returnArray[i] = fromArray(source[i]);
		}
		
		return returnArray;
	}	
	
	public static int[] fromArray(int[] source) {
		return toBitsetIntegerArray(source);
	}
	
	private static int[] toBitsetIntegerArray(int[] source) {
		BitSet bsTemp = new BitSet(source.length);
		
		for(int i = 0; i < source.length; ++i) {
			if (source[i] > 0) {
				bsTemp.set(i);
			}
		}
		
		return bits2Ints(bsTemp, source.length);
	}
	
	private static int[] bits2Ints(BitSet bs, int originalLength) {
		int[] temp = new int[bs.size() / Integer.SIZE];

		for (int i = 0; i < temp.length; ++i) {
			for (int j = 0; j < Integer.SIZE; ++j) {
				if (bs.get(i * Integer.SIZE + j)) {
					temp[i] |= 1 << j;
				}
			}
		}
		
		int ceil = (int)Math.ceil((double)originalLength / (double)Integer.SIZE);
		if ( (ceil * Integer.SIZE) < bs.size() ) {
			temp = Arrays.copyOfRange(temp, 0, ceil);
		}
		
		return temp;
	}
}
