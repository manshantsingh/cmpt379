package asmCodeGenerator.runtime;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import static asmCodeGenerator.runtime.RunTime.NULL_ARRAY_RUNTIME_ERROR;

import asmCodeGenerator.ASMCodeGenerator;
import asmCodeGenerator.ASMConstants;

import static asmCodeGenerator.ASMConstants.*;
import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.operators.ArrayLengthCodeGenerator;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
public class RunTime {
	public static final String EAT_LOCATION_ZERO      	= "$eat-location-zero";		// helps us distinguish null pointers from real ones.
	public static final String INTEGER_PRINT_FORMAT   	= "$print-format-integer";
	public static final String FLOATING_PRINT_FORMAT  	= "$print-format-floating";
	public static final String CHARACTER_PRINT_FORMAT  	= "$print-format-character";
	public static final String RATIONAL_PRINT_FORMAT  	= "$print-format-rational";
	public static final String STRING_PRINT_FORMAT   	= "$print-format-string";
	public static final String BOOLEAN_PRINT_FORMAT   	= "$print-format-boolean";
	public static final String NEWLINE_PRINT   			= "$print-newline";
	public static final String SPACE_PRINT     			= "$print-space";
	public static final String TAB_SPACE_PRINT   		= "$print-tabspace";
	public static final String MINUS_PRINT   			= "$print-minus";
	public static final String ARRAY_START_BRACKET		= "$print-array-start";
	public static final String ARRAY_ELEMENT_SEPERATOR	= "$print-array-element-seperater";
	public static final String ARRAY_END_BRACKET		= "$print-array-end";
	public static final String BOOLEAN_TRUE_STRING    	= "$boolean-true-string";
	public static final String BOOLEAN_FALSE_STRING   	= "$boolean-false-string";
	public static final String GLOBAL_MEMORY_BLOCK    	= "$global-memory-block";
	public static final String USABLE_MEMORY_START    	= "$usable-memory-start";
	public static final String MAIN_PROGRAM_LABEL     	= "$$main";

	public static final String GENERAL_RUNTIME_ERROR = "$$general-runtime-error";

	public static final String INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR = "$$i-divide-by-zero";
	public static final String FLOATING_DIVIDE_BY_ZERO_RUNTIME_ERROR = "$$f-divide-by-zero";
	public static final String RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR = "$$r-divide-by-zero";

	public static final String NULL_ARRAY_RUNTIME_ERROR = "$$a-null-array-runtime_error";
	public static final String INDEX_OUT_OF_BOUND_ARRAY_RUNTIME_ERROR = "$$a-index-out-of-bound-runtime_error";
	public static final String NEGATIVE_LENGTH_ARRAY_RUNTIME_ERROR = "$$a-negative-length-runtime_error";

	public static final String RECORD_CREATION_TEMPORARY = "$$record-create-temporary";
	public static final String ARRAY_INDEXING_ARRAY = "$$a-indexing-array";
	public static final String ARRAY_INDEXING_INDEX = "$$a-indexing-index";
	public static final String RATIONAL_NUMERATOR_TEMPORARY = "$$r-numerator-temporary";
	public static final String RATIONAL_DENOMINATOR_TEMPORARY = "$$r-denominator-temporary";
	public static final String RATIONAL_COMMON_DENOMINATOR_TEMPORARY = "$$r-common-denominator-temporary";
	public static final String INNER_MOST_PRINT_CALL = "$$inner-most-print-call";

	public static final String CLEAR_N_BYTES = "$procedure-clear-n-bytes";
	public static final String CLONE_N_BYTES = "$procedure-clone-n-bytes";
	public static final String LOWEST_TERMS = "$procedure-lowest-terms";
	public static final String RECORD_RELEASE = "$procedure-record-release";

	public static final String PRINT_RATIONAL	= "$procedure-print-rational";
	public static final String PRINT_ARRAY		= "$procedure-print-array";
	public static final String PRINT_BOOLEAN	= "$procedure-print-boolean";
	public static final String PRINT_STRING		= "$procedure-print-string";
	public static final String PRINT_INTEGER	= "$procedure-print-integer";
	public static final String PRINT_FLOAT		= "$procedure-print-float";
	public static final String PRINT_CHARACTER	= "$procedure-print-character";

