import java.io.*;
import java.nio.*;

import tc.wata.io.*;

public class GraphIO {
	
	public int n;
	public int[][] adj;
	
	public void read(File file) {
		ByteBuffer in = IOUtils.getReadBuffer(file);
		if (in == null) throw new IllegalArgumentException("No such file: " + file);
		n = in.getInt();
		adj = new int[n][];
		for (int i = 0; i < n; i++) {
			int d = in.getInt();
			adj[i] = new int[d];
			for (int j = 0; j < d; j++) adj[i][j] = in.getInt();
		}
	}
	
	public void write(File file) {
		int m = 0;
		for (int i = 0; i < n; i++) m += adj[i].length;
		long size = 4L * (1 + n + m);
		ByteBuffer out = IOUtils.getWriteBuffer(file, size);
		out.putInt(n);
		for (int i = 0; i < n; i++) {
			out.putInt(adj[i].length);
			for (int e : adj[i]) out.putInt(e);
		}
	}
	
}
