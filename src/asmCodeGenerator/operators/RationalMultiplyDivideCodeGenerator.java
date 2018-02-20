package asmCodeGenerator.operators;

import static asmCodeGenerator.codeStorage.ASMOpcode.Call;
import static asmCodeGenerator.runtime.RunTime.LOWEST_TERMS;

import asmCodeGenerator.Macros;
import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import static asmCodeGenerator.runtime.RunTime.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import parseTree.ParseNode;

public class RationalMultiplyDivideCodeGenerator implements SimpleCodeGenerator {
	boolean isMultiply;

	public RationalMultiplyDivideCodeGenerator(boolean isMultiply) {
		this.isMultiply = isMultiply;
	}

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		if(!isMultiply) {
			frag.add(Exchange);
		}
		Macros.storeITo(frag, RATIONAL_DENOMINATOR_TEMPORARY);
		Macros.storeITo(frag, RATIONAL_NUMERATOR_TEMPORARY);
		frag.add(Exchange);		// [... b a]
		Macros.loadIFrom(frag, RATIONAL_NUMERATOR_TEMPORARY);
		frag.add(Multiply);		// [... b a*p]
		frag.add(Exchange);
		Macros.loadIFrom(frag, RATIONAL_DENOMINATOR_TEMPORARY);
		frag.add(Multiply);		// [...  a*p  b*q]

		frag.add(Call, LOWEST_TERMS);
		return frag;
	}

}
