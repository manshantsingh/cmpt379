package parser;

import java.util.ArrayList;
import java.util.Arrays;

import logging.PikaLogger;
import parseTree.*;
import parseTree.nodeTypes.ArrayNode;
import parseTree.nodeTypes.AssignmentNode;
import parseTree.nodeTypes.OperatorNode;
import parseTree.nodeTypes.ParameterNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.CastNode;
import parseTree.nodeTypes.CharacterConstantNode;
import parseTree.nodeTypes.BlockStatementsNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.FloatConstantNode;
import parseTree.nodeTypes.ForStatementNode;
import parseTree.nodeTypes.LambdaNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.LoopJumperNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.ReturnNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.TabSpaceNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.SpecialType;
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
		while(nowReading.isLextant(Keyword.FUNC) || startsDeclaration(nowReading)) {
			if(startsDeclaration(nowReading)) {
				program.appendChild(parseDeclaration());
				continue;
			}
			Token funcStart = nowReading;
			readToken();
			ParseNode identifier = parseIdentifier();
			ParseNode lambda = parseLambdaConstant();
			program.appendChild(DeclarationNode.withChildren(funcStart, identifier, lambda, false, true));
		}
		
		expect(Keyword.EXEC);
		ParseNode mainBlock = parseBlockStatements();
		program.appendChild(mainBlock);
		
		if(!(nowReading instanceof NullToken)) {
			return syntaxErrorNode("end of program");
		}

		return program;
	}
	private boolean startsProgram(Token token) {
		return token.isLextant(Keyword.EXEC) ||
				token.isLextant(Keyword.FUNC) ||
				startsDeclaration(token);
	}
	
	
	///////////////////////////////////////////////////////////
	// mainBlock
	
	// blockStatement -> { statement* }
	private ParseNode parseBlockStatements() {
		if(!startsBlockStatements(nowReading)) {
			return syntaxErrorNode("Block statement");
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

	private ParseNode parseIfStatement() {
		if(!startsIfStatement(nowReading)) {
			return syntaxErrorNode("If statement");
		}
		IfStatementNode node = new IfStatementNode(nowReading);
		readToken();
		expect(Punctuator.OPEN_ROUND);
		node.appendChild(parseExpression());
		expect(Punctuator.CLOSE_ROUND);
		node.appendChild(parseBlockStatements());
		if(nowReading.isLextant(Keyword.ELSE)) {
			readToken();
			node.appendChild(parseBlockStatements());
		}
		return node;
	}
	private boolean startsIfStatement(Token token) {
		return token.isLextant(Keyword.IF);
	}

	private ParseNode parseWhileStatement() {
		if(!startsWhileStatement(nowReading)) {
			return syntaxErrorNode("While statement");
		}
		Token whileKeyword = nowReading;
		readToken();
		expect(Punctuator.OPEN_ROUND);
		ParseNode condition = parseExpression();
		expect(Punctuator.CLOSE_ROUND);
		ParseNode blockStatement = parseBlockStatements();
		return WhileStatementNode.make(whileKeyword, condition, blockStatement);
	}
	private boolean startsWhileStatement(Token token) {
		return token.isLextant(Keyword.WHILE);
	}

	private ParseNode parseForStatement() {
		if(!startsForStatement(nowReading)) {
			return syntaxErrorNode("for statement");
		}
		// change me
		Token forToken = nowReading;
		readToken();
		boolean byIndex = nowReading.isLextant(Keyword.INDEX);
		expect(Keyword.INDEX, Keyword.ELEM);
		ParseNode identifier = parseIdentifier();
		expect(Keyword.OF);
		ParseNode expression = parseExpression();
		ParseNode blockStatement = parseBlockStatements();
		return ForStatementNode.make(forToken, byIndex, identifier, expression, blockStatement);
	}
	private boolean startsForStatement(Token token) {
		return token.isLextant(Keyword.FOR);
	}

	private ParseNode parseReleaseStatement() {
		if(!startsReleaseStatement(nowReading)) {
			return syntaxErrorNode("release statement");
		}
		Token releaseKeyword = nowReading;
		readToken();
		ParseNode exp = parseExpression();
		expect(Punctuator.TERMINATOR);
		return OperatorNode.withChildren(releaseKeyword, exp);
	}

	private boolean startsReleaseStatement(Token token) {
		return token.isLextant(Keyword.RELEASE);
	}

	private ParseNode parseLoopJumperStatement() {
		if(!startsLoopJumperStatement(nowReading)) {
			return syntaxErrorNode("Loop jumper statement");
		}
		Token jumperToken = nowReading;
		readToken();
		expect(Punctuator.TERMINATOR);
		return new LoopJumperNode(jumperToken);
	}
	private boolean startsLoopJumperStatement(Token token) {
		return token.isLextant(Keyword.BREAK, Keyword.CONTINUE);
	}

	private ParseNode parseReturnStatement() {
		if(!startsReturnStatement(nowReading)) {
			return syntaxErrorNode("Return statement");
		}
		ReturnNode node = new ReturnNode(nowReading);
		readToken();
		if(startsExpression(nowReading)) {
			node.appendChild(parseExpression());
		}
		expect(Punctuator.TERMINATOR);
		return node;
	}
	private boolean startsReturnStatement(Token token) {
		return token.isLextant(Keyword.RETURN);
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
		if(startsIfStatement(nowReading)) {
			return parseIfStatement();
		}
		if(startsWhileStatement(nowReading)) {
			return parseWhileStatement();
		}
		if(startsReleaseStatement(nowReading)) {
			return parseReleaseStatement();
		}
		if(startsLoopJumperStatement(nowReading)) {
			return parseLoopJumperStatement();
		}
		if(startsReturnStatement(nowReading)) {
			return parseReturnStatement();
		}
		if(startsCallStatement(nowReading)) {
			return parseCallStatement();
		}
		if(startsForStatement(nowReading)) {
			return parseForStatement();
		}
		return syntaxErrorNode("statement");
	}
	private boolean startsStatement(Token token) {
		return startsPrintStatement(token) ||
				startsAssignment(token) ||
				startsDeclaration(token) ||
				startsBlockStatements(token) ||
				startsIfStatement(token) ||
				startsWhileStatement(token) ||
				startsReleaseStatement(token) ||
				startsLoopJumperStatement(token) ||
				startsReturnStatement(token) ||
				startsCallStatement(token) ||
				startsForStatement(token);
	}

	private boolean startsCallStatement(Token token) {
		return token.isLextant(Keyword.CALL);
	}

	private ParseNode parseCallStatement() {
		if(!startsCallStatement(nowReading)) {
			return syntaxErrorNode("call statement");
		}
		Token callKeyword = nowReading;
		readToken();
		ParseNode exp = parseExpression();
		expect(Punctuator.TERMINATOR);
		return OperatorNode.withChildren(callKeyword, exp);
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


	private boolean startsLambdaConstant(Token token) {
		return token.isLextant(Punctuator.LESS);
	}
	private boolean startsLambdaType(Token token) {
		return token.isLextant(Punctuator.LESS);
	}
	private ParseNode parseLambdaConstant() {
		if(!startsLambdaConstant(nowReading)) {
			return syntaxErrorNode("Lambda");
			
		}
		Token lamdaStart = nowReading;
		readToken();
		ArrayList<ParameterNode> params = new ArrayList<ParameterNode>();
		while((params.isEmpty() && startsType(nowReading) || (!params.isEmpty() && nowReading.isLextant(Punctuator.SEPARATOR)))) {
			if(!params.isEmpty()) {
				readToken();
			}
			Token paramToken = nowReading;
			Type t = parseTypeVariable();
			ParseNode identifer = parseIdentifier();
			params.add(ParameterNode.make(paramToken, t, identifer));
		}
		expect(Punctuator.GREATER);
		expect(Punctuator.ARROW);
		Type returnType;
		if(nowReading.isLextant(Keyword.VOID)) {
			readToken();
			returnType = SpecialType.VOID;
		}
		else {
			returnType = parseTypeVariable();
		}
		ParseNode block = parseBlockStatements();
		if(!(block instanceof BlockStatementsNode)) {
			return syntaxErrorNode("Lambda");
		}
		return LambdaNode.make(lamdaStart, params, returnType, (BlockStatementsNode) block);
	}
	
	
	// declaration -> CONST identifier := expression .
	private ParseNode parseDeclaration() {
		if(!startsDeclaration(nowReading)) {
			return syntaxErrorNode("declaration");
		}
		boolean isStatic = false;
		if(nowReading.isLextant(Keyword.STATIC)) {
			isStatic=true;
			readToken();
		}
		if(!startsDeclaration(nowReading)) {
			return syntaxErrorNode("declaration");
		}
		Token declarationToken = nowReading;
		readToken();
		
		ParseNode identifier = parseIdentifier();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		return DeclarationNode.withChildren(declarationToken, identifier, initializer, isStatic, declarationToken.isLextant(Keyword.CONST));
	}
	private boolean startsDeclaration(Token token) {
		return token.isLextant(Keyword.CONST, Keyword.VAR, Keyword.STATIC);
	}

	private ParseNode parseAssignment() {
		if(!startsAssignment(nowReading)) {
			return syntaxErrorNode("assignment");
		}
		ParseNode identifier = parseExpression(); // msk TODO
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		return AssignmentNode.withChildren(identifier, initializer);
	}

	private boolean startsAssignment(Token token) {
		return startsExpression(token);
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
		
		ParseNode left = parseFoldExpression();
		while(nowReading.isLextant(Punctuator.MULTIPLY, Punctuator.DIVIDE,
				Punctuator.OVER, Punctuator.EXPRESS_OVER, Punctuator.RATIONALIZE))
		{
			Token multiplicativeToken = nowReading;
			readToken();
			ParseNode right = parseFoldExpression();
			
			left = OperatorNode.withChildren(multiplicativeToken, left, right);
		}
		return left;
	}
	private boolean startsMultiplicativeExpression(Token token) {
		return startsFoldExpression(token);
	}

	private ParseNode parseFoldExpression() {
		if(!startsFoldExpression(nowReading)) {
			return syntaxErrorNode("foldExpression");
		}
		
		ParseNode left = parseMapReduceExpression();
		while(nowReading.isLextant(Keyword.FOLD)) {
			Token foldToken = nowReading;
			readToken();
			ParseNode middle = null;
			if(nowReading.isLextant(Punctuator.OPEN_SQUARE)) {
				readToken();
				middle = parseExpression();
				expect(Punctuator.CLOSE_SQUARE);
			}
			ParseNode right = parseMapReduceExpression();
			if(middle!=null) {
				left = OperatorNode.withChildren(foldToken, left, middle, right);
			}
			else{
				left = OperatorNode.withChildren(foldToken, left, right);
			}
		}
		return left;
	}
	private boolean startsFoldExpression(Token token) {
		return startsMapReduceExpression(token);
	}
	
	private ParseNode parseMapReduceExpression() {
		if(!startsMapReduceExpression(nowReading)) {
			return syntaxErrorNode("map or reduce Expression");
		}
		
		ParseNode left = parseUnaryOperatorExpression();
		while(nowReading.isLextant(Keyword.MAP, Keyword.REDUCE)) {
			Token mapReduceToken = nowReading;
			readToken();
			ParseNode right = parseUnaryOperatorExpression();
			
			left = OperatorNode.withChildren(mapReduceToken, left, right);
		}
		return left;
	}
	private boolean startsMapReduceExpression(Token token) {
		return startsUnaryPrecedenceExpression(token);
	}
	

	// multiplicativeExpression -> atomicExpression [MULT atomicExpression]*  (left-assoc)
	private ParseNode parseArrayIndexExpression() {
		if(!startsArrayIndexExpression(nowReading)) {
			return syntaxErrorNode("Array Indexing Expression");
		}

		ParseNode left = parseAtomicExpression();
		while(nowReading.isLextant(Punctuator.OPEN_SQUARE, Punctuator.OPEN_ROUND)) {
			if(nowReading.isLextant(Punctuator.OPEN_SQUARE)) {
				Token token = LextantToken.artificial(nowReading, Punctuator.ARRAY_INDEXING);
				readToken();
				ParseNode index = parseExpression();
				if(nowReading.isLextant(Punctuator.SEPARATOR)) {
					readToken();
					ParseNode index2 = parseExpression();
					expect(Punctuator.CLOSE_SQUARE);
					left = OperatorNode.withChildren(token, left, index, index2);
				}
				else {
					expect(Punctuator.CLOSE_SQUARE);
					left = OperatorNode.withChildren(token, left, index);
				}
			}
			else if(nowReading.isLextant(Punctuator.OPEN_ROUND)) {
				Token token = LextantToken.artificial(nowReading, Punctuator.FUNCTION_INVOCATION);
				readToken();
				ArrayList<ParseNode> arr = new ArrayList<ParseNode>();
				arr.add(left);
				while((arr.size()==1 && startsExpression(nowReading))
						|| (arr.size()>1 && nowReading.isLextant(Punctuator.SEPARATOR)))
				{
					if(arr.size()>1) {
						readToken();
					}
					arr.add(parseExpression());
				}
				expect(Punctuator.CLOSE_ROUND);
				left = OperatorNode.withChildren(token, arr.toArray(new ParseNode[arr.size()]));
			}
		}
		return left;
	}
	private boolean startsArrayIndexExpression(Token token) {
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
		return startsLiteral(token) ||
				startsBracket(token) ||
				startsEmptyArrayCreation(token);
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
			return parseIdentifier();
		}
		if(startsBooleanConstant(nowReading)) {
			return parseBooleanConstant();
		}
		if(startsLambdaConstant(nowReading)) {
			return parseLambdaConstant();
		}

		return syntaxErrorNode("literal");
	}
	private boolean startsLiteral(Token token) {
		return startsIntConstant(token) ||
				startsFloatConstant(token) ||
				startsCharacterConstant(token) ||
				startsStringConstant(token) ||
				startsIdentifier(token) ||
				startsBooleanConstant(token) ||
				startsLambdaConstant(token);
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

	private Type parseLambdaType() {
		if(!startsLambdaType(nowReading)) {
			syntaxError(nowReading, "expecting a lambda type");
			readToken();
			return null;
		}
		readToken();
		ArrayList<Type> params = new ArrayList<Type>();
		while((params.isEmpty() && startsType(nowReading)) ||
				(!params.isEmpty() && nowReading.isLextant(Punctuator.SEPARATOR)))
		{
			if(!params.isEmpty()) {
				readToken();
			}
			params.add(parseTypeVariable());
		}
		expect(Punctuator.GREATER);
		expect(Punctuator.ARROW);
		Type returnType;
		if(nowReading.isLextant(Keyword.VOID)) {
			readToken();
			returnType = SpecialType.VOID;
		}
		else {
			returnType = parseTypeVariable();
		}
		return new LambdaType(params, returnType);
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
		if(startsLambdaType(nowReading)) {
			return parseLambdaType();
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
		readToken();
		ParseNode exp = parseExpression();
		if(open.isLextant(Punctuator.OPEN_ROUND)) {
			expect(Punctuator.CLOSE_ROUND);
			return exp;
		}
		else if(open.isLextant(Punctuator.OPEN_SQUARE)) {
			if(nowReading.isLextant(Punctuator.PIPE)) {
				return parseCast(exp, open);
			}
			if(nowReading.isLextant(Punctuator.SEPARATOR, Punctuator.CLOSE_SQUARE)) {
				return parsePopulatedArray(exp, open);
			}
			return syntaxErrorNode("square bracket expression");
		}
		return syntaxErrorNode("bracket expression");
	}

	private ParseNode parseCast(ParseNode exp, Token token) {
		readToken();
		if(!startsType(nowReading)) {
			return syntaxErrorNode("cast type");
		}
		Type type = parseTypeVariable();
		expect(Punctuator.CLOSE_SQUARE);
		return CastNode.make(token, exp, type);
	}

	private ParseNode parsePopulatedArray(ParseNode firstElm, Token token) {
		ArrayList<ParseNode> list = new ArrayList<ParseNode>();
		list.add(firstElm);
		while(nowReading.isLextant(Punctuator.SEPARATOR)) {
			readToken();
			list.add(parseExpression());
		}
		expect(Punctuator.CLOSE_SQUARE);
		return ArrayNode.make(token, list);
	}

	private boolean startsUnaryPrecedenceExpression(Token token) {
		return startsExplicitUnaryOperatorExpression(token) ||
				startsArrayIndexExpression(token);
	}

	private boolean startsExplicitUnaryOperatorExpression(Token token) {
		return token.isLextant(Punctuator.LOGICAL_NOT, Keyword.CLONE, Keyword.LENGTH, Keyword.REVERSE, Keyword.ZIP);
	}

	private ParseNode parseUnaryOperatorExpression() {
		if(!startsUnaryPrecedenceExpression(nowReading)) {
			return syntaxErrorNode("Unary operator expression");
		}
		if(startsExplicitUnaryOperatorExpression(nowReading)) {
			Token operator = nowReading;
			readToken();
			if(operator.isLextant(Keyword.ZIP)) {
				ParseNode exp1 = parseExpression();
				expect(Punctuator.SEPARATOR);
				ParseNode exp2 = parseExpression();
				expect(Punctuator.SEPARATOR);
				ParseNode exp3 = parseUnaryOperatorExpression();
				return OperatorNode.withChildren(operator, exp1, exp2, exp3);
			}
			else {
				return OperatorNode.withChildren(operator, parseUnaryOperatorExpression());
			}
		}
		if(startsArrayIndexExpression(nowReading)) {
			return parseArrayIndexExpression();
		}
		return syntaxErrorNode("Unary operator expression");
	}

	private boolean startsType(Token token) {
		return startsPrimativeType(token) || startsArrayType(token) || startsLambdaType(token);
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
				Keyword.FLOAT,
				Keyword.RATIONAL
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

//	private ParseNode parseTargetable() {
//		ParseNode node = parseIdentifier();
//		while(nowReading.isLextant(Punctuator.OPEN_SQUARE)) {
//			Token token = LextantToken.artificial(nowReading, Punctuator.ARRAY_INDEXING);
//			readToken();
//			ParseNode index = parseExpression();
//
//			node = OperatorNode.withChildren(token, node, index);
//			expect(Punctuator.CLOSE_SQUARE);
//		}
//		return node;
//	}

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
			// MSK debug
//			System.out.println("now: "+nowReading.fullString());
//			for(StackTraceElement elm: Thread.currentThread().getStackTrace()) {
//				System.out.println(elm);
//			}
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

