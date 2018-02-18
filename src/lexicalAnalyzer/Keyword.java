package lexicalAnalyzer;

import tokens.LextantToken;
import tokens.Token;


public enum Keyword implements Lextant {
	CONST("const"),
	VAR("var"),
	PRINT("print"),
	NEWLINE("_n_"),
	TAB_SPACE("_t_"),
	TRUE("_true_"),
	FALSE("_false_"),
	EXEC("exec"),

	NEW("new"),
	LENGTH("length"),
	CLONE("clone"),

	BOOLEAN("bool"),
	CHARACTER("char"),
	STRING("string"),
	INTEGER("int"),
	FLOAT("float"),
	RATIONAL("rat"),

	NULL_KEYWORD("");

	private String lexeme;
	private Token prototype;
	
	
	private Keyword(String lexeme) {
		this.lexeme = lexeme;
		this.prototype = LextantToken.make(null, lexeme, this);
	}
	public String getLexeme() {
		return lexeme;
	}
	public Token prototype() {
		return prototype;
	}
	
	public static Keyword forLexeme(String lexeme) {
		for(Keyword keyword: values()) {
			if(keyword.lexeme.equals(lexeme)) {
				return keyword;
			}
		}
		return NULL_KEYWORD;
	}
	public static boolean isAKeyword(String lexeme) {
		return forLexeme(lexeme) != NULL_KEYWORD;
	}
	
	/*   the following hashtable lookup can replace the serial-search implementation of forLexeme() above. It is faster but less clear. 
	private static LexemeMap<Keyword> lexemeToKeyword = new LexemeMap<Keyword>(values(), NULL_KEYWORD);
	public static Keyword forLexeme(String lexeme) {
		return lexemeToKeyword.forLexeme(lexeme);
	}
	*/
}
