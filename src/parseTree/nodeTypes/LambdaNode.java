package parseTree.nodeTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import asmCodeGenerator.Labeller;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.Type;
import tokens.StringConstantToken;
import tokens.Token;

public class LambdaNode extends ParseNode {
	
	private String functionLocationLabel, returnCodeLabel, endLabel;

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
		node.setType(new LambdaType(paramsType, returnType));
		node.appendChild(block);

		Labeller labeller = new Labeller("Lambda");
		node.functionLocationLabel = labeller.newLabel("function-location");
		node.returnCodeLabel = labeller.newLabel("return-code");
		node.endLabel = labeller.newLabel("end");

		return node;
	}

	public String getFunctionLocationLabel() {
		return functionLocationLabel;
	}
	public String getReturnCodeLabel() {
		return returnCodeLabel;
	}
	public String getEndLabel() {
		return endLabel;
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
}
