package parser;

import java.util.Arrays;

import logging.PikaLogger;
import parseTree.*;
import parseTree.nodeTypes.ArrayNode;
import parseTree.nodeTypes.AssignmentNode;
import parseTree.nodeTypes.OperatorNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.CastNode;
import parseTree.nodeTypes.CharacterConstantNode;
import parseTree.nodeTypes.BlockStatementsNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.FloatConstantNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.TabSpaceNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import tokens.*;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import lexicalAnalyzer.Scanner;


public class Parser {
	private Scanner scanner;
	private Token nowReading;
	private Token previouslyRead;
	
	public static ParseNode parse(Scanner scanner) {
		Parser parser = new Parser(scanner);
		return parser.parse();
	}
	public Parser(Scanner scanner) {
		super();
		this.scanner = scanner;
	}
	
	public ParseNode parse() {
		readToken();
		return parseProgram();
	}

	////////////////////////////////////////////////////////////
	// "program" is the start symbol S
	// S -> EXEC mainBlock
	
	private ParseNode parseProgram() {
		if(!startsProgram(nowReading)) {
			return syntaxErrorNode("program");
		}
		ParseNode program = new ProgramNode(nowReading);
		
		expect(Keyword.EXEC);
		ParseNode mainBlock = parseBlockStatements();
		program.appendChild(mainBlock);
		
		if(!(nowReading instanceof NullToken)) {
			return syntaxErrorNode("end of program");
		}
		
		return program;
	}
	private boolean startsProgram(Token token) {
		return token.isLextant(Keyword.EXEC);
	}
	
	
	///////////////////////////////////////////////////////////
	// mainBlock
	
	// mainBlock -> { statement* }
	private ParseNode parseBlockStatements() {
		if(!startsBlockStatements(nowReading)) {
			return syntaxErrorNode("mainBlock");
		}
		ParseNode mainBlock = new BlockStatementsNode(nowReading);
		expect(Punctuator.OPEN_BRACE);
		
		while(startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			mainBlock.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return mainBlock;
	}
	private boolean startsBlockStatements(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACE);
	}
	
	
	///////////////////////////////////////////////////////////
	// statements
	
	// statement-> declaration | assignment | printStmt | block
	private ParseNode parseStatement() {
		if(!startsStatement(nowReading)) {
			return syntaxErrorNode("statement");
		}
		if(startsDeclaration(nowReading)) {
			return parseDeclaration();
		}
		if(startsAssignment(nowReading)) {
			return parseAssignment();
		}
		if(startsPrintStatement(nowReading)) {
			return parsePrintStatement();
		}
		if(startsBlockStatements(nowReading)) {
			return parseBlockStatements();
		}
		return syntaxErrorNode("statement");
	}
	private boolean startsStatement(Token token) {
		return startsPrintStatement(token) ||
				startsAssignment(token) ||
				startsDeclaration(token) ||
				startsBlockStatements(token);
	}
	
	// printStmt -> PRINT printExpressionList .
	private ParseNode parsePrintStatement() {
		if(!startsPrintStatement(nowReading)) {
			return syntaxErrorNode("print statement");
		}
		PrintStatementNode result = new PrintStatementNode(nowReading);
		
		readToken();
		result = parsePrintExpressionList(result);
		
		expect(Punctuator.TERMINATOR);
		return result;
	}
	private boolean startsPrintStatement(Token token) {
		return token.isLextant(Keyword.PRINT);
	}	

	// This adds the printExpressions it parses to the children of the given parent
	// printExpressionList -> printExpression* bowtie (,|;)  (note that this is nullable)

	private PrintStatementNode parsePrintExpressionList(PrintStatementNode parent) {
		while(startsPrintExpression(nowReading) || startsPrintSeparator(nowReading)) {
			parsePrintExpression(parent);
			parsePrintSeparator(parent);
		}
		return parent;
	}
	

	// This adds the printExpression it parses to the children of the given parent
	// printExpression -> (expr | nl)?     (nullable)
	
	private void parsePrintExpression(PrintStatementNode parent) {
		if(startsExpression(nowReading)) {
			ParseNode child = parseExpression();
			parent.appendChild(child);
		}
		else if(nowReading.isLextant(Keyword.NEWLINE)) {
			readToken();
			ParseNode child = new NewlineNode(previouslyRead);
			parent.appendChild(child);
		}
		else if(nowReading.isLextant(Keyword.TAB_SPACE)) {
			readToken();
			ParseNode child = new TabSpaceNode(previouslyRead);
			parent.appendChild(child);
		}
		// else we interpret the printExpression as epsilon, and do nothing
	}
	private boolean startsPrintExpression(Token token) {
		return startsExpression(token) || token.isLextant(Keyword.NEWLINE, Keyword.TAB_SPACE) ;
	}
	
	
	// This adds the printExpression it parses to the children of the given parent
	// printExpression -> expr? ,? nl? 
	
