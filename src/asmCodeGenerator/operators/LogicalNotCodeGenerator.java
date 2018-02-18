package asmCodeGenerator.operators;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import parseTree.ParseNode;

public class LogicalNotCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		Labeller labeller = new Labeller("logical-not");
		String trueLabel  = labeller.newLabel("true");
		String end = labeller.newLabel("end");

		frag.add(JumpTrue, trueLabel);
		frag.add(PushI, 1);
		frag.add(Jump, end);
		frag.add(Label, trueLabel);
		frag.add(PushI, 0);
		frag.add(Label, end);

		return frag;
	}
}
