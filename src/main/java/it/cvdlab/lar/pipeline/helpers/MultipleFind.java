package it.cvdlab.lar.pipeline.helpers;

public class MultipleFind {
	private MultipleFind() {}
	
	// L'intero successivo multiplo della base
	public static int toMultipleOf(int N, int base) {
		return (int) (Math.ceil((double)N / (double)base) * base);
	}
}