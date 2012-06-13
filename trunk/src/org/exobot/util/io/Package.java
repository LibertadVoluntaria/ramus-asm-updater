package org.exobot.util.io;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Ramus
 */
public class Package {

	public static List<Class<?>> getClasses(final String packageName) throws IOException {
		final List<Class<?>> classes = new LinkedList<Class<?>>();
		final String packageNameSlashed = "src/" + packageName.replaceAll("\\.", "/");
		final URL directoryURL = new File(packageNameSlashed).toURI().toURL();
		if (directoryURL == null) {
			return classes;
		}
		final String directoryString = directoryURL.getFile().replaceAll("%20", " ");
		if (directoryString == null) {
			return classes;
		}
		final File directory = new File(directoryString);
		if (directory.exists()) {
			final String[] files = directory.list();
			for (final String fileName : files) {
				if (fileName.endsWith(".class") || fileName.endsWith(".java")) {
					try {
						classes.add(
								Class.forName(packageName + "." + fileName.replace(".java", "").replace(".class", "")));
					} catch (final ClassNotFoundException ignored) {
					}
				}
			}
		}
		return classes;
	}
}