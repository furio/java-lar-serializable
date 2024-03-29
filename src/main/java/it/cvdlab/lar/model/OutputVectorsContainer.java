package it.cvdlab.lar.model;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreProperties(ignoreUnknown=true)
public class OutputVectorsContainer {

	@JsonProperty("lista_offset")
	private List<List<Integer>> vectorOffset;
	
	@JsonProperty("lista_vettori")
	private List<List<Byte>> vectorList;
	
	@JsonProperty("bench")
	private List<Long> vectorStats;	
	
	@JsonIgnore
	private static final Logger logger = LoggerFactory.getLogger(InputVectorsContainer.class);
	
	public OutputVectorsContainer() {}	

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
	
	public List<Long> getVectorStats() {
		return vectorStats;
	}

	public void setVectorStats(List<Long> vectorStats) {
		this.vectorStats = vectorStats;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((vectorList == null) ? 0 : vectorList.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OutputVectorsContainer other = (OutputVectorsContainer) obj;
		if (vectorList == null) {
			if (other.vectorList != null)
				return false;
		} else if (!vectorList.equals(other.vectorList))
			return false;
		return true;
	}

	public boolean equalsWithOffset(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OutputVectorsContainer other = (OutputVectorsContainer) obj;
		if (vectorList == null) {
			if (other.vectorList != null)
				return false;
		} else if (!vectorList.equals(other.vectorList))
			return false;
		if (vectorOffset == null) {
			if (other.vectorOffset != null)
				return false;
		} else if (!vectorOffset.equals(other.vectorOffset))
			return false;
		return true;
	}
}
