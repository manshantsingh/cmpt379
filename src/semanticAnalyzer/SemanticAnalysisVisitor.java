package semanticAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.operators.FoldCodeGenerator;
import asmCodeGenerator.operators.MapReduceGenerator;
import asmCodeGenerator.operators.ZipCodeGenerator;
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
import parseTree.nodeTypes.ParameterNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.CastNode;
import parseTree.nodeTypes.CharacterConstantNode;
import parseTree.nodeTypes.BlockStatementsNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.FloatConstantNode;
import parseTree.nodeTypes.ForStatementNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.LambdaNode;
import parseTree.nodeTypes.LoopJumperNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.ReturnNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.TabSpaceNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.signatures.FunctionSignatures;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.SpecialType;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.TypeVariable;
import symbolTable.Binding;
import symbolTable.Scope;
import tokens.IdentifierToken;
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
		node.rearrangeChildren();
		enterProgramScope(node);
		for(ParseNode child: node.getChildren()) {
			if(child instanceof DeclarationNode && child.getToken().isLextant(Keyword.FUNC)) {
				DeclarationNode func = (DeclarationNode) child;
				IdentifierNode identifier = (IdentifierNode) func.child(0);
				Type funcType = func.child(1).getType();
				addBinding(identifier, funcType, false, true);
//				System.out.println(child+"\ntype: "+funcType);
			}
		}
	}
	public void visitLeave(ProgramNode node) {
		leaveScope(node);
	}
	public void visitEnter(BlockStatementsNode node) {
		if(node.getParent() instanceof LambdaNode) {
			enterProcedurescope(node);
		}
		else {
			enterSubscope(node);
			if(node.getParent() instanceof ForStatementNode) {
				//identifer binding
				ForStatementNode parent = (ForStatementNode) node.getParent();
				IdentifierNode identifierNode = (IdentifierNode) parent.child(0);
				Type type;
				if(parent.isByIndex()) {
					type = PrimitiveType.INTEGER;
				}
				else {
					ParseNode exp = parent.child(1);
					if(exp.getType() == PrimitiveType.STRING) {
						type = PrimitiveType.CHARACTER;
					}
					else if(exp.getType() instanceof Array) {
						type = ((Array)exp.getType()).getSubType();
					}
					else {
						ForExpressionMustBeRecordType(node, exp.getType());
						type = PrimitiveType.ERROR;
					}
				}
				Binding binding = node.getScope().createBinding(identifierNode, type, false, true);
				identifierNode.setBinding(binding);
				
				Labeller labeller = new Labeller("loop-variables");
				
				IdentifierNode terminator = new IdentifierNode(IdentifierToken.make(node.getToken().getLocation(), labeller.newLabel("hidden_loop_terminator")));
				Binding yetbinding = node.getScope().createBinding(terminator, PrimitiveType.INTEGER, false, true);
				terminator.setBinding(yetbinding);
				parent.appendChild(terminator);
				
				if(!parent.isByIndex()) {
					IdentifierNode index = new IdentifierNode(IdentifierToken.make(parent.getToken().getLocation(), labeller.newLabel("hidden_index")));
					Binding anotherbinding = node.getScope().createBinding(index, PrimitiveType.INTEGER, false, true);
					index.setBinding(anotherbinding);
					parent.appendChild(index);
				}
			}
		}
	}
	public void visitLeave(BlockStatementsNode node) {
		
		leaveScope(node);
	}
	public void visitEnter(LambdaNode node) {
		enterParameterscope(node);
	}
	public void visitLeave(LambdaNode node) {
		leaveScope(node);
	}
	public void visitLeave(ReturnNode node) {
		for(ParseNode n: node.pathToRoot()) {
			if(n instanceof LambdaNode) {
				LambdaNode lambda = (LambdaNode) n;
				Type retType = ((LambdaType) lambda.getType()).getReturnType();
				if(retType == SpecialType.VOID) {
					if(node.nChildren() == 0) {
						node.setType(retType);
						node.setFunctionReturnLabel(lambda.getReturnCodeLabel());
					}
					else {
						voidShouldNotHaveReturnExpression(node);
						node.setType(PrimitiveType.ERROR);
					}
				}
				else if(retType.equivalent(node.child(0).getType())) {
					node.setType(retType);
					node.setFunctionReturnLabel(lambda.getReturnCodeLabel());
				}
				else {
					wrongReturnType(node, retType, node.child(0).getType());
					node.setType(PrimitiveType.ERROR);
				}
				return;
			}
		}
		returnParentNotFound(node);
		node.setType(PrimitiveType.ERROR);
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
	private void enterParameterscope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createParameterScope();
		node.setScope(scope);
	}
	private void enterProcedurescope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createProcedureScope();
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
	public void visitLeave(ForStatementNode node) {
		ParseNode exp = node.child(1);
		if(exp.getType() != PrimitiveType.STRING && !(exp.getType() instanceof Array)) {
			ForExpressionMustBeRecordType(node, exp.getType());
			node.setType(PrimitiveType.ERROR);
		}
	}
	@Override
	public void visitLeave(DeclarationNode node) {
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		ParseNode initializer = node.child(1);
		Type declarationType = initializer.getType();

		node.setType(declarationType);		
		identifier.setType(declarationType);

		if(	! node.getToken().isLextant(Keyword.FUNC)	) {
			addBinding(identifier, declarationType, node.getIsStatic(), node.getIsConstant());
			
		}
	}
	@Override
	public void visitEnter(ParameterNode node) {
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		
		identifier.setType(node.getType());
		addBinding(identifier, node.getType(), false, true);
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
				typeMismatchError(target.getToken(), targetType, assignmentType);
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
	private int hackedPromotionTargetLevel(Type type) {
		return Math.min(promotionLevel(type),1);
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
						hackedPromotionTargetLevel(t[0]),
						hackedPromotionTargetLevel(t[1])
					};
				}
				int x=0, y=0;
				int mx = precendenceOrder[0][0], my = precendenceOrder[0][1];
				for(int i=1;i<precendenceOrder.length; i++) {
					if(precendenceOrder[i][0]<precendenceOrder[x][0]) {
						x = i;
						mx = precendenceOrder[x][0];
					}
					if(precendenceOrder[i][1]<precendenceOrder[y][1]) {
						y = i;
						my = precendenceOrder[y][1];
					}
				}
				int count = 0;
				for(int[] o: precendenceOrder) {
					if(o[0]==mx && o[1]==my) {
						count++;
					}
				}
				if(count==1) {
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

	private void manageCallOperator(OperatorNode node) {
		// make sure the child is an operator node with function invocation as operator
		if(node.nChildren()!=1 || !(node.child(0) instanceof OperatorNode) || 
				((OperatorNode)node.child(0)).getOperator() != Punctuator.FUNCTION_INVOCATION )
		{
			callWithoutFunctionCall(node);
			node.setType(PrimitiveType.ERROR);
		}
	}

	private void manageFunctionCall(OperatorNode node, ArrayList<Type> childTypes) {
		if(childTypes.get(0) instanceof LambdaType) {

			LambdaType lambda = (LambdaType) childTypes.get(0);
			ArrayList<Type> params = lambda.getParamTypes();

			if(childTypes.size()-1 == params.size()) {

				boolean success=true;
				for(int i=0;i<params.size();i++) {
					if(!childTypes.get(i+1).equivalent(params.get(i))) {
						success=false;
						break;
					}
				}
				if(success) {
					if(lambda.getReturnType() == SpecialType.VOID && 
							( !(node.getParent() instanceof OperatorNode) ||  
									((OperatorNode)node.getParent()).getOperator() != Keyword.CALL)
							) 
					{
						voidFunctionWithoutCall(node);
						node.setType(PrimitiveType.ERROR);
					}
					else {
						node.setType(lambda.getReturnType());
					}
					return;
				}
			}
		}
		typeCheckError(node, childTypes);
		node.setType(PrimitiveType.ERROR);
	}
	
	private void manageMapOperator(OperatorNode node) {
		if(node.nChildren()!=2) {
			wrongNumberOfOperators(node.getToken(), 2, node.nChildren());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		if(!(node.child(0).getType() instanceof Array)) {
			typeMismatchError(node.getToken(), new Array(new TypeVariable("Any_Array")), node.child(0).getType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		Array left = (Array)node.child(0).getType();
		if(!(node.child(1).getType() instanceof LambdaType)) {
			typeMismatchError(node.getToken(), 
					new LambdaType(new ArrayList<>(Arrays.asList(left.getSubType())), new TypeVariable("Any_Value_Type")),
					node.child(1).getType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		LambdaType right = (LambdaType)node.child(1).getType();
		if(right.getParamTypes().size()!=1) {
			wrongNumberOfOperators(node.getToken(), 1, right.getParamTypes().size());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		if(!right.getParamTypes().get(0).equivalent(left.getSubType())) {
			typeMismatchError(node.getToken(),right.getParamTypes().get(0), left.getSubType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		if(right.getReturnType()==SpecialType.VOID) {
			cannotHaveVoidReturn(node.getToken());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		node.setSignature(new FunctionSignature(new MapReduceGenerator(false), right.getReturnType()));
		node.setType(new Array(right.getReturnType()));
	}
	
	private void manageReduceOperator(OperatorNode node) {
		if(node.nChildren()!=2) {
			wrongNumberOfOperators(node.getToken(), 2, node.nChildren());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		if(!(node.child(0).getType() instanceof Array)) {
			typeMismatchError(node.getToken(), new Array(new TypeVariable("Any_Array")), node.child(0).getType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		Array left = (Array)node.child(0).getType();
		if(!(node.child(1).getType() instanceof LambdaType)) {
			typeMismatchError(node.getToken(), 
					new LambdaType(new ArrayList<>(Arrays.asList(left.getSubType())), PrimitiveType.BOOLEAN),
					node.child(1).getType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		LambdaType right = (LambdaType)node.child(1).getType();
		if(right.getParamTypes().size()!=1) {
			wrongNumberOfOperators(node.getToken(), 1, right.getParamTypes().size());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		if(!right.getParamTypes().get(0).equivalent(left.getSubType())) {
			typeMismatchError(node.getToken(),right.getParamTypes().get(0), left.getSubType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		if(right.getReturnType()!=PrimitiveType.BOOLEAN) {
			typeMismatchError(node.getToken(), PrimitiveType.BOOLEAN, right.getReturnType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		node.setSignature(new FunctionSignature(new MapReduceGenerator(true), left));
		node.setType(new Array(left));
	}
	
	private void manageFoldOperator(OperatorNode node) {
		boolean hasStart=false;
		if(node.nChildren()==3) {
			hasStart=true;
		}
		else if(node.nChildren()!=2) {
			wrongNumberOfOperators(node.getToken(), 2, node.nChildren());
			wrongNumberOfOperators(node.getToken(), 3, node.nChildren());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		
		if(!(node.child(0).getType() instanceof Array)) {
			typeMismatchError(node.getToken(), new Array(new TypeVariable("Any_Array")), node.child(0).getType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		Array arr = (Array) node.child(0).getType();
		
		
		
		
		if(hasStart) {
			Type baseType = node.child(1).getType();
			if(!(node.child(2).getType() instanceof LambdaType)) {
				typeMismatchError(node.getToken(), 
						new LambdaType(new ArrayList<>(Arrays.asList(baseType, arr.getSubType())), baseType),
						node.child(2).getType());
				node.setType(PrimitiveType.ERROR);
				return;
			}
			LambdaType lambda = (LambdaType)node.child(2).getType();
			if(lambda.getParamTypes().size()!=2) {
				wrongNumberOfOperators(node.getToken(), 2, lambda.getParamTypes().size());
				node.setType(PrimitiveType.ERROR);
				return;
			}
			if(!lambda.getParamTypes().get(0).equivalent(baseType)) {
				typeMismatchError(node.getToken(),lambda.getParamTypes().get(0), baseType);
				node.setType(PrimitiveType.ERROR);
				return;
			}
			if(!lambda.getParamTypes().get(1).equivalent(arr.getSubType())) {
				typeMismatchError(node.getToken(),lambda.getParamTypes().get(1), arr.getSubType());
				node.setType(PrimitiveType.ERROR);
				return;
			}
			if(lambda.getReturnType()!=baseType) {
				typeMismatchError(node.getToken(),lambda.getReturnType(), baseType);
				node.setType(PrimitiveType.ERROR);
				return;
			}
			node.setSignature(new FunctionSignature(new FoldCodeGenerator(true), baseType));
			node.setType(baseType);
		}
		else {
			if(!(node.child(1).getType() instanceof LambdaType)) {
				typeMismatchError(node.getToken(), 
						new LambdaType(new ArrayList<>(Arrays.asList(arr.getSubType(),arr.getSubType())), arr.getSubType()),
						node.child(1).getType());
				node.setType(PrimitiveType.ERROR);
				return;
			}
			LambdaType lambda = (LambdaType)node.child(1).getType();
			if(lambda.getParamTypes().size()!=2) {
				wrongNumberOfOperators(node.getToken(), 2, lambda.getParamTypes().size());
				node.setType(PrimitiveType.ERROR);
				return;
			}
			if(!lambda.getParamTypes().get(0).equivalent(arr.getSubType())) {
				typeMismatchError(node.getToken(),lambda.getParamTypes().get(0), arr.getSubType());
				node.setType(PrimitiveType.ERROR);
				return;
			}
			if(!lambda.getParamTypes().get(1).equivalent(arr.getSubType())) {
				typeMismatchError(node.getToken(),lambda.getParamTypes().get(1), arr.getSubType());
				node.setType(PrimitiveType.ERROR);
				return;
			}
			if(lambda.getReturnType()!=arr.getSubType()) {
				typeMismatchError(node.getToken(),lambda.getReturnType(), arr.getSubType());
				node.setType(PrimitiveType.ERROR);
				return;
			}
			node.setSignature(new FunctionSignature(new FoldCodeGenerator(false), arr.getSubType()));
			node.setType(arr.getSubType());
		}
	}
	
	private void manageZipOperator(OperatorNode node) {
		if(node.nChildren()!=3) {
			wrongNumberOfOperators(node.getToken(), 3, node.nChildren());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		if(!(node.child(0).getType() instanceof Array)) {
			typeMismatchError(node.getToken(), new Array(new TypeVariable("Any_Array")), node.child(0).getType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		if(!(node.child(1).getType() instanceof Array)) {
			typeMismatchError(node.getToken(), new Array(new TypeVariable("Any_Array")), node.child(1).getType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		Array arr1 = (Array) node.child(0).getType();
		Array arr2 = (Array) node.child(1).getType();
		if(!(node.child(2).getType() instanceof LambdaType)) {
			typeMismatchError(node.getToken(), 
					new LambdaType(new ArrayList<>(Arrays.asList(arr1.getSubType(), arr2.getSubType())), new TypeVariable("Any_type")),
					node.child(2).getType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		LambdaType lambda = (LambdaType)node.child(2).getType();
		if(lambda.getParamTypes().size()!=2) {
			wrongNumberOfOperators(node.getToken(), 2, lambda.getParamTypes().size());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		if(!lambda.getParamTypes().get(0).equivalent(arr1.getSubType())) {
			typeMismatchError(node.getToken(),lambda.getParamTypes().get(0), arr1.getSubType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		if(!lambda.getParamTypes().get(1).equivalent(arr2.getSubType())) {
			typeMismatchError(node.getToken(),lambda.getParamTypes().get(1), arr2.getSubType());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		if(lambda.getReturnType()==SpecialType.VOID) {
			cannotHaveVoidReturn(node.getToken());
			node.setType(PrimitiveType.ERROR);
			return;
		}
		node.setSignature(new FunctionSignature(new ZipCodeGenerator(), lambda.getReturnType()));
		node.setType(new Array(lambda.getReturnType()));
	}

	@Override
	public void visitLeave(OperatorNode node) {
		Lextant operator = node.getOperator();
		ArrayList<Type> childTypes = new ArrayList<Type>();
		for(ParseNode child: node.getChildren()) {
			childTypes.add(child.getType());
		}
		if(operator == Punctuator.FUNCTION_INVOCATION) {
			manageFunctionCall(node, childTypes);
			return;
		}
		if(operator == Keyword.CALL) {
			manageCallOperator(node);
			return;
		}
		if(operator == Keyword.MAP) {
			manageMapOperator(node);
			return;
		}
		if(operator == Keyword.REDUCE) {
			manageReduceOperator(node);
			return;
		}
		if(operator == Keyword.FOLD) {
			manageFoldOperator(node);
			return;
		}
		if(operator == Keyword.ZIP) {
			manageZipOperator(node);
			return;
		}

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
	@Override
	public void visit(LoopJumperNode node) {
		boolean found=false;
		for(ParseNode current: node.pathToRoot()) {
			if(current instanceof WhileStatementNode) {
				node.setJumpLabelFromParent((WhileStatementNode)current);
				found=true;
				break;
			}
			if(current instanceof ForStatementNode) {
				node.setJumpLabelFromParent((ForStatementNode)current);
				found=true;
				break;
			}
		}
		if(!found) {
			loopParentNotFound(node);
		}
	}
	///////////////////////////////////////////////////////////////////////////
	// IdentifierNodes, with helper methods
	@Override
	public void visit(IdentifierNode node) {
		if(!parentIsHandlingit(node)) {		
			Binding binding = node.findVariableBinding();
			node.setType(binding.getType());
			node.setBinding(binding);
		}
		// else parent DeclarationNode does the processing.
	}
	private boolean parentIsHandlingit(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof DeclarationNode || parent instanceof ParameterNode || parent instanceof ForStatementNode)
				&& (node == parent.child(0));
	}
	private void addBinding(IdentifierNode identifierNode, Type type, boolean isStatic, boolean constant) {
		Scope scope = identifierNode.getLocalScope();
		Binding binding = scope.createBinding(identifierNode, type, isStatic, constant);
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
	private void loopParentNotFound(ParseNode node) {
		logError("No Loop parent found for "+node.getToken().getLexeme()+" at" + node.getToken().getLocation());
	}
	private void castTypeCheckError(ParseNode node, Type from, Type to) {
		logError("Cannot cast from "+from+" to "+to+" at " + node.getToken().getLocation());
	}
	private void callWithoutFunctionCall(ParseNode node) {
		logError("Call keyword must be followed by expression evaluating to a function call at " + node.getToken().getLocation());
	}
	private void voidFunctionWithoutCall(ParseNode node) {
		logError("Void function must have call keyword before invocation " + node.getToken().getLocation());
	}
	private void wrongReturnType(ParseNode node, Type expected, Type received) {
		logError("Expected return type "+expected+" but received "+received+" at " + node.getToken().getLocation());
	}
	private void voidShouldNotHaveReturnExpression(ParseNode node) {
		logError("Void function should not have an expression in return at " + node.getToken().getLocation());
	}
	private void returnParentNotFound(ParseNode node) {
		logError("No return parent found for return statement at" + node.getToken().getLocation());
	}
	private void ForExpressionMustBeRecordType(ParseNode node, Type type) {
		logError("Expected a record type but received "+type+" at " + node.getToken().getLocation());
	}
	private void assignmentToConstant(Token token) {
		logError("Cannot write to const variable \""+
				token.getLexeme()+"\" at "+token.getLocation());
	}
	private void assignmentToUntargetableType(Token token) {
		logError("Expression is not a targetable type \""+
				token.getLexeme()+"\" at "+token.getLocation());
	}
	private void typeMismatchError(Token token, Type expected, Type received) {
		logError("Expected type "+expected+" for "+token.getLexeme()+
				", but received "+received+" at "+token.getLocation());
	}
	private void cannotHaveVoidReturn(Token token) {
		logError("Cannot have lambda with return VOID at "+token.getLocation());
	}
	private void wrongNumberOfOperators(Token token, int expected, int received) {
		logError("Expected "+expected+" operators for "+token.getLexeme()+
				", but received "+received+" at "+token.getLocation());
	}
	private void wrongNumberOfParameters(Token token, int expected, int received) {
		logError("Expected "+expected+" parameters for "+token.getLexeme()+
				", but received "+received+" at "+token.getLocation());
	}
	private void logError(String message) {
		PikaLogger log = PikaLogger.getLogger("compiler.semanticAnalyzer");
		log.severe(message);
	}
}