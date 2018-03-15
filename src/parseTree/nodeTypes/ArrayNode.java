package parseTree.nodeTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.Type;
import tokens.StringConstantToken;
import tokens.Token;

public class ArrayNode extends ParseNode {
	private boolean newDeclaration;

	public ArrayNode(Token token) {
		super(token);
	}
	public ArrayNode(ParseNode node) {
		super(node);
	}

	public static ArrayNode make(Token token, Type type, ParseNode exp) {
		ArrayNode node = commonMake(token, Arrays.asList(exp));
		node.setType(type);
		node.setNewDeclaration(true);
		return node;
	}

	public static ArrayNode make(Token token, ArrayList<ParseNode> list) {
		ArrayNode node = commonMake(token, list);
		node.setNewDeclaration(false);
		return node;
	}

	private static ArrayNode commonMake(Token token, List<ParseNode> list) {
		ArrayNode node = new ArrayNode(token);
		for(ParseNode n: list) {
			node.appendChild(n);
		}
		return node;
	}

////////////////////////////////////////////////////////////
// attributes

	public boolean isNewDeclaration() {
		return newDeclaration;
	}

	public void setNewDeclaration(boolean isNew) {
		newDeclaration = isNew;
	}

///////////////////////////////////////////////////////////
// accept a visitor

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
