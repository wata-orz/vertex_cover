package tc.wata.util;

import static java.lang.Math.*;

import java.util.*;

/**
 * ユーティリティ
 */
public class Utils {
	
	private Utils() {
	}
	
	/**
	 * 最小値．
	 */
	public static int minAll(int...is) {
		int res = is[0];
		for (int i = 1; i < is.length; i++) {
			if (res > is[i]) res = is[i];
		}
		return res;
	}
	
	/**
	 * 最大値．
	 */
	public static int maxAll(int...is) {
		int res = is[0];
		for (int i = 1; i < is.length; i++) {
			if (res < is[i]) res = is[i];
		}
		return res;
	}
	
	/**
	 * is[i]>=vとなる最小のiを求める．
	 */
	public static int lowerBound(int[] is, int v) {
		int s = -1, t = is.length;
		while (t - s > 1) {
			int m = (s + t) >>> 1;
			if (is[m] >= v) t = m;
			else s = m;
		}
		return t;
	}
	
	/**
	 * is[i]>vとなる最小のiを求める．
	 */
	public static int upperBound(int[] is, int v) {
		int s = -1, t = is.length;
		while (t - s > 1) {
			int m = (s + t) >>> 1;
			if (is[m] > v) t = m;
			else s = m;
		}
		return t;
	}
	
	/**
	 * 重複の除去．入力はソートされていること．
	 */
	public static int[] unique(int[] is) {
		int n = is.length, m = 0;
		int[] js = new int[n];
		for (int i = 0; i < n; i++) {
			if (m == 0 || js[m - 1] != is[i]) js[m++] = is[i];
		}
		int[] res = new int[m];
		System.arraycopy(js, 0, res, 0, m);
		return res;
	}
	
	/**
	 * Integer[] -> int[] への変換
	 */
	public static int[] toi(Integer[] Is) {
		int n = Is.length;
		int[] is = new int[n];
		for (int i = 0; i < n; i++) is[i] = Is[i];
		return is;
	}
	
	/**
	 * Collection&lt;Integer&gt;からint[]への変換
	 */
	public static int[] toi(Collection<Integer> c) {
		return toi(c.toArray(new Integer[0]));
	}
	
	/**
	 * Long[] -> long[] への変換
	 */
	public static long[] tol(Long[] Is) {
		int n = Is.length;
		long[] is = new long[n];
		for (int i = 0; i < n; i++) is[i] = Is[i];
		return is;
	}
	
	/**
	 * Collection&lt;Long&gt;からlong[]への変換
	 */
	public static long[] tol(Collection<Long> c) {
		return tol(c.toArray(new Long[0]));
	}
	
	/**
	 * Double[] -> double[] への変換
	 */
	public static double[] tod(Double[] Ds) {
		int n = Ds.length;
		double[] ds = new double[n];
		for (int i = 0; i < n; i++) ds[i] = Ds[i];
		return ds;
	}
	
	/**
	 * Collection&lt;Double&gt;からdouble[]への変換
	 */
	public static double[] tod(Collection<Double> c) {
		return tod(c.toArray(new Double[0]));
	}
	
	/**
	 * xが[lb,ub)に入っているか
	 */
	public static boolean inRange(int x, int lb, int ub) {
		return lb <= x && x < ub;
	}
	
	/**
	 * 辞書順で次の置換を返す
	 */
	public static boolean nextPermutation(int[] is) {
		int n = is.length;
		for (int i = n - 1; i > 0; i--) {
			if (is[i - 1] < is[i]) {
				int j = n;
				while (is[i - 1] >= is[--j]);
				swap(is, i - 1, j);
				rev(is, i, n);
				return true;
			}
		}
		rev(is, 0, n);
		return false;
	}
	
	/**
	 * i番目とj番目を入れ替え
	 */
	public static void swap(int[] is, int i, int j) {
		int t = is[i]; is[i] = is[j]; is[j] = t;
	}
	
