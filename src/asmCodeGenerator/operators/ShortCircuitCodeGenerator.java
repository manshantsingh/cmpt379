package asmCodeGenerator.operators;

import asmCodeGenerator.FullCodeGenerator;
import asmCodeGenerator.Labeller;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import parseTree.ParseNode;

public class ShortCircuitCodeGenerator implements FullCodeGenerator {
	private boolean isAnd;

	public ShortCircuitCodeGenerator(boolean isAnd) {
		this.isAnd = isAnd;
	}

	@Override
	public ASMCodeFragment generate(ParseNode node, ASMCodeFragment... args) {
		assert(args.length == 2);
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		Labeller labeller = new Labeller("short-circuit-" + (isAnd?"and":"or"));
		String end  = labeller.newLabel("end");

		frag.append(args[0]);
		frag.add(Duplicate);
		if(isAnd) {
			frag.add(JumpFalse, end);
		}
		else {
			frag.add(JumpTrue, end);
		}
		frag.add(Pop);
		frag.append(args[1]);
		frag.add(Label, end);

		return frag;
	}

}
