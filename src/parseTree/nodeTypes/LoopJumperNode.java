package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class LoopJumperNode extends ParseNode {

	private String jumpLabel;

	public LoopJumperNode(ParseNode node) {
		super(node);
	}
	public LoopJumperNode(Token token) {
		super(token);
	}

	public String getJumpLabel() {
		return jumpLabel;
	}

	public void setJumpLabelFromParent(WhileStatementNode node) {
		if(token.isLextant(Keyword.CONTINUE)) {
			jumpLabel = node.getTopLabel();
		}
		else if(token.isLextant(Keyword.BREAK)) {
			jumpLabel = node.getEndLabel();
		}
		else {
			System.out.println("Found unknown Loop Jumper token: "+token);
		}
	}
	public void setJumpLabelFromParent(ForStatementNode node) {
		if(token.isLextant(Keyword.CONTINUE)) {
			jumpLabel = node.getTopLabel();
		}
		else if(token.isLextant(Keyword.BREAK)) {
			jumpLabel = node.getEndLabel();
		}
		else {
			System.out.println("Found unknown Loop Jumper token: "+token);
		}
	}
	///////////////////////////////////////////////////////////
	// boilerplate for visitors

	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