	public static final String LOAD_PRINT_RATIONAL	= "$procedure-load-print-rational";
	public static final String LOAD_PRINT_ARRAY		= "$procedure-load-print-array";
	public static final String LOAD_PRINT_BOOLEAN	= "$procedure-load-print-boolean";
	public static final String LOAD_PRINT_STRING	= "$procedure-load-print-string";
	public static final String LOAD_PRINT_INTEGER	= "$procedure-load-print-integer";
	public static final String LOAD_PRINT_FLOAT		= "$procedure-load-print-float";
	public static final String LOAD_PRINT_CHARACTER	= "$procedure-load-print-character";


	private ASMCodeFragment environmentASM() {
		ASMCodeFragment result = new ASMCodeFragment(GENERATES_VOID);
		result.append(jumpToMain());
		result.append(stringsForPrintf());
		result.append(runtimeErrors());
		result.append(temporaryVariables());
		result.append(additionalSubroutines());
		result.add(DLabel, USABLE_MEMORY_START);
		return result;
	}
	
	private ASMCodeFragment temporaryVariables() {
		ASMCodeFragment frag  = new ASMCodeFragment(GENERATES_VOID);

		Macros.declareI(frag, RECORD_CREATION_TEMPORARY);

		Macros.declareI(frag, ARRAY_INDEXING_ARRAY);
		Macros.declareI(frag, ARRAY_INDEXING_INDEX);

		Macros.declareI(frag, RATIONAL_NUMERATOR_TEMPORARY);
		Macros.declareI(frag, RATIONAL_DENOMINATOR_TEMPORARY);
		Macros.declareI(frag, RATIONAL_COMMON_DENOMINATOR_TEMPORARY);

		Macros.declareI(frag, INNER_MOST_PRINT_CALL);

		return frag;
	}

	private ASMCodeFragment additionalSubroutines() {
		ASMCodeFragment frag  = new ASMCodeFragment(GENERATES_VOID);
		frag.append(printRational());
		frag.append(printArray());
		frag.append(printRemaining());
		frag.append(recordRelease());
		frag.append(lowestTerms());
		frag.append(cloneBytes());
		frag.append(clearBytes());
		return frag;
	}

