package org.exobot.updater.processor;

import java.io.IOException;
import org.exobot.updater.container.HookContainer;
import org.exobot.util.io.StreamWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Add process for adding a getter method to a class.
 *
 * @author Ramus
 */
public class AddGetterProcessor extends Processor {

	private final HookContainer cc;
	private final String name;
	private final String parent;
	private final String field;
	private final Type returnDesc;
	private final Type fieldDesc;
	private final int access;
	private final boolean isStatic;
	private final int multiplier;
	private final int arrayIndex;

	public AddGetterProcessor(final HookContainer cc, final String name, final String returnDesc, final String parent, final String field, final String fieldDesc, final boolean isStatic) {
		this(cc, name, returnDesc, parent, field, fieldDesc, isStatic, -1, -1, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);
	}

	public AddGetterProcessor(final HookContainer cc, final String name, final String returnDesc, final String parent, final String field, final String fieldDesc, final boolean isStatic, final int multiplier) {
		this(cc, name, returnDesc, parent, field, fieldDesc, isStatic, multiplier, -1,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);
	}

	public AddGetterProcessor(final HookContainer cc, final String name, final String returnDesc, final String parent, final String field, final String fieldDesc, final boolean isStatic, final int multiplier, final int arrayIndex) {
		this(cc, name, returnDesc, parent, field, fieldDesc, isStatic, multiplier, arrayIndex,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);
	}

	public AddGetterProcessor(final HookContainer cc, final String name, final String returnDesc, final String parent, final String field, final String fieldDesc, final boolean isStatic, final int multiplier, final int arrayIndex, final int access) {
		this.cc = cc;
		this.name = name;
		this.parent = parent;
		this.field = field;
		this.returnDesc = Type.getType(returnDesc);
		this.fieldDesc = Type.getType(fieldDesc);
		this.access = access;
		this.isStatic = isStatic;
		this.multiplier = multiplier;
		this.arrayIndex = arrayIndex;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof AddGetterProcessor)) {
			return false;
		}
		final AddGetterProcessor p = (AddGetterProcessor) o;
		return p.name.equals(name) && p.cc.equals(cc) && p.returnDesc.equals(returnDesc) && p.isStatic == isStatic &&
				p.parent.equals(parent) && p.field.equals(field) && p.arrayIndex == arrayIndex &&
				p.multiplier == multiplier;
	}

	@Override
	public HookContainer getContainer() {
		return cc;
	}

	@Override
	public String getOutput() {
		return "@ " + name + "() --> " + (isStatic ? "static " : "") + returnDesc + " " + parent + "." + field +
				(arrayIndex != -1 ? "[" + arrayIndex + "]" : "") +
				(multiplier != -1 ? " * " + multiplier : "");
	}

	@Override
	public int getId() {
		return isStatic ? Id.GET_STATIC : Id.GET_FIELD;
	}

	@Override
	public void process(final StreamWriter stream) throws IOException {
		stream.writeInt(access);
		stream.writeString(name);
		stream.writeString(Type.getMethodDescriptor(returnDesc));
		stream.writeString(parent);
		stream.writeString(field);
		stream.writeString(fieldDesc.getDescriptor());
		stream.writeInt(arrayIndex);
		stream.writeInt(multiplier);
		stream.write(0xA);
	}
}