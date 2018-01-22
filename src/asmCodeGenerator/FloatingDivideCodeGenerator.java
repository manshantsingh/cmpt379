package asmCodeGenerator;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.runtime.RunTime;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import parseTree.ParseNode;

public class FloatingDivideCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment fragment = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		fragment.add(ASMOpcode.Duplicate);
		fragment.add(ASMOpcode.JumpFZero, RunTime.FLOATING_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		fragment.add(ASMOpcode.FDivide);
		return fragment;
	}

}
