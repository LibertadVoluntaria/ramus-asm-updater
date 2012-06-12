package org.exobot.updater;

import org.objectweb.asm.tree.ClassNode;

/**
 * @author Ramus
 */
public interface Condition {

	public boolean isValid(final String name, final ClassNode cn);
}