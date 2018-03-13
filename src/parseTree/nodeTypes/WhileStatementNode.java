package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import asmCodeGenerator.Labeller;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class WhileStatementNode extends ParseNode {

	private String topLabel, endLabel;

	public WhileStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.WHILE));

		Labeller labeller = new Labeller("while");
		topLabel = labeller.newLabel("top");
		endLabel = labeller.newLabel("end");
	}

	public WhileStatementNode(ParseNode node) {
		super(node);

		assert(node instanceof WhileStatementNode);
		WhileStatementNode whileNode = (WhileStatementNode) node;
		topLabel = whileNode.getTopLabel();
		endLabel = whileNode.getEndLabel();
	}

	public static WhileStatementNode make(Token token, ParseNode condition, ParseNode blockCode) {
		WhileStatementNode node = new WhileStatementNode(token);
		node.appendChild(condition);
		node.appendChild(blockCode);
		return node;
	}

	public String getTopLabel() {
		return topLabel;
	}
	public String getEndLabel() {
		return endLabel;
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
