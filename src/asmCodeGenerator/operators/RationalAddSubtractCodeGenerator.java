package asmCodeGenerator.operators;

import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import static asmCodeGenerator.runtime.RunTime.*;

import asmCodeGenerator.Macros;
import parseTree.ParseNode;

public class RationalAddSubtractCodeGenerator implements SimpleCodeGenerator {
	boolean isAdd;

	public RationalAddSubtractCodeGenerator(boolean isAdd) {
		this.isAdd = isAdd;
	}

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		commonDenominator(frag);
		if(isAdd) {
			frag.add(Add);
		}
		else {
			frag.add(Subtract);
		}
		Macros.loadIFrom(frag, RATIONAL_COMMON_DENOMINATOR_TEMPORARY);	// [... nume denom]

		frag.add(Call, LOWEST_TERMS);
		return frag;
	}

	public static void commonDenominator(ASMCodeFragment frag) {
		Macros.storeITo(frag, RATIONAL_DENOMINATOR_TEMPORARY);
		Macros.storeITo(frag, RATIONAL_NUMERATOR_TEMPORARY);	// [... a b]
		frag.add(Duplicate);
		Macros.loadIFrom(frag, RATIONAL_DENOMINATOR_TEMPORARY);
		frag.add(Multiply);
		Macros.storeITo(frag, RATIONAL_COMMON_DENOMINATOR_TEMPORARY);	// [... a b]
		Macros.loadIFrom(frag, RATIONAL_NUMERATOR_TEMPORARY);
		frag.add(Multiply);
		frag.add(Exchange);		// [...  b*p  a]
		Macros.loadIFrom(frag, RATIONAL_DENOMINATOR_TEMPORARY);
		frag.add(Multiply);		// [...  b*p  a*q]
		frag.add(Exchange);
	}

}
