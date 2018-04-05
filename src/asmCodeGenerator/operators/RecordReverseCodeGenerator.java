package asmCodeGenerator.operators;

import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import static asmCodeGenerator.runtime.MemoryManager.MEM_MANAGER_ALLOCATE;
import static asmCodeGenerator.runtime.RunTime.*;

import asmCodeGenerator.ASMCodeGenerator;
import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;
import asmCodeGenerator.RecordsCodeGenerator;

import static asmCodeGenerator.ASMConstants.*;
import parseTree.ParseNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class RecordReverseCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		Labeller labeller = new Labeller("reverse-array");
		String nullArray = labeller.newLabel("null-array");
		String end = labeller.newLabel("end");
		
		String loopTop = labeller.newLabel("loop-top");
		String loopMiddle = labeller.newLabel("loop-middle");
		String loopEnd = labeller.newLabel("loop-end");
		
		boolean isArray;
		Type subElementType;
		if(node.child(0).getType() == PrimitiveType.STRING) {
			subElementType = PrimitiveType.CHARACTER;
			isArray=false;
		}
		else{
			subElementType = ((Array) node.child(0).getType()).getSubType();
			isArray=true;
		}


		// TODO copy pasted from ArrayLengthGenerator
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		frag.add(Duplicate);	// [... recordAddr recordAddr]
		frag.add(JumpFalse, nullArray);
		frag.add(Duplicate);
		if(isArray) {
			frag.add(PushI, ARRAY_LENGTH_OFFSET);
		}
		else {
			frag.add(PushI, STRING_LENGTH_OFFSET);
		}
		frag.add(Add);
		frag.add(LoadI);	// [... recordAddr nElems]
		frag.add(PushI, subElementType.getSize());	// [... recordAddr nElms subTypeByteSize]
		frag.add(Multiply);
		if(isArray) {
			frag.add(PushI, ARRAY_HEADER_OFFSET);
		}
		else {
			frag.add(PushI, STRING_HEADER_OFFSET);
		}
		frag.add(Add);
		frag.add(Duplicate);	// [... recordAddr totalByteSize totalByteSize]
		frag.add(Call, MEM_MANAGER_ALLOCATE);
		Macros.storeITo(frag, RECORD_CREATION_TEMPORARY);	// [... recordAddr totalByteSize]
		frag.add(Exchange);
		Macros.loadIFrom(frag, RECORD_CREATION_TEMPORARY);

		// [... totalByteSize recordAddr newRecordAddr]
		frag.add(Call, RunTime.CLONE_N_BYTES);
		Macros.loadIFrom(frag, RECORD_CREATION_TEMPORARY);
		frag.add(Duplicate);
		
		
		// do the non-clone stuff
		if(isArray) {
			frag.add(PushI, ARRAY_HEADER_OFFSET);
		}
		else {
			frag.add(PushI, STRING_HEADER_OFFSET);
		}
		frag.add(Add);
		frag.add(Duplicate);
		Macros.loadIFrom(frag, RECORD_CREATION_TEMPORARY);
		if(isArray) {
			frag.add(PushI, ARRAY_LENGTH_OFFSET);
		}
		else {
			frag.add(PushI, STRING_LENGTH_OFFSET);
		}
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushI, 1);
		frag.add(Subtract);
		frag.add(PushI, subElementType.getSize());
		frag.add(Multiply);
		frag.add(Add);

		// extra: [...  firstElmPtr  lastElmPtr]
		Macros.storeITo(frag, RECORD_REVERSE_BOTTOM);
		Macros.storeITo(frag, RECORD_REVERSE_TOP);

		frag.add(Label, loopTop);
		Macros.loadIFrom(frag, RECORD_REVERSE_TOP);
		frag.add(Duplicate);
		Macros.loadIFrom(frag, RECORD_REVERSE_BOTTOM);
		frag.add(Subtract);
		frag.add(JumpNeg, loopMiddle);
		frag.add(Jump, loopEnd);

		frag.add(Label, loopMiddle);	// [...  topPtr]
		frag.add(Duplicate);
		Macros.loadIFrom(frag, RECORD_REVERSE_BOTTOM);
		ASMCodeGenerator.loadFromAddress(frag, subElementType);
		Macros.loadIFrom(frag, RECORD_REVERSE_BOTTOM);
		Macros.loadIFrom(frag, RECORD_REVERSE_TOP);
		ASMCodeGenerator.loadFromAddress(frag, subElementType);
		ASMCodeGenerator.storeToAddress(frag, subElementType);
		ASMCodeGenerator.storeToAddress(frag, subElementType);
		frag.add(PushI, subElementType.getSize());
		frag.add(Add);
		Macros.storeITo(frag, RECORD_REVERSE_TOP);
		Macros.loadIFrom(frag, RECORD_REVERSE_BOTTOM);
		frag.add(PushI, subElementType.getSize());
		frag.add(Subtract);
		Macros.storeITo(frag, RECORD_REVERSE_BOTTOM);
		frag.add(Jump, loopTop);
		
		frag.add(Label, loopEnd);
		// do cleanup here
		frag.add(Pop);
		
		
		frag.add(Jump, end);

		frag.add(Label, nullArray);
		frag.add(Pop);
		frag.add(PushI, 0);
		frag.add(Label, end);

		return frag;
	}

}
