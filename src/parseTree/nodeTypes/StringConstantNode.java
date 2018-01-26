package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.StringConstantToken;
import tokens.Token;

public class StringConstantNode extends ParseNode {
	public StringConstantNode(Token token) {
		super(token);
		assert(token instanceof StringConstantToken);
	}
	public StringConstantNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
// attributes

	public String getValue() {
		return stringToken().getValue();
	}

	public StringConstantToken stringToken() {
		return (StringConstantToken)token;
	}

///////////////////////////////////////////////////////////
// accept a visitor

	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
