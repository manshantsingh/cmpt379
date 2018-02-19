package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import parseTree.SomeArbitaryOperatorNode;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import tokens.LextantToken;
import tokens.Token;

public class CastNode extends SomeArbitaryOperatorNode {
	
	public CastNode(Token token) {
		super(token);
	}

	public CastNode(ParseNode node) {
		super(node);
	}

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
	
	public static CastNode make(Token token, ParseNode exp, Type type) {
		CastNode node = new CastNode(token);
		node.appendChild(exp);
		node.setType(type);
		return node;
	}
}
