package tokens;

import inputHandler.TextLocation;

public class FloatConstantToken extends TokenImp {
	protected double value;
	
	protected FloatConstantToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	protected void setValue(double value) {
		this.value = value;
	}
	public double getValue() {
		return value;
	}
	
	public static FloatConstantToken make(TextLocation location, String lexeme) {
		FloatConstantToken result = new FloatConstantToken(location, lexeme);
		result.setValue(Double.parseDouble(lexeme));
		return result;
	}
	
	@Override
	protected String rawString() {
		return "floatConst, " + value;
	}
}
