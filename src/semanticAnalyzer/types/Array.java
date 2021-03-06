package semanticAnalyzer.types;

public class Array implements Type {

	private Type subType;

	public Array(Type type) {
		subType = type;
	}

	@Override
	public int getSize() {
		// TODO confirm this again
		return PrimitiveType.STRING.getSize();
	}

	@Override
	public String infoString() {
		return "[" + subType.infoString() + "]";
	}

	public Type getSubType() {
		return subType;
	}

	public boolean equivalent(Type other) {
		if(other instanceof Array) {
			Array arr = (Array) other;
			return subType.equivalent(arr.getSubType());
		}
		return false;
	}

	public Type getConcreteType() {
		return new Array(subType.getConcreteType());
	}

	public String toString() {
		return infoString();
	}

	public Type innerMostType() {
		if(subType instanceof Array) {
			return ((Array) subType).innerMostType();
		}
		return subType;
	}
}
