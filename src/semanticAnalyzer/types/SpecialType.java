package semanticAnalyzer.types;

import tokens.LextantToken;

public enum SpecialType implements Type {
	
	VOID(0);

	private int sizeInBytes;
	private String infoString;
	
	private SpecialType(int size) {
		this.sizeInBytes = size;
		this.infoString = toString();
	}
	private SpecialType(int size, String infoString) {
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

}
