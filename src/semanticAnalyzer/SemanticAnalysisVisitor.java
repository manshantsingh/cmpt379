package semanticAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import logging.PikaLogger;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import parseTree.SomeArbitaryOperatorNode;
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
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.TabSpaceNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.signatures.FunctionSignatures;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import tokens.LextantToken;
import tokens.Token;

class SemanticAnalysisVisitor extends ParseNodeVisitor.Default {
	@Override
	public void visitLeave(ParseNode node) {
		throw new RuntimeException("Node class unimplemented in SemanticAnalysisVisitor: " + node.getClass());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// constructs larger than statements
	@Override
	public void visitEnter(ProgramNode node) {
		enterProgramScope(node);
	}
	public void visitLeave(ProgramNode node) {
		leaveScope(node);
	}
	public void visitEnter(BlockStatementsNode node) {
		enterSubscope(node);
	}
	public void visitLeave(BlockStatementsNode node) {
		leaveScope(node);
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	// helper methods for scoping.
	private void enterProgramScope(ParseNode node) {
		Scope scope = Scope.createProgramScope();
		node.setScope(scope);
	}
	private void enterSubscope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createSubscope();
		node.setScope(scope);
	}		
	private void leaveScope(ParseNode node) {
		node.getScope().leave();
	}
	
	///////////////////////////////////////////////////////////////////////////
	// statements and declarations
	@Override
	public void visitLeave(PrintStatementNode node) {
	}
	@Override
	public void visitLeave(IfStatementNode node) {
		assertCorrectType(node, PrimitiveType.BOOLEAN, node.child(0).getType());
	}
	@Override
	public void visitLeave(WhileStatementNode node) {
		assertCorrectType(node, PrimitiveType.BOOLEAN, node.child(0).getType());
	}
	@Override
	public void visitLeave(DeclarationNode node) {
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		ParseNode initializer = node.child(1);
		
		Type declarationType = initializer.getType();
		node.setType(declarationType);
		
		identifier.setType(declarationType);
		addBinding(identifier, declarationType, node.getToken().isLextant(Keyword.CONST));
	}
	@Override
	public void visitLeave(AssignmentNode node) {
		ParseNode target = node.child(0);
		ParseNode assignment = node.child(1);
		Type targetType = target.getType();
		Type assignmentType = assignment.getType();

		node.setType(targetType);

		if(target instanceof IdentifierNode) {
			if(((IdentifierNode)target).getBinding().isConstant()) {
				assignmentToConstant(target.getToken());
				node.setType(PrimitiveType.ERROR);
			}
		}
		else if( !	(
						target instanceof SomeArbitaryOperatorNode &&
						((SomeArbitaryOperatorNode)target).getSignature().checkIfTargetable()
					)
			)
		{
			assignmentToUntargetableType(target.getToken());
			node.setType(PrimitiveType.ERROR);
		}
		if(!targetType.equivalent(assignmentType)) {
			assignmentTypeMismatchError(target.getToken(), targetType, assignmentType);
			node.setType(PrimitiveType.ERROR);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// expressions
	@Override
	public void visitLeave(OperatorNode node) {
		List<Type> childTypes = new ArrayList<Type>();
		for(ParseNode child: node.getChildren()) {
			childTypes.add(child.getType());
		}

		Lextant operator = ((LextantToken) node.getToken()).getLextant();
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(operator);
		FunctionSignature signature = signatures.acceptingSignature(childTypes);
		
		if(signature.accepts(childTypes)) {
			node.setSignature(signature);
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}
	@Override
	public void visitLeave(CastNode node) {
		assert node.nChildren() == 1;

		Type from  = node.child(0).getType();
		Type to = node.getType();
		if(from.equivalent(to)) {
			return;
		}
		List<Type> params = Arrays.asList(from);
		FunctionSignature signature = FunctionSignatures.signaturesOf(Punctuator.PIPE).acceptingSignature(params,to);

		if(signature.accepts(params, to)) {
			node.setSignature(signature);
		}
		else {
			castTypeCheckError(node, from, to);
			node.setType(PrimitiveType.ERROR);
		}
	}

	public void visitLeave(ArrayNode node) {
		// TODO
	}


	///////////////////////////////////////////////////////////////////////////
	// simple leaf nodes
	@Override
	public void visit(BooleanConstantNode node) {
		node.setType(PrimitiveType.BOOLEAN);
	}
	@Override
	public void visit(ErrorNode node) {
		node.setType(PrimitiveType.ERROR);
	}
	@Override
	public void visit(IntegerConstantNode node) {
		node.setType(PrimitiveType.INTEGER);
	}
	@Override
	public void visit(FloatConstantNode node) {
		node.setType(PrimitiveType.FLOAT);
	}
	@Override
	public void visit(CharacterConstantNode node) {
		node.setType(PrimitiveType.CHARACTER);
	}
	@Override
	public void visit(StringConstantNode node) {
		node.setType(PrimitiveType.STRING);
	}
	@Override
	public void visit(NewlineNode node) {
	}
	@Override
	public void visit(SpaceNode node) {
	}
	@Override
	public void visit(TabSpaceNode node) {
	}
	///////////////////////////////////////////////////////////////////////////
	// IdentifierNodes, with helper methods
	@Override
	public void visit(IdentifierNode node) {
		if(!isBeingDeclared(node)) {		
			Binding binding = node.findVariableBinding();
			
			node.setType(binding.getType());
			node.setBinding(binding);
		}
		// else parent DeclarationNode does the processing.
	}
	private boolean isBeingDeclared(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof DeclarationNode) && (node == parent.child(0));
	}
	private void addBinding(IdentifierNode identifierNode, Type type, boolean constant) {
		Scope scope = identifierNode.getLocalScope();
		Binding binding = scope.createBinding(identifierNode, type, constant);
		identifierNode.setBinding(binding);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// error logging/printing

	private void assertCorrectType(ParseNode node, Type expected, Type received) {
		if(!expected.equivalent(received)) {
			logError("expected " + expected + " for " + node.getToken().getLexeme()
					+ " but received " + received
					+ " at " + node.getToken().getLocation());
		}
	}

	private void typeCheckError(ParseNode node, List<Type> operandTypes) {
		Token token = node.getToken();
		
		logError("operator " + token.getLexeme() + " not defined for types " 
				 + operandTypes  + " at " + token.getLocation());	
	}
	private void castTypeCheckError(ParseNode node, Type from, Type to) {
		logError("Cannot cast from "+from+" to "+to+" at " + node.getToken().getLocation());
	}
	private void assignmentToConstant(Token token) {
		logError("Cannot write to const variable \""+
				token.getLexeme()+"\" at "+token.getLocation());
	}
	private void assignmentToUntargetableType(Token token) {
		logError("Expression is not a targetable type \""+
				token.getLexeme()+"\" at "+token.getLocation());
	}
	private void assignmentTypeMismatchError(Token token, Type expected, Type received) {
		logError("Expected type "+expected+" for "+token.getLexeme()+
				", but received "+received+" at "+token.getLocation());
	}
	private void logError(String message) {
		PikaLogger log = PikaLogger.getLogger("compiler.semanticAnalyzer");
		log.severe(message);
	}
}