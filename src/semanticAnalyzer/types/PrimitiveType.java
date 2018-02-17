package semanticAnalyzer.types;

import tokens.LextantToken;

public enum PrimitiveType implements Type {
	BOOLEAN(1),
	CHARACTER(1),
	STRING(4),
	INTEGER(4),
	FLOAT(8),
	RATIONAL(8),
	ERROR(0),			// use as a value when a syntax error has occurred
	NO_TYPE(0, "");	// use as a value when no type has been assigned.
	
	private int sizeInBytes;
	private String infoString;
	
	private PrimitiveType(int size) {
		this.sizeInBytes = size;
		this.infoString = toString();
	}
	private PrimitiveType(int size, String infoString) {
		this.sizeInBytes = size;
		this.infoString = infoString;
	}
	public int getSize() {
		return sizeInBytes;
	}
	public String infoString() {
		return infoString;
	}
	public boolean equivalent(Type other) {
		return this == other;
	}
	public Type getConcreteType() {
		return this;
	}
	public static PrimitiveType fromTypeVariable(LextantToken token) {
		return valueOf(token.getLextant().toString());
	}
}
