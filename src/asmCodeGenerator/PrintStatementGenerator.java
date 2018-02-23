package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import parseTree.ParseNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.TabSpaceNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import asmCodeGenerator.ASMCodeGenerator.CodeVisitor;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.operators.ArrayLengthCodeGenerator;
import asmCodeGenerator.runtime.RunTime;

public class PrintStatementGenerator {
	ASMCodeFragment code;
	ASMCodeGenerator.CodeVisitor visitor;
	
	
	public PrintStatementGenerator(ASMCodeFragment code, CodeVisitor visitor) {
		super();
		this.code = code;
		this.visitor = visitor;
	}

	public void generate(PrintStatementNode node) {
		for(ParseNode child : node.getChildren()) {
			if(child instanceof NewlineNode || child instanceof SpaceNode || child instanceof TabSpaceNode) {
				ASMCodeFragment childCode = visitor.removeVoidCode(child);
				code.append(childCode);
			}
			else {
				appendPrintCode(child);
			}
		}
	}

	private void appendPrintCode(ParseNode node) {
		code.append(visitor.removeValueCode(node));
		code.add(Call, printSubroutine(node.getType()));
	}

	private String printSubroutine(Type type) {
		if(type instanceof Array) {
			Array arr = (Array) type;
			code.add(PushD, printSubroutineFromAddress(arr.innerMostType()));
			Macros.storeITo(code, RunTime.INNER_MOST_PRINT_CALL);
			return RunTime.PRINT_ARRAY;
		}

		assert type instanceof PrimitiveType;

		switch((PrimitiveType)type) {
		case INTEGER:		return RunTime.PRINT_INTEGER;
		case FLOAT:			return RunTime.PRINT_FLOAT;
		case CHARACTER:		return RunTime.PRINT_CHARACTER;
		case STRING:		return RunTime.PRINT_STRING;
		case BOOLEAN:		return RunTime.PRINT_BOOLEAN;
		case RATIONAL:		return RunTime.PRINT_RATIONAL;
		default:
			assert false : "Type " + type + " unimplemented in PrintStatementGenerator.printSubroutine()";
			return "";
		}
	}

	private String printSubroutineFromAddress(Type type) {
		assert type instanceof PrimitiveType;

		switch((PrimitiveType)type) {
		case INTEGER:		return RunTime.LOAD_PRINT_INTEGER;
		case FLOAT:			return RunTime.LOAD_PRINT_FLOAT;
		case CHARACTER:		return RunTime.LOAD_PRINT_CHARACTER;
		case STRING:		return RunTime.LOAD_PRINT_STRING;
		case BOOLEAN:		return RunTime.LOAD_PRINT_BOOLEAN;
		case RATIONAL:		return RunTime.LOAD_PRINT_RATIONAL;
		default:		
			assert false : "Type " + type + " unimplemented in PrintStatementGenerator.printSubroutineFromAddress()";
			return "";
		}
	}
}
