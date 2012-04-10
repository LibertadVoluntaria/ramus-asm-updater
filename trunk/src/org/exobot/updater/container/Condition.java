package org.exobot.updater.container;

import org.objectweb.asm.tree.ClassNode;

/**
 * @author Ramus
 */
public interface Condition {

	public boolean validate(final String name, final ClassNode cn);
}