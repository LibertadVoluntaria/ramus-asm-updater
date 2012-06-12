package org.exobot.updater.container;

import java.util.Iterator;
import org.exobot.updater.Task;
import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddGetterProcessor;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.util.RIS;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * @author Ramus
 */
public class EntityNodeContainer extends HookContainer implements Task {

	@Override
	public int getGetters() {
		return 2;
	}

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public void execute(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("EntityNode", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "EntityNode"));
		for (final MethodNode mn : cn.methods) {
			if ((mn.access & Opcodes.ACC_STATIC) != 0) {
				continue;
			}
			final RIS ris = new RIS(mn);
			final Iterator<AbstractInsnNode[]> iterator = ris.nextPattern("getfield aload getfield putfield");
			AbstractInsnNode[] nodes = null;
			while (iterator.hasNext()) {
				nodes = iterator.next();
			}
			if (nodes == null) {
				continue;
			}
			final FieldInsnNode prev = (FieldInsnNode) nodes[0];
			final FieldInsnNode next = (FieldInsnNode) nodes[2];
			addProcessor(new AddGetterProcessor(this, "getPrevious", "L" + ACCESSOR_DESC + "EntityNode;", cn.name, prev.name, prev.desc, false));
			addProcessor(new AddGetterProcessor(this, "getNext", "L" + ACCESSOR_DESC + "EntityNode;", cn.name, next.name, next.desc, false));
			break;
		}
	}

	@Override
	public boolean isValid(final String name, final ClassNode cn) {
		if (!cn.superName.equals("java/lang/Object")) {
			return false;
		}
		int selfInst = 0;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) != 0) {
				continue;
			}
			if (fn.desc.equals("L" + cn.name + ";")) {
				selfInst++;
			}
		}
		return selfInst == 3;
	}
}