package parseTree.nodeTypes;

import parseTree.ParseNode;
import semanticAnalyzer.types.Type;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ParameterNode extends ParseNode {

	public ParameterNode(ParseNode node) {
		super(node);
	}
	public ParameterNode(Token token) {
		super(token);
	}
	
	public static ParameterNode make(Token token, Type paramType, ParseNode identifier) {
		ParameterNode node = new ParameterNode(token);
		node.setType(paramType);
		node.appendChild(identifier);
		return node;
	}
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
