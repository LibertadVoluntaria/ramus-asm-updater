package org.exobot.updater.container;

import java.awt.*;
import java.util.*;
import org.exobot.updater.*;
import org.exobot.updater.processor.*;
import org.exobot.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * @author Ramus
 */
public class ClientContainer extends HookContainer implements Task {

	@Override
	public Class<?>[] getDependencies() {
		return new Class<?>[]{ KeyboardContainer.class, MouseContainer.class, ViewportContainer.class };
	}

	@Override
	public int getGetters() {
		return 7;
	}

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public void run(final String name, final ClassNode cn) {
		if (cn.name.equals("client")) {
			addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Client"));
			for (final MethodNode mn : cn.methods) {
				if (!mn.name.equals("<clinit>")) {
					continue;
				}
				final RIS ris = new RIS(mn);
				final Iterator<AbstractInsnNode[]> iterator = ris.nextPattern("ldc putstatic");
				if (!iterator.hasNext() || iterator.next() == null || !iterator.hasNext()) {
					break;
				}
				final FieldInsnNode fin = (FieldInsnNode) iterator.next()[1];
				final int multiplier = new MultiplierSearch(fin.owner, fin.name).getMultiplier();
				if (multiplier == -1) {
					continue;
				}
				addProcessor(new AddGetterProcessor(this, "getConnectionState", fin.desc, fin.owner, fin.name, fin.desc, !fin.owner.equals("client"), multiplier));
				break;
			}
		}
		final String keyboardSuperDesc = "L" + Updater.getInstance().getClasses().get("Keyboard").superName + ";";
		final String mouseSuperDesc = "L" + Updater.getInstance().getClasses().get("Mouse").superName + ";";
		final String renderData = Updater.getInstance().getClasses().get("Viewport").name;
		final String renderDesc = "L" + Updater.getInstance().getClasses().get("Render").name + ";";
		outer:
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == 0) {
				continue;
			}
			if (cn.name.equals(renderData)) {
				addProcessor(new AddGetterProcessor(this, "getRenderData", "L" + ACCESSOR_DESC + "RenderData;", cn.name, fn.name, fn.desc, !cn.name.equals("client")));
			} else if ((fn.access & Opcodes.ACC_VOLATILE) != 0 && fn.desc.equals("I")) {
				final int multiplier = new MultiplierSearch(cn.name, fn.name).getMultiplier();
				if (multiplier == -1) {
					continue;
				}
				addProcessor(new AddGetterProcessor(this, "getMainWidgetIndex", fn.desc, cn.name, fn.name, fn.desc, !cn.name.equals("client"), multiplier));
			} else if (fn.desc.equals(Type.getDescriptor(Canvas.class))) {
				addProcessor(new AddGetterProcessor(this, "getCanvas", fn.desc, cn.name, fn.name, fn.desc, !cn.name.equals("client")));
			} else if (fn.desc.equals(keyboardSuperDesc)) {
				addProcessor(new AddGetterProcessor(this, "getKeyboard", "L" + ACCESSOR_DESC + "input/Keyboard;", cn.name, fn.name, fn.desc, !cn.name.equals("client")));
			} else if (fn.desc.equals(mouseSuperDesc)) {
				addProcessor(new AddGetterProcessor(this, "getMouse", "L" + ACCESSOR_DESC + "input/Mouse;", cn.name, fn.name, fn.desc, !cn.name.equals("client")));
			} else if (fn.desc.equals(renderDesc)) {
				final ClassNode client = Updater.getInstance().getClasses().get("client");
				for (final MethodNode mn : client.methods) {
					if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
						continue;
					}
					final RIS ris = new RIS(mn);
					FieldInsnNode fin;
					while ((fin = ris.next(FieldInsnNode.class, Opcodes.GETSTATIC)) != null) {
						if (fin.owner.equals(cn.name) && fin.name.equals(fn.name) && fin.desc.equals(fn.desc)) {
							addProcessor(new AddGetterProcessor(this, "getViewport", "L" + ACCESSOR_DESC + "Viewport;", cn.name, fn.name, fn.desc, !cn.name.equals("client")));
							continue outer;
						}
					}
				}
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		return true;
	}
}