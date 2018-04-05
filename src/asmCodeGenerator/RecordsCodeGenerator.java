package asmCodeGenerator;


import asmCodeGenerator.codeStorage.ASMCodeFragment;
import parseTree.nodeTypes.ArrayNode;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

import static asmCodeGenerator.runtime.MemoryManager.*;
import static asmCodeGenerator.runtime.RunTime.*;
import static asmCodeGenerator.ASMConstants.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

public class RecordsCodeGenerator {

	// [... nElms] => [... recordPtr]
	public static void createEmptyArrayRecord(ASMCodeFragment code, int statusFlags, int subTypeSize) {
		code.add(Duplicate);
		code.add(JumpNeg, NEGATIVE_LENGTH_ARRAY_RUNTIME_ERROR);

		code.add(Duplicate);
		code.add(PushI, subTypeSize);
		code.add(Multiply);
		code.add(Duplicate);
		code.add(PushI, ARRAY_HEADER_OFFSET);
		code.add(Add);

		// [... nElms valSize recordSize]
		createRecord(code, ARRAY_TYPE_ID, statusFlags);

		Macros.loadIFrom(code, RECORD_CREATION_TEMPORARY);
		code.add(PushI, ARRAY_HEADER_OFFSET);
		code.add(Add);
		code.add(Exchange); // [... nElms firstRecordPtr bytesForElements]
		code.add(Call, CLEAR_N_BYTES);

		// [... nElms]
		writeIBaseOffset(code, RECORD_CREATION_TEMPORARY, ARRAY_SUBELEMENT_SIZE_OFFSET, subTypeSize);
		writeIPtrOffset(code, RECORD_CREATION_TEMPORARY, ARRAY_LENGTH_OFFSET);

		Macros.loadIFrom(code, RECORD_CREATION_TEMPORARY);
	}

	public static void createPopulatedArrayRecord(ASMCodeFragment code, ASMCodeFragment[] frags, int statusFlags, Type subType) {
		int recordSize = ARRAY_HEADER_OFFSET + subType.getSize() * frags.length;

		code.add(PushI, recordSize);
		createRecord(code, ARRAY_TYPE_ID, statusFlags);

		Macros.loadIFrom(code, RECORD_CREATION_TEMPORARY);

		int offset = ARRAY_HEADER_OFFSET;
		for(ASMCodeFragment frag: frags) {
			code.add(Duplicate);
			code.add(PushI, offset);
			code.add(Add);
			code.append(frag);
			ASMCodeGenerator.storeToAddress(code, subType);

			offset += subType.getSize();
		}
		code.add(Duplicate);
		Macros.storeITo(code, RECORD_CREATION_TEMPORARY);
		writeIBaseOffset(code, RECORD_CREATION_TEMPORARY, ARRAY_SUBELEMENT_SIZE_OFFSET, subType.getSize());
		writeIBaseOffset(code, RECORD_CREATION_TEMPORARY, ARRAY_LENGTH_OFFSET, frags.length);
	}

	// [...] => [... recordPtr]
	public static void createStringRecord(ASMCodeFragment code, String str) {
		int recordSize = STRING_HEADER_OFFSET +
				PrimitiveType.CHARACTER.getSize() * (str.length() + 1);

		code.add(PushI, recordSize);
		createRecord(code, STRING_TYPE_ID, STRING_STATUS);

		Macros.loadIFrom(code, RECORD_CREATION_TEMPORARY);
		code.add(Duplicate);
		code.add(PushI, STRING_LENGTH_OFFSET);
		code.add(Add);
		code.add(PushI, str.length());
		code.add(StoreI);

		int offset = STRING_HEADER_OFFSET;
		for(char c: str.toCharArray()) {
			addChartoString(code, offset, (int)c);
			offset += PrimitiveType.CHARACTER.getSize();
		}
		addChartoString(code, offset, 0);
	}

	// [... length] => [... recordPosition]
	public static void createUnfilledStringRecord(ASMCodeFragment code) {
		code.add(Duplicate);
		Macros.storeITo(code, RATIONAL_DENOMINATOR_TEMPORARY);
		code.add(PushI, PrimitiveType.CHARACTER.getSize());
		code.add(Multiply);
		code.add(PushI, STRING_HEADER_OFFSET);
		code.add(Add);
		createRecord(code, STRING_TYPE_ID, STRING_STATUS);
		
		Macros.loadIFrom(code, RECORD_CREATION_TEMPORARY);
		code.add(Duplicate);
		code.add(PushI, STRING_LENGTH_OFFSET);
		code.add(Add);
		Macros.loadIFrom(code, RATIONAL_DENOMINATOR_TEMPORARY);		// [...  recordPos  recordPos+lenOffset  strLen]
		code.add(StoreI);
	}

	// [... size] => [...]
	private static void createRecord(ASMCodeFragment code, int typeCode, int statusFlags) {
		code.add(Call, MEM_MANAGER_ALLOCATE);
		Macros.storeITo(code, RECORD_CREATION_TEMPORARY);

		writeIBaseOffset(code, RECORD_CREATION_TEMPORARY, RECORD_TYPE_ID_OFFSET, typeCode);
		writeIBaseOffset(code, RECORD_CREATION_TEMPORARY, RECORD_STATUS_OFFSET, statusFlags);
	}

	private static void addChartoString(ASMCodeFragment code, int offset, int c) {
		code.add(Duplicate);
		code.add(PushI, offset);
		code.add(Add);
		code.add(PushI, c);
		code.add(StoreC);
	}

	private static void writeIBaseOffset(ASMCodeFragment code, String location, int offset, int val) {
		Macros.loadIFrom(code, location);
		code.add(PushI, offset);
		code.add(Add);
		code.add(PushI, val);
		code.add(StoreI);
	}

	private static void writeIPtrOffset(ASMCodeFragment code, String location, int offset) {
		Macros.loadIFrom(code, location);
		Macros.writeIOffset(code, offset);
	}
}
