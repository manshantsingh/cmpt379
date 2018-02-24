package asmCodeGenerator.operators;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import parseTree.ParseNode;

public class FloatRationalCastCodeGenerator implements SimpleCodeGenerator {
	private static final int SPECIAL_DIVISOR = 223092870;
	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		frag.add(PushI, SPECIAL_DIVISOR);
		frag.append(new ExpressOverFloatCodeGenerator(true).generate(node));

		return frag;
	}
}
