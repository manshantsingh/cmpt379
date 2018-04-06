package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import symbolTable.Scope;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class DeclarationNode extends ParseNode {

	private Scope declarationScope;
	private boolean isStatic, isConstant;

	public DeclarationNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.CONST, Keyword.VAR, Keyword.FUNC));
	}

	public DeclarationNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes

	public Lextant getDeclarationType() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}
	

	public boolean getIsStatic() {
		return isStatic;
	}
	public boolean getIsConstant() {
		return isConstant;
	}
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static DeclarationNode withChildren(Token token, ParseNode declaredName, ParseNode initializer, boolean isStatic, boolean isConstant) {
		DeclarationNode node = new DeclarationNode(token);
		node.appendChild(declaredName);
		node.appendChild(initializer);
		node.isStatic=isStatic;
		node.isConstant=isConstant;
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
