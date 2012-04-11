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
public class InteractableDataContainer extends HookContainer implements Task {

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
		Updater.getInstance().getClasses().set("InteractableData", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "InteractableData;"));
		for (final FieldNode fn : cn.fields) {
			if (fn.desc.equals("L" + Updater.getInstance().getClasses().get("InteractableLocation").name + ";")) {
				addProcessor(new AddMethodProcessor(this, "getLocation", fn.desc, cn.name, fn.name, fn.desc, false));
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if (!cn.superName.equals("java/lang/Object") || cn.interfaces.size() > 0 || cn.fields.size() != 2) {
			return false;
		}
		int locations = 0;
		FieldNode locField = null;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				return false;
			}
			if (fn.desc.equals("L" + Updater.getInstance().getClasses().get("InteractableLocation").name + ";")) {
				locField = fn;
				locations++;
			}
		}
		if (locations != 1) {
			return false;
		}
		final ClassNode client = Updater.getInstance().getClasses().get("client");
		for (final MethodNode mn : client.methods) {
			final RIS ris = new RIS(mn);
			FieldInsnNode fin;
			while ((fin = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)) != null) {
				if (!fin.desc.equals(locField.desc) || !fin.name.equals(locField.name) || !fin.owner.equals(cn.name)) {
					continue;
				}
				return true;
			}
		}
		return false;
	}
}