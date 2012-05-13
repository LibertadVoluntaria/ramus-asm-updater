package org.exobot.updater.container;

import java.util.Hashtable;
import org.exobot.updater.Task;
import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Ramus
 */
public class RenderContainer extends HookContainer implements Task {

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public void run(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("Render", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Render"));
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if ((cn.access & Opcodes.ACC_ABSTRACT) != Opcodes.ACC_ABSTRACT || !cn.superName.equals("java/lang/Object")) {
			return false;
		}
		int ints = 0;
		int hashTables = 0;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				continue;
			}
			if (fn.desc.equals(Type.INT_TYPE.getDescriptor())) {
				ints++;
			} else if (fn.desc.equals(Type.getDescriptor(Hashtable.class))) {
				hashTables++;
			}
		}
		if (ints != 2 || hashTables != 1) {
			return false;
		}
		int count = 0;
		for (final ClassNode node : Updater.getInstance().getClasses().values()) {
			if (node.superName.equals(cn.name)) {
				count++;
			}
		}
		return count == 4;
	}
}