package org.exobot.updater.container;

import org.exobot.updater.Task;
import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddGetterProcessor;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.util.RIS;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ramus
 */
public class AnimableContainer extends HookContainer implements Task {

	@Override
	public Class<?>[] getDependencies() {
		return new Class<?>[]{InteractableContainer.class};
	}

	@Override
	public int getGetters() {
		return 4;
	}

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public void execute(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("Animable", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Animable"));
		for (final MethodNode mn : cn.methods) {
			if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE)) != 0 || !mn.desc.equals("()Z")) {
				continue;
			}
			final RIS ris = new RIS(mn);
			FieldInsnNode fin;
			for (int i = 0; i < 4; i++) {
				if ((fin = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)) == null) {
					continue;
				}
				addProcessor(new AddGetterProcessor(this, "get" + ((i & 1) != 0 ? "Max" : "Min") + (char) (Math.max(0, Math.min(1, i - 1)) + 88), fin.desc, cn.name, fin.name, fin.desc, false));
			}
			break;
		}
	}

	@Override
	public boolean isValid(final String name, final ClassNode cn) {
		if (!cn.superName.equals(Updater.getInstance().getClasses().get("Interactable").name) || (cn.access & Opcodes.ACC_ABSTRACT) == 0) {
			return false;
		}
		int shorts = 0;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) != 0) {
				continue;
			}
			if (fn.desc.equals("S")) {
				shorts++;
			}
		}
		return shorts == 4;
	}
}