package org.exobot.updater.container;

import java.util.Hashtable;
import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.updater.processor.AddMethodProcessor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Ramus
 */
public class RenderContainer extends HookContainer implements Task {

	@Override
	public Class<?>[] getDependencies() {
		return new Class<?>[]{DataContainer.class};
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
		Updater.getInstance().getClasses().set("Render", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Render"));
		final String dataDesc = "L" + Updater.getInstance().getClasses().get("Data").name + ";";
		for (final ClassNode node : Updater.getInstance().getClasses().values()) {
			if (!node.superName.equals(cn.name)) {
				continue;
			}
			int nonstatic = 0;
			int data = 0;
			FieldNode dataField = null;
			for (final FieldNode fn : node.fields) {
				if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
					continue;
				}
				nonstatic++;
				if (fn.desc.equals(dataDesc)) {
					dataField = fn;
					data++;
				}
			}
			if (nonstatic != 1 || data != 1) {
				continue;
			}
			addProcessor(new AddMethodProcessor(this, "getData", "java/lang/Object", node.name, dataField.name, dataField.desc, false));
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if ((cn.access & Opcodes.ACC_ABSTRACT) != Opcodes.ACC_ABSTRACT || !cn.superName.equals("java/lang/Object")) {
			return false;
		}
		int privInts = 0;
		int hashTables = 0;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				continue;
			}
			if ((fn.access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE && fn.desc.equals(Type.INT_TYPE.getDescriptor())) {
				privInts++;
			} else if (fn.desc.equals(Type.getDescriptor(Hashtable.class))) {
				hashTables++;
			}
		}
		if (privInts != 1 || hashTables != 1) {
			return false;
		}
		for (final ClassNode node : Updater.getInstance().getClasses().values()) {
			if (node.superName.equals(cn.name)) {
				return true;
			}
		}
		return false;
	}
}