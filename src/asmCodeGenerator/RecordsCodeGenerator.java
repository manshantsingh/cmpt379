package asmCodeGenerator;


import asmCodeGenerator.codeStorage.ASMCodeFragment;
import static asmCodeGenerator.runtime.MemoryManager.*;
import static asmCodeGenerator.runtime.RunTime.*;
import static asmCodeGenerator.ASMConstants.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

public class RecordsCodeGenerator {

	public static void createEmptyArrayRecord(ASMCodeFragment code, int statusFlags, int subTypeSize) {
		code.add(Duplicate);
		code.add(JumpNeg, NEGATIVE_LENGTH_ARRAY_RUNTIME_ERROR);

		code.add(Duplicate);
		code.add(PushI, subTypeSize);
		code.add(Multiply);
		code.add(Duplicate);
		code.add(PushI, ARRAY_HEADER_OFFSET);
		code.add(Add);

		createRecord(code, ARRAY_TYPE_ID, statusFlags);

		Macros.loadIFrom(code, RECORD_CREATION_TEMPORARY);
		code.add(PushI, ARRAY_HEADER_OFFSET);
		code.add(Add);
		code.add(Exchange);
		code.add(Call, CLEAR_N_BYTES);

		writeIBaseOffset(code, RECORD_CREATION_TEMPORARY, ARRAY_SUBELEMENT_SIZE_OFFSET, subTypeSize);
		writeIPtrOffset(code, RECORD_CREATION_TEMPORARY, ARRAY_LENGTH_OFFSET);

		Macros.loadIFrom(code, RECORD_CREATION_TEMPORARY);
	}

	private static void createRecord(ASMCodeFragment code, int typeCode, int statusFlags) {
		code.add(Call, MEM_MANAGER_ALLOCATE);
		Macros.storeITo(code, RECORD_CREATION_TEMPORARY);

		writeIBaseOffset(code, RECORD_CREATION_TEMPORARY, RECORD_TYPE_ID_OFFSET, typeCode);
		writeIBaseOffset(code, RECORD_CREATION_TEMPORARY, RECORD_STATUS_OFFSET, statusFlags);
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
