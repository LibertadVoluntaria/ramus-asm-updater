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
public class NodeContainer extends HookContainer implements Task {

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public int getMethods() {
		return 3;
	}

	@Override
	public void run(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("Node", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Node"));
		String getNext = null;
		for (final MethodNode mn : cn.methods) {
			if (!mn.desc.endsWith(")Z")) {
				continue;
			}
			final FieldInsnNode fin = new RIS(mn).next(FieldInsnNode.class, Opcodes.GETFIELD);
			addProcessor(new AddMethodProcessor(this, "getNext", "L" + ACCESSOR_DESC + "Node;", cn.name, getNext = fin.name, fin.desc, false));
			break;
		}
		if (getNext == null) {
			return;
		}
		for (final FieldNode fn : cn.fields) {
			if (fn.desc.equals("J")) {
				addProcessor(new AddMethodProcessor(this, "getId", "J", cn.name, fn.name, "J", false));
			} else if (!fn.name.equals(getNext)) {
				addProcessor(new AddMethodProcessor(this, "getPrevious", "L" + ACCESSOR_DESC + "Node;", cn.name, fn.name, fn.desc, false));
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if (!cn.superName.equals("java/lang/Object")) {
			return false;
		}
		int nonstatic = 0;
		int selfInst = 0;
		int longs = 0;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				break;
			}
			if (fn.desc.equals("L" + cn.name + ";")) {
				selfInst++;
			} else if (fn.desc.equals("J")) {
				longs++;
			}
			nonstatic++;
		}
		return nonstatic == 3 && selfInst == 2 && longs == 1;
	}
}