	private void parsePrintSeparator(PrintStatementNode parent) {
		if(!startsPrintSeparator(nowReading) && !nowReading.isLextant(Punctuator.TERMINATOR)) {
			ParseNode child = syntaxErrorNode("print separator");
			parent.appendChild(child);
			return;
		}
		
		if(nowReading.isLextant(Punctuator.SPACE)) {
			readToken();
			ParseNode child = new SpaceNode(previouslyRead);
			parent.appendChild(child);
		}
		else if(nowReading.isLextant(Punctuator.SEPARATOR)) {
			readToken();
		}		
		else if(nowReading.isLextant(Punctuator.TERMINATOR)) {
			// we're at the end of the bowtie and this printSeparator is not required.
			// do nothing.  Terminator is handled in a higher-level nonterminal.
		}
	}
	private boolean startsPrintSeparator(Token token) {
		return token.isLextant(Punctuator.SEPARATOR, Punctuator.SPACE) ;
	}
	
	
	// declaration -> CONST identifier := expression .
	private ParseNode parseDeclaration() {
		if(!startsDeclaration(nowReading)) {
			return syntaxErrorNode("declaration");
		}
		Token declarationToken = nowReading;
		readToken();
		
		ParseNode identifier = parseIdentifier();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		return DeclarationNode.withChildren(declarationToken, identifier, initializer);
	}
	private boolean startsDeclaration(Token token) {
		return token.isLextant(Keyword.CONST, Keyword.VAR);
	}

	private ParseNode parseAssignment() {
		if(!startsAssignment(nowReading)) {
			return syntaxErrorNode("assignment");
		}
		ParseNode identifier = parseTargetable();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		return AssignmentNode.withChildren(identifier, initializer);
	}

	private boolean startsAssignment(Token token) {
		return startsIdentifier(token);
	}
	
	///////////////////////////////////////////////////////////
	// expressions
	// expr                     -> comparisonExpression
	// comparisonExpression     -> additiveExpression [> additiveExpression]?
	// additiveExpression       -> multiplicativeExpression [+ multiplicativeExpression]*  (left-assoc)
	// multiplicativeExpression -> atomicExpression [MULT atomicExpression]*  (left-assoc)
	// atomicExpression         -> literal
	// literal                  -> intNumber | identifier | booleanConstant

	// expr  -> comparisonExpression
	private ParseNode parseExpression() {		
		if(!startsExpression(nowReading)) {
			return syntaxErrorNode("expression");
		}
		return parseLogicalOrExpression();
	}
	private boolean startsExpression(Token token) {
		return startsLogicalOrExpression(token);
	}

