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
public class InteractableContainer extends HookContainer implements Task {

	@Override
	public Class<?>[] getDependencies() {
		return new Class<?>[]{EntityNodeContainer.class};
	}

	@Override
	public int getGetters() {
		return 1;
	}

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public void run(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("Interactable", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Interactable"));
		outer:
		for (final MethodNode mn : cn.methods) {
			if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE)) != 0) {
				continue;
			}
			final RIS ris = new RIS(mn);
			FieldInsnNode fin;
			while ((fin = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)) != null) {
				if (fin.owner.equals(cn.name) && fin.desc.equals("B")) {
					addProcessor(new AddGetterProcessor(this, "getPlane", fin.desc, cn.name, fin.name, fin.desc, false));
					break outer;
				}
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if (!cn.superName.equals(Updater.getInstance().getClasses().get("EntityNode").name)) {
			return false;
		}
		int selfInst = 0;
		int ints = 0;
		int bytes = 0;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) != 0) {
				continue;
			}
			if (fn.desc.equals("L" + cn.name + ";")) {
				selfInst++;
			} else if (fn.desc.equals("I")) {
				ints++;
			} else if (fn.desc.equals("B")) {
				bytes++;
			}
		}
		return selfInst == 1 && ints == 1 && bytes == 2;
	}
}