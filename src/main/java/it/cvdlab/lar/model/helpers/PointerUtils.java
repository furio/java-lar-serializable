package it.cvdlab.lar.model.helpers;

import java.util.List;

import org.bridj.Pointer;

import com.google.common.collect.Lists;

public final class PointerUtils {
	public static <T extends Number> void copyToPointer(List<T> iList, Pointer<T> oPointer) {
		for(int i = 0; i < iList.size(); i++) {
			oPointer.set(i, iList.get(i));
		}
	}
	
	public static <T extends Number> List<T> copyFromPointer(Pointer<T> lPointer) {
		return Lists.newArrayList(lPointer.asList());
	}

	/*
	public static List<Byte> copyFromPointerByte(Pointer<Byte> bPointer) {
		List<Byte> tmpList = Lists.newArrayList();
		for(Byte singleData: bPointer) {
			tmpList.add( new Byte(singleData) );
		}
		return Lists.newArrayList(tmpList);
	}
	*/
	
	public static List<Byte> copyFromPointerByte(Pointer<Byte> bPointer) {
		return copyFromPointer(bPointer);
	}	
	
	public static List<Integer> copyFromPointerInteger(Pointer<Integer> iPointer) {
		return copyFromPointer(iPointer);
	}
	
	public static List<Float> copyFromPointerFloat(Pointer<Float> fPointer) {
		return copyFromPointer(fPointer);
	}
}