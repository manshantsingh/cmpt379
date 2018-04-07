package parseTree.nodeTypes;

import java.util.ArrayList;
import java.util.List;

import lexicalAnalyzer.Keyword;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ProgramNode extends ParseNode {

	public ProgramNode(Token token) {
		super(token);
	}
	public ProgramNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// no attributes

	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
	
	public void rearrangeChildren() {
		ArrayList<ParseNode> first = new ArrayList<>();
		ArrayList<ParseNode> last = new ArrayList<>();
		for(ParseNode child: children) {
			if(child instanceof DeclarationNode && !child.getToken().isLextant(Keyword.FUNC)) {
				first.add(child);
			}
			else {
				last.add(child);
			}
		}
		for(ParseNode child: last) {
			first.add(child);
		}
		children = first;
	}
}
