package semanticAnalyzer.types;

import java.util.ArrayList;

public class LambdaType implements Type {
	private ArrayList<Type> params;
	private Type returnType;
	
	public LambdaType(ArrayList<Type> paramsList, Type retType) {
		params = paramsList;
		returnType = retType;
	}

	@Override
	public int getSize() {
		return PrimitiveType.STRING.getSize();
	}

	@Override
	public String infoString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{<");
		if(params.size()>0) {
			sb.append(params.get(0));
		}
		for(int i=1;i<params.size();i++) {
			sb.append(", ");
			sb.append(params.get(i));
		}
		sb.append("> -> ");
		sb.append(returnType.infoString());
		sb.append("}");
		return sb.toString();
	}

	@Override
	public boolean equivalent(Type other) {
		if(other instanceof LambdaType) {
			LambdaType l = (LambdaType) other;
			if(l.params.size() != params.size() || !l.returnType.equivalent(returnType)) {
				return false;
			}
			for(int i=0;i<params.size();i++) {
				if(!l.params.get(i).equivalent(params.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public Type getConcreteType() {
		ArrayList<Type> concreteParams = new ArrayList<Type>();
		for(Type t: params) {
			concreteParams.add(t.getConcreteType());
		}
		return new LambdaType(concreteParams, returnType.getConcreteType());
	}

	public String toString() {
		return infoString();
	}
	
	public ArrayList<Type> getParamTypes(){
		return params;
	}
	
	public Type getReturnType(){
		return returnType;
	}

}
