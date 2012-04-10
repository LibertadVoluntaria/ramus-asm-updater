package org.exobot.updater.container;

import java.awt.Canvas;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.updater.processor.AddMethodProcessor;
import org.exobot.util.MultiplierSearch;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Ramus
 */
public class ClientContainer extends HookContainer implements Task {

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
		if (cn.name.equals("client")) {
			addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Client"));
		}
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) {
				continue;
			}
			if ((fn.access & Opcodes.ACC_VOLATILE) == Opcodes.ACC_VOLATILE && fn.desc.equals("I")) {
				final int multiplier = new MultiplierSearch(cn.name, fn.name).getMultiplier();
				if (multiplier == -1) {
					continue;
				}
				addProcessor(new AddMethodProcessor(this, "getGUIRSInterfaceIndex", fn.desc, cn.name, fn.name, fn.desc, true, multiplier));
			} else if (fn.desc.equals(Type.getDescriptor(Canvas.class))) {
				addProcessor(new AddMethodProcessor(this, "getCanvas", fn.desc, cn.name, fn.name, fn.desc, true));
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		return true;
	}
}