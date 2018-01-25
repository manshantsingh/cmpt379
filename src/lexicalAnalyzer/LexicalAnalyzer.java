package lexicalAnalyzer;


import logging.PikaLogger;
import inputHandler.InputHandler;
import inputHandler.LocatedChar;
import inputHandler.LocatedCharStream;
import inputHandler.PushbackCharStream;
import inputHandler.TextLocation;
import tokens.FloatConstantToken;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.NullToken;
import tokens.IntegerConstantToken;
import tokens.Token;

import static lexicalAnalyzer.PunctuatorScanningAids.*;

import java.util.ArrayList;

public class LexicalAnalyzer extends ScannerImp implements Scanner {
	public static LexicalAnalyzer make(String filename) {
		InputHandler handler = InputHandler.fromFilename(filename);
		PushbackCharStream charStream = PushbackCharStream.make(handler);
		return new LexicalAnalyzer(charStream);
	}

	public LexicalAnalyzer(PushbackCharStream input) {
		super(input);
	}

	
	//////////////////////////////////////////////////////////////////////////////
	// Token-finding main dispatch	

	@Override
	protected Token findNextToken() {
		LocatedChar ch = nextNonWhitespaceChar();
		
		while(ch.isCommentStart()) {
			ch = endOfCommentNonWhiteSpaceChar();
		}

		if(ch.isNumberStart()) {
			Token token = scanNumber(ch);
			if(token != null) return token;
		}
		if(ch.isAlphaOrUnderscore()) {
			return scanIdentifier(ch);
		}
		else if(isPunctuatorStart(ch)) {
			return PunctuatorScanner.scan(ch, input);
		}
		else if(isEndOfInput(ch)) {
			return NullToken.make(ch.getLocation());
		}
		else {
			lexicalError(ch);
			return findNextToken();
		}
	}

	private LocatedChar nextNonWhitespaceChar() {
		LocatedChar ch = input.next();
		while(ch.isWhitespace()) {
			ch = input.next();
		}
		return ch;
	}
	
	private LocatedChar endOfCommentNonWhiteSpaceChar() {
		LocatedChar ch = input.next();
		while(!ch.isCommentEnd()) {
			ch = input.next();
		}
		ch = input.next();
		while(ch.isWhitespace()) {
			ch = input.next();
		}
		return ch;
	}


	//////////////////////////////////////////////////////////////////////////////
	// Integer lexical analysis	

	private Token scanNumber(LocatedChar firstChar) {
		ArrayList<LocatedChar> list = new ArrayList<LocatedChar>();
		if(firstChar.isPlusOrMinus()) {
			list.add(firstChar);
		}
		else {
			input.pushback(firstChar);
		}
		boolean b = appendSubsequentDigits(list);
		LocatedChar c = input.next();
		if(c.isDecimal()) {
			list.add(c);
			b = appendSubsequentDigits(list);
			if(b) {
				c = input.next();
				if(c.getCharacter() == 'E') {
					list.add(c);
					c=input.next();
					if(c.isPlusOrMinus()) {
						list.add(c);
					}
					else {
						input.pushback(c);
					}
					b = appendSubsequentDigits(list);
				}
				else input.pushback(c);
				if(b) {
					return FloatConstantToken.make(firstChar.getLocation(), locatedCharListToString(list));
				}
			}
			else {
				if(!b && list.size()>1) {
					input.pushback(list.get(list.size()-1));
					list.remove(list.size()-1);
					return IntegerConstantToken.make(firstChar.getLocation(), locatedCharListToString(list));
				}
			}
		}
		else {
			if(b && list.size()>0) {
				input.pushback(c);
				return IntegerConstantToken.make(firstChar.getLocation(), locatedCharListToString(list));
			}
		}

		// Note: we are ignoring the last character here since findNextToken
		//       has already scanned it
		for(int i=list.size()-1; i>0; i--) {
			input.pushback(list.get(i));
		}
		return null;
	}
	private boolean appendSubsequentDigits(ArrayList<LocatedChar> list) {
		LocatedChar c = input.next();
		boolean found=false;
		while(c.isDigit()) {
			list.add(c);
			c = input.next();
			found=true;
		}
		input.pushback(c);
		return found;
	}

	private String locatedCharListToString(ArrayList<LocatedChar> list) {
		StringBuffer buffer = new StringBuffer();
		for(LocatedChar c: list) {
			buffer.append(c.getCharacter());
		}
		return buffer.toString();
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Identifier and keyword lexical analysis	

	private Token scanIdentifier(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(firstChar.getCharacter());
		appendSubsequentIdentifierCharacters(buffer);

		String lexeme = buffer.toString();
		if(Keyword.isAKeyword(lexeme)) {
			return LextantToken.make(firstChar.getLocation(), lexeme, Keyword.forLexeme(lexeme));
		}
		else {
			return IdentifierToken.make(firstChar.getLocation(), lexeme);
		}
	}
	private void appendSubsequentIdentifierCharacters(StringBuffer buffer) {
		LocatedChar c = input.next();
		while(c.isIdentifierSubsequentCharacter()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		input.pushback(c);
	}


	//////////////////////////////////////////////////////////////////////////////
	// Character-classification routines specific to Pika scanning.	

	private boolean isPunctuatorStart(LocatedChar lc) {
		char c = lc.getCharacter();
		return isPunctuatorStartingCharacter(c);
	}

	private boolean isEndOfInput(LocatedChar lc) {
		return lc == LocatedCharStream.FLAG_END_OF_INPUT;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Error-reporting	

	private void lexicalError(LocatedChar ch) {
		PikaLogger log = PikaLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: invalid character " + ch);
	}

	
}
