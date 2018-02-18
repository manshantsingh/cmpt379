package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class WhileStatementNode extends ParseNode {

	public WhileStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.WHILE));
	}

	public WhileStatementNode(ParseNode node) {
		super(node);
	}

	public static WhileStatementNode make(Token token, ParseNode condition, ParseNode blockCode) {
		WhileStatementNode node = new WhileStatementNode(token);
		node.appendChild(condition);
		node.appendChild(blockCode);
		return node;
	}


	////////////////////////////////////////////////////////////
	// attributes


	///////////////////////////////////////////////////////////
	// boilerplate for visitors

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
