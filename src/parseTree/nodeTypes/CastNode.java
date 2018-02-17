package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import tokens.LextantToken;
import tokens.Token;

public class CastNode extends ParseNode {

	private FunctionSignature signature = FunctionSignature.nullInstance();
	private Type resultType;

	public FunctionSignature getSignature() {
		return signature;
	}

	public void setSignature(FunctionSignature signature) {
		this.signature = signature;
		setType(signature.resultType());
	}
	
	public Type getResultType() {
		return resultType;
	}
	
	public CastNode(Token token) {
		super(token);
		assert(token instanceof LextantToken);
		resultType = PrimitiveType.fromTypeVariable((LextantToken) token);
	}

	public CastNode(ParseNode node) {
		super(node);
	}

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
	
	public static CastNode make(Token token, ParseNode exp) {
		CastNode node = new CastNode(token);
		node.appendChild(exp);
		return node;
	}
}
