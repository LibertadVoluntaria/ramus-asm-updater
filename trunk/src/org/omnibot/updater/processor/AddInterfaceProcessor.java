package org.omnibot.updater.processor;

import java.io.IOException;
import org.omnibot.updater.container.HookContainer;
import org.omnibot.util.io.StreamWriter;

/**
 * @author Ramus
 */
public class AddInterfaceProcessor extends Processor {

	private final HookContainer cc;
	private final String clazz;
	private final String iface;

	public AddInterfaceProcessor(final HookContainer cc, final String clazz, final String iface) {
		this.cc = cc;
		this.clazz = clazz;
		this.iface = iface;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof AddInterfaceProcessor)) {
			return false;
		}
		final AddInterfaceProcessor p = (AddInterfaceProcessor) o;
		return p.cc.equals(cc) && p.clazz.equals(clazz) && p.iface.equals(iface);
	}

	@Override
	public HookContainer getContainer() {
		return cc;
	}

	@Override
	public String getOutput() {
		return "^ " + clazz + " implements " + iface;
	}

	@Override
	public int getId() {
		return Id.ADD_INTERFACE;
	}

	@Override
	public void process(final StreamWriter stream) throws IOException {
		stream.write(Id.ADD_INTERFACE);
		stream.writeString(clazz);
		stream.writeString(iface);
	}
}