package tokens;

import inputHandler.TextLocation;

public class IntegerConstantToken extends TokenImp {
	protected int value;
	
	protected IntegerConstantToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	protected void setValue(int value) {
		this.value = value;
	}
	public int getValue() {
		return value;
	}
	
	public static IntegerConstantToken make(TextLocation location, String lexeme) {
		IntegerConstantToken result = new IntegerConstantToken(location, lexeme);
		result.setValue(Integer.parseInt(lexeme));
		return result;
	}
	
	@Override
	protected String rawString() {
		return "integerConst, " + value;
	}
}
