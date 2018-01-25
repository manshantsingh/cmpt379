package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class TabSpaceNode extends ParseNode {

	public TabSpaceNode(ParseNode node) {
		super(node);
	}
	public TabSpaceNode(Token token) {
		super(token);
	}


	///////////////////////////////////////////////////////////
	// boilerplate for visitors

	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
