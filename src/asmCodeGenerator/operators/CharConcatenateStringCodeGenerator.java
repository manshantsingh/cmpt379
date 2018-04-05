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

public class CharConcatenateStringCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_ADDRESS);

		Macros.storeITo(frag, ARRAY_INDEXING_ARRAY);
		Macros.storeITo(frag, ARRAY_INDEXING_INDEX);

		Macros.loadIFrom(frag, ARRAY_INDEXING_ARRAY);
		frag.add(JumpFalse, NULL_STRING_RUNTIME_ERROR);

		Macros.loadIFrom(frag, ARRAY_INDEXING_ARRAY);
		frag.add(PushI, STRING_LENGTH_OFFSET);
		frag.add(Add);
		frag.add(LoadI);			// [...  lengthOfArray1]
		frag.add(Duplicate);

		frag.add(PushI, 1);			// [...  lengthOfArray1  lengthOfArray1  lengthOfArray2]

		frag.add(Add);
		RecordsCodeGenerator.createUnfilledStringRecord(frag);	// [... nBytesArray1  newRecordPos]
		frag.add(PushI, STRING_HEADER_OFFSET);
		frag.add(Add);
		frag.add(Duplicate);
		Macros.loadIFrom(frag, ARRAY_INDEXING_INDEX);
		frag.add(StoreC);
		frag.add(PushI, 1);
		frag.add(Add);
		
		Macros.loadIFrom(frag, ARRAY_INDEXING_ARRAY);
		frag.add(PushI, STRING_HEADER_OFFSET);
		frag.add(Add);
		frag.add(Exchange);		// [...  nBytesArray1  fromAddr  toAddr]
		frag.add(Call, RunTime.CLONE_N_BYTES);
		
		Macros.loadIFrom(frag, RECORD_CREATION_TEMPORARY);

		return frag;
	}

}
