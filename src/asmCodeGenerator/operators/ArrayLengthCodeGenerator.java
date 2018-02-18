package asmCodeGenerator.operators;

import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import asmCodeGenerator.runtime.RunTime;
import static asmCodeGenerator.ASMConstants.*;
import parseTree.ParseNode;

public class ArrayLengthCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		frag.add(Duplicate);
		frag.add(JumpFalse, RunTime.NULL_ARRAY_RUNTIME_ERROR);
		frag.add(PushI, ARRAY_LENGTH_OFFSET);
		frag.add(Add);
		frag.add(LoadI);

		return frag;
	}

}
