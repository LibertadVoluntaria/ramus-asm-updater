package org.exobot.updater.container;

import java.util.Hashtable;
import org.exobot.updater.Task;
import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddGetterProcessor;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.util.RIS;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ramus
 */
public class RenderContainer extends HookContainer implements Task {

	@Override
	public int getGetters() {
		return 4; // 8
	}

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public void run(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("Render", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Render"));
		outer:
		for (final ClassNode node : Updater.getInstance().getClasses().values()) {
			if (!node.superName.equals(cn.name) || node.interfaces.size() > 0) {
				continue;
			}
			int floats = 0;
			int ints = 0;
			for (final FieldNode fn : node.fields) {
				if ((fn.access & Opcodes.ACC_STATIC) != 0) {
					continue;
				}
				if (fn.desc.equals("Ljava/nio/ByteBuffer;") || fn.desc.equals("[B")) {
					continue outer;
				} else if (fn.desc.equals("F")) {
					floats++;
				} else if (fn.desc.equals("I")) {
					ints++;
				}
			}
			if (ints < 4 || floats < 4) {
				continue;
			}
			// TODO find int getters.. getXMultiplier(), getYMultiplier(), getZFar(), getZNear()
			for (final MethodNode mn : node.methods) {
				if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) != 0 || !mn.desc.equals("(FFF[F)V")) {
					continue;
				}
				final RIS ris = new RIS(mn);
				FieldInsnNode fin = ris.next(FieldInsnNode.class, Opcodes.GETFIELD);
				for (int i = 0; i < 4 && fin != null; i++, fin = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)) {
					if (!fin.desc.equals("F")) {
						i--;
						continue;
					}
					addProcessor(new AddGetterProcessor(this, "getAbsolute" + ((i & 1) != 0 ? "Max" : "Min") + (char) (Math.max(0, Math.min(1, i - 1)) + 88), fin.desc, node.name, fin.name, fin.desc, false));
				}
				break;
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if ((cn.access & Opcodes.ACC_ABSTRACT) != Opcodes.ACC_ABSTRACT || !cn.superName.equals("java/lang/Object")) {
			return false;
		}
		int ints = 0;
		int hashTables = 0;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				continue;
			}
			if (fn.desc.equals(Type.INT_TYPE.getDescriptor())) {
				ints++;
			} else if (fn.desc.equals(Type.getDescriptor(Hashtable.class))) {
				hashTables++;
			}
		}
		if (ints != 2 || hashTables != 1) {
			return false;
		}
		int count = 0;
		for (final ClassNode node : Updater.getInstance().getClasses().values()) {
			if (node.superName.equals(cn.name)) {
				count++;
			}
		}
		return count == 4;
	}
}