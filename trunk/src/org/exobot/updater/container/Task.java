package org.exobot.updater.container;

import org.objectweb.asm.tree.ClassNode;

/**
 * @author Ramus
 */
public interface Task {

	public void run(final String name, final ClassNode cn);
}