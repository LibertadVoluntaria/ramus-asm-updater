package org.exobot.updater.container;

import org.exobot.updater.Updater;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Ramus
 */
public class DataContainer extends HookContainer implements Task {

	public DataContainer() {
		setHidden(true);
	}

	@Override
	public void run(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("Data", cn);
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		boolean foundByte = false;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				continue;
			}
			if (!fn.desc.equals("B")) {
				return false;
			}
			foundByte = true;
		}
		if (!foundByte) {
			return false;
		}
		int subClasses = 0;
		for (final ClassNode node : Updater.getInstance().getClasses().values()) {
			if (node.superName.equals(cn.name)) {
				subClasses++;
			}
		}
		return subClasses == 9;
	}
}