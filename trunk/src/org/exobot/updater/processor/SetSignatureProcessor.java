package org.exobot.updater.processor;

import java.io.IOException;
import org.exobot.updater.container.HookContainer;
import org.exobot.util.io.StreamWriter;

/**
 * @author Ramus
 */
public class SetSignatureProcessor extends Processor {

	private final HookContainer cc;
	private final String name;
	private final String desc;
	private final int newAccess;
	private final String newName;
	private final String newDesc;

	public SetSignatureProcessor(final HookContainer cc, final String name, final String desc, final int newAccess, final String newName, final String newDesc) {
		this.cc = cc;
		this.name = name;
		this.desc = desc;
		this.newAccess = newAccess;
		this.newName = newName;
		this.newDesc = newDesc;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof SetSignatureProcessor)) {
			return false;
		}
		final SetSignatureProcessor p = (SetSignatureProcessor) o;
		return p.cc.equals(cc) && p.name.equals(name) && p.desc.equals(desc) && p.newAccess == newAccess &&
				p.newName.equals(newName) && p.newDesc.equals(newDesc);
	}

	@Override
	public HookContainer getContainer() {
		return cc;
	}

	@Override
	public String getOutput() {
		return "~ " + name + desc + " --> " + newName + newDesc;
	}

	@Override
	public int getId() {
		return Id.SET_SIGNATURE;
	}

	@Override
	public void process(final StreamWriter stream) throws IOException {
		stream.writeString(name);
		stream.writeString(desc);
		stream.writeInt(newAccess);
		stream.writeString(newName);
		stream.writeString(newDesc);
	}
}