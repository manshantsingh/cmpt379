package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class FunctionDeclarationNode extends ParseNode {

	public FunctionDeclarationNode(Token token) {
		super(token);
	}
	public FunctionDeclarationNode(ParseNode node) {
		super(node);
	}
	public static FunctionDeclarationNode make(Token token, ParseNode identifier, ParseNode lambda) {
		FunctionDeclarationNode node = new FunctionDeclarationNode(token);
		node.appendChild(identifier);
		node.appendChild(lambda);
		return node;
	}
	////////////////////////////////////////////////////////////
	// no attributes

	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
