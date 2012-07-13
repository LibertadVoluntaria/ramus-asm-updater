package org.omnibot.game.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.zip.GZIPInputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Ramus
 */
public class Decrypter {

	private static final int[] CHAR_CODES;

	static {
		CHAR_CODES = new int[128];
		for (int i = 0; i < CHAR_CODES.length; i++) {
			CHAR_CODES[i] = -1;
		}
		CHAR_CODES[42] = 62;
		CHAR_CODES[43] = 62;
		CHAR_CODES[45] = 63;
		CHAR_CODES[47] = 63;
		for (int i = 48; i < 58; i++) {
			CHAR_CODES[i] = i + 4;
		}
		for (int i = 65; i < 91; i++) {
			CHAR_CODES[i] = i - 65;
		}
		for (int i = 97; i < 123; i++) {
			CHAR_CODES[i] = i - 71;
		}
	}

	private static int getCharCode(final char c) {
		return c > -1 && c < CHAR_CODES.length ? CHAR_CODES[c] : -1;
	}

	private final Map<String, byte[]> classes = new LinkedHashMap<>();
	private final byte[] gamepack;
	private final String paramn1, param0;
	private int hashCode = -1;

	public Decrypter(final byte[] gamepack, final String paramn1, final String param0) {
		this.gamepack = gamepack;
		this.paramn1 = paramn1;
		this.param0 = param0;
	}

	private byte[] decrypt(final String encrypted) {
		final int encLen = encrypted.length();
		if (encLen == 0) {
			return new byte[0];
		}
		final int charIdx = -4 & encLen + 3;
		int decLen = charIdx / 4 * 3;
		if (encLen < charIdx - 1 || getCharCode(encrypted.charAt(charIdx - 2)) == -1) {
			decLen -= 2;
		} else if (encLen < charIdx || getCharCode(encrypted.charAt(charIdx - 1)) == -1) {
			decLen--;
		}
		final byte[] decrypted = new byte[decLen];
		int off = 0;
		int charPos = 0;
		while (charPos < encLen) {
			final int curChar = getCharCode(encrypted.charAt(charPos));
			final int key1 = charPos + 1 < encLen ? getCharCode(encrypted.charAt(charPos + 1)) : -1;
			final int key2 = charPos + 2 < encLen ? getCharCode(encrypted.charAt(charPos + 2)) : -1;
			final int key3 = charPos + 3 < encLen ? getCharCode(encrypted.charAt(charPos + 3)) : -1;
			decrypted[off++] = (byte) (curChar << 2 | key1 >>> 4);
			if (key2 == -1) {
				break;
			}
			decrypted[off++] = (byte) (240 & key1 << 4 | key2 >>> 2);
			if (key3 == -1) {
				break;
			}
			decrypted[off++] = (byte) (192 & key2 << 6 | key3);
			charPos += 4;
		}
		return decrypted;
	}

	public void decrypt() throws Exception {
		final byte[] key = decrypt(param0);
		final SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
		final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		final byte[] ivParameterSpec = decrypt(paramn1);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivParameterSpec));
		final ByteArrayInputStream gamepackBytes = new ByteArrayInputStream(gamepack);
		final JarInputStream gamepackJar = new JarInputStream(gamepackBytes);
		JarEntry innerpackJar = null;
		JarEntry entry;
		while ((entry = gamepackJar.getNextJarEntry()) != null) {
			if (entry.getName().equals("inner.pack.gz")) {
				innerpackJar = entry;
				break;
			}
		}
		if (innerpackJar == null) {
			throw new IOException("Unable to locate inner.pack.gz!");
		}
		final byte[] data = new byte[0x500000];
		int offset = 0;
		int read;
		while ((read = gamepackJar.read(data, offset, 0x500000 - offset)) != -1) {
			offset += read;
		}
		gamepackBytes.close();
		gamepackJar.close();
		final byte[] encryptedData = new byte[offset];
		System.arraycopy(data, 0, encryptedData, 0, offset);
		final byte[] decryptedData = cipher.doFinal(encryptedData);
		final Unpacker unpacker = Pack200.newUnpacker();
		final ByteArrayOutputStream packStream = new ByteArrayOutputStream(0x500000);
		final JarOutputStream packJar = new JarOutputStream(packStream);
		final GZIPInputStream decryptedStream = new GZIPInputStream(new ByteArrayInputStream(decryptedData));
		unpacker.unpack(decryptedStream, packJar);
		decryptedStream.close();
		packJar.close();
		final JarInputStream gameJar = new JarInputStream(new ByteArrayInputStream(packStream.toByteArray()));
		packStream.close();
		while ((entry = gameJar.getNextJarEntry()) != null) {
			final String entryName = entry.getName();
			if (!entryName.endsWith(".class")) {
				continue;
			}
			offset = 0;
			while ((read = gameJar.read(data, offset, 0x500000 - offset)) != -1) {
				offset += read;
			}
			final byte[] entryBytes = new byte[offset];
			System.arraycopy(data, 0, entryBytes, 0, offset);
			if (entryName.equals("client.class")) {
				hashCode = Arrays.hashCode(entryBytes);
			}
			classes.put(entryName.substring(0, entryName.length() - 6).replace('/', '.'), entryBytes);
			gameJar.closeEntry();
		}
		gameJar.close();
	}

	public Map<String, byte[]> getClasses() {
		final Map<String, byte[]> clone = new LinkedHashMap<>(classes.size());
		for (final Map.Entry<String, byte[]> entry : classes.entrySet()) {
			clone.put(entry.getKey(), entry.getValue().clone());
		}
		return clone;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}