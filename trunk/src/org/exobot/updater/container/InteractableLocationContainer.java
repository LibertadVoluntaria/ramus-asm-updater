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
public class InteractableLocationContainer extends HookContainer implements Task {


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
		Updater.getInstance().getClasses().set("InteractableLocation", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "InteractableLocation;"));
		String xName = null;
		String otherF = null;
		for (final MethodNode mn : cn.methods) {
			if ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC || !mn.desc.endsWith(")V")) {
				continue;
			}
			final RIS ris = new RIS(mn);
			if (ris.next(Opcodes.GETFIELD) == null || ris.next(Opcodes.FSTORE) == null || ris.next(Opcodes.FMUL) == null || ris.next(Opcodes.FADD) == null) {
				continue;
			}
			ris.setPosition(0);
			final FieldInsnNode xField = ris.next(FieldInsnNode.class, Opcodes.GETFIELD);
			addProcessor(new AddMethodProcessor(this, "getX", "F", cn.name, xName = xField.name, "F", false));
			final FieldInsnNode fin = ris.next(FieldInsnNode.class, Opcodes.GETFIELD);
			otherF = fin.name;
			break;
		}
		if (otherF == null || xName == null) {
			return;
		}
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				continue;
			}
			if (fn.desc.equals("F") && !fn.name.equals(otherF) && !fn.name.equals(xName)) {
				addProcessor(new AddMethodProcessor(this, "getY", "F", cn.name, fn.name, "F", false));
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if (!cn.superName.equals("java/lang/Object") || cn.interfaces.size() > 0) {
			return false;
		}
		int floats = 0;
		int selfInst = 0;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				if (fn.desc.equals("[L" + cn.name + ";")) {
					selfInst++;
				}
				continue;
			}
			if (!fn.desc.equals("F")) {
				return false;
			}
			floats++;
		}
		return selfInst == 1 && floats == 3;
	}
}