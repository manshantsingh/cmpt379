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
	private ParseNode sizeExpression;
	private ArrayList<ParseNode> arrayList;
	private Array resultType;

	public ArrayNode(Token token) {
		super(token);
	}
	public ArrayNode(ParseNode node) {
		super(node);
	}

	public static ArrayNode make(Token token, Type type, ParseNode exp) {
		ArrayNode node = commonMake(token, type, Arrays.asList(exp));
		node.setSizeExpression(exp);
		return node;
	}

	public static ArrayNode make(Token token, Type type, ArrayList<ParseNode> list) {
		ArrayNode node = commonMake(token, type, list);
		node.setArrayList(list);
		return node;
	}

	private static ArrayNode commonMake(Token token, Type type, List<ParseNode> list) {
		ArrayNode node = new ArrayNode(token);
		node.setType(type);
		for(ParseNode n: list) {
			node.appendChild(n);
		}
		return node;
	}

////////////////////////////////////////////////////////////
// attributes

	public ParseNode getSizeExpression() {
		return sizeExpression;
	}

	private void setSizeExpression(ParseNode exp) {
		sizeExpression = exp;
	}

	public ArrayList<ParseNode> getArrayList(){
		return arrayList;
	}

	private void setArrayList(ArrayList<ParseNode> list) {
		arrayList = list;
	}

	public Array getType() {
		return resultType;
	}

	public void setType(Type type) {
		assert(type instanceof Array);
		resultType = (Array) type;
	}

///////////////////////////////////////////////////////////
// accept a visitor

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
