package semanticAnalyzer.signatures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.operators.ArrayCloneCodeGenerator;
import asmCodeGenerator.operators.ArrayIndexingCodeGenerator;
import asmCodeGenerator.operators.ArrayLengthCodeGenerator;
import asmCodeGenerator.operators.RecordReleaseCodeGenerator;
import asmCodeGenerator.operators.BooleanCastCodeGenerator;
import asmCodeGenerator.operators.CharConcatenateStringCodeGenerator;
import asmCodeGenerator.operators.ExpressOverFloatCodeGenerator;
import asmCodeGenerator.operators.ExpressOverRationalCodeGenerator;
import asmCodeGenerator.operators.FloatRationalCastCodeGenerator;
import asmCodeGenerator.operators.FloatingDivideCodeGenerator;
import asmCodeGenerator.operators.FormRationalCodeGenerator;
import asmCodeGenerator.operators.IntegerCharacterCastCodeGenerator;
import asmCodeGenerator.operators.IntegerDivideCodeGenerator;
import asmCodeGenerator.operators.IntegerRationalCodeGenerator;
import asmCodeGenerator.operators.LogicalNotCodeGenerator;
import asmCodeGenerator.operators.RationalAddSubtractCodeGenerator;
import asmCodeGenerator.operators.RationalFloatCastCodeGenerator;
import asmCodeGenerator.operators.RationalMultiplyDivideCodeGenerator;
import asmCodeGenerator.operators.ShortCircuitCodeGenerator;
import asmCodeGenerator.operators.StringConcatenateCharCodeGenerator;
import asmCodeGenerator.operators.StringConcatenateStringCodeGenerator;
import asmCodeGenerator.operators.StringIndexingCodeGenerator;
import asmCodeGenerator.operators.StringLengthCodeGenerator;
import asmCodeGenerator.operators.StringSubstringCodeGenerator;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Punctuator;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.TypeVariable;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;


public class FunctionSignatures extends ArrayList<FunctionSignature> {
	private static final long serialVersionUID = -4907792488209670697L;
	private static Map<Object, FunctionSignatures> signaturesForKey = new HashMap<Object, FunctionSignatures>();
	
	Object key;
	
	public FunctionSignatures(Object key, FunctionSignature ...functionSignatures) {
		this.key = key;
		for(FunctionSignature functionSignature: functionSignatures) {
			add(functionSignature);
		}
		signaturesForKey.put(key, this);
	}
	
	public Object getKey() {
		return key;
	}
	public boolean hasKey(Object key) {
		return this.key.equals(key);
	}
	
	public FunctionSignature acceptingSignature(List<Type> types) {
		for(FunctionSignature functionSignature: this) {
			if(functionSignature.accepts(types)) {
				return functionSignature;
			}
		}
		return FunctionSignature.nullInstance();
	}
	public FunctionSignature acceptingSignature(List<Type> types, Type returnType) {
		for(FunctionSignature functionSignature: this) {
			if(functionSignature.accepts(types, returnType)) {
				return functionSignature;
			}
		}
		return FunctionSignature.nullInstance();
	}
	public boolean accepts(List<Type> types) {
		return !acceptingSignature(types).isNull();
	}

	
	/////////////////////////////////////////////////////////////////////////////////
	// access to FunctionSignatures by key object.
	
	public static FunctionSignatures nullSignatures = new FunctionSignatures(0, FunctionSignature.nullInstance());

	public static FunctionSignatures signaturesOf(Object key) {
		if(signaturesForKey.containsKey(key)) {
			return signaturesForKey.get(key);
		}
		return nullSignatures;
	}
	public static FunctionSignature signature(Object key, List<Type> types) {
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(key);
		return signatures.acceptingSignature(types);
	}

	
	
	/////////////////////////////////////////////////////////////////////////////////
	// Put the signatures for operators in the following static block.
	
