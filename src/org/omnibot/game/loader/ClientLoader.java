package org.omnibot.game.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.omnibot.game.GameEnvironment;
import org.omnibot.game.GameScene;
import org.omnibot.util.io.Internet;

/**
 * @author Ramus
 */
public class ClientLoader {

	public static boolean LOAD_LOCAL = true;

	private final Map<String, ClassNode> classes = new LinkedHashMap<>();
	private final GameEnvironment game;

	public ClientLoader() {
		game = new GameScene();
		load();
	}

	private Map<String, byte[]> decryptGamepack(final byte[] gamepack) {
		System.out.println("Decrypting gamepack.");
		try {
			final Decrypter decrypter = new Decrypter(gamepack, game.getParameter("-1"), game.getParameter("0"));
			decrypter.decrypt();
			System.out.println("Successfully decrypted gamepack.");
			return decrypter.getClasses();
		} catch (final Exception e) {
			System.out.println("Unable to decrypt gamepack.");
			e.printStackTrace();
		}
		return null;
	}

	public Map<String, ClassNode> getClasses() {
		return classes;
	}

	private byte[] getGamepack() {
		try {
			System.out.println("Loading game environment.");
			game.load();
			System.out.println("Successfully loaded game environment.");
		} catch (final IOException e) {
			System.out.println("Unable to load game environment.");
			e.printStackTrace();
			return null;
		}
		try {
			System.out.println();
			System.out.println("Downloading gamepack.");
			final String directGame = game.getDirectGame();
			final byte[] data = Internet.downloadBinary(directGame.substring(0, directGame.indexOf(".com") + 5) + game.getArchive());
			System.out.println("Successfully downloaded gamepack.");
			return data;
		} catch (final IOException e) {
			System.out.println("Unable to download gamepack.");
			e.printStackTrace();
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
			final byte[] gamepack = getGamepack();
			if (gamepack == null) {
				return;
			}
			System.out.println();
			final Map<String, byte[]> innerpack = decryptGamepack(gamepack);
			if (innerpack == null) {
				return;
			}
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
			} catch (final Exception ignored) {
			}
		}
		System.out.println();
	}
}