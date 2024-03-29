package org.omnibot.updater.container;

import org.omnibot.updater.Task;
import org.omnibot.updater.Updater;
import org.omnibot.updater.processor.AddGetterProcessor;
import org.omnibot.updater.processor.AddInterfaceProcessor;
import org.omnibot.util.RIS;
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
		return new Class<?>[]{EntityContainer.class};
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
	public void execute(final String name, final ClassNode cn) {
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
	public boolean isValid(final String name, final ClassNode cn) {
		if (!cn.superName.equals(Updater.getInstance().getClasses().get("Entity").name)) {
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