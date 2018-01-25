package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.CharacterConstantToken;
import tokens.Token;

public class CharacterConstantNode extends ParseNode {
	public CharacterConstantNode(Token token) {
		super(token);
		assert(token instanceof CharacterConstantToken);
	}
	public CharacterConstantNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
// attributes

	public int getValue() {
		return characterToken().getValue();
	}

	public CharacterConstantToken characterToken() {
		return (CharacterConstantToken)token;
	}

///////////////////////////////////////////////////////////
// accept a visitor

	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
