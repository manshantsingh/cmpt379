package tokens;

import inputHandler.TextLocation;

public class CharacterConstantToken extends TokenImp {
	protected char value;

	protected CharacterConstantToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	protected void setValue(char value) {
		this.value = value;
	}
	public char getValue() {
		return value;
	}

	public static CharacterConstantToken make(TextLocation location, String lexeme) {
		CharacterConstantToken result = new CharacterConstantToken(location, lexeme);
		result.setValue(lexeme.charAt(0));
		return result;
	}

	@Override
	protected String rawString() {
		return "characterConst, ^" + value + "^";
	}
}
