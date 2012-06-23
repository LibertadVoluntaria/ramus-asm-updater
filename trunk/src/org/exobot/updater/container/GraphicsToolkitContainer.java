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

	private final String[] ORDER = {"Z", "X", "Y"};
	private final String[] OFFSETS = {"ZOffset", "XOffset", "YOffset"};
	private final String[] TOOLKIT = {"AbsoluteX", "XMultiplier", "AbsoluteY", "YMultiplier"};

	@Override
	public Class<?>[] getDependencies() {
		return new Class<?>[]{CameraMatrixContainer.class};
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
			if (!node.superName.equals(cn.name) || node.interfaces.size() > 0) {
				continue;
			}
			int floatCount = 0;
			for (final FieldNode fn : node.fields) {
				if ((fn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC || !fn.desc.equals("F")) {
					continue;
				}
				floatCount++;
			}
			if (floatCount != 6) {
				continue;
			}
			for (final MethodNode mn : node.methods) {
				if ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC || !mn.desc.equals("(FFF[F)V") || mn.instructions.size() > 175) {
					continue;
				}
				final RIS ris = new RIS(mn);
				final FieldInsnNode matrix;
				if (!(matrix = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)).desc.equals(matrixDesc)) {
					continue;
				}
				addProcessor(new AddGetterProcessor(this, "getCameraMatrix", ACCESSOR_DESC + "CameraMatrix;", matrix.owner, matrix.name, matrix.desc, false));
				final FieldInsnNode floats = ris.next(FieldInsnNode.class, Opcodes.GETFIELD);
				if (!floats.desc.equals("[F")) {
					continue;
				}
				final HookContainer matrixHC = Updater.getInstance().getContainer("CameraMatrix");
				for (int i = 0; i < 3; i++) {
					ris.setPosition(0);
					addOffsets(matrixHC, ris, floats, i);
				}
				ris.setPosition(0);
				addFloats(matrixHC, ris, floats);
				FieldInsnNode floatNode;
				int index = 0;
				while ((floatNode = ris.next(FieldInsnNode.class, Opcodes.GETFIELD)) != null) {
					if (floatNode.desc.equals("F")) {
						addProcessor(new AddGetterProcessor(this, "get" + TOOLKIT[index++], "F", floatNode.owner, floatNode.name, "F", false));
					}
				}
				break;
			}
		}
	}

	private void addFloats(final HookContainer hc, final RIS ris, final FieldInsnNode matrix) {
		final int[] ops = {Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4,
				Opcodes.ICONST_5, Opcodes.BIPUSH};
		int count = 0;
		VarInsnNode load;
		outer:
		while (count < 9 && (load = ris.next(VarInsnNode.class, Opcodes.FLOAD)) != null) {
			final int pos = ris.getPosition();
			ris.setPosition(pos + 4);
			AbstractInsnNode floatNode = ris.current();
			for (final int op : ops) {
				if (floatNode.getOpcode() == op) {
					final int value;
					if (floatNode instanceof IntInsnNode) {
						value = ((IntInsnNode) floatNode).operand;
					} else {
						value = floatNode.getOpcode() - 3;
					}
					hc.addProcessor(new AddGetterProcessor(hc, "get" + ORDER[count / 3] + (char) ('X' + load.var - 1), "F", matrix.owner, matrix.name, "F", false, -1, value));
					count++;
					ris.setPosition(pos);
					continue outer;
				}
			}
			ris.setPosition(pos - 2);
			floatNode = ris.current();
			for (final int op : ops) {
				if (floatNode.getOpcode() == op) {
					final int value;
					if (floatNode instanceof IntInsnNode) {
						value = ((IntInsnNode) floatNode).operand;
					} else {
						value = floatNode.getOpcode() - 3;
					}
					hc.addProcessor(new AddGetterProcessor(hc, "get" + ORDER[count / 3] + (char) ('X' + load.var - 1), "F", matrix.owner, matrix.name, "F", false, -1, value));
					count++;
					ris.setPosition(pos);
					continue outer;
				}
			}
			ris.setPosition(pos);
		}
	}

	private void addOffsets(final HookContainer hc, final RIS ris, final FieldInsnNode matrix, final int brackets) {
		final Iterator<AbstractInsnNode[]> adds = ris.nextPattern(brackets == 2 ? "fadd fadd fadd" : brackets > 0 ? "fmul fadd fadd (fload|aload)" : "faload fadd (fload|aload)");
		while (adds.hasNext()) {
			adds.next();
			AbstractInsnNode arrIdx = null;
			for (int i = 0; i < (brackets > 0 ? 2 : 1); i++) {
				arrIdx = ris.previous(Opcodes.BIPUSH, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5);
			}
			if (arrIdx == null) {
				continue;
			}
			final int pos = ris.getPosition();
			int index = 4;
			for (int i = 0; i < 4; i++) {
				if (ris.next(Opcodes.FSTORE) != null) {
					index -= 1;
				}
			}
			ris.setPosition(pos);
			if (index > 3) {
				continue;
			}
			final int value;
			if (arrIdx instanceof IntInsnNode) {
				value = ((IntInsnNode) arrIdx).operand;
			} else {
				value = arrIdx.getOpcode() - 3;
			}
			if (ris.next(Opcodes.FSTORE) == null || ris.next(Opcodes.FSTORE) == null) {
				ris.setPosition(pos);
				continue;
			}
			hc.addProcessor(new AddGetterProcessor(hc, "get" + OFFSETS[index], "F", matrix.owner, matrix.name, "F", false, -1, value));
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