	private ASMCodeFragment jumpToMain() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(Jump, MAIN_PROGRAM_LABEL);
		return frag;
	}

	private ASMCodeFragment stringsForPrintf() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(DLabel, EAT_LOCATION_ZERO);
		frag.add(DataZ, 8);
		frag.add(DLabel, INTEGER_PRINT_FORMAT);
		frag.add(DataS, "%d");
		frag.add(DLabel, FLOATING_PRINT_FORMAT);
		frag.add(DataS, "%g");
		frag.add(DLabel, CHARACTER_PRINT_FORMAT);
		frag.add(DataS, "%c");
		frag.add(DLabel, RATIONAL_PRINT_FORMAT);
		frag.add(DataS, "_%d/%d");
		frag.add(DLabel, STRING_PRINT_FORMAT);
		frag.add(DataS, "%s");
		frag.add(DLabel, BOOLEAN_PRINT_FORMAT);
		frag.add(DataS, "%s");
		frag.add(DLabel, NEWLINE_PRINT);
		frag.add(DataS, "\n");
		frag.add(DLabel, SPACE_PRINT);
		frag.add(DataS, " ");
		frag.add(DLabel, TAB_SPACE_PRINT);
		frag.add(DataS, "\t");
		frag.add(DLabel, MINUS_PRINT);
		frag.add(DataS, "-");
		frag.add(DLabel, ARRAY_START_BRACKET);
		frag.add(DataS, "[");
		frag.add(DLabel, ARRAY_ELEMENT_SEPERATOR);
		frag.add(DataS, ", ");
		frag.add(DLabel, ARRAY_END_BRACKET);
		frag.add(DataS, "]");
		frag.add(DLabel, BOOLEAN_TRUE_STRING);
		frag.add(DataS, "true");
		frag.add(DLabel, BOOLEAN_FALSE_STRING);
		frag.add(DataS, "false");
		
		return frag;
	}
	
	
	private ASMCodeFragment runtimeErrors() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		generalRuntimeError(frag);

		integerDivideByZeroError(frag);
		floatingDivideByZeroError(frag);
		rationalDivideByZeroError(frag);

		nullArrayError(frag);
		indexOutOfBoundArrayError(frag);
		negativeLengthArrayError(frag);
		
		return frag;
	}
	private ASMCodeFragment generalRuntimeError(ASMCodeFragment frag) {
		String generalErrorMessage = "$errors-general-message";

		frag.add(DLabel, generalErrorMessage);
		frag.add(DataS, "Runtime error: %s\n");

		frag.add(Label, GENERAL_RUNTIME_ERROR);
		frag.add(PushD, generalErrorMessage);
		frag.add(Printf);
		frag.add(Halt);
		return frag;
	}
	private void integerDivideByZeroError(ASMCodeFragment frag) {
		String intDivideByZeroMessage = "$errors-int-divide-by-zero";
		
		frag.add(DLabel, intDivideByZeroMessage);
		frag.add(DataS, "integer divide by zero");
		
		frag.add(Label, INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		frag.add(PushD, intDivideByZeroMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void floatingDivideByZeroError(ASMCodeFragment frag) {
		String floatDivideByZeroMessage = "$errors-float-divide-by-zero";

		frag.add(DLabel, floatDivideByZeroMessage);
		frag.add(DataS, "floating divide by zero");

		frag.add(Label, FLOATING_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		frag.add(PushD, floatDivideByZeroMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void rationalDivideByZeroError(ASMCodeFragment frag) {
		String rationalDivideByZeroMessage = "$errors-rational-divide-by-zero";

		frag.add(DLabel, rationalDivideByZeroMessage);
		frag.add(DataS, "rational divide by zero");

		frag.add(Label, RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		frag.add(PushD, rationalDivideByZeroMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void nullArrayError(ASMCodeFragment frag) {
		String nullArrayErrorMessage = "$errors-null-array";

		frag.add(DLabel, nullArrayErrorMessage);
		frag.add(DataS, "null array");

		frag.add(Label, NULL_ARRAY_RUNTIME_ERROR);
		frag.add(PushD, nullArrayErrorMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void indexOutOfBoundArrayError(ASMCodeFragment frag) {
		String indexOutOfBoundArrayErrorMessage = "$errors-index-out-of-bound-array";

		frag.add(DLabel, indexOutOfBoundArrayErrorMessage);
		frag.add(DataS, "array index out of bound");

		frag.add(Label, INDEX_OUT_OF_BOUND_ARRAY_RUNTIME_ERROR);
		frag.add(PushD, indexOutOfBoundArrayErrorMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void negativeLengthArrayError(ASMCodeFragment frag) {
		String negativeLengthArrayErrorMessage = "$errors-negative-length-array";

		frag.add(DLabel, negativeLengthArrayErrorMessage);
		frag.add(DataS, "array negative length");

		frag.add(Label, NEGATIVE_LENGTH_ARRAY_RUNTIME_ERROR);
		frag.add(PushD, negativeLengthArrayErrorMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	public static ASMCodeFragment getEnvironment() {
		RunTime rt = new RunTime();
		return rt.environmentASM();
	}

	private ASMCodeFragment printRational() {
		// TODO confirm the port
		Labeller labeller = new Labeller("print-rational");

		String returnPtr = labeller.newLabel("return-ptr");

		String negativePrint = labeller.newLabel("negative-print");
		String negativeEnd = labeller.newLabel("negative-end");

		String denomIsOne = labeller.newLabel("denom-is-one");
		String numeratorIsSmall = labeller.newLabel("numerator-is-small");
		String numeratorDone = labeller.newLabel("numerator-done");
		String end = labeller.newLabel("end");

		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);

		Macros.declareI(code, returnPtr);
		code.add(Label, LOAD_PRINT_RATIONAL);
		Macros.storeITo(code, returnPtr);
		ASMCodeGenerator.loadFromAddress(code, PrimitiveType.RATIONAL);
		Macros.loadIFrom(code, returnPtr);

		code.add(Label, PRINT_RATIONAL);
		Macros.storeITo(code, returnPtr);	// [...  numerator  denominator]
		code.add(Exchange);
		code.add(Duplicate);
		code.add(JumpNeg, negativePrint);
		code.add(Jump, negativeEnd);

		code.add(Label, negativePrint);
		code.add(PushI, -1);
		code.add(Multiply);
		code.add(PushD, RunTime.MINUS_PRINT);
		code.add(Printf);

		code.add(Label, negativeEnd);
		code.add(Exchange);		// [...  abs(numerator)  denominator]
		code.add(Duplicate);
		code.add(PushI, 1);
		code.add(Subtract);
		code.add(JumpFalse, denomIsOne);
		code.add(Exchange);		// [... denominator numerator]
		code.add(Duplicate);
		code.add(JumpFalse, denomIsOne); // hacky but works
		code.add(Exchange);		// [... numerator denominator]
		code.add(Duplicate);
		Macros.storeITo(code, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);	// [... nume denom]
		code.add(Exchange);
		code.add(Duplicate);
		Macros.storeITo(code, RunTime.RATIONAL_NUMERATOR_TEMPORARY);
		code.add(Exchange);
		code.add(Divide);		// [...  nume/denom]
		code.add(Duplicate);
		code.add(JumpFalse, numeratorIsSmall);
		code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
		code.add(Printf);
		code.add(Jump, numeratorDone);

		code.add(Label, numeratorIsSmall);
		code.add(Pop);

		code.add(Label, numeratorDone);		// [...]
		Macros.loadIFrom(code, RunTime.RATIONAL_DENOMINATOR_TEMPORARY);
		code.add(Duplicate);
		Macros.loadIFrom(code, RunTime.RATIONAL_NUMERATOR_TEMPORARY);
		code.add(Exchange);		// [...  denom  nume  denom]
		code.add(Remainder);
		code.add(PushD, RunTime.RATIONAL_PRINT_FORMAT);
		code.add(Printf);
		code.add(Jump, end);

		code.add(Label, denomIsOne);
		code.add(Pop);
		code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
		code.add(Printf);

		code.add(Label, end);

		Macros.loadIFrom(code, returnPtr);
		code.add(Return);

		return code;
	}

	private ASMCodeFragment printRemaining() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);

		// boolean
		Labeller labeller = new Labeller("print-boolean");

		String trueLabel = labeller.newLabel("true");
		String endLabel = labeller.newLabel("join");

		frag.add(Label, LOAD_PRINT_BOOLEAN);
		frag.add(Exchange);
		ASMCodeGenerator.loadFromAddress(frag, PrimitiveType.BOOLEAN);
		frag.add(Exchange);

		frag.add(Label, PRINT_BOOLEAN);
		frag.add(Exchange);
		frag.add(JumpTrue, trueLabel);
		frag.add(PushD, BOOLEAN_FALSE_STRING);
		frag.add(Jump, endLabel);
		frag.add(Label, trueLabel);
		frag.add(PushD, BOOLEAN_TRUE_STRING);
		frag.add(Label, endLabel);
		frag.add(PushD, BOOLEAN_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(Return);

		// string
		frag.add(Label, LOAD_PRINT_STRING);
		frag.add(Exchange);
		ASMCodeGenerator.loadFromAddress(frag, PrimitiveType.STRING);
		frag.add(Exchange);

		frag.add(Label, PRINT_STRING);
		frag.add(Exchange);
		frag.add(PushI, ASMConstants.STRING_HEADER_OFFSET);
		frag.add(Add);
		frag.add(PushD, STRING_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(Return);

		// character
		frag.add(Label, LOAD_PRINT_CHARACTER);
		frag.add(Exchange);
		ASMCodeGenerator.loadFromAddress(frag, PrimitiveType.CHARACTER);
		frag.add(Exchange);

		frag.add(Label, PRINT_CHARACTER);
		frag.add(Exchange);
		frag.add(PushD, CHARACTER_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(Return);

		// float
		frag.add(Label, LOAD_PRINT_FLOAT);
		frag.add(Exchange);
		ASMCodeGenerator.loadFromAddress(frag, PrimitiveType.FLOAT);
		frag.add(Exchange);

		frag.add(Label, PRINT_FLOAT);
		frag.add(Exchange);
		frag.add(PushD, FLOATING_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(Return);

		// integer
		frag.add(Label, LOAD_PRINT_INTEGER);
		frag.add(Exchange);
		ASMCodeGenerator.loadFromAddress(frag, PrimitiveType.INTEGER);
		frag.add(Exchange);

		frag.add(Label, PRINT_INTEGER);
		frag.add(Exchange);
		frag.add(PushD, INTEGER_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(Return);

		return frag;
	}

	private ASMCodeFragment printArray() {
		Labeller labeller = new Labeller("print-array");

		String currentPrintProcedure = labeller.newLabel("current-print-procedure");
		String elementSize = labeller.newLabel("element-size");

		String confirmedNotString = labeller.newLabel("confirmed-not-string");

		String subtypeNotArray = labeller.newLabel("subtype-not-array");
		String subtypeCheckDone = labeller.newLabel("subtype-check-done");

		String top = labeller.newLabel("top");
		String end = labeller.newLabel("end");

		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		Macros.declareI(frag, currentPrintProcedure);
		Macros.declareI(frag, elementSize);

		frag.add(Label, LOAD_PRINT_ARRAY);
		frag.add(Exchange);
		ASMCodeGenerator.loadFromAddress(frag, new Array(null));
		frag.add(Exchange);

		frag.add(Label, PRINT_ARRAY);
		frag.add(Exchange);		// [...  returnPtr  array]

		frag.add(Duplicate);
		frag.add(JumpFalse, NULL_ARRAY_RUNTIME_ERROR);

		frag.add(Duplicate);
		frag.add(PushI, RECORD_TYPE_ID_OFFSET);
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushI, STRING_TYPE_ID);
		frag.add(Subtract);
		frag.add(JumpTrue, confirmedNotString);
		// [... returnPtr array]
		frag.add(Call, PRINT_STRING);
		frag.add(Return);

		frag.add(Label, confirmedNotString);
		frag.add(Duplicate);
		frag.add(PushI, RECORD_STATUS_OFFSET);
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushI, MASK_ARRAY_CHECK_REFERENCE_SUBTYPE);
		frag.add(BTAnd);	// [... returnPtr array bitwiseSubtypeAnd]
		frag.add(JumpFalse, subtypeNotArray);
		frag.add(PushD, LOAD_PRINT_ARRAY);
		frag.add(Jump, subtypeCheckDone);
		frag.add(Label, subtypeNotArray);
		Macros.loadIFrom(frag, INNER_MOST_PRINT_CALL);
		frag.add(Label, subtypeCheckDone);
		Macros.storeITo(frag, currentPrintProcedure);	// [... returnPtr array]

		// TODO
		frag.add(Duplicate);
		ArrayLengthCodeGenerator.getLength(frag);
		frag.add(Exchange);		// [... returnPtr nElm array]
		frag.add(Duplicate);
		frag.add(PushI, ARRAY_SUBELEMENT_SIZE_OFFSET);
		frag.add(Add);
		frag.add(LoadI);
		Macros.storeITo(frag, elementSize);		// [... returnPtr nElm array]

		frag.add(PushI, ARRAY_HEADER_OFFSET);
		frag.add(Add);		// [... returnPtr nElm addrOfFirstElm]

		frag.add(Exchange);
		frag.add(PushD, ARRAY_START_BRACKET);
		frag.add(Printf);
		frag.add(Duplicate);
		frag.add(JumpFalse, end);
		frag.add(Exchange);
		printInnerArray(frag, currentPrintProcedure, elementSize);

		frag.add(Label, top);	// [... returnPtr nElm addrOfPreviousElm]
		frag.add(Exchange);
		frag.add(PushI, 1);
		frag.add(Subtract);
		frag.add(Duplicate);
		frag.add(JumpFalse, end);
		frag.add(PushD, ARRAY_ELEMENT_SEPERATOR);
		frag.add(Printf);
		frag.add(Exchange);
		Macros.loadIFrom(frag, elementSize);
		frag.add(Add);

		printInnerArray(frag, currentPrintProcedure, elementSize);
		// [... returnPtr nElm addrOfPreviousElm]
		frag.add(Jump, top);

		frag.add(Label, end);
		frag.add(Pop);
		frag.add(Pop);
		frag.add(PushD, ARRAY_END_BRACKET);
		frag.add(Printf);
		frag.add(Return);

		return frag;
	}

	private void printInnerArray(ASMCodeFragment frag, String printInner, String elmSize) {
		frag.add(Duplicate);
		Macros.loadIFrom(frag, printInner);
		frag.add(Exchange);
		Macros.loadIFrom(frag, elmSize);
		frag.add(Exchange);
		Macros.loadIFrom(frag, printInner);
		frag.add(CallV);
		Macros.storeITo(frag, elmSize);
		Macros.storeITo(frag, printInner);
	}

	private ASMCodeFragment recordRelease() {
		Labeller labeller = new Labeller("release-array");

		String arrayNotNull = labeller.newLabel("array-not-null");
		String ignoreDelete = labeller.newLabel("ignore-delete");
		String notTypeArray = labeller.newLabel("not-type-array");
		String simpleSubElement = labeller.newLabel("simple-sub-element");

		String loopStart = labeller.newLabel("loop-start");
		String loopEnd = labeller.newLabel("loop-end");

		String end = labeller.newLabel("end");

		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VOID);

		// TODO
		frag.add(Label, RECORD_RELEASE);
		frag.add(Exchange); // [... returnPtr arrayAddr]

		frag.add(Duplicate);
		frag.add(JumpTrue, arrayNotNull);
		frag.add(Pop);
		frag.add(Return);

		frag.add(Label, arrayNotNull);
		frag.add(Duplicate);
		frag.add(PushI, RECORD_STATUS_OFFSET);
		frag.add(Add);
		frag.add(Duplicate);
		frag.add(LoadI);
		frag.add(PushI, MASK_RECORD_CHECK_ALLOWS_DELETION);
		frag.add(BTAnd);		// [... returnPtr arrayAddr statusLocation bitmaskResult]
		frag.add(JumpTrue, ignoreDelete);
		frag.add(Duplicate);
		frag.add(LoadI);
		frag.add(PushI, MASK_RECORD_SET_IS_DELETED);
		frag.add(BTOr);		// [... returnPtr arrayAddr statusLocation newStatusValue]
		frag.add(StoreI);

		// TODO
		frag.add(Duplicate);	// [... returnPtr arrayAddr arrayAddr]
		frag.add(PushI, RECORD_TYPE_ID_OFFSET);
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushI, ARRAY_TYPE_ID);
		frag.add(Subtract);
		frag.add(JumpTrue, notTypeArray);

		frag.add(Duplicate);	// [... returnPtr arrayAddr arrayAddr]
		frag.add(PushI, RECORD_STATUS_OFFSET);
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushI, MASK_ARRAY_CHECK_REFERENCE_SUBTYPE);
		frag.add(BTAnd);
		frag.add(JumpFalse, simpleSubElement);

		frag.add(Duplicate);
		frag.add(Duplicate);	// [... returnPtr arrayAddr arrayAddr arrayAddr]
		frag.add(PushI, ARRAY_LENGTH_OFFSET);
		frag.add(Add);
		frag.add(LoadI);
		frag.add(Exchange);	// [... returnPtr arrayAddr nElms arrayAddr]
//		frag.add(Duplicate);
		frag.add(PushI, ARRAY_HEADER_OFFSET);
		frag.add(Add);		// [... returnPtr arrayAddr nElms firstElmAddr]

		frag.add(Label, loopStart);
		frag.add(Exchange);		// [... returnPtr arrayAddr firstElmAddr nElms]
		frag.add(Duplicate);
		frag.add(JumpFalse, loopEnd);
		frag.add(PushI, 1);
		frag.add(Subtract);		// [... returnPtr arrayAddr firstElmAddr nElms]
		frag.add(Exchange);
		frag.add(Duplicate);
		frag.add(Call, RECORD_RELEASE);		// [... returnPtr arrayAddr nElms firstElmAddr]
		frag.add(Jump, loopStart);

		frag.add(Label, loopEnd);
		frag.add(Pop);
		frag.add(Pop);

		frag.add(Label, simpleSubElement);
		frag.add(Call, MemoryManager.MEM_MANAGER_DEALLOCATE);
		frag.add(Jump, end);

		frag.add(Label, ignoreDelete);	// [... returnPtr arrayAddr statusLocation]
		frag.add(Pop);
		frag.add(Label, notTypeArray);	// [... returnPtr arrayAddr]
		frag.add(Pop);
		frag.add(Label, end);
		frag.add(Return);

		return frag;
	}

	private ASMCodeFragment lowestTerms() {
		Labeller labeller = new Labeller("lowest-terms");

		String returnPtr = labeller.newLabel("return-ptr");
		String first = labeller.newLabel("first");
		String second = labeller.newLabel("second");
		String isNegative = labeller.newLabel("is-negative");

		String denomNegative = labeller.newLabel("denom-negative");
		String denomDone = labeller.newLabel("denom-done");
		String numeratorNegative = labeller.newLabel("numerator-negative");
		String numeratorDone = labeller.newLabel("numerator-done");
		String signChangeNotNeeded = labeller.newLabel("sign-change-not-needed");

		String top = labeller.newLabel("top");
		String end = labeller.newLabel("end");

		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		Macros.declareI(frag, returnPtr);
		Macros.declareI(frag, first);
		Macros.declareI(frag, second);
		Macros.declareI(frag, isNegative);


		frag.add(Label, LOWEST_TERMS);
		Macros.storeITo(frag, returnPtr);	// [... first second]
		frag.add(Duplicate);
		frag.add(JumpFalse, RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR);	// [... first second]

		frag.add(Duplicate);
		frag.add(JumpNeg, denomNegative);
		frag.add(PushI, 0);
		frag.add(Jump, denomDone);
		frag.add(Label, denomNegative);
		frag.add(PushI, -1);
		frag.add(Multiply);
		frag.add(PushI, 1);
		frag.add(Label, denomDone);
		Macros.storeITo(frag, isNegative);	// [... first abs(second)]

		frag.add(Duplicate);
		Macros.storeITo(frag, second);
		frag.add(Exchange);		// [... second first]

		frag.add(Duplicate);
		frag.add(JumpNeg, numeratorNegative);
		frag.add(Jump, numeratorDone);
		frag.add(Label, numeratorNegative);
		frag.add(PushI, -1);
		frag.add(Multiply);
		Macros.loadIFrom(frag, isNegative);
		frag.add(PushI, 1);
		frag.add(Add);
		Macros.storeITo(frag, isNegative);
		frag.add(Label, numeratorDone);		// [... second abs(first)]

		frag.add(Duplicate);
		Macros.storeITo(frag, first);	// [... second first]

		frag.add(Label, top);
		Macros.loadIFrom(frag, first);
		Macros.loadIFrom(frag, second);
		frag.add(Remainder);
		frag.add(Duplicate);
		frag.add(JumpFalse, end);
		Macros.loadIFrom(frag, second);
		Macros.storeITo(frag, first);
		Macros.storeITo(frag, second);
		frag.add(Jump, top);

		frag.add(Label, end);
		frag.add(Pop);		// [... second first]
		Macros.loadIFrom(frag, second);
		frag.add(Divide);	// [... second first/gcd]
		Macros.loadIFrom(frag, isNegative);
		frag.add(PushI, 2);
		frag.add(Remainder);	// [...  second  first/gcd  (1: sign change needed, 0: otherwise)]
		frag.add(JumpFalse, signChangeNotNeeded);
		frag.add(PushI, -1);
		frag.add(Multiply);
		frag.add(Label, signChangeNotNeeded);	// [... second first/gcd]
		frag.add(Exchange);
		Macros.loadIFrom(frag, second);
		frag.add(Divide);	// [...  first/gcd  second/gcd]

		Macros.loadIFrom(frag, returnPtr);
		frag.add(Return);

		return frag;
	}

	private ASMCodeFragment cloneBytes() {
		Labeller labeller = new Labeller("clone-n-bytes");

		String returnPtr = labeller.newLabel("return-ptr");
		String fromAddr = labeller.newLabel("fromAddr");
		String toAddr = labeller.newLabel("toAddr");

		String top = labeller.newLabel("top");
		String middle = labeller.newLabel("middle");
		String end = labeller.newLabel("end");

		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		Macros.declareI(frag, returnPtr);
		Macros.declareI(frag, fromAddr);
		Macros.declareI(frag, toAddr);

		frag.add(Label, CLONE_N_BYTES);
		Macros.storeITo(frag, returnPtr);	// [... nBytes fromAddr toAddr]
		Macros.storeITo(frag, toAddr);
		Macros.storeITo(frag, fromAddr);

		frag.add(Label, top);	// [... nBytes]
		frag.add(Duplicate);
		frag.add(JumpPos, middle);
		frag.add(Jump, end);

		frag.add(Label, middle);	// [... nBytes]
		frag.add(PushI, 1);
		frag.add(Subtract);
		Macros.loadIFrom(frag, fromAddr);
		Macros.loadIFrom(frag, toAddr);
		frag.add(Duplicate);
		Macros.loadIFrom(frag, fromAddr);	// [... nBytes fromAddr toAddr toAddr fromAddr]
		frag.add(LoadC);
		frag.add(StoreC);	// [... nBytes fromAddr toAddr]
		frag.add(PushI, 1);
		frag.add(Add);
		Macros.storeITo(frag, toAddr);	// [... nBytes fromAddr]
		frag.add(PushI, 1);
		frag.add(Add);
		Macros.storeITo(frag, fromAddr);	// [... nBytes]
		frag.add(Jump, top);

		frag.add(Label, end);
		frag.add(Pop);
		Macros.loadIFrom(frag, returnPtr);
		frag.add(Return);

		return frag;
	}

	private ASMCodeFragment clearBytes() {
		Labeller labeller = new Labeller("Clear-n-bytes");
		String returnPtr = labeller.newLabel("return-ptr");
		String top = labeller.newLabel("top");
		String middle = labeller.newLabel("middle");
		String end = labeller.newLabel("end");

		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		Macros.declareI(frag, returnPtr);

		frag.add(Label, CLEAR_N_BYTES);
		Macros.storeITo(frag, returnPtr);	// [ ... BaseAddr, numBytes ]

		frag.add(Label, top);
		frag.add(Duplicate);
		frag.add(JumpPos, middle);
		frag.add(Jump, end);

		frag.add(Label, middle);
		frag.add(PushI, 1);
		frag.add(Subtract);
		frag.add(Exchange);
		frag.add(Duplicate);
		frag.add(PushI, 0);
		frag.add(StoreC);
		frag.add(PushI, 1);
		frag.add(Add);
		frag.add(Exchange);
		frag.add(Jump, top);

		frag.add(Label, end);
		frag.add(Pop);
		frag.add(Pop);
		Macros.loadIFrom(frag, returnPtr);
		frag.add(Return);

		return frag;
	}
}
