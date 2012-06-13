package org.exobot.updater.container;

import java.util.Hashtable;
import java.util.Iterator;
import org.exobot.updater.Task;
import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddGetterProcessor;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.util.RIS;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * @author Ramus
 */
public class GraphicsToolkitContainer extends HookContainer implements Task {

	private final String[] FLOATS = { "ZOffset", "ZX", "ZY", "ZZ", "XOffset", "XX", "XY", "XZ", "YOffset", "YX", "YY",
			"YZ", "YOff" };
	private final String[] TOOLKIT = { "AbsoluteX", "AbsoluteY", "XMultiplier", "YMultiplier" };

	@Override
	public Class<?>[] getDependencies() {
		return new Class<?>[]{ CameraMatrixContainer.class };
	}

	@Override
	public int getGetters() {
		return 5;
	}

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public void execute(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("GraphicsToolkit", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "GraphicsToolkit"));
		final String matrixDesc = "L" + Updater.getInstance().getClasses().get("CameraMatrix").name + ";";
		for (final ClassNode node : Updater.getInstance().getClasses().values()) {
			if (!node.superName.equals(cn.name) || node.interfaces.size() != 1) {
				continue;
			}
			for (final MethodNode mn : node.methods) {
				if ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC || !mn.desc.equals("(FFF[F)V")) {
					continue;
				}
				final RIS ris = new RIS(mn);
				final FieldInsnNode matrix;
				if (!(matrix = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)).desc.equals(matrixDesc)) {
					continue;
				}
				addProcessor(
						new AddGetterProcessor(this, "getCameraMatrix", ACCESSOR_DESC + "CameraMatrix;", matrix.owner,
								matrix.name, matrix.desc, false));
				final FieldInsnNode floats = ris.next(FieldInsnNode.class, Opcodes.GETFIELD);
				if (!floats.desc.equals("[F")) {
					continue;
				}
				ris.setPosition(0);
				final Iterator<AbstractInsnNode[]> iterator = ris.nextPattern(
						"(iconst_0|iconst_1|iconst_2|iconst_3|iconst_4|iconst_5|bipush)");
				for (int i = 0; i < 12 && iterator.hasNext(); i++) {
					final AbstractInsnNode ain = iterator.next()[0];
					final int value;
					if (ain instanceof IntInsnNode) {
						value = ((IntInsnNode) ain).operand;
					} else if (ain instanceof InsnNode) {
						value = ain.getOpcode() - 3;
					} else {
						value = -1;
					}
					final HookContainer matrixHC = Updater.getInstance().getContainer("CameraMatrix");
					matrixHC.addProcessor(
							new AddGetterProcessor(matrixHC, "get" + FLOATS[i], "F", floats.owner, floats.name, "F",
									false, -1, value));
				}
				int i = 0;
				ris.setPosition(0);
				FieldInsnNode nextFloat;
				while ((nextFloat = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)) != null && i < 4) {
					if (nextFloat.desc.equals("F")) {
						addProcessor(
								new AddGetterProcessor(this, "get" + TOOLKIT[i++], "F", nextFloat.owner, nextFloat.name,
										"F", false));
					}
				}
				break;
			}
		}
	}

	@Override
	public boolean isValid(final String name, final ClassNode cn) {
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