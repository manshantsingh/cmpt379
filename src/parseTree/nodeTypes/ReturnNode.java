package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ReturnNode extends ParseNode {
	private String functionReturnLabel;

	public ReturnNode(ParseNode node) {
		super(node);
	}
	public ReturnNode(Token token) {
		super(token);
	}

	public String getFunctionReturnLabel() {
		return functionReturnLabel;
	}
	public void setFunctionReturnLabel(String val) {
		functionReturnLabel = val;
	}
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
