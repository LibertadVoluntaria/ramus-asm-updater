package org.omnibot.util;

import org.omnibot.updater.Updater;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * @author Ramus
 */
public class MultiplierSearch {

	private final String className;
	private final String fieldName;
	private final boolean isStatic;
	private final int multiplier;

	public MultiplierSearch(final String className, final String fieldName) {
		this(className, fieldName, true);
	}

	public MultiplierSearch(final String className, final String fieldName, final boolean isStatic) {
		this.className = className;
		this.fieldName = fieldName;
		this.isStatic = isStatic;
		multiplier = search();
	}

	public int getMultiplier() {
		return multiplier;
	}

	private int search() {
		for (final ClassNode cn : Updater.getInstance().getClasses().values()) {
			for (final MethodNode mn : cn.methods) {
				if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
					continue;
				}
				for (final AbstractInsnNode ain : mn.instructions.toArray()) {
					if (ain.getOpcode() != Opcodes.LDC) {
						continue;
					} else if (ain.getNext().getOpcode() != (isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD)) {
						continue;
					}
					final FieldInsnNode fin = (FieldInsnNode) ain.getNext();
					if (!fin.owner.equals(className) || !fin.name.equals(fieldName)) {
						continue;
					}
					final LdcInsnNode ldc = (LdcInsnNode) ain;
					return (Integer) ldc.cst;
				}
			}
		}
		return -1;
	}
}