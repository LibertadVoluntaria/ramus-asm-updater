package org.exobot.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * @author Ramus
 */
public class RIS {

	private static List<Integer> compilePattern(final String pattern) {
		final List<Integer> opcodes = new LinkedList<Integer>();
		if (pattern.startsWith("(") && pattern.contains("|") && pattern.endsWith(")")) {
			final String trimmed = pattern.substring(1, pattern.length() - 1);
			final String[] parts = trimmed.split("\\|");
			for (final String insn : parts) {
				opcodes.add(getOpcode(insn));
			}
		} else {
			opcodes.add(getOpcode(pattern));
		}
		return opcodes;
	}

	private static int getOpcode(final String name) {
		try {
			return (Integer) Opcodes.class.getField(name.toUpperCase()).get(null);
		} catch (final Exception e) {
			return 0;
		}
	}

	private final InsnList iList;
	private int index = 0;

	public RIS(final MethodNode m) {
		iList = m.instructions;
		final AbstractInsnNode[] nodes = iList.toArray();
	}

	public AbstractInsnNode current() {
		if (index < 0 || index >= iList.size()) {
			return null;
		}
		return iList.get(index);
	}

	public AbstractInsnNode[] getArray() {
		return iList.toArray();
	}

	public InsnList getInsnList() {
		return iList;
	}

	public int getPosition() {
		return index;
	}

	public AbstractInsnNode next() {
		++index;
		return current();
	}

	public <T> T next(final Class<T> insn) {
		for (++index; index < iList.size(); ++index) {
			final AbstractInsnNode cur = current();
			if (cur == null || !insn.isAssignableFrom(cur.getClass())) {
				continue;
			}
			return insn.cast(cur);
		}
		return null;
	}

	public <T> T next(final Class<T> insn, final int opcode) {
		for (++index; index < iList.size(); ++index) {
			final AbstractInsnNode cur = current();
			if (cur == null || !insn.isAssignableFrom(cur.getClass()) || cur.getOpcode() != opcode) {
				continue;
			}
			return insn.cast(cur);
		}
		return null;
	}

	public AbstractInsnNode next(final int opcode) {
		for (++index; index < iList.size(); ++index) {
			final AbstractInsnNode cur = current();
			if (cur == null || cur.getOpcode() != opcode) {
				continue;
			}
			return cur;
		}
		return null;
	}

	public FieldInsnNode nextFieldInsnNode(final int opcode, final String desc) {
		for (++index; index < iList.size(); ++index) {
			final AbstractInsnNode cur = current();
			if (cur == null || !(cur instanceof FieldInsnNode) || cur.getOpcode() != opcode) {
				continue;
			}
			final FieldInsnNode fin = (FieldInsnNode) cur;
			if (desc == null || desc.isEmpty()) {
				return fin;
			}
			if (fin.desc.equals(desc)) {
				return fin;
			}
		}
		return null;
	}

	public MethodInsnNode nextMethodInsn(final int opcode, final String... methodNames) {
		for (++index; index < iList.size(); ++index) {
			final AbstractInsnNode cur = current();
			if (cur == null || !(cur instanceof MethodInsnNode) || cur.getOpcode() != opcode) {
				continue;
			}
			final MethodInsnNode min = (MethodInsnNode) cur;
			if (methodNames.length == 0) {
				return min;
			}
			for (final String methodName : methodNames) {
				if (min.name.equals(methodName)) {
					return min;
				}
			}
		}
		return null;
	}

	public Iterator<AbstractInsnNode[]> nextPattern(final String pattern) {
		final List<AbstractInsnNode[]> matches = new LinkedList<AbstractInsnNode[]>();
		final AbstractInsnNode[] array = getArray();
		final String[] parts = pattern.split(" ");
		int nodeIdx = 0;
		int patternIdx = 0;
		outer:
		for (++index; index < array.length; ++index) {
			final AbstractInsnNode ain = array[index];
			final List<Integer> codes = compilePattern(parts[patternIdx]);
			for (final int code : codes) {
				if (code != ain.getOpcode()) {
					continue;
				}
				if (patternIdx < parts.length - 1) {
					patternIdx++;
				} else {
					final AbstractInsnNode[] nodes = new AbstractInsnNode[parts.length];
					System.arraycopy(getArray(), nodeIdx, nodes, 0, parts.length);
					matches.add(nodes);
					index = nodeIdx++;
					patternIdx = 0;
				}
				continue outer;
			}
			index = nodeIdx++;
			patternIdx = 0;
		}
		return matches.iterator();
	}

	public AbstractInsnNode previous() {
		--index;
		return current();
	}

	public <T> T previous(final Class<T> insn) {
		for (--index; index > 0; --index) {
			final AbstractInsnNode cur = current();
			if (cur == null || !insn.isAssignableFrom(cur.getClass())) {
				continue;
			}
			return insn.cast(cur);
		}
		return null;
	}

	public <T> T previous(final Class<T> insn, final int opcode) {
		for (--index; index > 0; --index) {
			final AbstractInsnNode cur = current();
			if (cur == null || !insn.isAssignableFrom(cur.getClass()) || cur.getOpcode() != opcode) {
				continue;
			}
			return insn.cast(cur);
		}
		return null;
	}

	public AbstractInsnNode previous(final int opcode) {
		for (--index; index > 0; --index) {
			final AbstractInsnNode cur = current();
			if (cur == null || cur.getOpcode() != opcode) {
				continue;
			}
			return cur;
		}
		return null;
	}

	public FieldInsnNode previousFieldInsnNode(final int opcode, final String desc) {
		for (--index; index < iList.size(); --index) {
			final AbstractInsnNode cur = current();
			if (cur == null || !(cur instanceof FieldInsnNode) || cur.getOpcode() != opcode) {
				continue;
			}
			final FieldInsnNode fin = (FieldInsnNode) cur;
			if (desc == null || desc.isEmpty()) {
				return fin;
			}
			if (fin.desc.equals(desc)) {
				return fin;
			}
		}
		return null;
	}

	public MethodInsnNode previousMethodInsn(final int opcode, final String... methodNames) {
		for (--index; index > 0; --index) {
			final AbstractInsnNode cur = current();
			if (cur == null || !(cur instanceof MethodInsnNode) || cur.getOpcode() != opcode) {
				continue;
			}
			final MethodInsnNode min = (MethodInsnNode) cur;
			if (methodNames.length == 0) {
				return min;
			}
			for (final String methodName : methodNames) {
				if (min.name.equals(methodName)) {
					return min;
				}
			}
		}
		return null;
	}

	public void setPosition(final int index) {
		this.index = index;
	}
}