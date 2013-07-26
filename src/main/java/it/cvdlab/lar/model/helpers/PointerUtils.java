package it.cvdlab.lar.model.helpers;

import java.util.List;

import org.bridj.Pointer;

import com.google.common.collect.Lists;

public final class PointerUtils {
	public static <T> void copyToPointer(List<T> iList, Pointer<T> oPointer) {
		for(int i = 0; i < iList.size(); i++) {
			oPointer.set(i, iList.get(i));
		}
	}
	
	public static <T extends Number> List<T> copyFromPointer(Pointer<T> lPointer) {
		List<T> tmpList = Lists.newArrayList();
		for(T singleData: lPointer) {
			tmpList.add( singleData );
		}
		return Lists.newArrayList(tmpList);
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
		return Lists.newArrayList(bPointer.asList());
	}	
	
	public static List<Integer> copyFromPointerInteger(Pointer<Integer> iPointer) {
		List<Integer> tmpList = Lists.newArrayList();
		for(Integer singleData: iPointer) {
			tmpList.add( new Integer(singleData) );
		}
		return Lists.newArrayList(tmpList);
	}
	
	public static List<Float> copyFromPointerFloat(Pointer<Float> fPointer) {
		List<Float> tmpList = Lists.newArrayList();
		for(Float singleData: fPointer) {
			tmpList.add( new Float(singleData) );
		}
		return Lists.newArrayList(tmpList);
	}
}