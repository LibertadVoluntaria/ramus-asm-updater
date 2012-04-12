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
public class RenderDataContainer extends HookContainer implements Task {

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
		Updater.getInstance().getClasses().set("RenderData", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "RenderData"));
		for (final FieldNode fn : cn.fields) {
			if (fn.desc.equals("[F")) {
				addProcessor(new AddMethodProcessor(this, "getFloats", "[F", cn.name, fn.name, "[F", false));
				break;
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if (!cn.superName.equals("java/lang/Object") || cn.interfaces.size() > 0 || cn.fields.size() != 2) {
			return false;
		}
		boolean foundFloats = false;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				if (!fn.desc.equals("L" + cn.name + ";")) {
					return false;
				}
				continue;
			}
			if (!fn.desc.equals("[F")) {
				return false;
			}
			foundFloats = true;
		}
		return foundFloats;
	}
}