	private ParseNode parseLogicalOrExpression() {
		if(!startsLogicalOrExpression(nowReading)) {
			return syntaxErrorNode("logical or expression");
		}

		ParseNode left = parseLogicalAndExpression();
		while(nowReading.isLextant(Punctuator.LOGICAL_OR)) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseLogicalAndExpression();

			left = OperatorNode.withChildren(compareToken, left, right);
		}
		return left;
	}
	private boolean startsLogicalOrExpression(Token token) {
		return startsLogicalAndExpression(token);
	}

	private ParseNode parseLogicalAndExpression() {
		if(!startsLogicalAndExpression(nowReading)) {
			return syntaxErrorNode("logical and expression");
		}

		ParseNode left = parseComparisonExpression();
		while(nowReading.isLextant(Punctuator.LOGICAL_AND)) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseComparisonExpression();

			left = OperatorNode.withChildren(compareToken, left, right);
		}
		return left;
	}
	private boolean startsLogicalAndExpression(Token token) {
		return startsComparisonExpression(token);
	}

	// comparisonExpression -> additiveExpression [> additiveExpression]?
	private ParseNode parseComparisonExpression() {
		if(!startsComparisonExpression(nowReading)) {
			return syntaxErrorNode("comparison expression");
		}
		
		ParseNode left = parseAdditiveExpression();
		while(nowReading.isLextant(Punctuator.COMPARISONS)) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseAdditiveExpression();
			
			left = OperatorNode.withChildren(compareToken, left, right);
		}
		return left;

	}
	private boolean startsComparisonExpression(Token token) {
		return startsAdditiveExpression(token);
	}

	// additiveExpression -> multiplicativeExpression [+ multiplicativeExpression]*  (left-assoc)
	private ParseNode parseAdditiveExpression() {
		if(!startsAdditiveExpression(nowReading)) {
			return syntaxErrorNode("additiveExpression");
		}
		
		ParseNode left = parseMultiplicativeExpression();
		while(nowReading.isLextant(Punctuator.ADD, Punctuator.SUBTRACT)) {
			Token additiveToken = nowReading;
			readToken();
			ParseNode right = parseMultiplicativeExpression();
			
			left = OperatorNode.withChildren(additiveToken, left, right);
		}
		return left;
	}
	private boolean startsAdditiveExpression(Token token) {
		return startsMultiplicativeExpression(token);
	}	

	// multiplicativeExpression -> atomicExpression [MULT atomicExpression]*  (left-assoc)
	private ParseNode parseMultiplicativeExpression() {
		if(!startsMultiplicativeExpression(nowReading)) {
			return syntaxErrorNode("multiplicativeExpression");
		}
		
		ParseNode left = parseAtomicExpression();
		while(nowReading.isLextant(Punctuator.MULTIPLY, Punctuator.DIVIDE)) {
			Token multiplicativeToken = nowReading;
			readToken();
			ParseNode right = parseAtomicExpression();
			
			left = OperatorNode.withChildren(multiplicativeToken, left, right);
		}
		return left;
	}
	private boolean startsMultiplicativeExpression(Token token) {
		return startsAtomicExpression(token);
	}
	
	// atomicExpression -> literal or bracketed expression
	private ParseNode parseAtomicExpression() {
		if(!startsAtomicExpression(nowReading)) {
			return syntaxErrorNode("atomic expression");
		}
		if(startsLiteral(nowReading)) {
			return parseLiteral();
		}
		else if(startsBracket(nowReading)) {
			return parseBracket();
		}
		else if(startsEmptyArrayCreation(nowReading)) {
			return parseEmptyArrayCreation();
		}
		return syntaxErrorNode("atomic expression");
	}
	private boolean startsAtomicExpression(Token token) {
		return startsLiteral(token) || startsBracket(token) || startsEmptyArrayCreation(token);
	}
	
	// literal -> number | identifier | booleanConstant
	private ParseNode parseLiteral() {
		if(!startsLiteral(nowReading)) {
			return syntaxErrorNode("literal");
		}
		
		if(startsIntConstant(nowReading)) {
			return parseIntConstant();
		}
		if(startsFloatConstant(nowReading)) {
			return parseFloatConstant();
		}
		if(startsCharacterConstant(nowReading)) {
			return parseCharacterConstant();
		}
		if(startsStringConstant(nowReading)) {
			return parseStringConstant();
		}
		if(startsIdentifier(nowReading)) {
			return parseTargetable();
		}
		if(startsBooleanConstant(nowReading)) {
			return parseBooleanConstant();
		}

		return syntaxErrorNode("literal");
	}
	private boolean startsLiteral(Token token) {
		return startsIntConstant(token) ||
				startsFloatConstant(token) ||
				startsCharacterConstant(token) ||
				startsStringConstant(token) ||
				startsIdentifier(token) ||
				startsBooleanConstant(token);
	}

	private boolean startsEmptyArrayCreation(Token token) {
		return token.isLextant(Keyword.NEW);
	}

	private ParseNode parseEmptyArrayCreation() {
		if(!startsEmptyArrayCreation(nowReading)) {
			return syntaxErrorNode("empty array creation expression");
		}
		Token newToken = nowReading;
		readToken();
		Type type = parseTypeVariable();
		expect(Punctuator.OPEN_ROUND);
		ParseNode exp = parseExpression();
		expect(Punctuator.CLOSE_ROUND);
		if(type instanceof Array) {
			return ArrayNode.make(newToken, type, exp);
		}
		return syntaxErrorNode("empty array creation expression");
	}

	private Type parseTypeVariable() {
		if(!startsType(nowReading)) {
			syntaxError(nowReading, "expecting a type");
			readToken();
			return null;
		}
		if(startsArrayType(nowReading)) {
			readToken();
			Type subType = parseTypeVariable();
			expect(Punctuator.CLOSE_SQUARE);
			if(subType == null) {
				return null;
			}
			return new Array(subType);
		}
		if(startsPrimativeType(nowReading)) {
			readToken();
			return PrimitiveType.fromTypeVariable((LextantToken) previouslyRead);
		}
		// Theoretically should not reach here
		syntaxError(nowReading, "expecting a type");
		readToken();
		return null;
	}

	private boolean startsBracket(Token token) {
		return token.isLextant(Punctuator.OPEN_ROUND, Punctuator.OPEN_SQUARE);
	}

	private ParseNode parseBracket() {
		if(!startsBracket(nowReading)) {
			return syntaxErrorNode("bracket expression");
		}
		Token open = nowReading;
		expect(Punctuator.OPEN_ROUND, Punctuator.OPEN_SQUARE);
		ParseNode exp = parseExpression();
		if(open.isLextant(Punctuator.OPEN_ROUND)) {
			expect(Punctuator.CLOSE_ROUND);
			return exp;
		}
		else if(open.isLextant(Punctuator.OPEN_SQUARE)) {
			expect(Punctuator.PIPE);
			if(!startsType(nowReading)) {
				return syntaxErrorNode("cast type");
			}
			CastNode node = CastNode.make(nowReading, exp);
			readToken();
			expect(Punctuator.CLOSE_SQUARE);
			return node;
		}
		return syntaxErrorNode("bracket expression");
	}

	private boolean startsType(Token token) {
		return startsPrimativeType(token) || startsArrayType(token);
	}

	private boolean startsArrayType(Token token) {
		return token.isLextant(Punctuator.OPEN_SQUARE);
	}

	private boolean startsPrimativeType(Token token) {
		return token.isLextant(
				Keyword.BOOLEAN,
				Keyword.CHARACTER,
				Keyword.STRING,
				Keyword.INTEGER,
				Keyword.FLOAT
		);
	}

	// number (terminal)
	private ParseNode parseIntConstant() {
		if(!startsIntConstant(nowReading)) {
			return syntaxErrorNode("integer constant");
		}
		readToken();
		return new IntegerConstantNode(previouslyRead);
	}
	private boolean startsIntConstant(Token token) {
		return token instanceof IntegerConstantToken;
	}
	
	private ParseNode parseFloatConstant() {
		if(!startsFloatConstant(nowReading)) {
			return syntaxErrorNode("float constant");
		}
		readToken();
		return new FloatConstantNode(previouslyRead);
	}
	private boolean startsFloatConstant(Token token) {
		return token instanceof FloatConstantToken;
	}

	private ParseNode parseCharacterConstant() {
		if(!startsCharacterConstant(nowReading)) {
			return syntaxErrorNode("character constant");
		}
		readToken();
		return new CharacterConstantNode(previouslyRead);
	}
	private boolean startsCharacterConstant(Token token) {
		return token instanceof CharacterConstantToken;
	}

	private ParseNode parseStringConstant() {
		if(!startsStringConstant(nowReading)) {
			return syntaxErrorNode("string constant");
		}
		readToken();
		return new StringConstantNode(previouslyRead);
	}
	private boolean startsStringConstant(Token token) {
		return token instanceof StringConstantToken;
	}

	// identifier (terminal)
	private ParseNode parseIdentifier() {
		if(!startsIdentifier(nowReading)) {
			return syntaxErrorNode("identifier");
		}
		readToken();
		return new IdentifierNode(previouslyRead);
	}
	private boolean startsIdentifier(Token token) {
		return token instanceof IdentifierToken;
	}

	private ParseNode parseTargetable() {
		ParseNode node = parseIdentifier();
		while(nowReading.isLextant(Punctuator.OPEN_SQUARE)) {
			Token token = LextantToken.artificial(nowReading, Punctuator.ARRAY_INDEXING);
			readToken();
			ParseNode index = parseExpression();

			node = OperatorNode.withChildren(token, node, index);
			expect(Punctuator.CLOSE_SQUARE);
		}
		return node;
	}

	// boolean constant (terminal)
	private ParseNode parseBooleanConstant() {
		if(!startsBooleanConstant(nowReading)) {
			return syntaxErrorNode("boolean constant");
		}
		readToken();
		return new BooleanConstantNode(previouslyRead);
	}
	private boolean startsBooleanConstant(Token token) {
		return token.isLextant(Keyword.TRUE, Keyword.FALSE);
	}

	private void readToken() {
		previouslyRead = nowReading;
		nowReading = scanner.next();
	}	
	
	// if the current token is one of the given lextants, read the next token.
	// otherwise, give a syntax error and read next token (to avoid endless looping).
	private void expect(Lextant ...lextants ) {
		if(!nowReading.isLextant(lextants)) {
			syntaxError(nowReading, "expecting " + Arrays.toString(lextants));
		}
		readToken();
	}	
	private ErrorNode syntaxErrorNode(String expectedSymbol) {
		syntaxError(nowReading, "expecting " + expectedSymbol);
		ErrorNode errorNode = new ErrorNode(nowReading);
		readToken();
		return errorNode;
	}
	private void syntaxError(Token token, String errorDescription) {
		String message = "" + token.getLocation() + " " + errorDescription;
		error(message);
	}
	private void error(String message) {
		PikaLogger log = PikaLogger.getLogger("compiler.Parser");
		log.severe("syntax error: " + message);
	}	
}

