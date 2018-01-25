package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class AssignmentNode extends ParseNode {

	public AssignmentNode(Token token) {
		super(token);
	}

	public AssignmentNode(ParseNode node) {
		super(node);
	}

	////////////////////////////////////////////////////////////
	// convenience factory

	public static AssignmentNode withChildren(ParseNode declaredName, ParseNode initializer) {
		AssignmentNode node = new AssignmentNode(declaredName.getToken());
		node.appendChild(declaredName);
		node.appendChild(initializer);
		return node;
	}


	///////////////////////////////////////////////////////////
	// boilerplate for visitors

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
