package org.exobot.updater.container;

import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.updater.processor.AddMethodProcessor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ramus
 */
public class AnimableNodeContainer extends HookContainer implements Task {

	@Override
	public Class<?>[] getDependencies() {
		return new Class<?>[]{DataContainer.class};
	}

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
		Updater.getInstance().getClasses().set("AnimableNode", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "AnimableNode;"));
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				continue;
			}
			if (fn.desc.equals("L" + cn.name + ";")) {
				addProcessor(new AddMethodProcessor(this, "getNext", ACCESSOR_DESC + "AnimableNode;", cn.name, fn.name, fn.desc, false));
			} else {
				addProcessor(new AddMethodProcessor(this, "getAnimable", "java/lang/Object", cn.name, fn.name, fn.desc, false));
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		final String dataDesc = "L" + Updater.getInstance().getClasses().get("Data").name + ";";
		int selfInst = 0;
		int data = 0;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) != 0) {
				continue;
			}
			if (fn.desc.equals("L" + cn.name + ";")) {
				selfInst++;
			} else if (fn.desc.equals(dataDesc)) {
				data++;
			}
		}
		if (selfInst != 1 || data != 1) {
			return false;
		}
		int finals = 0;
		for (final MethodNode mn : cn.methods) {
			if (mn.access != Opcodes.ACC_FINAL) {
				continue;
			}
			finals++;
		}
		return finals == 1;
	}
}