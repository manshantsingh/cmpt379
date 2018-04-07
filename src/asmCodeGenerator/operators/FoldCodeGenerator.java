package asmCodeGenerator.operators;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;

import static asmCodeGenerator.runtime.RunTime.*;

import asmCodeGenerator.ASMCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.FullCodeGenerator;
import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;
import asmCodeGenerator.RecordsCodeGenerator;
import asmCodeGenerator.SimpleCodeGenerator;
import parseTree.ParseNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

import static asmCodeGenerator.ASMConstants.*;

public class FoldCodeGenerator implements SimpleCodeGenerator {
	
	boolean hasStrartingValue;
	
	public FoldCodeGenerator(boolean hasStrartingValue) {
		this.hasStrartingValue = hasStrartingValue;
	}

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment code = new ASMCodeFragment(CodeType.GENERATES_ADDRESS);
		
		Type baseType;
		Type subType = ((Array)node.child(0).getType()).getSubType();
		if(hasStrartingValue) {
			baseType = node.child(1).getType();
		}
		else {
			baseType = subType;
		}
		
		Labeller labeller = new Labeller("fold");
		String top = labeller.newLabel("top");
		String end = labeller.newLabel("end");

		// [... recordPtr (optional_base_value) lambdaPtr]
		Macros.storeITo(code, MAP_REDUCE_LAMBDA);
		
		if(hasStrartingValue) {
			if(baseType == PrimitiveType.RATIONAL) {
				Macros.storeITo(code, RATIONAL_DENOMINATOR_TEMPORARY);
				code.add(Exchange);
				Macros.loadIFrom(code, RATIONAL_DENOMINATOR_TEMPORARY);
				code.add(Exchange);
			}
			else {
				code.add(Exchange);
			}
		}
		
		// [... (optional_base_value) recordPtr]
		code.add(Duplicate);
		code.add(JumpFalse, NULL_ARRAY_RUNTIME_ERROR);
		code.add(Duplicate);
		code.add(PushI, ARRAY_LENGTH_OFFSET);
		code.add(Add);
		code.add(LoadI);
		
		if(!hasStrartingValue) {
			code.add(Duplicate);
			code.add(JumpFalse, FOLD_ARRAY_LENGTH_ZERO);
		}
		
		// [... (optional_base_value) recordPtr numElms]
		code.add(Exchange);
		code.add(PushI, ARRAY_HEADER_OFFSET);
		code.add(Add);
		Macros.storeITo(code, MAP_REDUCE_ARRAY);
		
		// [... (optional_base_value) numElms]
		if(!hasStrartingValue) {
			code.add(PushI, 1);
			code.add(Subtract);
			Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
			code.add(Duplicate);
			code.add(PushI, subType.getSize());
			code.add(Add);
			Macros.storeITo(code, MAP_REDUCE_ARRAY);
			ASMCodeGenerator.loadFromAddress(code, subType);

			if(subType == PrimitiveType.RATIONAL) {
				Macros.storeITo(code, RATIONAL_DENOMINATOR_TEMPORARY);
				code.add(Exchange);
				Macros.loadIFrom(code, RATIONAL_DENOMINATOR_TEMPORARY);
				code.add(Exchange);
			}
			else {
				code.add(Exchange);
			}
		}

		
		code.add(Label, top);		// [...  base_value(s)  numElms]
		code.add(PushI, 1);
		code.add(Subtract);
		code.add(Duplicate);
		code.add(JumpNeg, end);
		
		// [...  base_value(s)  numElms]
		if(baseType == PrimitiveType.RATIONAL) {
			code.add(Exchange);
			Macros.storeITo(code, RATIONAL_DENOMINATOR_TEMPORARY);
			code.add(Exchange);
			Macros.loadIFrom(code, RATIONAL_DENOMINATOR_TEMPORARY);
		}
		else {
			code.add(Exchange);
		}
		// [... numElms base_values(s)
		
