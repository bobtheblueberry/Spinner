/**
 *    
 *    Copyright (C) 2016 Serge Humphrey <sergehumphrey@gmail.com>
 * 
 *    This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package com.btbb.spinner.db;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class StreamDecoder extends InputStream {
	protected InputStream in;
	protected int pos = 0;
	protected int markPos = 0;

	public StreamDecoder(InputStream in) {
		if (in instanceof BufferedInputStream)
			this.in = in;
		else
			this.in = new BufferedInputStream(in);
	}

	public StreamDecoder(String path) throws FileNotFoundException {
		in = new BufferedInputStream(new FileInputStream(path));
	}

	public StreamDecoder(File f) throws FileNotFoundException {
		in = new BufferedInputStream(new FileInputStream(f));
	}

	public int read(byte b[]) throws IOException {
		return read(b, 0, b.length);
	}

	public int read(byte b[], int off, int len) throws IOException {
		int read = in.read(b, off, len);
		if (read != len) {
			throw new IOException("Unexpected end of file");
		}
		pos += len;
		return read;
	}

	public int read() throws IOException {
		int t = in.read();
		if (t == -1) {
			throw new IOException("Unexpected end of file");
		}
		pos++;
		return t;
	}

	public int read2() throws IOException {
		int a = read();
		int b = read();
		return (a | (b << 8));
	}

	public int read3() throws IOException {
		int a = read();
		int b = read();
		int c = read();
		return (a | (b << 8) | (c << 16));
	}

	public int read4() throws IOException {
		int a = read();
		int b = read();
		int c = read();
		int d = read();
		return (a | (b << 8) | (c << 16) | (d << 24));
	}
	
	public long read8() throws IOException {
		long a = read();
		for (int i = 1; i < 8; i++)
			a = a | (((long)read()) << (i*8));
		return a;
	}

	public double readD() throws IOException {
		byte[] b = new byte[8];
		read(b);
		long r = b[0] & 0xFF;
		for (int i = 1; i < 8; i++)
			r |= (b[i] & 0xFFL) << (8 * i);
		return Double.longBitsToDouble(r);
	}

	public String readStr() throws IOException {
		char[] chars = new char[read4()];
		for (int i = 0; i < chars.length; i++)
			chars[i] = (char) read2();
		return String.valueOf(chars);
	}

	public void close() throws IOException {
		in.close();
	}

	public long skip(long length) throws IOException {
		long total = in.skip(length);
		while (total < length) {
			total += in.skip(length - total);
		}
		pos += (int) length;
		return total;
	}

	public InputStream getInputStream() {
		return in;
	}

	public boolean markSupported() {
		return in.markSupported();
	}

	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
		markPos = pos;
	}

	public synchronized void reset() throws IOException {
		in.reset();
		pos = markPos;
	}

	public long getPos() {
		return this.pos;
	}
}
