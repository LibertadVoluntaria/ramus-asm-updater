package org.omnibot.updater.container;

import org.omnibot.updater.Task;
import org.omnibot.updater.Updater;
import org.omnibot.updater.processor.SetSuperProcessor;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Ramus
 */
public class CanvasContainer extends HookContainer implements Task {

	@Override
	public int getSupers() {
		return 1;
	}

	@Override
	public void execute(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("Canvas", cn);
		addProcessor(new SetSuperProcessor(this, cn.name, ACCESSOR_DESC + "input/Canvas"));
	}

	@Override
	public boolean isValid(final String name, final ClassNode cn) {
		return cn.superName.equals("java/awt/Canvas");
	}
}