		if(baseType == PrimitiveType.RATIONAL) {
			Macros.storeITo(code, RATIONAL_DENOMINATOR_TEMPORARY);
		}
		Macros.storeITo(code, RATIONAL_NUMERATOR_TEMPORARY);	// [... numElms]

		Macros.loadIFrom(code, RunTime.STACK_POINTER);
		code.add(PushI, baseType.getSize());
		code.add(Subtract);
		code.add(Duplicate);
		Macros.storeITo(code, RunTime.STACK_POINTER);
		Macros.loadIFrom(code, RATIONAL_NUMERATOR_TEMPORARY);
		if(baseType == PrimitiveType.RATIONAL) {
			Macros.loadIFrom(code, RATIONAL_DENOMINATOR_TEMPORARY);
		}
		ASMCodeGenerator.storeToAddress(code, baseType);

		Macros.loadIFrom(code, RunTime.STACK_POINTER);
		code.add(PushI, subType.getSize());
		code.add(Subtract);
		code.add(Duplicate);
		Macros.storeITo(code, RunTime.STACK_POINTER);
		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
		ASMCodeGenerator.loadFromAddress(code, subType);
		ASMCodeGenerator.storeToAddress(code, subType);
		
		// [... numElms]
		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
		code.add(PushI, subType.getSize());
		code.add(Add);
		
		Macros.loadIFrom(code, MAP_REDUCE_LAMBDA);
		code.add(Duplicate);
		
		// [... numElms arrayElemPtr lambda lambda]
		code.add(CallV);

		Macros.storeITo(code, MAP_REDUCE_LAMBDA);
		Macros.storeITo(code, MAP_REDUCE_ARRAY);	// [... numElms]
		
		Macros.loadIFrom(code, RunTime.STACK_POINTER);
		ASMCodeGenerator.loadFromAddress(code, baseType);
		Macros.loadIFrom(code, RunTime.STACK_POINTER);
		code.add(PushI, baseType.getSize());
		code.add(Add);
		Macros.storeITo(code, RunTime.STACK_POINTER);	// [... numElms base_value(s)]
		
		if(baseType == PrimitiveType.RATIONAL) {
			Macros.storeITo(code, RATIONAL_DENOMINATOR_TEMPORARY);
			code.add(Exchange);
			Macros.loadIFrom(code, RATIONAL_DENOMINATOR_TEMPORARY);
			code.add(Exchange);
		}
		else {
			code.add(Exchange);
		}
		// [... base_value(s) numElms]
		code.add(Jump, top);
		
