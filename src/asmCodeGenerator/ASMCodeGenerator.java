package asmCodeGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.operators.ArrayIndexingCodeGenerator;
import asmCodeGenerator.operators.RationalAddSubtractCodeGenerator;
import asmCodeGenerator.operators.RecordReleaseCodeGenerator;
import asmCodeGenerator.runtime.MemoryManager;
import asmCodeGenerator.runtime.RunTime;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
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
import parseTree.nodeTypes.FloatConstantNode;
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
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.SpecialType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;

import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import static asmCodeGenerator.ASMConstants.*;

// do not call the code generator if any errors have occurred during analysis.
public class ASMCodeGenerator {
	public static final int ADDRESS_SIZE = PrimitiveType.STRING.getSize();
	public static final int FRAME_ADDITIONAL_SIZE = ADDRESS_SIZE * 2;

	ParseNode root;

	public static ASMCodeFragment generate(ParseNode syntaxTree) {
		ASMCodeGenerator codeGenerator = new ASMCodeGenerator(syntaxTree);
		return codeGenerator.makeASM();
	}
	public ASMCodeGenerator(ParseNode root) {
		super();
		this.root = root;
	}
	
	public ASMCodeFragment makeASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);

		code.append( MemoryManager.codeForInitialization() );
		code.append( RunTime.getEnvironment() );
		code.append( globalVariableBlockASM() );
		code.append( programASM() );
		code.append( MemoryManager.codeForAfterApplication() );
		
		return code;
	}
	private ASMCodeFragment globalVariableBlockASM() {
		assert root.hasScope();
		Scope scope = root.getScope();
		int globalBlockSize = scope.getAllocatedSize();
		
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.add(DLabel, RunTime.GLOBAL_MEMORY_BLOCK);
		code.add(DataZ, globalBlockSize);
		return code;
	}
	private ASMCodeFragment programASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);

		code.add(    Label, RunTime.MAIN_PROGRAM_LABEL);
		code.append( programCode());
		code.add(    Halt );
		
		return code;
	}
	private ASMCodeFragment programCode() {
		CodeVisitor visitor = new CodeVisitor();
		root.accept(visitor);
		return visitor.removeRootCode(root);
	}

	public static void loadFromAddress(ASMCodeFragment code, Type type) {
		if(type == PrimitiveType.INTEGER || type == PrimitiveType.STRING ||
				type instanceof Array || type instanceof LambdaType)
		{
			code.add(LoadI);
		}
		else if(type == PrimitiveType.FLOAT) {
			code.add(LoadF);
		}
		else if(type == PrimitiveType.BOOLEAN || type == PrimitiveType.CHARACTER) {
			code.add(LoadC);
		}
		else if(type == PrimitiveType.RATIONAL) {
			code.add(Duplicate);
			code.add(LoadI);
			code.add(Exchange);	// [... numerator address]
			code.add(PushI, PrimitiveType.INTEGER.getSize());
			code.add(Add);
			code.add(LoadI);
		}
		else {
			assert false : "unhandled type: " + type;
		}
	}

	public static void storeToAddress(ASMCodeFragment code, Type type) {
		if(type == PrimitiveType.INTEGER || type == PrimitiveType.STRING ||
				type instanceof Array || type instanceof LambdaType)
		{
			code.add(StoreI);
		}
		else if(type == PrimitiveType.FLOAT) {
			code.add(StoreF);
		}
		else if(type == PrimitiveType.BOOLEAN || type == PrimitiveType.CHARACTER) {
			code.add(StoreC);
		}
		else if(type == PrimitiveType.RATIONAL) {
			// [... Location numerator denominator]
			Macros.storeITo(code, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);
			Macros.storeITo(code, RunTime.RATIONAL_NUMERATOR_TEMPORARY);
			code.add(Duplicate);
			code.add(PushI, PrimitiveType.INTEGER.getSize());
			code.add(Add);	// [... Location Location+1]
			Macros.loadIFrom(code, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);
			code.add(StoreI);
			Macros.loadIFrom(code, RunTime.RATIONAL_NUMERATOR_TEMPORARY);
			code.add(StoreI);
		}
		else{
			assert false: "Type " + type + " unimplemented in opcodeForStore()";
		}
	}

	protected class CodeVisitor extends ParseNodeVisitor.Default {
		private Map<ParseNode, ASMCodeFragment> codeMap;
		ASMCodeFragment code;
		
		public CodeVisitor() {
			codeMap = new HashMap<ParseNode, ASMCodeFragment>();
		}


		////////////////////////////////////////////////////////////////////
        // Make the field "code" refer to a new fragment of different sorts.
		private void newAddressCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_ADDRESS);
			codeMap.put(node, code);
		}
		private void newValueCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VALUE);
			codeMap.put(node, code);
		}
		private void newVoidCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VOID);
			codeMap.put(node, code);
		}

	    ////////////////////////////////////////////////////////////////////
        // Get code from the map.
		private ASMCodeFragment getAndRemoveCode(ParseNode node) {
			ASMCodeFragment result = codeMap.get(node);
			codeMap.remove(result);
			return result;
		}
	    public  ASMCodeFragment removeRootCode(ParseNode tree) {
			return getAndRemoveCode(tree);
		}		
		ASMCodeFragment removeValueCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			makeFragmentValueCode(frag, node);
			return frag;
		}		
		private ASMCodeFragment removeAddressCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isAddress();
			return frag;
		}		
		ASMCodeFragment removeVoidCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isVoid();
			return frag;
		}
		
	    ////////////////////////////////////////////////////////////////////
        // convert code to value-generating code.
		private void makeFragmentValueCode(ASMCodeFragment code, ParseNode node) {
			assert !code.isVoid();
			
			if(code.isAddress()) {
				turnAddressIntoValue(code, node);
			}	
		}
		private void turnAddressIntoValue(ASMCodeFragment code, ParseNode node) {
			loadFromAddress(code, node.getType());
			code.markAsValue();
		}

	    ////////////////////////////////////////////////////////////////////
        // ensures all types of ParseNode in given AST have at least a visitLeave	
		public void visitLeave(ParseNode node) {
			assert false : "node " + node + " not handled in ASMCodeGenerator";
		}
		
		
		
		///////////////////////////////////////////////////////////////////////////
		// constructs larger than statements
		public void visitLeave(ProgramNode node) {
			newVoidCode(node);

			for(int i=0;i<node.nChildren();i++) {
				ASMCodeFragment childCode = removeVoidCode(node.child(i));
				code.append(childCode);
			}
		}
		public void visitLeave(BlockStatementsNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}

		///////////////////////////////////////////////////////////////////////////
		// function specials

		public void visitLeave(LambdaNode node) {
			
			Type returnType = ((LambdaType) node.getType()).getReturnType();

			newValueCode(node);

			code.add(Jump, node.getEndLabel());

			code.add(Label, node.getFunctionLocationLabel());
			Macros.loadIFrom(code, RunTime.STACK_POINTER);
			code.add(PushI, ADDRESS_SIZE);
			code.add(Subtract);
			code.add(Duplicate);	// [...  caller_return_addr  SP_val-4  SP_val-4]
			Macros.loadIFrom(code, RunTime.FRAME_POINTER);
			code.add(StoreI);		// [...  caller_return_addr  SP_val-4]
			code.add(PushI, ADDRESS_SIZE);
			code.add(Subtract);
			code.add(Exchange);		// [...  SP_val-8  caller_return_addr]
			code.add(StoreI);
			Macros.loadIFrom(code, RunTime.STACK_POINTER);
			Macros.storeITo(code, RunTime.FRAME_POINTER);		// [...]

			ParseNode bodyCode = node.child(node.nChildren()-1);
			Macros.loadIFrom(code, RunTime.STACK_POINTER);
			code.add(PushI, bodyCode.getScope().getAllocatedSize() + FRAME_ADDITIONAL_SIZE);
			code.add(Subtract);
			Macros.storeITo(code, RunTime.STACK_POINTER);		// [...]
			
			code.append(removeVoidCode(bodyCode));

			code.add(Jump, RunTime.LAMBDA_REACHED_END_OF_FUNCTION_NO_RETURN);


			code.add(Label, node.getReturnCodeLabel());		// [...  returnValue]
			Macros.loadIFrom(code, RunTime.FRAME_POINTER);
			code.add(PushI, 2*ADDRESS_SIZE);
			code.add(Subtract);
			code.add(LoadI);		// [...  returnValue  returnAddr]
			Macros.loadIFrom(code, RunTime.FRAME_POINTER);
			Macros.storeITo(code, RunTime.FRAME_POINTER);
			if(returnType.equivalent(PrimitiveType.RATIONAL)) {
				// [...  numer  denom  returnAddr]
				code.add(Exchange);
				Macros.storeITo(code, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);	// [... numer retAddr]
				code.add(Exchange);
				Macros.loadIFrom(code, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);	// [... retAddr numer denom]
			}
			else if(returnType.equivalent(SpecialType.VOID)){
				// [...  returnAddr]
			}
			else {
				code.add(Exchange);		// [...  returnAddr  returnValue]
			}
			Macros.loadIFrom(code, RunTime.STACK_POINTER);
			code.add(PushI, node.getScope().getAllocatedSize() + FRAME_ADDITIONAL_SIZE);
			code.add(Add);
			Macros.storeITo(code, RunTime.STACK_POINTER);	// [...  retAddr  retVal]
			
			if(returnType.equivalent(SpecialType.VOID)) {
				// [... retAddr]
			}
			else {
				Macros.loadIFrom(code, RunTime.STACK_POINTER); // [...  retAddr  retVal  SP_val]
				code.add(PushI, returnType.getSize());
				code.add(Subtract);
				code.add(Duplicate);
				Macros.storeITo(code, RunTime.STACK_POINTER);	// [...  retAddr  retVal  SP_val-type.size]
				if(returnType.equivalent(PrimitiveType.RATIONAL)) {
					// [...  retAddr  numer  denom  SP_val-8]
					code.add(Exchange);
					Macros.storeITo(code, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);
					// [...  retAddr  numer SP_val-8]
					code.add(Exchange);
					Macros.loadIFrom(code, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);
					// [... retAddr  SP_val-8  numer  denom]
				}
				else {
					
					code.add(Exchange); // [...  retAddr  SP_val-type.size  retVal]
				}
				storeToAddress(code, returnType);
				// [... retAddr]
			}
			code.add(Return);
			
			code.add(Label, node.getEndLabel());
			code.add(PushD, node.getFunctionLocationLabel());
		}
		public void visitLeave(ParameterNode node) {
			newVoidCode(node);
			// TODO; msk
		}
		public void visitLeave(ReturnNode node) {
			Type type = node.getType();

			newVoidCode(node);
			if(type == SpecialType.VOID) {
				newVoidCode(node);
			}
			else if(type == PrimitiveType.STRING || type instanceof Array) {
//				newAddressCode(node);
				code.append(removeAddressCode(node.child(0)));
			}
			else {
//				newValueCode(node);
				code.append(removeValueCode(node.child(0)));
			}
			code.add(Jump, node.getFunctionReturnLabel());
		}

		///////////////////////////////////////////////////////////////////////////
		// statements and declarations

		public void visitLeave(PrintStatementNode node) {
			newVoidCode(node);
			new PrintStatementGenerator(code, this).generate(node);	
		}
		public void visitLeave(IfStatementNode node) {
			newVoidCode(node);

			ASMCodeFragment conditionCode = removeValueCode(node.child(0));
			ASMCodeFragment blockCode = removeVoidCode(node.child(1));

			Labeller labeller = new Labeller("if");
			String elseLabel = labeller.newLabel("else");
			String end = labeller.newLabel("end");

			code.append(conditionCode);
			code.add(JumpFalse, elseLabel);
			code.append(blockCode);
			code.add(Jump, end);

			code.add(Label, elseLabel);
			if(node.getChildren().size()>2) {
				ASMCodeFragment elseBlock = removeVoidCode(node.child(2));
				code.append(elseBlock);
			}

			code.add(Label, end);
		}
		public void visitLeave(WhileStatementNode node) {
			newVoidCode(node);

			ASMCodeFragment conditionCode = removeValueCode(node.child(0));
			ASMCodeFragment blockCode = removeVoidCode(node.child(1));

			code.add(Label, node.getTopLabel());
			code.append(conditionCode);
			code.add(JumpFalse, node.getEndLabel());
			code.append(blockCode);
			code.add(Jump, node.getTopLabel());

			code.add(Label, node.getEndLabel());
		}
		public void visit(NewlineNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.NEWLINE_PRINT);
			code.add(Printf);
		}
		public void visit(SpaceNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.SPACE_PRINT);
			code.add(Printf);
		}
		public void visit(TabSpaceNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.TAB_SPACE_PRINT);
			code.add(Printf);
		}
		public void visit(LoopJumperNode node) {
			newVoidCode(node);
			code.add(Jump, node.getJumpLabel());
		}
		

		public void visitLeave(DeclarationNode node) {
			storeToLocation(node);
		}

		public void visitLeave(AssignmentNode node) {
			storeToLocation(node);
		}

		private void storeToLocation(ParseNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));
			ASMCodeFragment rvalue = removeValueCode(node.child(1));

			code.append(lvalue);
			code.append(rvalue);

			storeToAddress(code, node.getType());
		}


		///////////////////////////////////////////////////////////////////////////
		// expressions
		public void visitLeave(CastNode node) {
			newValueCode(node);
			ASMCodeFragment arg1 = removeValueCode(node.child(0));

			callFunctionSignature(node, node.getSignature(), arg1);
		}

		public void visitLeave(OperatorNode node) {
			Lextant operator = node.getOperator();

			if(operator == Punctuator.FUNCTION_INVOCATION) {
				visitFunctionCall(node);
			}
			else if(Punctuator.isComparison(operator)) {
				visitComparisonOperatorNode(node, (Punctuator) operator);
			}
			else {
				visitNormalOperatorNode(node);
			}
		}
		private void visitComparisonOperatorNode(OperatorNode node, Punctuator cmp) {

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));

			Type type = node.child(0).getType();

			Labeller labeller = new Labeller("compare");

			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");

			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);

			if(type == PrimitiveType.RATIONAL) {
				// remove the denominators and leave the numerator only
				RationalAddSubtractCodeGenerator.commonDenominator(code);
			}

			if(		type == PrimitiveType.INTEGER ||
					type == PrimitiveType.CHARACTER ||
					type == PrimitiveType.RATIONAL)
			{
				code.add(Subtract);
				switch(cmp) {
				case GREATER:
					code.add(JumpPos, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case GREATER_EQUAL:
					code.add(JumpNeg, falseLabel);
					code.add(Jump, trueLabel);
					break;
				case LESS:
					code.add(JumpNeg, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case LESS_EQUAL:
					code.add(JumpPos, falseLabel);
					code.add(Jump, trueLabel);
					break;
				case EQUALITY:
					code.add(JumpFalse, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case INEQUALITY:
					code.add(JumpTrue, trueLabel);
					code.add(Jump, falseLabel);
					break;
				}
			}
			else if(type == PrimitiveType.FLOAT) {
				code.add(FSubtract);
				switch(cmp) {
				case GREATER:
					code.add(JumpFPos, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case GREATER_EQUAL:
					code.add(JumpFNeg, falseLabel);
					code.add(Jump, trueLabel);
					break;
				case LESS:
					code.add(JumpFNeg, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case LESS_EQUAL:
					code.add(JumpFPos, falseLabel);
					code.add(Jump, trueLabel);
					break;
				case EQUALITY:
					code.add(JumpFZero, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case INEQUALITY:
					code.add(JumpFZero, falseLabel);
					code.add(Jump, trueLabel);
					break;
				}
			}
			else if(type == PrimitiveType.BOOLEAN) {
				code.add(Subtract);
				switch(cmp) {
				case EQUALITY:
					code.add(JumpFalse, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case INEQUALITY:
					code.add(JumpTrue, trueLabel);
					code.add(Jump, falseLabel);
					break;
				}
			}
			else if(type==PrimitiveType.STRING || type instanceof Array) {
				code.add(Subtract);
				switch(cmp) {
				case EQUALITY:
					code.add(JumpFalse, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case INEQUALITY:
					code.add(JumpTrue, trueLabel);
					code.add(Jump, falseLabel);
					break;
				}
			}
			else {
				System.out.println("Some unimplemented type got here: " + type);
			}

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);

		}
		private void visitFunctionCall(OperatorNode node) {
			// TODO: msk
			Type type = node.getType();
			if(type == SpecialType.VOID) {
				newVoidCode(node);
			}
			else if(type == PrimitiveType.STRING || type instanceof Array) {
				newAddressCode(node);
			}
			else {
				newValueCode(node);
			}

			ASMCodeFragment[] args = new ASMCodeFragment[node.nChildren()];
			for(int i=0;i<args.length;i++) {
				args[i] = removeValueCode(node.child(i));
			}

			for(int i=1;i<args.length;i++) {
				Type argType = node.child(i).getType();
				Macros.loadIFrom(code, RunTime.STACK_POINTER);
				code.add(PushI, argType.getSize());
				code.add(Subtract);
				code.add(Duplicate);
				Macros.storeITo(code, RunTime.STACK_POINTER);
				code.append(args[i]);
				storeToAddress(code, argType);
			}
			code.append(args[0]);
			code.add(CallV);

			// msk TODO hehe
			if(type != SpecialType.VOID) {
				Macros.loadIFrom(code, RunTime.STACK_POINTER);
				loadFromAddress(code, type);
				Macros.loadIFrom(code, RunTime.STACK_POINTER);
				code.add(PushI, type.getSize());
				code.add(Add);
				Macros.storeITo(code, RunTime.STACK_POINTER);
			}
		}
		private void visitNormalOperatorNode(OperatorNode node) {
			Object varient = node.getSignature().getVariant();
			if(varient instanceof ArrayIndexingCodeGenerator) {
				newAddressCode(node);
			}
			else if(varient instanceof RecordReleaseCodeGenerator) {
				newVoidCode(node);
			}
			else {
				newValueCode(node);
			}

			ASMCodeFragment[] args = new ASMCodeFragment[node.getChildren().size()];
			for(int i=0;i<args.length;i++) {
				args[i] = removeValueCode(node.child(i));
			}

			callFunctionSignature(node, node.getSignature(), args);
		}

		private void callFunctionSignature(ParseNode node, FunctionSignature signature, ASMCodeFragment... args) {
			Object variant = signature.getVariant();

			if(variant instanceof FullCodeGenerator) {
				FullCodeGenerator generator = (FullCodeGenerator) variant;
				ASMCodeFragment fragment = generator.generate(node, args);
				code.append(fragment);
				return;
			}

			// append all arguments
			for(ASMCodeFragment frag: args) {
				code.append(frag);
			}

			if(variant instanceof ASMOpcode) {
				ASMOpcode opcode = (ASMOpcode) variant;
				code.add(opcode);
				// type-dependent! (opcode is different for floats and for ints)
			}
			else if(variant instanceof SimpleCodeGenerator) {
				SimpleCodeGenerator generator = (SimpleCodeGenerator) variant;
				ASMCodeFragment fragment = generator.generate(node);
				code.append(fragment);
			}
			else {
				// Do nothing
			}
		}

		public void visitLeave(ArrayNode node) {
			newValueCode(node);

			Type subType = ((Array)node.getType()).getSubType();
			int statusFlags;
			if(subType instanceof Array || subType == PrimitiveType.STRING) {
				// TODO: fix this for string in procedure call
				statusFlags = ARRAY_STATUS_WITH_REFERENCE_SUBTYPE;
			}
			else {
				statusFlags = ARRAY_STATUS_WITHOUT_REFERENCE_SUBTYPE;
			}

			if(node.isNewDeclaration()) {
				ASMCodeFragment arg1 = removeValueCode(node.child(0));
				code.append(arg1);
				RecordsCodeGenerator.createEmptyArrayRecord(code, statusFlags, subType.getSize());
			}
			else {
				List<ParseNode> list = node.getChildren();
				ASMCodeFragment[] frags = new ASMCodeFragment[list.size()];
				for(int i=0; i<frags.length; i++) {
					frags[i] = removeValueCode(list.get(i));
				}
				RecordsCodeGenerator.createPopulatedArrayRecord(code, frags, statusFlags, subType);
			}
		}

		///////////////////////////////////////////////////////////////////////////
		// leaf nodes (ErrorNode not necessary)
		public void visit(BooleanConstantNode node) {
			newValueCode(node);
			code.add(PushI, node.getValue() ? 1 : 0);
		}
		public void visit(IdentifierNode node) {
			newAddressCode(node);
			Binding binding = node.getBinding();
			
			binding.generateAddress(code);
		}		
		public void visit(IntegerConstantNode node) {
			newValueCode(node);
			
			code.add(PushI, node.getValue());
		}		
		public void visit(FloatConstantNode node) {
			newValueCode(node);
			
			code.add(PushF, node.getValue());
		}
		public void visit(CharacterConstantNode node) {
			newValueCode(node);

			code.add(PushI, node.getValue());
		}
		public void visit(StringConstantNode node) {
			newValueCode(node);

			Labeller labeller = new Labeller("stringConst");
			String strLabel = labeller.newLabel(node.getValue());

			// TODO: change me
			RecordsCodeGenerator.createStringRecord(code, node.getValue());
		}
	}
}
