package org.exobot.updater.processor;

import java.io.IOException;
import org.exobot.updater.container.HookContainer;
import org.exobot.util.io.StreamWriter;

/**
 * An abstract process class.
 *
 * @author Ramus
 */
public abstract class Processor {

	public static interface Id {

		public static int ATTRIBUTE = 1;
		public static int GET_STATIC = 2;
		public static int GET_FIELD = 3;
		public static int ADD_FIELD = 4;
		public static int ADD_METHOD = 5;
		public static int ADD_INTERFACE = 6;
		public static int SET_SUPER = 7;
		public static int SET_SIGNATURE = 8;
		public static int INSERT_CODE = 9;
		public static int OVERRIDE_CLASS = 10;
	}

	public abstract HookContainer getContainer();

	public abstract String getOutput();

	public abstract int getId();

	public abstract void process(final StreamWriter stream) throws IOException;
}