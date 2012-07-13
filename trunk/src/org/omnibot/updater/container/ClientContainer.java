package org.omnibot.updater.container;

import java.awt.Canvas;
import java.util.Iterator;
import org.omnibot.updater.Task;
import org.omnibot.updater.Updater;
import org.omnibot.updater.processor.AddGetterProcessor;
import org.omnibot.updater.processor.AddInterfaceProcessor;
import org.omnibot.util.MultiplierSearch;
import org.omnibot.util.RIS;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * @author Ramus
 */
public class ClientContainer extends HookContainer implements Task {

	@Override
	public Class<?>[] getDependencies() {
		return new Class<?>[]{CameraMatrixContainer.class, KeyboardContainer.class, MouseContainer.class,
				GraphicsToolkitContainer.class};
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
	public void execute(final String name, final ClassNode cn) {
		if (cn.name.equals("client")) {
			addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Client"));
		}
		final String toolkit = Updater.getInstance().getClasses().get("GraphicsToolkit").name;
		final String matrix = Updater.getInstance().getClasses().get("CameraMatrix").name;
		for (final MethodNode mn : cn.methods) {
			final RIS ris = new RIS(mn);
			if (cn.name.equals("client") && mn.name.equals("<clinit>")) {
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
				continue;
			}
			FieldInsnNode toolkitNode;
			FieldInsnNode fin;
			while ((fin = ris.next(FieldInsnNode.class, Opcodes.GETSTATIC)) != null) {
				if (!fin.desc.equals("L" + toolkit + ";")) {
					continue;
				}
				toolkitNode = fin;
				AbstractInsnNode ain = ris.next();
				if (ain.getOpcode() != Opcodes.GETSTATIC) {
					continue;
				}
				fin = (FieldInsnNode) ain;
				if (!fin.desc.equals("L" + matrix + ";")) {
					continue;
				}
				ain = ris.next();
				if (ain.getOpcode() != Opcodes.INVOKEVIRTUAL) {
					continue;
				}
				final MethodInsnNode min = (MethodInsnNode) ain;
				if (!min.owner.equals(toolkit) || !min.desc.equals("(L" + matrix + ";)V")) {
					continue;
				}
				addProcessor(new AddGetterProcessor(this, "getGraphicsToolkit", ACCESSOR_DESC + "GraphicsToolkit", toolkitNode.owner, toolkitNode.name, toolkitNode.desc, !toolkitNode.owner.equals("client")));
				break;
			}
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
				addProcessor(new AddGetterProcessor(this, "getMainWidgetIndex", fn.desc, cn.name, fn.name, fn.desc, !cn.name.equals("client"), multiplier));
			} else if (fn.desc.equals(Type.getDescriptor(Canvas.class))) {
				addProcessor(new AddGetterProcessor(this, "getCanvas", fn.desc, cn.name, fn.name, fn.desc, !cn.name.equals("client")));
			} else if (fn.desc.equals(keyboardSuperDesc)) {
				addProcessor(new AddGetterProcessor(this, "getKeyboard", "L" + ACCESSOR_DESC + "input/Keyboard;", cn.name, fn.name, fn.desc, !cn.name.equals("client")));
			} else if (fn.desc.equals(mouseSuperDesc)) {
				addProcessor(new AddGetterProcessor(this, "getMouse", "L" + ACCESSOR_DESC + "input/Mouse;", cn.name, fn.name, fn.desc, !cn.name.equals("client")));
			}
		}
	}

	@Override
	public boolean isValid(final String name, final ClassNode cn) {
		return true;
	}
}