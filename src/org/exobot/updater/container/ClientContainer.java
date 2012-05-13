package org.exobot.updater.container;

import java.awt.Canvas;
import org.exobot.updater.Task;
import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddGetterProcessor;
import org.exobot.updater.processor.AddInterfaceProcessor;
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
	public Class<?>[] getDependencies() {
		return new Class<?>[]{KeyboardContainer.class, MouseContainer.class};
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
	public void run(final String name, final ClassNode cn) {
		if (cn.name.equals("client")) {
			addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Client"));
		}
		final String keyboardSuperDesc = "L" + Updater.getInstance().getClasses().get("Keyboard").superName + ";";
		final String mouseSuperDesc = "L" + Updater.getInstance().getClasses().get("Mouse").superName + ";";
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == 0) {
				continue;
			}
			if ((fn.access & Opcodes.ACC_VOLATILE) != 0 && fn.desc.equals("I")) {
				final int multiplier = new MultiplierSearch(cn.name, fn.name).getMultiplier();
				if (multiplier == -1) {
					continue;
				}
				addProcessor(new AddGetterProcessor(this, "getGUIRSInterfaceIndex", fn.desc, cn.name, fn.name, fn.desc, true, multiplier));
			} else if (fn.desc.equals(Type.getDescriptor(Canvas.class))) {
				addProcessor(new AddGetterProcessor(this, "getCanvas", fn.desc, cn.name, fn.name, fn.desc, true));
			} else if (fn.desc.equals(keyboardSuperDesc)) {
				addProcessor(new AddGetterProcessor(this, "getKeyboard", "L" + ACCESSOR_DESC + "input/Keyboard;", cn.name, fn.name, fn.desc, true));
			} else if (fn.desc.equals(mouseSuperDesc)) {
				addProcessor(new AddGetterProcessor(this, "getMouse", "L" + ACCESSOR_DESC + "input/Mouse;", cn.name, fn.name, fn.desc, true));
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		return true;
	}
}