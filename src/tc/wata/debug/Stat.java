package tc.wata.debug;

import java.io.*;
import java.util.*;

/**
 * デバッグ用の呼び出し回数・時間計測など
 */
public class Stat implements Closeable {
	
	private String key;
	
	public Stat(String key) {
		this.key = key;
		start(key);
	}
	
	@Override
	public void close() {
		end(key);
	}
	
	private static long sTime;
	
	private static HashMap<String, long[]> stat = new HashMap<String, long[]>();
	
	/**
	 * 初期化する
	 */
	public static void init() {
		stat.clear();
	}
	
	/**
	 * 時間計測を開始する
	 */
	public static void start(String key) {
		long[] s = stat.get(key);
		if (s == null) stat.put(key, s = new long[2]);
		s[0] -= System.currentTimeMillis();
		s[1]++;
	}
	
	/**
	 * 時間計測を終了する
	 */
	public static void end(String key) {
		long t = System.currentTimeMillis();
		stat.get(key)[0] += t;
	}
	
	/**
	 * カウンターを増やす
	 */
	public static void count(String key) {
		count(key, 1);
	}
	
	/**
	 * カウンターを指定の数だけ増やす
	 */
	public static void count(String key, long add) {
		long[] s = stat.get(key);
		if (s == null) stat.put(key, s = new long[2]);
		s[1] += add;
	}
	
	/**
	 * カウンターの値を返す
	 */
	public static long getCount(String key) {
		long[] s = stat.get(key);
		if (s == null) return 0;
		return s[1];
	}
	
	/**
	 * 統計情報を表示する
	 */
	public static void stat() {
		String[] keys = stat.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		for (String key : keys) {
			long[] s = stat.get(key);
			long t = s[0];
			if (t < 0) t += System.currentTimeMillis();
			System.err.printf("%s: %.3f (%d)%n", key, t * 1e-3, s[1]);
		}
	}
	
	/**
	 * 終了時に統計情報を表示させる
	 */
	public static void setShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				stat();
			}
		});
	}
	
}
