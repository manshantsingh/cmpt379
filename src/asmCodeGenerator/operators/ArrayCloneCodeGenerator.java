package asmCodeGenerator.operators;

import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import static asmCodeGenerator.runtime.MemoryManager.MEM_MANAGER_ALLOCATE;
import static asmCodeGenerator.runtime.RunTime.RECORD_CREATION_TEMPORARY;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;
import asmCodeGenerator.RecordsCodeGenerator;

import static asmCodeGenerator.ASMConstants.*;
import parseTree.ParseNode;

public class ArrayCloneCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		Labeller labeller = new Labeller("clone-array");
		String nullArray = labeller.newLabel("null-array");
		String end = labeller.newLabel("end");


		// TODO copy pasted from ArrayLengthGenerator
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		frag.add(Duplicate);	// [... recordAddr recordAddr]
		frag.add(JumpFalse, nullArray);
		frag.add(Duplicate);
		frag.add(Duplicate);
		frag.add(PushI, ARRAY_LENGTH_OFFSET);
		frag.add(Add);
		frag.add(LoadI);	// [... recordAddr recordAddr nElems]
		frag.add(Exchange);
		frag.add(PushI, ARRAY_SUBELEMENT_SIZE_OFFSET);
		frag.add(Add);
		frag.add(LoadI);	// [... recordAddr nElms subTypeByteSize]
		frag.add(Multiply);
		frag.add(PushI, ARRAY_HEADER_OFFSET);
		frag.add(Add);
		frag.add(Duplicate);	// [... recordAddr totalByteSize totalByteSize]
		frag.add(Call, MEM_MANAGER_ALLOCATE);
		Macros.storeITo(frag, RECORD_CREATION_TEMPORARY);	// [... recordAddr totalByteSize]
		frag.add(Exchange);
		Macros.loadIFrom(frag, RECORD_CREATION_TEMPORARY);

		// [... totalByteSize recordAddr newRecordAddr]
		frag.add(Call, RunTime.CLONE_N_BYTES);
		Macros.loadIFrom(frag, RECORD_CREATION_TEMPORARY);
		frag.add(Jump, end);

		frag.add(Label, nullArray);
		frag.add(Pop);
		frag.add(PushI, 0);
		frag.add(Label, end);

		return frag;
	}

}
