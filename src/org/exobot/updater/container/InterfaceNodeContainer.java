package org.exobot.updater.container;

import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.updater.processor.AddMethodProcessor;
import org.exobot.util.RIS;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * @author Ramus
 */
public class InterfaceNodeContainer extends HookContainer implements Task {

	@Override
	public Class<?>[] getDependencies() {
		return new Class<?>[]{DataContainer.class, NodeContainer.class};
	}

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
		Updater.getInstance().getClasses().set("InterfaceNode", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "InterfaceNode;"));
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				continue;
			}
			if (fn.desc.equals("L" + Updater.getInstance().getClasses().get("Data").name + ";")) {
				addProcessor(new AddMethodProcessor(this, "getData", "Ljava/lang/Object;", cn.name, fn.name, fn.desc, false));
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if (!cn.superName.equals(Updater.getInstance().getClasses().get("Node").name) || cn.interfaces.size() > 0) {
			return false;
		}
		final String dataDesc = "L" + Updater.getInstance().getClasses().get("Data").name + ";";
		int nonstatic = 0;
		int data = 0;
		FieldNode dataField = null;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				continue;
			}
			nonstatic++;
			if (fn.desc.equals(dataDesc)) {
				data++;
				dataField = fn;
			}
		}
		if (nonstatic != 1 || data != 1) {
			return false;
		}
		final ClassNode client = Updater.getInstance().getClasses().get("client");
		for (final MethodNode mn : client.methods) {
			final RIS ris = new RIS(mn);
			final TypeInsnNode cc;
			final FieldInsnNode gf;
			if ((cc = ris.next(TypeInsnNode.class, Opcodes.CHECKCAST)) == null || (gf = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)) == null) {
				continue;
			}
			if (!cc.desc.equals(cn.name) || !gf.name.equals(dataField.name) || !gf.desc.equals(dataField.desc) || !gf.owner.equals(cn.name)) {
				continue;
			}
			return true;
		}
		return false;
	}
}