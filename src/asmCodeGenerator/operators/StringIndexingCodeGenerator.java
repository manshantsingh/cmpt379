package asmCodeGenerator.operators;

import asmCodeGenerator.Macros;
import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import static asmCodeGenerator.runtime.RunTime.*;

import asmCodeGenerator.Labeller;

import static asmCodeGenerator.ASMConstants.*;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import parseTree.ParseNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class StringIndexingCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);

		Macros.storeITo(frag, ARRAY_INDEXING_INDEX);
		Macros.storeITo(frag, ARRAY_INDEXING_ARRAY);

		Macros.loadIFrom(frag, ARRAY_INDEXING_ARRAY);
		frag.add(JumpFalse, NULL_STRING_RUNTIME_ERROR);

		Macros.loadIFrom(frag, ARRAY_INDEXING_INDEX);
		frag.add(JumpNeg, INDEX_OUT_OF_BOUND_STRING_RUNTIME_ERROR);

		Macros.loadIFrom(frag, ARRAY_INDEXING_INDEX);
		Macros.loadIFrom(frag, ARRAY_INDEXING_ARRAY);
		frag.add(PushI, STRING_LENGTH_OFFSET);
		frag.add(Add);
		frag.add(LoadI);
		frag.add(Subtract);
		Labeller labeller = new Labeller("string-indexing");
		String inBound = labeller.newLabel("in-bounds");
		frag.add(JumpNeg, inBound);
		frag.add(Jump, INDEX_OUT_OF_BOUND_STRING_RUNTIME_ERROR);
		frag.add(Label, inBound);

		Macros.loadIFrom(frag, ARRAY_INDEXING_ARRAY);
		frag.add(PushI, STRING_HEADER_OFFSET);
		frag.add(Add);
		Macros.loadIFrom(frag, ARRAY_INDEXING_INDEX);
		frag.add(PushI, PrimitiveType.CHARACTER.getSize());
		frag.add(Multiply);
		frag.add(Add);
		frag.add(LoadC);

		return frag;
	}

}
