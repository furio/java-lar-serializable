package it.cvdlab.lar.model;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputVectorsContainer {

	@JsonProperty("lista_vettori")
	private List<List<Byte>> vectorList;

	@JsonProperty("lista_offset")
	private List<List<Integer>> vectorOffset;
	
	@JsonIgnore
	private static final Logger logger = LoggerFactory.getLogger(InputVectorsContainer.class);
	
	public InputVectorsContainer() {}	

	public List<List<Byte>> getVectorList() {
		return vectorList;
	}

	public void setVectorList(List<List<Byte>> vectorList) {
		this.vectorList = vectorList;
	}

	public List<List<Integer>> getVectorOffset() {
		return vectorOffset;
	}

	public void setVectorOffset(List<List<Integer>> vectorOffset) {
		this.vectorOffset = vectorOffset;
	}
	
	/*
	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper jacksonMapper = new ObjectMapper();
		VectorsContainer v = new VectorsContainer();
		Integer[] robbaExtra = {3,4,5};
		List<Integer> inside = Lists.asList(1, 2, robbaExtra);
		List<List<Integer>> outsideList = Lists.newArrayList();
		v.setVectorList(outsideList);
		outsideList.add(inside);
		outsideList.add(inside);
		outsideList.add(inside);
		jacksonMapper.writeValue(System.out, v);
	}*/

}
