package asmCodeGenerator.operators;

import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Macros;
import parseTree.ParseNode;

public class ExpressOverRationalCodeGenerator implements SimpleCodeGenerator {
	private boolean rationalizeAfter;

	public ExpressOverRationalCodeGenerator(boolean rationalizeAfter) {
		this.rationalizeAfter = rationalizeAfter;
	}

	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);

		frag.add(Duplicate);
		frag.add(JumpFalse, RunTime.RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		frag.add(Duplicate);
		Macros.storeITo(frag, RunTime.RATIONAL_COMMON_DENOMINATOR_TEMPORARY);
		frag.add(Exchange);
		Macros.storeITo(frag, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);	// [... nume expressDenom]
		frag.add(Multiply);
		Macros.loadIFrom(frag, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);
		frag.add(Divide);

		if(rationalizeAfter) {
			Macros.loadIFrom(frag, RunTime.RATIONAL_COMMON_DENOMINATOR_TEMPORARY);
			frag.add(Call, RunTime.LOWEST_TERMS);
		}

		return frag;
	}

}
