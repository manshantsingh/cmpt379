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

public class MapReduceGenerator implements SimpleCodeGenerator {
	private boolean isReduce;
	
	public MapReduceGenerator(boolean isReduce) {
		this.isReduce = isReduce;
	}

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment code = new ASMCodeFragment(CodeType.GENERATES_ADDRESS);
		
		Type originalSubType = ((Array)node.child(0).getType()).getSubType();
		Type newSubType = ((LambdaType)node.child(1).getType()).getReturnType();
		int statusFlags;
		if(newSubType instanceof Array || newSubType == PrimitiveType.STRING) {
			// TODO: fix this for string in procedure call
			statusFlags = ARRAY_STATUS_WITH_REFERENCE_SUBTYPE;
		}
		else {
			statusFlags = ARRAY_STATUS_WITHOUT_REFERENCE_SUBTYPE;
		}
		
		Labeller labeller = new Labeller(isReduce?"reduce-operator": "map-operator");
		String mapTop = labeller.newLabel("map-top");
		String mapEnd = labeller.newLabel("map-end");
		String reduceCounterTop = labeller.newLabel("counter-top");
		String reduceCounterDontAdd = labeller.newLabel("counter-dont-add");
		String reduceCounterEnd = labeller.newLabel("counter-end");
		String finalTop = labeller.newLabel("final-top");
		String finalDontCopy = labeller.newLabel("final-dont-copy");
		String finalEnd = labeller.newLabel("final-end");

		// [... recordPtr lambdaPtr]
		Macros.storeITo(code, MAP_REDUCE_LAMBDA);
		code.add(Duplicate);
		code.add(JumpFalse, NULL_ARRAY_RUNTIME_ERROR);
		code.add(Duplicate);
		code.add(Duplicate);	// (special left alone one)
		code.add(PushI, ARRAY_HEADER_OFFSET);
		code.add(Add);
		code.add(Exchange);
		code.add(PushI, ARRAY_LENGTH_OFFSET);
		code.add(Add);
		code.add(LoadI);	// [...  firstElmPtr  length]
		code.add(Duplicate);
		RecordsCodeGenerator.createEmptyArrayRecord(code, statusFlags, newSubType.getSize());
		code.add(PushI, ARRAY_HEADER_OFFSET);
		code.add(Add);

		// [...  firstElmPtr  Length  newRecordPtr(firstElmPtr)]
		Macros.storeITo(code, MAP_REDUCE_ARRAY);
		code.add(Exchange);
		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
		code.add(Exchange);		// [...  length newRecordPtr firstElmPtr]
		Macros.storeITo(code, MAP_REDUCE_ARRAY);	// [...  length newRecordPtr]
		
		
		code.add(Label, mapTop);
		code.add(Exchange);
		code.add(Duplicate);
		code.add(JumpFalse, mapEnd);
		code.add(Exchange);		// [... length newRecordPtr]
		
		Macros.loadIFrom(code, RunTime.STACK_POINTER);
		code.add(PushI, originalSubType.getSize());
		code.add(Subtract);
		code.add(Duplicate);
		Macros.storeITo(code, RunTime.STACK_POINTER);
		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
		code.add(LoadI);
		ASMCodeGenerator.storeToAddress(code, originalSubType);
		
		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
		
		// [...  length newRecordPtr firstElmPtr]
		Macros.loadIFrom(code, MAP_REDUCE_LAMBDA);
		code.add(Duplicate);
		
		// [...  length newRecordPtr firstElmPtr lambda lambda]
		code.add(CallV);
		
		// [...  length newRecordPtr firstElmPtr lambda]
		Macros.storeITo(code, MAP_REDUCE_LAMBDA);
		code.add(PushI, originalSubType.getSize());
		code.add(Add);
		Macros.storeITo(code, MAP_REDUCE_ARRAY);
		code.add(Duplicate);	// [...  length newRecordPtr newRecordPtr]

		Macros.loadIFrom(code, RunTime.STACK_POINTER);
		ASMCodeGenerator.loadFromAddress(code, newSubType);
		Macros.loadIFrom(code, RunTime.STACK_POINTER);
		code.add(PushI, newSubType.getSize());
		code.add(Add);
		Macros.storeITo(code, RunTime.STACK_POINTER);
		
		ASMCodeGenerator.storeToAddress(code, newSubType);	// [...  length newRecordPtr]
		code.add(PushI, newSubType.getSize());
		code.add(Add);
		code.add(Exchange);
		code.add(PushI, 1);
		code.add(Subtract);
		code.add(Exchange);		// [... length newRecordPtr]
		code.add(Jump, mapTop);
		
		code.add(Label, mapEnd);	// [... (specialLeftAlone_originalRecordPtr) newRecordPtr length]
		code.add(Pop);
		code.add(Exchange);
		code.add(Duplicate);
		Macros.storeITo(code, MAP_REDUCE_ARRAY);	// [...  newRecordPtr+len*subSize  originalRecordPtr]
		
		code.add(PushI, ARRAY_LENGTH_OFFSET);
		code.add(Add);
		code.add(LoadI);
		code.add(PushI, newSubType.getSize());
		code.add(Multiply);
		code.add(Subtract);		// [... newRecordFirstElmPtr]
		
