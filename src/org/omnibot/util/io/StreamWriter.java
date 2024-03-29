package org.omnibot.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * @author Ramus
 */
public class StreamWriter extends OutputStream {

	private static final byte EOL = 0xA;
	private byte[] data = new byte[32];
	private int count = 0;

	private void resize(final int newCapacity) {
		data = Arrays.copyOf(data, newCapacity);
	}

	public int size() {
		return count;
	}

	public void subSequence(final int start, final int end) {
		final int len = end - start;
		if (len < 1) {
			return;
		}
		resize(len);
		System.arraycopy(data, start, data, 0, len);
	}

	@Override
	public void write(final int b) throws IOException {
		resize(count + 1);
		data[count] = (byte) b;
		count++;
	}

	public void writeInt(final int i) throws IOException {
		for (int j = 3; j > -1; j--) {
			write((i >>> (j * 8)) & 0xFF);
		}
	}

	public void writeLong(final long l) throws IOException {
		for (int j = 7; j > -1; j--) {
			write((byte) (l >>> (j * 8)));
		}
	}

	public void writeString(final String s) throws IOException {
		writeSegment(s.getBytes());
	}

	public void writeSegment(final byte[] data) throws IOException {
		for (final int b : data) {
			write(b);
		}
		write(EOL);
	}

	public void writeShort(final int s) throws IOException {
		for (int j = 1; j > -1; j--) {
			write((s >>> (j * 8)) & 0xFF);
		}
	}

	public void writeTo(final OutputStream out) throws IOException {
		out.write(data);
	}
}