package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.signatures.FunctionSignature;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class UnaryOperatorNode extends ParseNode {

	private FunctionSignature signature = FunctionSignature.nullInstance();

	public FunctionSignature getSignature() {
		return signature;
	}

	public void setSignature(FunctionSignature signature) {
		this.signature = signature;
		setType(signature.resultType());
	}

	public UnaryOperatorNode(Token token) {
		super(token);
		assert(token instanceof LextantToken);
	}

	public UnaryOperatorNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public Lextant getOperator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static UnaryOperatorNode withChildren(Token token, ParseNode child) {
		UnaryOperatorNode node = new UnaryOperatorNode(token);
		node.appendChild(child);
		return node;
	}
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
