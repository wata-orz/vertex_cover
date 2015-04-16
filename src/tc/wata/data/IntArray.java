package tc.wata.data;

import static java.util.Arrays.*;

import java.util.*;

/**
 * ArrayList&lt;Integer&gt;の高速版
 */
public class IntArray {
	
	public int[] get = new int[4];
	public int length;
	
	public void add(int v) {
		if (length == get.length) get = copyOf(get, length * 2);
		get[length++] = v;
	}
	
	public int[] toArray() {
		return copyOf(get, length);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < length; i++) {
			if (i > 0) sb.append(',');
			sb.append(get[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
}
