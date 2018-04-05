package asmCodeGenerator.operators;

import asmCodeGenerator.Macros;
import asmCodeGenerator.RecordsCodeGenerator;
import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import static asmCodeGenerator.runtime.RunTime.*;

import asmCodeGenerator.Labeller;

import static asmCodeGenerator.ASMConstants.*;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class StringSubstringCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_ADDRESS);

		Labeller labeller = new Labeller("string-substring");
		String inBound1 = labeller.newLabel("in-bounds1");
//		String length

		Macros.storeITo(frag, ARRAY_INDEXING_INDEX_SECOND);
		Macros.storeITo(frag, ARRAY_INDEXING_INDEX);
		Macros.storeITo(frag, ARRAY_INDEXING_ARRAY);

		Macros.loadIFrom(frag, ARRAY_INDEXING_ARRAY);
		frag.add(JumpFalse, NULL_STRING_RUNTIME_ERROR);

		Macros.loadIFrom(frag, ARRAY_INDEXING_INDEX);
		frag.add(JumpNeg, INDEX_OUT_OF_BOUND_STRING_RUNTIME_ERROR);

		Macros.loadIFrom(frag, ARRAY_INDEXING_INDEX_SECOND);
		frag.add(JumpNeg, INDEX_OUT_OF_BOUND_STRING_RUNTIME_ERROR);

		Macros.loadIFrom(frag, ARRAY_INDEXING_ARRAY);
		frag.add(PushI, STRING_LENGTH_OFFSET);
		frag.add(Add);
		frag.add(LoadI);			// [...  lengthOfArray]
		frag.add(Duplicate);
		Macros.loadIFrom(frag, ARRAY_INDEXING_INDEX);
		frag.add(Subtract);		// [... length  length-index1]
		frag.add(JumpPos, inBound1);
		frag.add(Jump, INDEX_OUT_OF_BOUND_STRING_RUNTIME_ERROR);
		frag.add(Label, inBound1);		//[...  length]
		Macros.loadIFrom(frag, ARRAY_INDEXING_INDEX_SECOND);
		frag.add(Subtract);		// [... length-index2]
		frag.add(JumpNeg, INDEX_OUT_OF_BOUND_STRING_RUNTIME_ERROR);

		// [...]
		Macros.loadIFrom(frag, ARRAY_INDEXING_INDEX_SECOND);
		Macros.loadIFrom(frag, ARRAY_INDEXING_INDEX);
		frag.add(Subtract);
		frag.add(Duplicate);
		frag.add(JumpNeg, SECOND_INDEX_SMALLER_STRING_RUNTIME_ERROR);
		
		// [...  length]
		frag.add(Duplicate);
		Macros.loadIFrom(frag, ARRAY_INDEXING_ARRAY);
		frag.add(PushI, STRING_HEADER_OFFSET);
		frag.add(Add);
		Macros.loadIFrom(frag, ARRAY_INDEXING_INDEX);
		frag.add(Add);
		frag.add(Exchange);		// [...  length  stringPtr+header+index1  length]
		RecordsCodeGenerator.createUnfilledStringRecord(frag);
		frag.add(PushI, STRING_HEADER_OFFSET);
		frag.add(Add);		// [...  nBytes  fromAddr  toAddr]
		frag.add(Call, RunTime.CLONE_N_BYTES);
		Macros.loadIFrom(frag, RECORD_CREATION_TEMPORARY);

		return frag;
	}

}
