package it.cvdlab.lar.model;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputVectorsContainer {

	@JsonProperty("lista_vettori")
	private List<List<Integer>> vectorList;

	@JsonProperty("lista_offset")
	private List<List<Integer>> vectorOffset;
	
	@JsonProperty("bench")
	private List<Integer> vectorStats;	
	
	@JsonIgnore
	private static final Logger logger = LoggerFactory.getLogger(InputVectorsContainer.class);
	
	public OutputVectorsContainer() {}	

	public List<List<Integer>> getVectorList() {
		return vectorList;
	}

	public void setVectorList(List<List<Integer>> vectorList) {
		this.vectorList = vectorList;
	}

	public List<List<Integer>> getVectorOffset() {
		return vectorOffset;
	}

	public void setVectorOffset(List<List<Integer>> vectorOffset) {
		this.vectorOffset = vectorOffset;
	}
}
