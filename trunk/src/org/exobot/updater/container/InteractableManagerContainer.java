package org.exobot.updater.container;

import java.util.Iterator;
import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.updater.processor.AddMethodProcessor;
import org.exobot.util.RIS;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * @author Ramus
 */
public class InteractableManagerContainer extends HookContainer implements Task {

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
		Updater.getInstance().getClasses().set("InteractableManager", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "InteractableManager;"));
		final String dataDesc = "L" + Updater.getInstance().getClasses().get("InteractableData").name + ";";
		for (final MethodNode mn : cn.methods) {
			if ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC || !mn.desc.equals("()" + dataDesc)) {
				continue;
			}
			final RIS ris = new RIS(mn);
			if (ris.next(Opcodes.PUTFIELD) == null) {
				continue;
			}
			final Iterator<AbstractInsnNode[]> iterator = ris.nextPattern("aload getfield areturn");
			if (!iterator.hasNext()) {
				continue;
			}
			final FieldInsnNode fin = (FieldInsnNode) iterator.next()[1];
			if (!fin.desc.equals(dataDesc)) {
				continue;
			}
			addProcessor(new AddMethodProcessor(this, "getData", "L" + ACCESSOR_DESC + "InteractableData;", cn.name, fin.name, fin.desc, false));
			break;
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if (!cn.superName.equals("java/lang/Object") || cn.interfaces.size() > 0 || cn.fields.size() != 10) {
			return false;
		}
		int data = 0;
		int selfInst = 0;
		int booleans = 0;
		int objects = 0;
		final String dataDesc = "L" + Updater.getInstance().getClasses().get("InteractableData").name + ";";
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				return false;
			}
			if (fn.desc.equals(dataDesc)) {
				data++;
			} else if (fn.desc.equals("L" + cn.name + ";")) {
				selfInst++;
			} else if (fn.desc.equals("Z")) {
				booleans++;
			} else {
				objects++;
			}
		}
		return data == 3 && selfInst == 3 && booleans == 3 && objects == 1;
	}
}