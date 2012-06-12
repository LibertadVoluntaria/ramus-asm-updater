package org.exobot.updater.container;

import org.exobot.updater.Task;
import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Ramus
 */
public class CameraMatrixContainer extends HookContainer implements Task {

	@Override
	public int getGetters() {
		return 12;
	}

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public void execute(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("CameraMatrix", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "CameraMatrix"));
	}

	@Override
	public boolean isValid(final String name, final ClassNode cn) {
		if (!cn.superName.equals("java/lang/Object") || cn.interfaces.size() > 0) {
			return false;
		}
		int floatArr = 0;
		boolean foundInst = false;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) != 0) {
				if (fn.desc.equals("L" + cn.name + ";")) {
					foundInst = true;
				}
				continue;
			}
			if (fn.desc.equals("[F")) {
				floatArr++;
			}
		}
		return foundInst && floatArr == 1;
	}
}