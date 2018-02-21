package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import parseTree.ParseNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.TabSpaceNode;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import asmCodeGenerator.ASMCodeGenerator.CodeVisitor;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
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

		if(node.getType() == PrimitiveType.RATIONAL) {
			printRational();
			return;
		}

		String format = printFormat(node.getType());
		convertToStringIfBoolean(node);

		code.add(PushD, format);
		code.add(Printf);
	}
	private void convertToStringIfBoolean(ParseNode node) {
		if(node.getType() != PrimitiveType.BOOLEAN) {
			return;
		}
		
		Labeller labeller = new Labeller("print-boolean");
		String trueLabel = labeller.newLabel("true");
		String endLabel = labeller.newLabel("join");

		code.add(JumpTrue, trueLabel);
		code.add(PushD, RunTime.BOOLEAN_FALSE_STRING);
		code.add(Jump, endLabel);
		code.add(Label, trueLabel);
		code.add(PushD, RunTime.BOOLEAN_TRUE_STRING);
		code.add(Label, endLabel);
	}


	private static String printFormat(Type type) {
		assert type instanceof PrimitiveType;
		
		switch((PrimitiveType)type) {
		case INTEGER:		return RunTime.INTEGER_PRINT_FORMAT;
		case FLOAT:			return RunTime.FLOATING_PRINT_FORMAT;
		case CHARACTER:		return RunTime.CHARACTER_PRINT_FORMAT;
		case STRING:		return RunTime.STRING_PRINT_FORMAT;
		case BOOLEAN:		return RunTime.BOOLEAN_PRINT_FORMAT;
		default:		
			assert false : "Type " + type + " unimplemented in PrintStatementGenerator.printFormat()";
			return "";
		}
	}

	private void printRational() {
		Labeller labeller = new Labeller("print-rational");

		String negativePrint = labeller.newLabel("negative-print");
		String negativeEnd = labeller.newLabel("negative-end");

		String denomIsOne = labeller.newLabel("denom-is-one");
		String numeratorIsSmall = labeller.newLabel("numerator-is-small");
		String numeratorDone = labeller.newLabel("numerator-done");
		String end = labeller.newLabel("end");

		// [...  numerator  denominator]
		code.add(Exchange);
		code.add(Duplicate);
		code.add(JumpNeg, negativePrint);
		code.add(Jump, negativeEnd);

		code.add(Label, negativePrint);
		code.add(PushI, -1);
		code.add(Multiply);
		code.add(PushD, RunTime.MINUS_PRINT);
		code.add(Printf);

		code.add(Label, negativeEnd);
		code.add(Exchange);		// [...  abs(numerator)  denominator]
		code.add(Duplicate);
		code.add(PushI, 1);
		code.add(Subtract);
		code.add(JumpFalse, denomIsOne);
		code.add(Duplicate);
		Macros.storeITo(code, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);	// [... nume denom]
		code.add(Exchange);
		code.add(Duplicate);
		Macros.storeITo(code, RunTime.RATIONAL_NUMERATOR_TEMPORARY);
		code.add(Exchange);
		code.add(Divide);		// [...  nume/denom]
		code.add(Duplicate);
		code.add(JumpFalse, numeratorIsSmall);
		code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
		code.add(Printf);
		code.add(Jump, numeratorDone);

		code.add(Label, numeratorIsSmall);
		code.add(Pop);

		code.add(Label, numeratorDone);		// [...]
		Macros.loadIFrom(code, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);
		code.add(Duplicate);
		Macros.loadIFrom(code, RunTime.RATIONAL_NUMERATOR_TEMPORARY);
		code.add(Exchange);		// [...  denom  nume  denom]
		code.add(Remainder);
		code.add(PushD, RunTime.RATIONAL_PRINT_FORMAT);
		code.add(Printf);
		code.add(Jump, end);

		code.add(Label, denomIsOne);
		code.add(Pop);
		code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
		code.add(Printf);

		code.add(Label, end);
	}
}
