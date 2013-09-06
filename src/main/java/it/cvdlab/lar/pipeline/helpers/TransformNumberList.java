package it.cvdlab.lar.pipeline.helpers;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class TransformNumberList {
	public static <T extends Number> List<Integer> toInteger(List<T> inputList) {
		Function<T, Integer> predicateTransform = new Function<T, Integer>() {
			public Integer apply(T input) {
				return input.intValue();
			}
		};
		
		return Lists.newArrayList( Lists.transform(inputList, predicateTransform) );
	}
	
	public static <T extends Number> List<Float> toFloat(List<T> inputList) {
		Function<T, Float> predicateTransform = new Function<T, Float>() {
			public Float apply(T input) {
				return input.floatValue();
			}
		};
		
		return Lists.newArrayList( Lists.transform(inputList, predicateTransform) );		
	}
	
	public static <T extends Number> List<Byte> toByte(List<T> inputList) {
		Function<T, Byte> predicateTransform = new Function<T, Byte>() {
			public Byte apply(T input) {
				return input.byteValue();
			}
		};
		
		return Lists.newArrayList( Lists.transform(inputList, predicateTransform) );		
	}	
}
