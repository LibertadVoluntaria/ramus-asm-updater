package org.exobot.updater.container;

import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.updater.processor.AddMethodProcessor;
import org.exobot.util.RIS;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ramus
 */
public class NodeSubContainer extends HookContainer implements Task {

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public int getMethods() {
		return 2;
	}

	@Override
	public void run(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("NodeSub", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "NodeSub"));
		String getNext = null;
		for (final MethodNode mn : cn.methods) {
			final RIS ris = new RIS(mn);
			FieldInsnNode fin;
			if ((fin = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)) != null) {
				addProcessor(new AddMethodProcessor(this, "getNextSub", "L" + ACCESSOR_DESC + "NodeSub;", cn.name, getNext = fin.name, fin.desc, false));
				break;
			}
		}
		if (getNext == null) {
			return;
		}
		for (final FieldNode fn : cn.fields) {
			if (fn.name.equals(getNext) || !fn.desc.equals("J")) {
				continue;
			}
			addProcessor(new AddMethodProcessor(this, "getPrevSub", "L" + ACCESSOR_DESC + "NodeSub;", cn.name, fn.name, fn.desc, false));
		}

	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if (!cn.superName.equals(Updater.getInstance().getClasses().get("Node").name)) {
			return false;
		}
		int nonstatic = 0;
		int subnodes = 0;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				break;
			}
			if (fn.desc.equals("L" + cn.name + ";")) {
				subnodes++;
			}
			nonstatic++;
		}
		return nonstatic == 3 && subnodes == 2;
	}
}