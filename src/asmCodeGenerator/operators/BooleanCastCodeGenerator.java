package asmCodeGenerator.operators;

import static asmCodeGenerator.codeStorage.ASMOpcode.Jump;
import static asmCodeGenerator.codeStorage.ASMOpcode.JumpTrue;
import static asmCodeGenerator.codeStorage.ASMOpcode.Label;
import static asmCodeGenerator.codeStorage.ASMOpcode.PushI;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import parseTree.ParseNode;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class BooleanCastCodeGenerator implements SimpleCodeGenerator {

	private Type initialType;

	public BooleanCastCodeGenerator(Type type) {
		initialType = type;
	}

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment fragment = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		Labeller labeller = new Labeller("boolean-cast");
		String trueLabel  = labeller.newLabel("true");
		String falseLabel = labeller.newLabel("false");
		String joinLabel  = labeller.newLabel("join");

		if(initialType == PrimitiveType.INTEGER || initialType == PrimitiveType.CHARACTER) {
			fragment.add(JumpTrue, trueLabel);
			fragment.add(Jump, falseLabel);
		}
//		Floating to boolean is not allowed
//		else if(initialType == PrimitiveType.FLOAT) {
//			fragment.add(JumpFZero, falseLabel);
//			fragment.add(Jump, trueLabel);
//		}

		fragment.add(Label, trueLabel);
		fragment.add(PushI, 1);
		fragment.add(Jump, joinLabel);
		fragment.add(Label, falseLabel);
		fragment.add(PushI, 0);
		fragment.add(Jump, joinLabel);
		fragment.add(Label, joinLabel);
		return fragment;
	}

}