	/**
	 * [s,t)を反転
	 */
	public static void rev(int[] is, int s, int t) {
		while (s < --t) swap(is, s++, t);
	}
	
	/**
	 * 空白で区切る．
	 * 連続する空白に対応．
	 * 先頭・末尾の空白は除去され，空白のみは長さ0になる．
	 */
	public static String[] split(String str) {
		ArrayList<String> list = new ArrayList<>();
		int s = 0, n = str.length();
		for (;;) {
			while (s < n && Character.isWhitespace(str.charAt(s))) s++;
			if (s == n) break;
			int t = s;
			while (t < n && !Character.isWhitespace(str.charAt(t))) t++;
			list.add(str.substring(s, t));
			s = t;
		}
		return list.toArray(new String[0]);
	}
	
	/**
	 * 文字列を書き換える．
	 */
	public static String replace(String str, int id, char c) {
		return str.substring(0, id) + c + str.substring(id + 1);
	}
	
	/**
	 * 桁数指定の二進数．
	 */
	public static char[] toBinary(long v, int len) {
		char[] cs = new char[len];
		for (int i = 0; i < len; i++) {
			cs[i] = (v >> (len - i - 1) & 1) != 0 ? '1' : '0';
		}
		return cs;
	}
	
	/**
	 * 累積和
	 */
	public static int[] accumulate(int[] a) {
		int n = a.length;
		int[] ac = new int[n + 1];
		for (int i = 0; i < n; i++) ac[i + 1] = ac[i] + a[i];
		return ac;
	}
	
	/**
	 * 配列の辞書順比較
	 */
	public static int compArray(int[] a, int[] b) {
		for (int i = 0; i < a.length && i < b.length; i++) {
			if (a[i] < b[i]) return -1;
			if (a[i] > b[i]) return 1;
		}
		return a.length - b.length;
	}
	
	public static <T extends Comparable<T>> int compList(ArrayList<T> a, ArrayList<T> b) {
		int s = min(a.size(), b.size());
		for (int i = 0; i < s; i++) {
			int cmp = a.get(i).compareTo(b.get(i));
			if (cmp != 0) return cmp;
		}
		return a.size() - b.size();
	}
	
	/**
	 * radixSort．
	 */
	public static void radixSort(int[] vs) {
		int n = vs.length;
		int[] us = new int[n];
		int[] num = new int[1 << 8];
		for (int i = 0; i < n; i++) vs[i] ^= 1 << 31;
		for (int i = 0; i < 32; i += 8) {
			Arrays.fill(num, 0);
			for (int j = 0; j < n; j++) {
				num[vs[j] >>> i & 0xff]++;
			}
			for (int j = 0; j < num.length - 1; j++) num[j + 1] += num[j];
			for (int j = n - 1; j >= 0; j--) {
				int p = vs[j] >>> i & 0xff;
				us[--num[p]] = vs[j];
			}
			int[] t = vs; vs = us; us = t;
		}
		for (int i = 0; i < n; i++) vs[i] ^= 1 << 31;
	}
	
	/**
	 * radixSort．
	 */
	public static void radixSort(long[] vs) {
		int n = vs.length;
		long[] us = new long[n];
		int[] num = new int[1 << 8];
		for (int i = 0; i < n; i++) vs[i] ^= 1L << 63;
		for (int i = 0; i < 64; i += 8) {
			Arrays.fill(num, 0);
			for (int j = 0; j < n; j++) {
				num[(int)(vs[j] >>> i & 0xff)]++;
			}
			for (int j = 0; j < num.length - 1; j++) num[j + 1] += num[j];
			for (int j = n - 1; j >= 0; j--) {
				int p = (int)(vs[j] >>> i & 0xff);
				us[--num[p]] = vs[j];
			}
			long[] t = vs; vs = us; us = t;
		}
		for (int i = 0; i < n; i++) vs[i] ^= 1L << 63;
	}
	
