package parseTree;

import semanticAnalyzer.signatures.FunctionSignature;
import tokens.Token;

public abstract class SomeArbitaryOperatorNode extends ParseNode {

	private FunctionSignature signature = FunctionSignature.nullInstance();

	public FunctionSignature getSignature() {
		return signature;
	}

	public void setSignature(FunctionSignature signature) {
		this.signature = signature;
		setType(signature.resultType());
	}

	public SomeArbitaryOperatorNode(Token token) {
		super(token);
	}
	public SomeArbitaryOperatorNode(ParseNode node) {
		super(node);
	}

}
