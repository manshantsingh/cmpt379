package asmCodeGenerator.runtime;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
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

	public static final String CLEAR_N_BYTES = "$procedure-clear-n-bytes";
	public static final String CLONE_N_BYTES = "$procedure-clone-n-bytes";
	public static final String LOWEST_TERMS = "$procedure-lowest-terms";


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

		return frag;
	}

	private ASMCodeFragment additionalSubroutines() {
		ASMCodeFragment frag  = new ASMCodeFragment(GENERATES_VOID);
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
