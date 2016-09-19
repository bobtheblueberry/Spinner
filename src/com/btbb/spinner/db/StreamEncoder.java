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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StreamEncoder extends OutputStream {
	protected OutputStream out;
	protected int pos = 0;

	public StreamEncoder(OutputStream o) {
		if (o instanceof BufferedOutputStream)
			out = o;
		else
			out = new BufferedOutputStream(o);
	}

	public StreamEncoder(File f) throws FileNotFoundException {
		out = new BufferedOutputStream(new FileOutputStream(f));
	}

	public StreamEncoder(String filePath) throws FileNotFoundException {
		out = new BufferedOutputStream(new FileOutputStream(filePath));
	}

	public void write(byte[] b) throws IOException {
		out.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
		pos += len;
	}

	public void write(int b) throws IOException {
		out.write(b);
		pos++;
	}

	public void write2(int val) throws IOException {
		short i = (short) val;
		write(i & 255);
		write((i >>> 8) & 255);
	}

	public void write3(int val) throws IOException {
		write(val & 255);
		write((val >>> 8) & 255);
		write((val >>> 16) & 255);
	}

	public void write4(int val) throws IOException {
		write(val & 255);
		write((val >>> 8) & 255);
		write((val >>> 16) & 255);
		write((val >>> 24) & 255);
	}

	public void write8(long val) throws IOException {
		for (int i = 0; i < 8; i++)
			write((int) ((val >>> (i * 8)) & 255));
	}

	public void writeD(double val) throws IOException {
		long num = Double.doubleToLongBits(val);
		byte[] b = new byte[8];
		b[0] = (byte) (num & 0xFF);
		for (int i = 1; i < 8; i++)
			b[i] = (byte) ((num >>> (8 * i)) & 0xFF);
		write(b);
	}

	public void writeStr(String val) throws IOException {
		write4(val.length());
		for (int i = 0; i < val.length(); i++)
			write2(val.charAt(i));
	}

	public void close() throws IOException {
		out.close();
	}

	public void fill(int count) throws IOException {
		for (int i = 0; i < count; i++) {
			write4(0);
		}
	}

	public void flush() throws IOException {
		out.flush();
	}
}
