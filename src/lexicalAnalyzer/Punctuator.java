package lexicalAnalyzer;

import tokens.LextantToken;
import tokens.Token;


public enum Punctuator implements Lextant {
	ADD("+"),
	SUBTRACT("-"),
	MULTIPLY("*"),
	DIVIDE("/"),

	GREATER(">"),
	GREATER_EQUAL(">="),
	LESS("<"),
	LESS_EQUAL("<="),
	EQUALITY("=="),
	INEQUALITY("!="),

	ASSIGN(":="),
	SEPARATOR(","),
	SPACE(";"),
	TERMINATOR("."),

	OPEN_BRACE("{"),
	CLOSE_BRACE("}"),
	OPEN_ROUND("("),
	CLOSE_ROUND(")"),
	OPEN_SQUARE("["),
	CLOSE_SQUARE("]"),
	PIPE("|"),

	NULL_PUNCTUATOR("");

	private String lexeme;
	private Token prototype;
	
	private Punctuator(String lexeme) {
		this.lexeme = lexeme;
		this.prototype = LextantToken.make(null, lexeme, this);
	}
	public String getLexeme() {
		return lexeme;
	}
	public Token prototype() {
		return prototype;
	}

	public static Punctuator forLexeme(String lexeme) {
		for(Punctuator punctuator: values()) {
			if(punctuator.lexeme.equals(lexeme)) {
				return punctuator;
			}
		}
		return NULL_PUNCTUATOR;
	}

	public static final Punctuator COMPARISONS[] = new Punctuator[] {
			GREATER, GREATER_EQUAL,
			LESS, LESS_EQUAL,
			EQUALITY, INEQUALITY
	};

	public static boolean isComparison(Lextant lextant) {
		for(Punctuator cmp: COMPARISONS) {
			if(cmp == lextant) return true;
		}
		return false;
	}
	
/*
	//   the following hashtable lookup can replace the implementation of forLexeme above. It is faster but less clear. 
	private static LexemeMap<Punctuator> lexemeToPunctuator = new LexemeMap<Punctuator>(values(), NULL_PUNCTUATOR);
	public static Punctuator forLexeme(String lexeme) {
		   return lexemeToPunctuator.forLexeme(lexeme);
	}
*/
	
}


