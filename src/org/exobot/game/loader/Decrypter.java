package org.exobot.game.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.*;
import java.util.jar.Pack200.Unpacker;
import java.util.zip.GZIPInputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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

	private static byte[] decrypt(final String encrypted) {
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

	private static int getCharCode(final char c) {
		return c > -1 && c < CHAR_CODES.length ? CHAR_CODES[c] : -1;
	}

	public static Map<String, byte[]> decryptPack(final File gamepack, final String paramn1, final String param0) throws Exception {
		final Map<String, byte[]> classes = new LinkedHashMap<String, byte[]>();
		final byte[] key = decrypt(param0);
		final SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
		final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		final byte[] ivParameterSpec = decrypt(paramn1);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivParameterSpec));
		final JarFile gamepackJar = new JarFile(gamepack);
		final JarEntry innerpackJar = gamepackJar.getJarEntry("inner.pack.gz");
		final InputStream innerpackStream = gamepackJar.getInputStream(innerpackJar);
		final byte[] data = new byte[0x500000];
		int offset = 0;
		int read;
		while ((read = innerpackStream.read(data, offset, 0x500000 - offset)) != -1) {
			offset += read;
		}
		final byte[] encryptedData = new byte[offset];
		System.arraycopy(data, 0, encryptedData, 0, offset);
		final byte[] decryptedData = cipher.doFinal(encryptedData);
		final Unpacker unpacker = Pack200.newUnpacker();
		final ByteArrayOutputStream packStream = new ByteArrayOutputStream(0x500000);
		final JarOutputStream packJar = new JarOutputStream(packStream);
		final GZIPInputStream decryptedStream = new GZIPInputStream(new ByteArrayInputStream(decryptedData));
		unpacker.unpack(decryptedStream, packJar);
		packJar.close();
		final JarInputStream gameJar = new JarInputStream(new ByteArrayInputStream(packStream.toByteArray()));
		JarEntry entry;
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
			classes.put(entryName.substring(0, entryName.length() - 6).replace('/', '.'), entryBytes);
		}
		return classes;
	}
}