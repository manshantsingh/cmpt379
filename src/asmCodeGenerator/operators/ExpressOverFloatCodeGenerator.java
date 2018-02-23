package asmCodeGenerator.operators;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Macros;
import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class ExpressOverFloatCodeGenerator implements SimpleCodeGenerator {
	private boolean rationalizeAfter;

	public ExpressOverFloatCodeGenerator(boolean rationalizeAfter) {
		this.rationalizeAfter = rationalizeAfter;
	}
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);

		frag.add(Duplicate);
		frag.add(JumpFalse, RunTime.RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR);

		if(rationalizeAfter) {
			frag.add(Duplicate);
			Macros.storeITo(frag, RunTime.RATIONAL_COMMON_DENOMINATOR_TEMPORARY);
		}

		frag.add(ConvertF);
		frag.add(FMultiply);
		frag.add(ConvertI);

		if(rationalizeAfter) {
			Macros.loadIFrom(frag, RunTime.RATIONAL_COMMON_DENOMINATOR_TEMPORARY);
			frag.add(Call, RunTime.LOWEST_TERMS);
		}

		return frag;
	}

}
