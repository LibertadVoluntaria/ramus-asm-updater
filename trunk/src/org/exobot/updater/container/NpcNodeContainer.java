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
public class NpcNodeContainer extends HookContainer implements Task {

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
		Updater.getInstance().getClasses().set("NpcNode", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "NpcNode"));
		final FieldNode fn = cn.fields.get(0);
		addProcessor(new AddMethodProcessor(this, "getData", "Ljava/lang/Object;", cn.name, fn.name, fn.desc, false));
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if (!cn.superName.equals(Updater.getInstance().getClasses().get("Node").name) || cn.fields.size() != 1 || cn.methods.size() != 1) {
			return false;
		}
		final FieldNode fn = cn.fields.get(0);
		final String dataDesc = "L" + Updater.getInstance().getClasses().get("Data").name + ";";
		if (fn.access > 0 || !fn.desc.equals(dataDesc)) {
			return false;
		}
		final MethodNode init = cn.methods.get(0);
		if (init.access > 0 || !init.name.equals("<init>")) {
			return false;
		}
		final ClassNode client = Updater.getInstance().getClasses().get("client");
		for (final MethodNode mn : client.methods) {
			final RIS ris = new RIS(mn);
			FieldInsnNode fin;
			while ((fin = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)) != null) {
				if (!fin.desc.equals(fn.desc) || !fin.name.equals(fn.name) || !fin.owner.equals(cn.name)) {
					continue;
				}
				return true;
			}
		}
		return false;
	}
}