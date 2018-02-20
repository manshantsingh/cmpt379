package asmCodeGenerator.operators;

import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import asmCodeGenerator.codeStorage.ASMOpcode;
import parseTree.ParseNode;

public class FormRationalCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment fragment = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		fragment.add(ASMOpcode.Call, RunTime.LOWEST_TERMS);
		return fragment;
	}

}
