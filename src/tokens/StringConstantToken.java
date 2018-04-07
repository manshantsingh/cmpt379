package tokens;

import inputHandler.TextLocation;

public class StringConstantToken extends TokenImp {
	protected String value;

	protected StringConstantToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	protected void setValue(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}

	public static StringConstantToken make(TextLocation location, String lexeme) {
		StringConstantToken result = new StringConstantToken(location, lexeme);
		result.setValue(lexeme);
		return result;
	}

	@Override
	protected String rawString() {
		return "stringConst, \"" + value + "\"";
	}
}
