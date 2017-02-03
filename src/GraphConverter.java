import static java.util.Arrays.*;

import java.io.*;
import java.util.zip.*;

import tc.wata.data.*;
import tc.wata.debug.*;
import tc.wata.io.*;
import tc.wata.util.*;
import tc.wata.util.SetOpt.*;

public class GraphConverter {
	
	@Option(abbr = 'f', required = true)
	public String file;
	
	@Option(abbr = 'o')
	public String out;
	
	@Option(abbr = 't', required = true)
	public String type = "snap";
	
	@Option(abbr = 'u')
	public boolean undirected = false;
	
	@Option(abbr = 'c')
	public boolean component = false;
	
	@Option(abbr = 's')
	public boolean sorting = false;
	
	public int n;
	public int[][] adj;
	public int[][] rev;
	public int[] vertexID;
	
	int[] readEdges() throws IOException {
		InputStream in = new FileInputStream(file);
		if (file.endsWith(".gz")) in = new GZIPInputStream(in);
		try (FastScanner sc = new FastScanner(in)) {
			IntArray list = new IntArray();
			if (type.equals("snap")) {
				while (sc.peek() == '#') sc.skipLine();
				while (sc.hasNext()) list.add(sc.nextInt());
			} else if (type.equals("dimacs")) {
				sc.skipLine();
				while (sc.hasNext()) {
					Debug.check(sc.next().equals("p"));
					list.add(sc.nextInt());
					list.add(sc.nextInt());
				}
			} else {
				throw new IllegalArgumentException("IllegalType: " + type);
			}
			return list.toArray();
		}
	}
	
	void createAdj(int[] es) {
		int[] sorted = es.clone();
		Utils.radixSort(sorted);
		sorted = Utils.unique(sorted);
		vertexID = sorted.clone();
		n = sorted.length;
		int maxID = 0;
		for (int e : es) if (maxID < e) maxID = e;
		int[] ids = new int[maxID + 1];
		for (int i = 0; i < n; i++) ids[sorted[i]] = i;
		for (int i = 0; i < es.length; i++) es[i] = ids[es[i]];
		if (undirected) {
			int m = es.length;
			es = copyOf(es, m * 2);
			for (int i = 0; i < m; i += 2) {
				es[m + i] = es[i + 1];
				es[m + i + 1] = es[i];
			}
		}
		int[] deg = new int[n];
		for (int i = 0; i < es.length; i += 2) if (es[i] != es[i + 1]) deg[es[i]]++;
		adj = new int[n][];
		for (int i = 0; i < n; i++) adj[i] = new int[deg[i]];
		fill(deg, 0);
		for (int i = 0; i < es.length; i += 2) if (es[i] != es[i + 1]) adj[es[i]][deg[es[i]]++] = es[i + 1];
	}
	
	void createRev() {
		int[] deg = new int[n];
		for (int i = 0; i < n; i++) {
			for (int e : adj[i]) deg[e]++;
		}
		rev = new int[n][];
		for (int i = 0; i < n; i++) rev[i] = new int[deg[i]];
		fill(deg, 0);
		for (int i = 0; i < n; i++) {
			for (int e : adj[i]) rev[e][deg[e]++] = i;
		}
	}
	
	void sorting() {
		createRev();
		adj = rev;
		for (int i = 0; i < n; i++) adj[i] = Utils.unique(adj[i]);
		createRev();
		int[][] tmp = adj;
		adj = rev;
		rev = tmp;
	}
	
	void component() {
		int[] comp = new int[n];
		int[] que = new int[n];
		fill(comp, -1);
		int max = -1, maxID = 0;
		for (int i = 0; i < n; i++) if (comp[i] < 0) {
			int qs = 0, qt = 0;
			que[qt++] = i;
			comp[i] = i;
			int size = 0;
			while (qs < qt) {
				int u = que[qs++];
				size++;
				for (int v : adj[u]) if (comp[v] < 0) {
					comp[v] = i;
					que[qt++] = v;
				}
				if (!undirected) {
					for (int v : rev[u]) if (comp[v] < 0) {
						comp[v] = i;
						que[qt++] = v;
					}
				}
			}
			if (max < size) {
				max = size;
				maxID = i;
			}
		}
		int[] ids = new int[n];
		int p = 0;
		for (int i = 0; i < n; i++) if (comp[i] == maxID) ids[i] = p++;
		int[][] tmp = new int[max][];
		for (int i = 0; i < n; i++) if (comp[i] == maxID) {
			tmp[ids[i]] = new int[adj[i].length];
			for (int j = 0; j < adj[i].length; j++) tmp[ids[i]][j] = ids[adj[i][j]];
		}
		n = max;
		adj = tmp;
		createRev();
	}
	
	void degSort() {
		int[] deg = new int[n];
		for (int i = 0; i < n; i++) deg[i] -= adj[i].length;
		if (!undirected) {
			for (int i = 0; i < n; i++) deg[i] -= rev[i].length;
		}
		int[] sorted = Utils.indexSort(deg);
		int[] ids = new int[n];
		for (int i = 0; i < n; i++) ids[sorted[i]] = i;
		int[] vid = new int[n];
		for (int i = 0; i < n; i++) vid[i] = vertexID[sorted[i]];
		vertexID = vid;
		int[][] tmp = new int[n][];
		for (int i = 0; i < n; i++) {
			tmp[ids[i]] = new int[adj[i].length];
			for (int j = 0; j < adj[i].length; j++) tmp[ids[i]][j] = ids[adj[i][j]];
		}
		adj = tmp;
		sorting();
	}
	
	public void read() throws IOException {
		createAdj(readEdges());
		sorting();
		if (component) {
			throw null;
		}
		if (sorting) degSort();
	}
	
	public static void main(String[] args) throws IOException {
		GraphConverter g = new GraphConverter();
		args = SetOpt.setOpt(g, args);
		g.read();
		String out = g.file;
		if (out.endsWith(".gz")) out = out.substring(0, out.length() - 3);
		if (out.endsWith(".txt")) out = out.substring(0, out.length() - 4);
		out += ".dat";
		if (g.out != null) out = g.out;
		GraphIO io = new GraphIO();
		io.n = g.n;
		io.adj = g.adj;
		io.write(new File(out));
	}
	
}
