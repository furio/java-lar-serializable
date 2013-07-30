package it.cvdlab.lar.pipeline.run.utilities;

import java.util.List;

import com.google.common.collect.Lists;

import it.cvdlab.lar.model.InputVectorsContainer;
import it.cvdlab.lar.model.OutputVectorsContainer;
import it.cvdlab.lar.model.serialize.InputVectorsSerialize;
import it.cvdlab.lar.model.serialize.OutputVectorsSerialize;

public class ReduceFileData {
	private ReduceFileData() {}
	
	public static final String INPUT_FILE = "C:/Android/output_test/selettori.json";
	public static final String OUTPUT_FILE = "C:/Android/output_test/out_vittorio.json";
	private static final int MAXIMUM_ARR = 2;
	
	public static void main(String[] args) {

		// cutInput();
		cutOutput();

	}
	
	private static void cutInput() {
		System.out.println("Deserializzo");
		InputVectorsContainer ivc = InputVectorsSerialize.fromFile(INPUT_FILE);
		
		System.out.println("Taglio");
		InputVectorsContainer ivcNew = new InputVectorsContainer();
		List<List<Integer>> listNewVett = Lists.newArrayList();
		List<List<Integer>> listNewOff = Lists.newArrayList();
		
		int toCut = MAXIMUM_ARR;
		for(List<Integer> currList: ivc.getVectorList()) {
			listNewVett.add(currList);
			if (--toCut == 0) {
				break;
			}
		}
		
		toCut = MAXIMUM_ARR;
		for(List<Integer> currList: ivc.getVectorOffset()) {
			listNewOff.add(currList);
			if (--toCut == 0) {
				break;
			}
		}
		
		ivcNew.setVectorList(listNewVett);
		ivcNew.setVectorOffset(listNewOff);
		
		System.out.println("Serializzo");
		InputVectorsSerialize.toFile(ivcNew, INPUT_FILE + ".new");
	}
	
	private static void cutOutput() {
		System.out.println("Deserializzo");
		OutputVectorsContainer ivc = OutputVectorsSerialize.fromFile(OUTPUT_FILE);
		
		System.out.println("Taglio");
		OutputVectorsContainer ivcNew = new OutputVectorsContainer();
		List<List<Byte>> listNewVett = Lists.newArrayList();
		List<List<Integer>> listNewOff = Lists.newArrayList();
		
		int toCut = MAXIMUM_ARR;
		for(List<Byte> currList: ivc.getVectorList()) {
			listNewVett.add(currList);
			if (--toCut == 0) {
				break;
			}
		}
		
		toCut = MAXIMUM_ARR;
		for(List<Integer> currList: ivc.getVectorOffset()) {
			listNewOff.add(currList);
			if (--toCut == 0) {
				break;
			}
		}
		
		ivcNew.setVectorList(listNewVett);
		ivcNew.setVectorOffset(listNewOff);
		
		System.out.println("Serializzo");
		OutputVectorsSerialize.toFile(ivcNew, OUTPUT_FILE + ".new");
	}	
}
