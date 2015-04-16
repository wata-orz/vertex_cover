package tc.wata.debug;

import java.io.*;

import tc.wata.util.*;

/**
 * tryブロックでの自動Close機能により時間計測を行う．
 */
public class Bench implements Closeable {
	
	String title;
	long sTime;
	
	public Bench() {
		sTime = System.currentTimeMillis();
	}
	
	public Bench(String s, Object...args) {
		title = String.format(s, args);
		System.err.printf("%s: start%n", title);
		sTime = System.currentTimeMillis();
	}
	
	@Override
	public void close() {
		if (title != null) System.err.printf("%s: ", title);
		System.err.printf("%.3f sec%n", (System.currentTimeMillis() - sTime) * 1e-3);
	}
	
}
