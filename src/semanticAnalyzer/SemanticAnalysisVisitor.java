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
import semanticAnalyzer.types.Array;
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
			if(
					(
							assignmentType == PrimitiveType.CHARACTER ||
							assignmentType == PrimitiveType.INTEGER
					) &&
					(
							targetType == PrimitiveType.INTEGER ||
							targetType == PrimitiveType.FLOAT ||
							targetType == PrimitiveType.RATIONAL
					)
				)
			{
				implicitCast(node, 1, targetType, assignmentType);
			}
			else {
				assignmentTypeMismatchError(target.getToken(), targetType, assignmentType);
				node.setType(PrimitiveType.ERROR);
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// expressions

	private Type[][] acceptablePromotions = new Type[][] {
		new Type[] { PrimitiveType.CHARACTER,	PrimitiveType.INTEGER},
		new Type[] { PrimitiveType.INTEGER,		PrimitiveType.FLOAT},
		new Type[] { PrimitiveType.INTEGER,		PrimitiveType.RATIONAL}
	};
	private Type[] acceptableElementsGrouped = new Type[] {
			PrimitiveType.CHARACTER, PrimitiveType.INTEGER, PrimitiveType.FLOAT, PrimitiveType.RATIONAL
	};

	private int promotionLevel(Type type) {
		int val = -1;
		for(int i=0;i<acceptablePromotions.length;i++) {
			if(type == acceptablePromotions[i][0]) {
				val = i;
				break;
			}
		}
		return val;
	}
	private int promotionTargetLevel(Type type) {
		int val = -1;
		for(int i=0;i<acceptablePromotions.length;i++) {
			if(type == acceptablePromotions[i][1]) {
				val = i;
				break;
			}
		}
		return val;
	}

	private FunctionSignature binaryPromatableSignature(OperatorNode node, FunctionSignatures group, ArrayList<Type> childTypes) {
		assert childTypes.size() == 2;
		FunctionSignature originalSignature = group.acceptingSignature(childTypes);
		if(originalSignature != FunctionSignature.nullInstance()) {
			return originalSignature;
		}

		int[] startPositions = new int[childTypes.size()];
		for(int i=0; i<startPositions.length; i++) {
			startPositions[i] = promotionLevel(childTypes.get(i));
		}

		int[] currPos = startPositions.clone();
		//left
		if(currPos[0]!=-1) {
			for(int i=currPos[0]; i<acceptablePromotions.length; i++) {
				FunctionSignature sig = group.acceptingSignature(Arrays.asList(
						acceptablePromotions[i][1],
						childTypes.get(1)
				));
				if(sig != FunctionSignature.nullInstance()) {
					implicitCast(node, 0, acceptablePromotions[i][1], childTypes);
					return sig;
				}
			}
		}
		//right
		if(currPos[1]!=-1) {
			for(int i=currPos[1]; i<acceptablePromotions.length; i++) {
				FunctionSignature sig = group.acceptingSignature(Arrays.asList(
						childTypes.get(0),
						acceptablePromotions[i][1]
				));
				if(sig != FunctionSignature.nullInstance()) {
					implicitCast(node, 1, acceptablePromotions[i][1], childTypes);
					return sig;
				}
			}
		}
		//both
		if(currPos[0]!=-1 && currPos[1]!=-1) {
			ArrayList<FunctionSignature> howManyWork = new ArrayList<FunctionSignature>();
			// outer array is right side, and inner array is left side
			for(int j=currPos[1]; j<acceptablePromotions.length; j++) {
				for(int i=currPos[0]; i<acceptablePromotions.length; i++) {
					FunctionSignature sig = group.acceptingSignature(Arrays.asList(
							acceptablePromotions[i][1],
							acceptablePromotions[j][1]
					));
					if(sig != FunctionSignature.nullInstance()) {
						howManyWork.add(sig);
					}
				}
			}
			if(howManyWork.size()>0) {
				int[][] precendenceOrder = new int[howManyWork.size()][];
				for(int i=0;i<precendenceOrder.length;i++) {
					Type[] t = howManyWork.get(i).getParams();
					precendenceOrder[i] = new int[] {
						promotionTargetLevel(t[0]),
						promotionTargetLevel(t[1])
					};
				}
				int x=0, y=0;
				for(int i=1;i<precendenceOrder.length; i++) {
					if(precendenceOrder[i][0]<precendenceOrder[x][0]) {
						x = i;
					}
					if(precendenceOrder[i][1]<precendenceOrder[y][1]) {
						y = i;
					}
				}
				if(x!=y) {
					if(precendenceOrder[x][1]==precendenceOrder[y][1]) {
						y=x;
					}
					else if(precendenceOrder[x][0]==precendenceOrder[y][0]) {
						x=y;
					}
				}
				if(x==y) {
					FunctionSignature sig = howManyWork.get(x);
					Type[] parameters = sig.getParams();
					for(int i=0; i<parameters.length; i++) {
						implicitCast(node, i, parameters[i], childTypes);
					}
					return sig;
				}
				multiplePossiblePromotionError(node, node.getToken().getLexeme(),
						childTypes.get(0), childTypes.get(1));
			}
		}
		return originalSignature;
	}

	private void implicitCast(ParseNode node, int index, Type resultType, ArrayList<Type> childTypes) {
		Type originalType = childTypes.get(index);
		if(originalType == resultType) {
			return;
		}
		implicitCast(node, index, resultType, originalType);

		childTypes.set(index, resultType);
	}

	private void implicitCast(ParseNode node, int index, Type resultType, Type originalType) {
		if(originalType == resultType) {
			return;
		}
		if(originalType == PrimitiveType.CHARACTER && resultType != PrimitiveType.INTEGER) {
			implicitCast(node, index, PrimitiveType.INTEGER, originalType);
		}
		ParseNode exp = node.child(index);
		CastNode castNode = CastNode.make(exp.getToken(), exp, resultType);
		node.replaceNthChild(index, castNode);

		castSemantics(castNode);
	}

	@Override
	public void visitLeave(OperatorNode node) {
		ArrayList<Type> childTypes = new ArrayList<Type>();
		for(ParseNode child: node.getChildren()) {
			childTypes.add(child.getType());
		}

		Lextant operator = ((LextantToken) node.getToken()).getLextant();
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(operator);
		FunctionSignature signature;
		if(childTypes.size()==2) {
			signature = binaryPromatableSignature(node, signatures, childTypes);
		}
		else {
			signature = signatures.acceptingSignature(childTypes);
		}
		
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
		castSemantics(node);
	}

	private void castSemantics(CastNode node) {
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
		// TODO promotion stuff and typechecking
		if(node.isNewDeclaration()) {
			ParseNode first = node.child(0);
			Type type = first.getType();
			if(type.equivalent(PrimitiveType.CHARACTER)) {
				implicitCast(node, 0, PrimitiveType.INTEGER, type);
			}
			else if(!type.equivalent(PrimitiveType.INTEGER)) {
				arrayEmptyCreationNonIntegerLength(node, type);
				node.setType(PrimitiveType.ERROR);
			}
		}
		else {
			assert node.nChildren() > 0;

			boolean foundRational=false;
			boolean foundFloat = false;
			ArrayList<Type> types = new ArrayList<Type>();
			for(ParseNode child: node.getChildren()) {
				Type childType = child.getType();
				boolean found=false;
				for(Type type: types) {
					if(type.equivalent(childType)) {
						found=true;
						break;
					}
				}
				if(!found) {
					types.add(childType);
					if(childType == PrimitiveType.FLOAT) {
						foundFloat=true;
					}
					else if(childType == PrimitiveType.RATIONAL) {
						foundRational=true;
					}
				}
			}

			Type selectedType = PrimitiveType.ERROR;
			if(types.size()==1) {
				selectedType = types.get(0);
			}
			else if(types.size()>0 && !(foundFloat && foundRational)) {
				boolean allTypesAcceptable = true;
				int mx=-1;
				for(Type type: types) {
					boolean found=false;
					for(int i=0; i<acceptableElementsGrouped.length; i++) {
						if(acceptableElementsGrouped[i] == type) {
							if(i>mx) {
								mx=i;
							}
							found=true;
							break;
						}
					}
					if(!found) {
						allTypesAcceptable=false;
						break;
					}
				}
				if(allTypesAcceptable) {
					selectedType = acceptableElementsGrouped[mx];
					for(int i=0;i<node.nChildren(); i++) {
						implicitCast(node, i, selectedType, node.child(i).getType());
					}
				}
			}

			if(selectedType == PrimitiveType.ERROR) {
				arrayCommonPromotionError(node, types);
				node.setType(selectedType);
			}
			else {
				node.setType(new Array(selectedType));
			}
		}
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
	private void multiplePossiblePromotionError(ParseNode node, String operator, Type left, Type right) {
		logError("Multiple possible promotions for operator " + operator + " with operands "+ left + " and " +
	right + " at " + node.getToken().getLocation());
	}
	private void arrayCommonPromotionError(ParseNode node, ArrayList<Type> types) {
		StringBuilder builder = new StringBuilder();
		builder.append("Cannot find a common promotion with unique types:");
		for(Type type: types) {
			builder.append(" " + type);
		}
		builder.append(" at " + node.getToken().getLocation());
		logError(builder.toString());
	}
	private void arrayEmptyCreationNonIntegerLength(ParseNode node, Type type) {
		logError("Array creation with \"new\" keyword cannot have length of type "+type+" at" + node.getToken().getLocation());
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