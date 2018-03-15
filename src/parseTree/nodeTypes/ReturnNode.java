package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ReturnNode extends ParseNode {
	private ParseNode expression = null;

	public ReturnNode(ParseNode node) {
		super(node);
	}
	public ReturnNode(Token token) {
		super(token);
	}
	
	public ParseNode getExpression() {
		return expression;
	}
	public void setExpression(ParseNode exp) {
		expression = exp;
	}
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
