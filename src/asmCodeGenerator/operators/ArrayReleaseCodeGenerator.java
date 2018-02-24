package asmCodeGenerator.operators;

import asmCodeGenerator.SimpleCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Labeller;

import static asmCodeGenerator.ASMConstants.*;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.MemoryManager;
import parseTree.ParseNode;

public class ArrayReleaseCodeGenerator implements SimpleCodeGenerator {
	public ASMCodeFragment generate(ParseNode node) {
		Labeller labeller = new Labeller("release-array");

		String ignoreDelete = labeller.newLabel("ignore-delete");
		String end = labeller.newLabel("end");

		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VOID);

		// TODO
		// [... arrayAddr]
		frag.add(Duplicate);
		frag.add(PushI, RECORD_STATUS_OFFSET);
		frag.add(Add);
		frag.add(Duplicate);
		frag.add(LoadI);
		frag.add(PushI, MASK_RECORD_CHECK_ALLOWS_DELETION);
		frag.add(BTAnd);		// [... arrayAddr statusLocation bitmaskResult]
		frag.add(JumpTrue, ignoreDelete);
		frag.add(Duplicate);
		frag.add(LoadI);
		frag.add(PushI, MASK_RECORD_SET_IS_DELETED);
		frag.add(BTOr);		// [... arrayAddr statusLocation newStatusValue]
		frag.add(StoreI);
		frag.add(Call, MemoryManager.MEM_MANAGER_DEALLOCATE);
		frag.add(Jump, end);

		frag.add(Label, ignoreDelete);	// [... arrayAddr statusLocation]
		frag.add(Pop);
		frag.add(Pop);
		frag.add(Label, end);

		return frag;
	}
}