		if(!isReduce) {
			code.add(PushI, ARRAY_HEADER_OFFSET);
			code.add(Subtract);		// [... newRecordPtr]
			return code;
		}
		// [... newRecordFirstElmPtr]
		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
		code.add(PushI, ARRAY_LENGTH_OFFSET);
		code.add(Add);
		code.add(LoadI);	// [... newRecordFirstElmPtr numElm]
		
		code.add(PushI, 0);
		Macros.storeITo(code, MAP_REDUCE_LAMBDA);
		
		code.add(Label, reduceCounterTop);		// [... newRecordFirstElmPtr numElm]
		code.add(Duplicate);
		code.add(JumpFalse, reduceCounterEnd);
		
		code.add(Exchange);
		code.add(Duplicate);
		ASMCodeGenerator.loadFromAddress(code, newSubType);
		code.add(JumpFalse, reduceCounterDontAdd);
		
		Macros.loadIFrom(code, MAP_REDUCE_LAMBDA);
		code.add(PushI, 1);
		code.add(Add);
		Macros.storeITo(code, MAP_REDUCE_LAMBDA);
		
		code.add(Label, reduceCounterDontAdd);		// [... numElm newRecordElmPtr]
		code.add(PushI, newSubType.getSize());
		code.add(Add);
		code.add(Exchange);
		code.add(PushI, 1);
		code.add(Subtract);		// [... newRecordElmPtr numElm]
		code.add(Jump, reduceCounterTop);

		code.add(Label, reduceCounterEnd);		// [... newRecordFirstElmPtr numElm]
		code.add(Pop);
		
		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
		code.add(PushI, ARRAY_LENGTH_OFFSET);
		code.add(Add);
		code.add(LoadI);
		code.add(PushI, newSubType.getSize());
		code.add(Multiply);
		code.add(Subtract);		// [... newRecordFirstElmPtr]
		
		code.add(Duplicate);
		code.add(PushI, ARRAY_HEADER_OFFSET);		// uncomment for release array
		code.add(Subtract);
		code.add(Exchange);

		
		if(originalSubType instanceof Array || originalSubType == PrimitiveType.STRING) {
			// TODO: fix this for string in procedure call
			statusFlags = ARRAY_STATUS_WITH_REFERENCE_SUBTYPE;
		}
		else {
			statusFlags = ARRAY_STATUS_WITHOUT_REFERENCE_SUBTYPE;
		}
		Macros.loadIFrom(code, MAP_REDUCE_LAMBDA);
		RecordsCodeGenerator.createEmptyArrayRecord(code, statusFlags, originalSubType.getSize());
		// [... boolRecordFirstPtr finalNewRecordPtr]
		
		code.add(PushI, ARRAY_HEADER_OFFSET);
		code.add(Add);		// [boolRecordFirstPtr finalNewRecordFirstPtr]
		
		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
		code.add(PushI, ARRAY_HEADER_OFFSET);
		code.add(Add);
		code.add(Exchange);
		Macros.storeITo(code, MAP_REDUCE_ARRAY);
		code.add(Exchange);		// [... originalArrayFirstPtr boolRecordFirstPtr]
		
		
		
		code.add(Label, finalTop);		// [... originalArrayFirstPtr boolRecordFirstPtr]
		Macros.loadIFrom(code, MAP_REDUCE_LAMBDA);
		code.add(JumpNeg, finalEnd);

		// [... originalArrayFirstPtr boolRecordFirstPtr]
		code.add(Duplicate);
		ASMCodeGenerator.loadFromAddress(code, newSubType);
		code.add(JumpFalse, finalDontCopy);

		code.add(Exchange);
		code.add(Duplicate);
		ASMCodeGenerator.loadFromAddress(code, originalSubType);
		Macros.loadIFrom(code, MAP_REDUCE_ARRAY);
		code.add(Duplicate);
		code.add(PushI, originalSubType.getSize());
		code.add(Add);
		Macros.storeITo(code, MAP_REDUCE_ARRAY);
		code.add(Exchange);
		ASMCodeGenerator.storeToAddress(code, originalSubType);		// [...  bPtr  oaPtr]
		code.add(Exchange);
		Macros.loadIFrom(code, MAP_REDUCE_LAMBDA);
		code.add(PushI, 1);
		code.add(Subtract);
		Macros.storeITo(code, MAP_REDUCE_LAMBDA);
		
		code.add(Label, finalDontCopy);		// [... originalArrayFirstPtr boolRecordFirstPtr]
		code.add(PushI, newSubType.getSize());
		code.add(Add);
		code.add(Exchange);
		code.add(PushI, originalSubType.getSize());
		code.add(Add);
		code.add(Exchange);
		code.add(Jump, finalTop);
		
		code.add(Label, finalEnd);		// [...  originalArrayFirstPtr boolRecordFirstPtr  (should be -1)]
		code.add(Pop);
		code.add(Pop);
		
		code.add(ASMOpcode.Call, RunTime.RECORD_RELEASE);
		Macros.loadIFrom(code, RECORD_CREATION_TEMPORARY);
		
		return code;
	}

}
