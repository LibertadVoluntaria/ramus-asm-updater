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
public class ModelContainer extends HookContainer implements Task {

	private static final String[] POINTS = {"X", "Y", "Z"};

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public int getMethods() {
		return 6;
	}

	@Override
	public void run(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("Model", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "Model"));
		for (final ClassNode node : Updater.getInstance().getClasses().values()) {
			if (!node.superName.equals(cn.name) || node.interfaces.size() > 0 || node.fields.size() < 50 || node.methods.size() < 70) {
				continue;
			}
			int selfArr = 0;
			for (final FieldNode fn : node.fields) {
				if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
					continue;
				}
				if (fn.desc.equals("[L" + node.name + ";")) {
					selfArr++;
				}
			}
			if (selfArr != 2) {
				continue;
			}
			boolean foundIndices = false;
			boolean foundPoints = false;
			outer:
			for (final MethodNode mn : node.methods) {
				if (foundIndices && foundPoints) {
					break;
				}
				if ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
					continue;
				}
				final RIS ris = new RIS(mn);
				if (!foundIndices) {
					String[] fields = new String[3];
					for (int i = 0; i < 3; i++) {
						final FieldInsnNode fin = ris.next(FieldInsnNode.class, Opcodes.GETFIELD);
						if (fin == null || !fin.desc.equals("[S") || (fields[0] != null && fin.name.equals(fields[0]))) {
							fields = null;
							break;
						}
						fields[i] = fin.name;
					}
					if (fields != null) {
						for (int i = 1; i < 4; i++) {
							addProcessor(new AddMethodProcessor(this, "getIndices" + i, "[S", node.name, fields[i - 1], "[S", false));
						}
						foundIndices = true;
					}
				}
				if (foundPoints) {
					continue;
				}
				ris.setPosition(0);
				AbstractInsnNode ain = ris.next(Opcodes.SIPUSH);
				if (ain == null) {
					continue;
				}
				for (int i = 0; i < 5; i++) {
					if (ain.getNext() == null || (ain = ain.getNext().getNext()) == null || ain.getOpcode() != Opcodes.SIPUSH) {
						continue outer;
					}
				}
				ris.next(Opcodes.GETFIELD);
				for (int i = 0; i < 3; i++) {
					final FieldInsnNode fin = ris.next(FieldInsnNode.class, Opcodes.GETFIELD);
					if (fin == null) {
						continue outer;
					}
					addProcessor(new AddMethodProcessor(this, "get" + POINTS[i] + "Points", "[I", node.name, fin.name, "[I", false));
				}
				foundPoints = true;
			}
		}
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		if ((cn.access & Opcodes.ACC_ABSTRACT) != Opcodes.ACC_ABSTRACT || !cn.superName.equals("java/lang/Object") || cn.interfaces.size() > 1 || cn.fields.size() != 1) {
			return false;
		}
		int abstracts = 0;
		for (final MethodNode mn : cn.methods) {
			if ((mn.access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT) {
				abstracts++;
			}
		}
		if (abstracts < 40) {
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