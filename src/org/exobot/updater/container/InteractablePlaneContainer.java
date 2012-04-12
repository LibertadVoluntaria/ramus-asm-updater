package org.exobot.updater.container;

import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.updater.processor.AddMethodProcessor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Ramus
 */
public class InteractablePlaneContainer extends HookContainer implements Task {

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
		return 1;
	}

	@Override
	public void run(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("InteractablePlane", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "InteractablePlane;"));
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				continue;
			}
			if (fn.desc.equals("B")) {
				addProcessor(new AddMethodProcessor(this, "getInteractablePlane", fn.desc, cn.name, fn.name, fn.desc, false));
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if (!cn.superName.equals(Updater.getInstance().getClasses().get("Data").name)) {
			return false;
		}
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				continue;
			}
			if (fn.desc.equals("B")) {
				return true;
			}
		}
		return false;
	}
}