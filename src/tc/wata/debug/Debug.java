package tc.wata.debug;

import static java.lang.Math.*;
import static java.util.Arrays.*;

import java.lang.reflect.*;
import java.util.*;

import javax.swing.*;

/**
 * デバッグ用ユーティリティ
 */
public class Debug {
	
	/**
	 * 変換後の文字列の上限
	 */
	public static int limit = 1000;
	
	/**
	 * スタック一段あたりのインデントの深さ
	 */
	public static int indent = 0;
	
	/**
	 * スタックの基準深さ
	 */
	public static int stackBase = 1;
	
	/**
	 * デバッグ出力のオフ
	 */
	public static boolean silent = false;
	
	/**
	 * 中身を展開しないパッケージ名
	 */
	public static String[] extPrefix = {"java.", "javax.", "org.", "sun.", "oracle."};
	
	private static HashSet<Class<?>> usedClass = new HashSet<Class<?>>();
	
	/**
	 * オブジェクトを中身を表す文字列に変換する
	 */
	public static String toString(Object o) {
		if (o == null) return "null";
		Class<?> c = o.getClass();
		if (c == char[].class) {
			return '"' + new String((char[])o) + '"';
		} else if (c.isArray()) {
			StringBuilder sb = new StringBuilder();
			sb.append('{');
			Object[] is = toObjectArray(o);
			for (int i = 0; i < is.length; i++) {
				if (i > 0) sb.append(',');
				if (sb.length() > limit) {
					sb.append("...");
					break;
				}
				sb.append(toString(is[i]));
			}
			sb.append('}');
			return sb.toString();
		} else if (c == String.class) {
			return '"' + o.toString() + '"';
		} else if (c == char.class || c == Character.class) {
			return "'" + o.toString() + "'";
		} else if (o instanceof Collection && c.getPackage() != null && c.getPackage().getName().startsWith("java.")) {
			Object[] os = ((Collection<?>)o).toArray();
			return toString(os);
		} else {
			boolean detail = !usedClass.contains(c);
			try {
				c.getDeclaredMethod("toString");
				detail = false;
			} catch (Exception e) {
			}
			if (c.getPackage() != null) {
				String pkg = c.getPackage().getName();
				for (String s : extPrefix) if (pkg.startsWith(s)) detail = false;
			}
			if (detail) {
				usedClass.add(c);
				StringBuilder sb = new StringBuilder();
				sb.append('[');
				boolean first = true;
				for (Field f : c.getDeclaredFields()) {
					try {
						int mod = f.getModifiers();
						if (Modifier.isPrivate(mod) || Modifier.isProtected(mod) || Modifier.isStatic(mod) || f.getName().startsWith("this$")) continue;
						f.setAccessible(true);
						String val = toString(f.get(o));
						if (!first) sb.append(',');
						first = false;
						sb.append(String.format("%s=%s", f.getName(), val));
					} catch (Exception e) {
					}
				}
				sb.append(']');
				usedClass.remove(c);
				return sb.toString();
			} else {
				return o.toString();
			}
		}
	}
	
	private static int getDepth() {
		StackTraceElement[] st = new Throwable().getStackTrace();
		int depth = 0;
		for (StackTraceElement s : st) {
			String name = s.getClassName();
			boolean ok = true;
			for (String t : extPrefix) if (name.startsWith(t)) ok = false;
			if (name.equals(Debug.class.getName())) ok = false;
			if (ok) depth++;
		}
		return depth;
	}
	
	/**
	 * スタック段数に応じてインデントする
	 */
	public static void indent() {
		if (indent == 0) return;
		int depth = max(0, getDepth() - stackBase);
		char[] cs = new char[depth * indent];
		fill(cs, ' ');
		System.err.print(cs);
	}
	
	/**
	 * インデントの基準となるスタックの深さを設定
	 */
	public static void setStackBase() {
		stackBase = getDepth();
	}
	
	/**
	 * デバッグ用出力
	 */
	public static void print(Object...os) {
		if (silent) return;
		String str = toString(os);
		if (os != null && os.length < 2) str = str.substring(1, str.length() - 1);
		indent();
		System.err.println(str);
	}
	
