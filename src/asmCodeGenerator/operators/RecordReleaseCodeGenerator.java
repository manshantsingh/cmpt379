package asmCodeGenerator.operators;

import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class RecordReleaseCodeGenerator implements SimpleCodeGenerator {
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VOID);
		frag.add(ASMOpcode.Call, RunTime.RECORD_RELEASE);
		return frag;
	}
}
