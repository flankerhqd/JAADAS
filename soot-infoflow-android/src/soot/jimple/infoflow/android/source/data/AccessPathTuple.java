package soot.jimple.infoflow.android.source.data;

import java.util.Arrays;

/**
 * Helper to save an AccessPath with the information about sink and sources.
 * 
 * @author Joern Tillmanns
 * @author Steven Arzt
 */
public class AccessPathTuple {
	
	private String[] fields;
	private String[] fieldTypes;
	private SinkSource sinkSource;

	public AccessPathTuple(String[] fields, String[] fieldTypes, SinkSource sinkSource) {
		this.fields = fields;
		this.fieldTypes = fieldTypes;
		this.sinkSource = sinkSource;
	}
	
	public static AccessPathTuple fromPathElements(String[] fields,
			String[] fieldTypes, boolean isSource, boolean isSink) {
		SinkSource sinkSource = SinkSource.None;
		if (isSource && isSink)
			sinkSource = SinkSource.Both;
		else if (isSource)
			sinkSource = SinkSource.Source;
		else if (isSink)
			sinkSource = SinkSource.Sink;
		return new AccessPathTuple(fields, fieldTypes, sinkSource);
	}
	
	public String[] getFields() {
		return this.fields;
	}
	
	public String[] getFieldTypes() {
		return this.fieldTypes;
	}
	
	public SinkSource getSinkSource() {
		return this.sinkSource;
	}
	
	/**
	 * Checks whether this definition models a source
	 * @return True if this definition models a source, otherwise false
	 */
	public boolean isSource() {
		return this.sinkSource == SinkSource.Source
				|| this.sinkSource == SinkSource.Both;
	}
	
	/**
	 * Checks whether this definition models a sink
	 * @return True if this definition models a sink, otherwise false
	 */
	public boolean isSink() {
		return this.sinkSource == SinkSource.Sink
				|| this.sinkSource == SinkSource.Both;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(fieldTypes);
		result = prime * result + Arrays.hashCode(fields);
		result = prime * result
				+ ((sinkSource == null) ? 0 : sinkSource.hashCode());
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
		AccessPathTuple other = (AccessPathTuple) obj;
		if (!Arrays.equals(fieldTypes, other.fieldTypes))
			return false;
		if (!Arrays.equals(fields, other.fields))
			return false;
		if (sinkSource != other.sinkSource)
			return false;
		return true;
	}

}

/**
 * Helper for sinks and sources.
 * 
 * @author Joern Tillmanns
 */
enum SinkSource {
	Sink, Source, Both, None
}