	static {
		// here's one example to get you started with FunctionSignatures: the signatures for addition.		
		// for this to work, you should statically import PrimitiveType.*

		new FunctionSignatures(Punctuator.ADD,
		    new FunctionSignature(ASMOpcode.Add, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
		    new FunctionSignature(ASMOpcode.FAdd, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
		    new FunctionSignature(new RationalAddSubtractCodeGenerator(true), PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL),
		    new FunctionSignature(new StringConcatenateStringCodeGenerator(), PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.STRING),
		    new FunctionSignature(new StringConcatenateCharCodeGenerator(), PrimitiveType.STRING, PrimitiveType.CHARACTER, PrimitiveType.STRING),
		    new FunctionSignature(new CharConcatenateStringCodeGenerator(), PrimitiveType.CHARACTER, PrimitiveType.STRING, PrimitiveType.STRING)
		);
		
		new FunctionSignatures(Punctuator.SUBTRACT,
		    new FunctionSignature(ASMOpcode.Subtract, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
		    new FunctionSignature(ASMOpcode.FSubtract, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
		    new FunctionSignature(new RationalAddSubtractCodeGenerator(false), PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL)
		);

		new FunctionSignatures(Punctuator.MULTIPLY,
		    new FunctionSignature(ASMOpcode.Multiply, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
		    new FunctionSignature(ASMOpcode.FMultiply, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
		    new FunctionSignature(new RationalMultiplyDivideCodeGenerator(true), PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL)
		);

		new FunctionSignatures(Punctuator.DIVIDE,
		    new FunctionSignature(new IntegerDivideCodeGenerator(), PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
		    new FunctionSignature(new FloatingDivideCodeGenerator(), PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
		    new FunctionSignature(new RationalMultiplyDivideCodeGenerator(false), PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL)
		);

		new FunctionSignatures(Punctuator.OVER,
		    new FunctionSignature(new FormRationalCodeGenerator(), PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.RATIONAL)
		);

		new FunctionSignatures(Punctuator.EXPRESS_OVER,
			    new FunctionSignature(new ExpressOverRationalCodeGenerator(false), PrimitiveType.RATIONAL, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
			    new FunctionSignature(new ExpressOverFloatCodeGenerator(false), PrimitiveType.FLOAT, PrimitiveType.INTEGER, PrimitiveType.INTEGER)
		);

		new FunctionSignatures(Punctuator.RATIONALIZE,
		    new FunctionSignature(new ExpressOverRationalCodeGenerator(true), PrimitiveType.RATIONAL, PrimitiveType.INTEGER, PrimitiveType.RATIONAL),
		    new FunctionSignature(new ExpressOverFloatCodeGenerator(true), PrimitiveType.FLOAT, PrimitiveType.INTEGER, PrimitiveType.RATIONAL)
		);

		new FunctionSignatures(Punctuator.PIPE,
			// pika-1 casts
		    new FunctionSignature(ASMOpcode.ConvertF, PrimitiveType.INTEGER, PrimitiveType.FLOAT),
		    new FunctionSignature(ASMOpcode.ConvertI, PrimitiveType.FLOAT, PrimitiveType.INTEGER),
		    new FunctionSignature(new IntegerCharacterCastCodeGenerator(), PrimitiveType.INTEGER, PrimitiveType.CHARACTER),
		    new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.INTEGER),
		    new FunctionSignature(new BooleanCastCodeGenerator(PrimitiveType.INTEGER), PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
		    new FunctionSignature(new BooleanCastCodeGenerator(PrimitiveType.CHARACTER), PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN),
		    // pika-2 casts
		    new FunctionSignature(ASMOpcode.Divide, PrimitiveType.RATIONAL, PrimitiveType.INTEGER),
		    new FunctionSignature(new RationalFloatCastCodeGenerator(), PrimitiveType.RATIONAL, PrimitiveType.FLOAT),
		    new FunctionSignature(new IntegerRationalCodeGenerator(), PrimitiveType.CHARACTER, PrimitiveType.RATIONAL),
		    new FunctionSignature(new IntegerRationalCodeGenerator(), PrimitiveType.INTEGER, PrimitiveType.RATIONAL),
		    new FunctionSignature(new FloatRationalCastCodeGenerator(), PrimitiveType.FLOAT, PrimitiveType.RATIONAL)
		);

		new FunctionSignatures(Punctuator.LOGICAL_AND,
		    new FunctionSignature(new ShortCircuitCodeGenerator(true), PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.LOGICAL_OR,
		    new FunctionSignature(new ShortCircuitCodeGenerator(false), PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.LOGICAL_NOT,
		    new FunctionSignature(new LogicalNotCodeGenerator(), PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
		);

		TypeVariable S = TypeVariable.getInstance();
		new FunctionSignatures(Punctuator.ARRAY_INDEXING,
			new FunctionSignature(new ArrayIndexingCodeGenerator(), new Array(S), PrimitiveType.INTEGER, S).setAsTargetable(),
			new FunctionSignature(new StringIndexingCodeGenerator(), PrimitiveType.STRING, PrimitiveType.INTEGER, PrimitiveType.CHARACTER),
			new FunctionSignature(new StringSubstringCodeGenerator(), PrimitiveType.STRING, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.STRING)
		);

		new FunctionSignatures(Keyword.LENGTH,
			new FunctionSignature(new ArrayLengthCodeGenerator(), new Array(S), PrimitiveType.INTEGER),
			new FunctionSignature(new StringLengthCodeGenerator(), PrimitiveType.STRING, PrimitiveType.INTEGER)
		);

		new FunctionSignatures(Keyword.CLONE,
			new FunctionSignature(new ArrayCloneCodeGenerator(), new Array(S), new Array(S))
		);

		new FunctionSignatures(Keyword.RELEASE,
			new FunctionSignature(new RecordReleaseCodeGenerator(), new Array(S), PrimitiveType.NO_TYPE),
			new FunctionSignature(new RecordReleaseCodeGenerator(), PrimitiveType.STRING, PrimitiveType.NO_TYPE)
		);

		for(Punctuator cmp: Punctuator.COMPARISONS) {
			FunctionSignature i = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
			FunctionSignature c = new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN);
			FunctionSignature f = new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN);
			FunctionSignature r = new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.BOOLEAN);
			FunctionSignature b = new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN);
			FunctionSignature s = new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOLEAN);
			FunctionSignature a = new FunctionSignature(1, new Array(S), new Array(S), PrimitiveType.BOOLEAN);

			if(cmp == Punctuator.EQUALITY || cmp == Punctuator.INEQUALITY) {
				new FunctionSignatures(cmp, i, c, f, r, b, s, a);
			}
			else {
				new FunctionSignatures(cmp, i, c, f, r);
			}
		}

		// First, we use the operator itself (in this case the Punctuator ADD) as the key.
		// Then, we give that key two signatures: one an (INT x INT -> INT) and the other
		// a (FLOAT x FLOAT -> FLOAT).  Each signature has a "whichVariant" parameter where
		// I'm placing the instruction (ASMOpcode) that needs to be executed.
		//
		// I'll follow the convention that if a signature has an ASMOpcode for its whichVariant,
		// then to generate code for the operation, one only needs to generate the code for
		// the operands (in order) and then add to that the Opcode.  For instance, the code for
		// floating addition should look like:
		//
		//		(generate argument 1)	: may be many instructions
		//		(generate argument 2)   : ditto
		//		FAdd					: just one instruction
		//
		// If the code that an operator should generate is more complicated than this, then
		// I will not use an ASMOpcode for the whichVariant.  In these cases I typically use
		// a small object with one method (the "Command" design pattern) that generates the
		// required code.

	}

}
