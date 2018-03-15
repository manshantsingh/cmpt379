package parseTree.nodeTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.Type;
import tokens.StringConstantToken;
import tokens.Token;

public class LambdaNode extends ParseNode {
	
	private LambdaType lamdaType;

	public LambdaNode(Token token) {
		super(token);
	}

	public LambdaNode(ParseNode node) {
		super(node);
	}

	public static LambdaNode make(Token token, List<ParameterNode> params, Type returnType, ParseNode block) {
		LambdaNode node = new LambdaNode(token);
		ArrayList<Type> paramsType = new ArrayList<Type>();
		for(ParameterNode n: params) {
			node.appendChild(n);
			paramsType.add(n.getType());
		}
		node.lamdaType = new LambdaType(paramsType, returnType);
		node.appendChild(block);
		return node;
	}

////////////////////////////////////////////////////////////
// attributes


///////////////////////////////////////////////////////////
// accept a visitor

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}

	public LambdaType getLamdaType() {
		return lamdaType;
	}
}
