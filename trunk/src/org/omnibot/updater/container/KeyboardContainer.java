package org.omnibot.updater.container;

import java.util.List;
import org.omnibot.updater.Task;
import org.omnibot.updater.Updater;
import org.omnibot.updater.processor.SetSignatureProcessor;
import org.omnibot.updater.processor.SetSuperProcessor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ramus
 */
public class KeyboardContainer extends HookContainer implements Task {

	@Override
	public int getSignatures() {
		return 5;
	}

	@Override
	public int getSupers() {
		return 1;
	}

	@Override
	public void execute(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("Keyboard", cn);
		final ClassNode supercn = Updater.getInstance().getClasses().get(cn.superName);
		addProcessor(new SetSuperProcessor(this, supercn.name, ACCESSOR_DESC + "input/Keyboard"));
		for (final MethodNode mn : cn.methods) {
			final String mName = mn.name;
			if (mName.startsWith("key") || mName.startsWith("focus")) {
				addProcessor(new SetSignatureProcessor(this, mName, mn.desc, mn.access, "_" + mName, mn.desc));
			}
		}
	}

	@Override
	public boolean isValid(final String name, final ClassNode cn) {
		final List<String> interfaces = cn.interfaces;
		for (final String iface : interfaces) {
			if (iface.equals("java/awt/event/KeyListener")) {
				return true;
			}
		}
		return false;
	}
}