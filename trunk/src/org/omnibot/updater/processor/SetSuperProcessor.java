package org.omnibot.updater.processor;

import java.io.IOException;
import org.omnibot.updater.container.HookContainer;
import org.omnibot.util.io.StreamWriter;

/**
 * @author Ramus
 */
public class SetSuperProcessor extends Processor {

	private final HookContainer cc;
	private final String clazz;
	private final String superClazz;

	public SetSuperProcessor(final HookContainer cc, final String clazz, final String superClazz) {
		this.cc = cc;
		this.clazz = clazz;
		this.superClazz = superClazz;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof SetSuperProcessor)) {
			return false;
		}
		final SetSuperProcessor p = (SetSuperProcessor) o;
		return p.cc.equals(cc) && p.clazz.equals(clazz) && p.superClazz.equals(superClazz);
	}

	@Override
	public HookContainer getContainer() {
		return cc;
	}

	@Override
	public String getOutput() {
		return "* " + clazz + " extends " + superClazz;
	}

	@Override
	public int getId() {
		return Id.SET_SUPER;
	}

	@Override
	public void process(final StreamWriter stream) throws IOException {
		stream.write(Id.SET_SUPER);
		stream.writeString(clazz);
		stream.writeString(superClazz);
	}
}