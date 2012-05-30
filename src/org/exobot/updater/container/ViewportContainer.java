package org.exobot.updater.container;

import org.exobot.updater.*;
import org.exobot.updater.processor.*;
import org.exobot.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * @author Ramus
 */
public class ViewportContainer extends HookContainer implements Task {

	private final String[] FIELDS = { "XX", "XY", "XZ", "XOff", "YX", "YY", "YZ", "YOff", "ZX", "ZY", "ZZ", "ZOff" };

	@Override
	public int getGetters() {
		return 12;
	}

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public void run(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("Viewport", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Viewport"));
		for (final MethodNode mn : cn.methods) {
			if (!mn.name.equals("toString")) {
				continue;
			}
			final RIS ris = new RIS(mn);
			FieldInsnNode fin;
			int i = 0;
			while (i < 12 && (fin = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)) != null) {
				addProcessor(new AddGetterProcessor(this, "get" + FIELDS[i++], "F", cn.name, fin.name, "F", false));
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if (!cn.superName.equals("java/lang/Object") || cn.interfaces.size() > 0) {
			return false;
		}
		int floats = 0;
		boolean foundInst = false;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) != 0) {
				if (fn.desc.equals("L" + cn.name + ";")) {
					foundInst = true;
				}
				continue;
			}
			if (fn.desc.equals("F")) {
				floats++;
			}
		}
		return foundInst && floats == 12;
	}
}