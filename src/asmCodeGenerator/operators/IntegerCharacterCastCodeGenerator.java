package asmCodeGenerator.operators;

import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import parseTree.ParseNode;

public class IntegerCharacterCastCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment fragment = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		fragment.add(ASMOpcode.PushI, 127);
		fragment.add(ASMOpcode.BTAnd);
		return fragment;
	}

}