	/**
	 * 名前付きデバッグ出力
	 */
	public static void printV(Object...os) {
		if (silent) return;
		indent();
		for (int i = 0; i < os.length; i += 2) {
			if (i > 0) System.err.print(", ");
			System.err.printf("%s = %s", os[i], toString(os[i + 1]));
		}
		System.err.println();
	}
	
	/**
	 * 配列を一行ずつ出力する
	 */
	public static void print1D(Object os) {
		if (silent) return;
		Object[] is = toObjectArray(os);
		for (Object i : is) {
			indent();
			if (i.getClass() == String.class) System.err.println(i);
			else System.err.println(toString(i));
		}
	}
	
	/**
	 * 配列を二次元の表で出力する
	 */
	public static void print2D(Object[] os) {
		if (silent) return;
		int n = os.length;
		if (n == 0) return;
		Object[][] is = new Object[n][];
		for (int i = 0; i < n; i++) is[i] = toObjectArray(os[i]);
		int m = is[0].length;
		String[][] ss = new String[n + 1][m + 1];
		ss[0][0] = "-";
		for (int i = 0; i < n; i++) ss[1 + i][0] = "" + i;
		for (int j = 0; j < m; j++) ss[0][1 + j] = "" + j;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				ss[1 + i][1 + j] = toString(is[i][j]);
			}
		}
		int[] size = new int[m + 1];
		for (int i = 0; i <= n; i++) {
			for (int j = 0; j <= m; j++) {
				size[j] = max(size[j], ss[i][j].length());
			}
		}
		for (int i = 0; i <= n; i++) {
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j <= m; j++) {
				sb.append('|');
				if (j == 1) sb.append('|');
				for (int k = 0; k < size[j] - ss[i][j].length(); k++) sb.append(' ');
				sb.append(ss[i][j]);
			}
			sb.append('|');
			indent();
			System.err.println(sb);
		}
	}
	
	/**
	 * 配列を二次元の表で出力する
	 */
	public static void print2D(Object o) {
		if (silent) return;
		print2D(new Object[]{o});
	}
	
	/**
	 * 呼び出し元の位置を出力する
	 */
	public static void printPos() {
		if (silent) return;
		indent();
		System.err.println(new Throwable().getStackTrace()[1]);
	}
	
	/**
	 * アサート
	 */
	public static void check(boolean b) {
		if (!b) {
			AssertionError e = new AssertionError();
			e.setStackTrace(copyOfRange(e.getStackTrace(), 1, e.getStackTrace().length));
			throw e;
		}
	}
	
	/**
	 * プリミティブの配列をオブジェクトの配列に変換する
	 */
	public static Object[] toObjectArray(Object os) {
		int len = Array.getLength(os);
		Object[] res = new Object[len];
		for (int i = 0; i < len; i++) res[i] = Array.get(os, i);
		return res;
	}
	
	/**
	 * オブジェクトの配列をプリミティブの配列に変換する
	 */
	@SuppressWarnings("unchecked")
	public static <T> T toPrimitiveArray(Object os) {
		int len = Array.getLength(os);
		Class<?> c;
		if (os instanceof Byte[]) c = byte.class;
		else if (os instanceof Short[]) c = short.class;
		else if (os instanceof Integer[]) c = int.class;
		else if (os instanceof Long[]) c = long.class;
		else if (os instanceof Float[]) c = float.class;
		else if (os instanceof Double[]) c = double.class;
		else if (os instanceof Character[]) c = char.class;
		else if (os instanceof Boolean[]) c = boolean.class;
		else throw new RuntimeException();
		Object res = Array.newInstance(c, len);
		for (int i = 0; i < len; i++) Array.set(res, i, Array.get(os, i));
		return (T)res;
	}
	
	/**
	 * 一時停止
	 */
	public static void stop() {
		JOptionPane.showMessageDialog(null, new Throwable().getStackTrace()[1] + "\nPress OK to continue", "Stopped", JOptionPane.DEFAULT_OPTION);
	}
	
	/**
	 * 現在のメソッド名を取得する．
	 */
	public static String getMethodName() {
		return new Throwable().getStackTrace()[1].getMethodName();
	}
	
}
