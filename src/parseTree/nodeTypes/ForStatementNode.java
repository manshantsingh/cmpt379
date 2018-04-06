package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import asmCodeGenerator.Labeller;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class ForStatementNode extends ParseNode {

	private String topLabel, endLabel;
	private boolean byIndex;

	public ForStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.FOR));

		Labeller labeller = new Labeller("for");
		topLabel = labeller.newLabel("top");
		endLabel = labeller.newLabel("end");
	}

	public ForStatementNode(ParseNode node) {
		super(node);

		assert(node instanceof ForStatementNode);
		ForStatementNode forNode = (ForStatementNode) node;
		topLabel = forNode.getTopLabel();
		endLabel = forNode.getEndLabel();
	}

	public static ForStatementNode make(Token token, boolean byIndex, ParseNode identifier, ParseNode record, ParseNode blockCode) {
		ForStatementNode node = new ForStatementNode(token);
		node.byIndex = byIndex;
		node.appendChild(identifier);
		node.appendChild(record);
		node.appendChild(blockCode);
		return node;
	}

	public String getTopLabel() {
		return topLabel;
	}
	public String getEndLabel() {
		return endLabel;
	}
	public boolean isByIndex() {
		return byIndex;
	}


	////////////////////////////////////////////////////////////
	// attributes


	///////////////////////////////////////////////////////////
	// boilerplate for visitors

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
//		visitChildren(visitor);
		int n = nChildren();
		for(int i=0;i<n;i++) {
			child(i).accept(visitor);
		}
		visitor.visitLeave(this);
	}
}
