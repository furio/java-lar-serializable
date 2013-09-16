package it.cvdlab.lar.pipeline.helpers;

import java.util.Arrays;

public class ArrayUtils {
	private ArrayUtils() {}
	
    public static void fill(byte[] a, byte val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }
    
	public static byte[] flatten(byte[][] first) {
		byte[] result = null;
		
		for(byte[] curr: first) {
			result = concat(result, curr);
		}
		
		return result;
	}
	
	public static int[] flatten(int[][] first) {
		int[] result = null;
		
		for(int[] curr: first) {
			result = concat(result, curr);
		}
		
		return result;
	}
	
	public static long[] flatten(long[][] first) {
		long[] result = null;
		
		for(long[] curr: first) {
			result = concat(result, curr);
		}
		
		return result;
	}
	
	public static <T> T[] flatten(T[][] first) {
		T[] result = null;
		
		for(T[] curr: first) {
			result = concat(result, curr);
		}
		
		return result;
	}	
	
	public static byte[] concat(byte[] first, byte[] second) {
		if (first == null) {
			if (second == null) {
				return null;
			}
			return second;
		}
		if (second == null) {
			return first;
		}
		
		byte[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	
	public static int[] concat(int[] first, int[] second) {
		if (first == null) {
			if (second == null) {
				return null;
			}
			return second;
		}
		if (second == null) {
			return first;
		}
		
		int[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	
	public static long[] concat(long[] first, long[] second) {
		if (first == null) {
			if (second == null) {
				return null;
			}
			return second;
		}
		if (second == null) {
			return first;
		}
		
		long[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}	
	
	public static <T> T[] concat(T[] first, T[] second) {
		if (first == null) {
			if (second == null) {
				return null;
			}
			return second;
		}
		if (second == null) {
			return first;
		}
		
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static byte[] concatAll(byte[] first, byte[]... rest) {
		int totalLength = (first == null) ? 0 : first.length;
		for (byte[] array : rest) {
			totalLength += (array == null) ? 0 : array.length;
		}
		
		byte[] result = (first == null) ? null : Arrays.copyOf(first, totalLength);
		int offset = (first == null) ? 0 : first.length;
		
		for (byte[] array : rest) {
			if (array != null) {
				System.arraycopy(array, 0, result, offset, array.length);
				offset += array.length;				
			}
		}
		
		return result;
	}

	public static int[] concatAll(int[] first, int[]... rest) {
		int totalLength = (first == null) ? 0 : first.length;
		for (int[] array : rest) {
			totalLength += (array == null) ? 0 : array.length;
		}
		
		int[] result = (first == null) ? null : Arrays.copyOf(first, totalLength);
		int offset = (first == null) ? 0 : first.length;
		
		for (int[] array : rest) {
			if (array != null) {
				System.arraycopy(array, 0, result, offset, array.length);
				offset += array.length;				
			}
		}
		
		return result;
	}	
	
	public static long[] concatAll(long[] first, long[]... rest) {
		int totalLength = (first == null) ? 0 : first.length;
		for (long[] array : rest) {
			totalLength += (array == null) ? 0 : array.length;
		}
		
		long[] result = (first == null) ? null : Arrays.copyOf(first, totalLength);
		int offset = (first == null) ? 0 : first.length;
		
		for (long[] array : rest) {
			if (array != null) {
				System.arraycopy(array, 0, result, offset, array.length);
				offset += array.length;				
			}
		}
		
		return result;
	}	
	
	@SafeVarargs
	public static <T> T[] concatAll(T[] first, T[]... rest) {
		int totalLength = (first == null) ? 0 : first.length;
		for (T[] array : rest) {
			totalLength += (array == null) ? 0 : array.length;
		}
		
		T[] result = (first == null) ? null : Arrays.copyOf(first, totalLength);
		int offset = (first == null) ? null : first.length;
		
		for (T[] array : rest) {
			if (array != null) {
				System.arraycopy(array, 0, result, offset, array.length);
				offset += array.length;
			}
		}
		
		return result;
	}	
}