		code.add(Label, end);	// [...  base_value(s)  numElms]
		code.add(Pop);
		return code;
		
//		Macros.storeITo(code, ZIP_ADDITIONAL_ARRAY);
//		code.add(Duplicate);
//		Macros.storeITo(code, MAP_REDUCE_ARRAY);
//
//		code.add(JumpFalse, NULL_ARRAY_RUNTIME_ERROR);
//		Macros.loadIFrom(code, ZIP_ADDITIONAL_ARRAY);
//		code.add(Duplicate);
//		code.add(JumpFalse, NULL_ARRAY_RUNTIME_ERROR);
//		code.add(PushI, ARRAY_LENGTH_OFFSET);
//		code.add(Add);
//		code.add(LoadI);	// [...  lengthArr2]
//		code.add(Duplicate);
//		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
//		code.add(PushI, ARRAY_LENGTH_OFFSET);
//		code.add(Add);
//		code.add(LoadI);
//		code.add(Subtract);
//		code.add(JumpTrue, ZIP_ARRAY_LENGTHS_NOT_SAME);
//		
//		// [... numElms]
//		code.add(Duplicate);
//		RecordsCodeGenerator.createEmptyArrayRecord(code, statusFlags, newSubType.getSize());
//		// [...  numElms  newRecordPtr]
//		code.add(Exchange);
//		Macros.loadIFrom(code, RECORD_CREATION_TEMPORARY);
//		// [...  (special_newRecordPtr)  numElms  newRecordPtr]
//		// extra: [...  numElms  newRecordPtr]
//		
//		code.add(PushI, ARRAY_HEADER_OFFSET);
//		code.add(Add);
//		
//		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
//		code.add(PushI, ARRAY_HEADER_OFFSET);
//		code.add(Add);
//		Macros.storeITo(code, MAP_REDUCE_ARRAY);
//		
//		Macros.loadIFrom(code, ZIP_ADDITIONAL_ARRAY);
//		code.add(PushI, ARRAY_HEADER_OFFSET);
//		code.add(Add);
//		Macros.storeITo(code, ZIP_ADDITIONAL_ARRAY);
//		
//		code.add(Label, top);	// [...  numElms  newRecordFirstElmPtr]
//		code.add(Exchange);
//		code.add(PushI, 1);
//		code.add(Subtract);
//		code.add(Duplicate);
//		code.add(JumpNeg, end);
//		code.add(Exchange);		// [...  numElms  newRecordFirstElmPtr]
//
//		Macros.loadIFrom(code, RunTime.STACK_POINTER);
//		code.add(PushI, subType1.getSize());
//		code.add(Subtract);
//		code.add(Duplicate);
//		Macros.storeITo(code, RunTime.STACK_POINTER);
//		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
//		ASMCodeGenerator.loadFromAddress(code, subType1);
//		ASMCodeGenerator.storeToAddress(code, subType1);
//		
//		Macros.loadIFrom(code, RunTime.STACK_POINTER);
//		code.add(PushI, subType2.getSize());
//		code.add(Subtract);
//		code.add(Duplicate);
//		Macros.storeITo(code, RunTime.STACK_POINTER);
//		Macros.loadIFrom(code, ZIP_ADDITIONAL_ARRAY);
//		ASMCodeGenerator.loadFromAddress(code, subType2);
//		ASMCodeGenerator.storeToAddress(code, subType2);
//		
//		// [... numElm newRecordElmPtr]
//
//		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
//		code.add(PushI, subType1.getSize());
//		code.add(Add);
//		
//		Macros.loadIFrom(code, ZIP_ADDITIONAL_ARRAY);
//		code.add(PushI, subType2.getSize());
//		code.add(Add);
//		
//		Macros.loadIFrom(code, MAP_REDUCE_LAMBDA);
//		code.add(Duplicate);
//		
//		// [... numElm newRecordElmPtr arr1ElmPtr arr2ElmPtr lambda lambda]
//		code.add(CallV);
//		
//		Macros.storeITo(code, MAP_REDUCE_LAMBDA);
//		Macros.storeITo(code, ZIP_ADDITIONAL_ARRAY);
//		Macros.storeITo(code, MAP_REDUCE_ARRAY);
//		
//		code.add(Duplicate);	// [... numElm newRecordElmPtr newRecordElmPtr]
//		
//		Macros.loadIFrom(code, RunTime.STACK_POINTER);
//		ASMCodeGenerator.loadFromAddress(code, newSubType);
//		Macros.loadIFrom(code, RunTime.STACK_POINTER);
//		code.add(PushI, newSubType.getSize());
//		code.add(Add);
//		Macros.storeITo(code, RunTime.STACK_POINTER);
//		
//		// [... numElm newRecordElmPtr newRecordElmPtr functionReturnedObject]
//		ASMCodeGenerator.storeToAddress(code, newSubType);
//		code.add(PushI, newSubType.getSize());
//		code.add(Add);		// [... numElm newRecordElmPtr]
//		code.add(Jump, top);
//		
//		code.add(Label, end);	// [... newRecordElmPtr (should_be_-1)]
//		code.add(Pop);
//		code.add(Pop);
//		return code;
	}

}
