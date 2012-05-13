package org.exobot.updater.container;

import java.awt.Canvas;
import java.util.Iterator;
import org.exobot.updater.Task;
import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddGetterProcessor;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.util.MultiplierSearch;
import org.exobot.util.RIS;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * @author Ramus
 */
public class ClientContainer extends HookContainer implements Task {

	@Override
	public Class<?>[] getDependencies() {
		return new Class<?>[]{KeyboardContainer.class, MouseContainer.class, RenderDataContainer.class};
	}

	@Override
	public int getGetters() {
		return 6;
	}

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public void run(final String name, final ClassNode cn) {
		final String renderData = Updater.getInstance().getClasses().get("RenderData").name;
		if (cn.name.equals("client")) {
			addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Client"));
			for (final MethodNode mn : cn.methods) {
				if (!mn.name.equals("<clinit>")) {
					continue;
				}
				final RIS ris = new RIS(mn);
				final Iterator<AbstractInsnNode[]> iterator = ris.nextPattern("ldc putstatic");
				if (!iterator.hasNext()) {
					break;
				}
				iterator.next();
				if (!iterator.hasNext()) {
					break;
				}
				final FieldInsnNode fin = (FieldInsnNode) iterator.next()[1];
				final int multiplier = new MultiplierSearch(fin.owner, fin.name).getMultiplier();
				if (multiplier == -1) {
					continue;
				}
				addProcessor(new AddGetterProcessor(this, "getLoginIndex", fin.desc, fin.owner, fin.name, fin.desc, true, multiplier));
				break;
			}
		}
		final String keyboardSuperDesc = "L" + Updater.getInstance().getClasses().get("Keyboard").superName + ";";
		final String mouseSuperDesc = "L" + Updater.getInstance().getClasses().get("Mouse").superName + ";";
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == 0) {
				continue;
			}
			if (cn.name.equals(renderData)) {
				addProcessor(new AddGetterProcessor(this, "getRenderData", "L" + ACCESSOR_DESC + "RenderData;", cn.name, fn.name, fn.desc, true));
			} else if ((fn.access & Opcodes.ACC_VOLATILE) != 0 && fn.desc.equals("I")) {
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