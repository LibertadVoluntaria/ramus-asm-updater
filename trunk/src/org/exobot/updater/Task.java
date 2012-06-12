package org.exobot.updater;

import org.objectweb.asm.tree.ClassNode;

/**
 * @author Ramus
 */
public interface Task {

	public void execute(final String name, final ClassNode cn);
}