	/**
	 * ソート後のインデックスを返す．<br>
	 * 入力はソートされない．<br>
	 * radixソートによる実装．<br>
	 */
	public static int[] indexSort(int[] vs) {
		int n = vs.length;
		int[] is = new int[n], js = new int[n];
		for (int i = 0; i < n; i++) is[i] = i;
		int[] us = vs.clone(), ws = new int[n];
		int[] num = new int[1 << 8];
		for (int i = 0; i < n; i++) us[i] ^= 1 << 31;
		for (int i = 0; i < 4; i++) {
			Arrays.fill(num, 0);
			for (int j = 0; j < n; j++) {
				num[us[j] & 0xff]++;
			}
			for (int j = 0; j < num.length - 1; j++) num[j + 1] += num[j];
			for (int j = n - 1; j >= 0; j--) {
				int p = us[j] & 0xff;
				num[p]--;
				js[num[p]] = is[j];
				ws[num[p]] = us[j] >>> 8;
			}
			int[] t = is; is = js; js = t;
			t = us; us = ws; ws = t;
		}
		return is;
	}
	
	/**
	 * ソート後のインデックスを返す．<br>
	 * 入力はソートされない．<br>
	 * radixソートによる実装．<br>
	 */
	public static int[] indexSort(long[] vs) {
		int n = vs.length;
		int[] is = new int[n], js = new int[n];
		for (int i = 0; i < n; i++) is[i] = i;
		long[] us = vs.clone(), ws = new long[n];
		int[] num = new int[1 << 8];
		for (int i = 0; i < n; i++) us[i] ^= 1L << 63;
		for (int i = 0; i < 8; i++) {
			Arrays.fill(num, 0);
			for (int j = 0; j < n; j++) {
				num[(int)(us[j] & 0xff)]++;
			}
			for (int j = 0; j < num.length - 1; j++) num[j + 1] += num[j];
			for (int j = n - 1; j >= 0; j--) {
				int p = (int)(us[j] & 0xff);
				num[p]--;
				js[num[p]] = is[j];
				ws[num[p]] = us[j] >>> 8;
			}
			int[] t = is; is = js; js = t;
			long[] t2 = us; us = ws; ws = t2;
		}
		return is;
	}
	
	/**
	 * ソート列二つをマージ
	 */
	public static int[] merge(int[] a, int[] b) {
		int[] c = new int[a.length + b.length];
		int p = 0, q = 0;
		while (p < a.length && q < b.length) {
			if (a[p] < b[q]) c[p + q] = a[p++];
			else c[p + q] = b[q++];
		}
		while (p < a.length) c[p + q] = a[p++];
		while (q < b.length) c[p + q] = b[q++];
		return c;
	}
	
	/**
	 * 二次元配列の初期化
	 */
	public static void fill(int[][] A, int a) {
		for (int i = 0; i < A.length; i++) Arrays.fill(A[i], a);
	}
	
	/**
	 * 二次元配列のコピー
	 */
	public static int[][] copy(int[][] A) {
		int[][] B = new int[A.length][];
		for (int i = 0; i < A.length; i++) B[i] = A[i].clone();
		return B;
	}
	
	/**
	 * 行列の転置
	 */
	public static int[][] transpose(int[][] A) {
		int[][] B = new int[A.length][A.length];
		for (int i = 0; i < A.length; i++) {
			for (int j = 0; j < A.length; j++) {
				B[i][j] = A[j][i];
			}
		}
		return B;
	}
	
	/**
	 * 配列をスペース区切りで文字列に変換
	 */
	public static String toString(int[] a) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < a.length; i++) {
			if (i > 0) sb.append(' ');
			sb.append(a[i]);
		}
		return sb.toString();
	}
	
	/**
	 * 指定されたms間sleepする．
	 * try-catchを省略しただけ．
	 */
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}
	
}
