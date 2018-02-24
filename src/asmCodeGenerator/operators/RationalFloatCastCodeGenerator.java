package asmCodeGenerator.operators;

import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import parseTree.ParseNode;

public class RationalFloatCastCodeGenerator implements SimpleCodeGenerator {
	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		frag.add(Exchange);
		frag.add(ConvertF);
		frag.add(Exchange);
		frag.add(ConvertF);
		frag.add(FDivide);

		return frag;
	}
}
