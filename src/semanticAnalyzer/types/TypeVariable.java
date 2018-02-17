package semanticAnalyzer.types;

public class TypeVariable implements Type {

	public static TypeVariable instance;

	public static TypeVariable getInstance() {
		if(instance == null) {
			instance = new TypeVariable("S");
		}
		return instance;
	}

	public static void resetTypeVariable() {
		getInstance().setType(PrimitiveType.NO_TYPE);
	}

	private String name;
	private Type typeConstraint;

	public TypeVariable(String name) {
		this.name = name;
		this.typeConstraint = PrimitiveType.NO_TYPE;
	}

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public String infoString() {
		return toString();
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return typeConstraint;
	}

	private void setType(Type type) {
		typeConstraint = type;
	}

	@Override
	public boolean equivalent(Type other) {
		if(other instanceof TypeVariable) {
			throw new RuntimeException("equal attempts on two types containing type variables");
		}
		if(getType() == PrimitiveType.NO_TYPE) {
			setType(other);
			return true;
		}
		return getType().equivalent(other);
	}

	public String toString() {
		return "<" + getName() + ">";
	}

	public Type getConcreteType() {
		return getType().getConcreteType();
	}
}
