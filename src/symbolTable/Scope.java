package symbolTable;

import asmCodeGenerator.ASMCodeGenerator;
import asmCodeGenerator.runtime.RunTime;
import inputHandler.TextLocation;
import logging.PikaLogger;
import parseTree.nodeTypes.IdentifierNode;
import semanticAnalyzer.types.Type;
import tokens.Token;

public class Scope {
	private Scope baseScope;
	private MemoryAllocator allocator;
	private SymbolTable symbolTable;
	private static Scope globalScope;
	
//////////////////////////////////////////////////////////////////////
// factories
	public static Scope getGlobalScope() {
		return globalScope;
	}
	public static Scope createProgramScope() {
		globalScope = new Scope(programScopeAllocator(), nullInstance());
		return globalScope;
	}
	public Scope createSubscope() {
		return new Scope(allocator, this);
	}
	public Scope createParameterScope() {
		return new Scope(new ParameterMemoryAllocator(MemoryAccessMethod.INDIRECT_ACCESS_BASE,
														RunTime.FRAME_POINTER),
						this);
	}
	public Scope createProcedureScope() {
		return new Scope(new NegativeMemoryAllocator(MemoryAccessMethod.INDIRECT_ACCESS_BASE,
													RunTime.FRAME_POINTER, -ASMCodeGenerator.FRAME_ADDITIONAL_SIZE),
						this);
	}
	
	private static MemoryAllocator programScopeAllocator() {
		return new PositiveMemoryAllocator(
				MemoryAccessMethod.DIRECT_ACCESS_BASE, 
				MemoryLocation.GLOBAL_VARIABLE_BLOCK);
	}
	
//////////////////////////////////////////////////////////////////////
// private constructor.	
	private Scope(MemoryAllocator allocator, Scope baseScope) {
		super();
		this.baseScope = (baseScope == null) ? this : baseScope;
		this.symbolTable = new SymbolTable();
		
		this.allocator = allocator;
		allocator.saveState();
	}
	
///////////////////////////////////////////////////////////////////////
//  basic queries	
	public Scope getBaseScope() {
		return baseScope;
	}
	public MemoryAllocator getAllocationStrategy() {
		return allocator;
	}
	public SymbolTable getSymbolTable() {
		return symbolTable;
	}
	
///////////////////////////////////////////////////////////////////////
//memory allocation
	// must call leave() when destroying/leaving a scope.
	public void leave() {
		allocator.restoreState();
	}
	public int getAllocatedSize() {
		return allocator.getMaxAllocatedSize();
	}

///////////////////////////////////////////////////////////////////////
//bindings
	public Binding createBinding(IdentifierNode identifierNode, Type type, boolean constant) {
		Token token = identifierNode.getToken();
		symbolTable.errorIfAlreadyDefined(token);

		String lexeme = token.getLexeme();
		Binding binding = allocateNewBinding(type, token.getLocation(), lexeme, constant);
		symbolTable.install(lexeme, binding);
		return binding;
	}
	private Binding allocateNewBinding(Type type, TextLocation textLocation, String lexeme, boolean constant) {
		MemoryLocation memoryLocation = allocator.allocate(type.getSize());
		return new Binding(type, textLocation, memoryLocation, lexeme, constant);
	}
	
///////////////////////////////////////////////////////////////////////
//toString
	public String toString() {
		String result = "scope: ";
		result += " hash "+ hashCode() + "\n";
		result += symbolTable;
		return result;
	}

////////////////////////////////////////////////////////////////////////////////////
//Null Scope object - lazy singleton (Lazy Holder) implementation pattern
	public static Scope nullInstance() {
		return NullScope.instance;
	}
	private static class NullScope extends Scope {
		private static NullScope instance = new NullScope();

		private NullScope() {
			super(	new PositiveMemoryAllocator(MemoryAccessMethod.NULL_ACCESS, "", 0),
					null);
		}
		public String toString() {
			return "scope: the-null-scope";
		}
		@Override
		public Binding createBinding(IdentifierNode identifierNode, Type type, boolean constant) {
			unscopedIdentifierError(identifierNode.getToken());
			return super.createBinding(identifierNode, type, constant);
		}
		// subscopes of null scope need their own strategy.  Assumes global block is static.
		public Scope createSubscope() {
			return new Scope(programScopeAllocator(), this);
		}
	}


///////////////////////////////////////////////////////////////////////
//error reporting
	private static void unscopedIdentifierError(Token token) {
		PikaLogger log = PikaLogger.getLogger("compiler.scope");
		log.severe("variable " + token.getLexeme() + 
				" used outside of any scope at " + token.getLocation());
	}

}
