package org.exobot.game.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.exobot.game.GameEnvironment;
import org.exobot.game.GameScene;
import org.exobot.util.ExoMap;
import org.exobot.util.io.Internet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Ramus
 */
public class ClientLoader {

	public static boolean LOAD_LOCAL = true;

	private final Map<String, ClassNode> classes = new ExoMap<String, ClassNode>();
	private final GameEnvironment game;

	public ClientLoader() {
		game = new GameScene();
		load();
	}

	private Map<String, byte[]> decryptGamepack(final File gamepack) {
		System.out.println("Decrypting gamepack.");
		try {
			final Map<String, byte[]> innerpack = Decrypter.decryptPack(gamepack, game.getParameter("-1"), game.getParameter("0"));
			System.out.println("Successfully decrypted gamepack.");
			return innerpack;
		} catch (final Exception e) {
			System.out.println("Unable to decrypt gamepack.");
		}
		return null;
	}

	public Map<String, ClassNode> getClasses() {
		return classes;
	}

	private File getGamepack() {
		System.out.println("Downloading gamepack.");
		try {
			game.load();
			final String directGame = game.getDirectGame();
			final File file = File.createTempFile("gamepack", ".jar");
			Internet.downloadTo(directGame.substring(0, directGame.indexOf(".com") + 5) + game.getArchive(), file);
			System.out.println("Successfully downloaded gamepack.");
			return file;
		} catch (final IOException e) {
			System.out.println("Unable to download gamepack.");
			return null;
		}
	}

	private synchronized void load() {
		if (LOAD_LOCAL) {
			System.out.println("Loading local gamepack.");
			final File gamepack = new File("gamepack.jar");
			if (!gamepack.exists()) {
				System.out.println("Unable to load local gamepack.");
				System.out.println();
				return;
			}
			System.out.println("Successfully loaded local gamepack.");
			System.out.println();
			System.out.println("Loading classes.");
			try {
				final JarFile jar = new JarFile(gamepack);
				final Enumeration<JarEntry> entries = jar.entries();
				int loaded = 0;
				while (entries.hasMoreElements()) {
					final JarEntry entry = entries.nextElement();
					String name = entry.getName();
					if (!name.endsWith(".class") || name.contains("/")) {
						continue;
					}
					name = name.substring(0, name.length() - 6);
					final ClassReader cr = new ClassReader(jar.getInputStream(entry));
					final ClassNode cn = new ClassNode();
					cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
					classes.put(name, cn);
					loaded++;
				}
				System.out.println("Successfully loaded " + loaded + " classes.");
			} catch (final IOException e) {
				System.out.println("Unable to load classes.");
			}
		} else {
			final File gamepack = getGamepack();
			if (gamepack == null) {
				return;
			}
			System.out.println();
			final Map<String, byte[]> innerpack = decryptGamepack(gamepack);
			System.out.println();
			System.out.println("Loading classes.");
			int loaded = 0;
			for (final Map.Entry<String, byte[]> entry : innerpack.entrySet()) {
				final String name = entry.getKey();
				final ClassReader cr = new ClassReader(entry.getValue());
				final ClassNode cn = new ClassNode();
				cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
				classes.put(name, cn);
				loaded++;
			}
			System.out.println("Successfully loaded " + loaded + " classes.");
			try {
				final FileOutputStream fos = new FileOutputStream(new File("gamepack.jar"));
				final JarOutputStream jos = new JarOutputStream(fos);
				for (final Map.Entry<String, ClassNode> entry : classes.entrySet()) {
					final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					final ClassNode cn = entry.getValue();
					cn.accept(cw);
					jos.putNextEntry(new JarEntry(cn.name + ".class"));
					jos.write(cw.toByteArray());
					jos.closeEntry();
				}
				jos.close();
				fos.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println();
	}
}