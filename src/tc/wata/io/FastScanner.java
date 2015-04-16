package tc.wata.io;

import java.io.*;
import java.util.*;

import tc.wata.debug.*;

/**
 * java.util.Scanner互換の高速入力クラス<br>
 * hasNextでは，間の空文字を読み飛ばすので，次がnextLineの場合に先頭の空白が読み飛ばされる<br>
 * 1～100万の整数値読み込みが，nextIntで100ms，nextで200ms，nextDoubleで300msくらい
 */
public class FastScanner implements Closeable {
	
	InputStream in;
	byte[] buf;
	int p, m;
	static boolean[] isSpace = new boolean[128];
	static {
		isSpace[' '] = isSpace['\n'] = isSpace['\r'] = isSpace['\t'] = true;
	}
	
	/**
	 * InputStreamから読み込む
	 */
	public FastScanner(InputStream in) {
		this.in = in;
		buf = new byte[1 << 15];
	}
	
	/**
	 * 文字列から読み込む
	 */
	public FastScanner(String str) {
		buf = str.getBytes();
		m = buf.length;
	}
	
	/**
	 * 空白を指定する．'\r'と'\n'は自動的に空白となる．
	 */
	public void setSpace(char...cs) {
		Arrays.fill(isSpace, false);
		isSpace['\r'] = isSpace['\n'] = true;
		for (char c : cs) isSpace[c] = true;
	}
	
	int read() {
		if (m == -1) return -1;
		if (p >= m) {
			p = 0;
			if (in == null) return m = -1;
			try {
				m = in.read(buf);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if (m <= 0) return -1;
		}
		return buf[p++];
	}
	
	/**
	 * 次の1文字を読み進めずにそのまま返す．空白は読み飛ばさない．
	 */
	public int peek() {
		int c = read();
		if (c >= 0) p--;
		return c;
	}
	
	/**
	 * 空白以外がまだあるか判定．
	 */
	public boolean hasNext() {
		int c = read();
		while (c >= 0 && isSpace[c]) c = read();
		if (c == -1) return false;
		p--;
		return true;
	}
	
	/**
	 * 現在の行に空白以外がまだあるか判定．
	 */
	public boolean hasNextInLine() {
		if (p > 0 && buf[p - 1] == '\n') return false;
		int c = read();
		while (c >= 0 && isSpace[c] && c != '\n' && c != '\r') c = read();
		if (c == -1) return false;
		p--;
		return c != '\n' && c != '\r';
	}
	
	/**
	 * 次の文字列を読み込む．
	 */
	public String next() {
		if (!hasNext()) throw new InputMismatchException();
		StringBuilder sb = new StringBuilder();
		int c = read();
		while (c >= 0 && !isSpace[c]) {
			sb.append((char)c);
			c = read();
		}
		return sb.toString();
	}
	
	/**
	 * 行末までを読み込む．
	 */
	public String nextLine() {
		StringBuilder sb = new StringBuilder();
		if (p > 0 && buf[p - 1] == '\n') {
			buf[p - 1] = ' ';
			return "";
		}
		int c = read();
		if (c < 0) throw new InputMismatchException();
		while (c >= 0 && c != '\n' && c != '\r') {
			sb.append((char)c);
			c = read();
		}
		if (c == '\r') read();
		if (p > 0) buf[p - 1] = ' ';
		return sb.toString();
	}
	
	/**
	 * 現在の行を読み飛ばす．
	 */
	public void skipLine() {
		if (p > 0 && buf[p - 1] == '\n') {
			buf[p - 1] = ' ';
			return;
		}
		int c = read();
		if (c < 0) return;
		while (c >= 0 && c != '\n' && c != '\r') {
			c = read();
		}
		if (c == '\r') read();
		if (p > 0) buf[p - 1] = ' ';
	}
	
	/**
	 * 次のint値を読み込む．
	 */
	public int nextInt() {
		if (!hasNext()) throw new InputMismatchException();
		int c = read();
		int sgn = 1;
		if (c == '-') {
			sgn = -1;
			c = read();
		}
		int res = 0;
		do {
			if (c < '0' || c > '9') throw new InputMismatchException();
			res *= 10;
			res += c - '0';
			c = read();
		} while (c >= 0 && !isSpace[c]);
		return res * sgn;
	}
	
	/**
	 * 現在の行のint値を読み込む．
	 * @param is 読み込み先の配列．この長さを超える場合はそこで一旦打ち切り．
	 * @return 読み込まれた個数．長さを超えた場合は-1を返す．
	 */
	public int nextInts(int[] is) {
		int len = 0;
		while (len < is.length && hasNextInLine()) {
			is[len++] = nextInt();
		}
		if (hasNextInLine()) return -1;
		skipLine();
		return len;
	}
	
	/**
	 * 次のlong値を読み込む．
	 */
	public long nextLong() {
		if (!hasNext()) throw new InputMismatchException();
		int c = read();
		int sgn = 1;
		if (c == '-') {
			sgn = -1;
			c = read();
		}
		long res = 0;
		do {
			if (c < '0' || c > '9') throw new InputMismatchException();
			res *= 10;
			res += c - '0';
			c = read();
		} while (c >= 0 && !isSpace[c]);
		return res * sgn;
	}
	
	/**
	 * 次のdouble値を読み込む．
	 */
	public double nextDouble() {
		return Double.parseDouble(next());
	}
	
	@Override
	public void close() {
		try {
			if (in != null) in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
