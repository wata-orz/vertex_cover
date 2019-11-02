import static java.util.Arrays.*;

import java.util.*;

import tc.wata.data.*;
import tc.wata.debug.*;

public class VCSolver {
	
	public static Random rand = new Random(4327897);
	
	public static int REDUCTION = 3;
	
	public static int LOWER_BOUND = 4;
	
	public static int BRANCHING = 2;
	
	public static boolean outputLP = true;
	
	public static long nBranchings;
	
	public static int debug;
	int depth = 0, maxDepth = 10, rootDepth;
	
	double SHRINK = 0.5;
	
	public int n, N;
	public int[][] adj;
	
	/**
	 * current best solution
	 */
	public int opt, y[];
	
	/**
	 * current solution (-1: not determined, 0: not in the vc, 1: in the vc, 2: removed by foldings)
	 */
	int crt, x[];
	
	/**
	 * #remaining vertices
	 */
	int rn;
	
	/**
	 * max flow
	 */
	int[] in, out;
	
	/**
	 * lower bound
	 */
	int lb;
	
	/**
	 * Packing constraints
	 */
	ArrayList<int[]> packing;
	
	public VCSolver(int[][] adj, int N) {
		n = adj.length;
		this.N = N;
		this.adj = adj;
		opt = n;
		y = new int[N];
		for (int i = 0; i < n; i++) y[i] = 1;
		for (int i = n; i < N; i++) y[i] = 2;
		crt = 0;
		x = new int[N];
		for (int i = 0; i < n; i++) x[i] = -1;
		for (int i = n; i < N; i++) x[i] = 2;
		rn = n;
		used = new FastSet(n * 2);
		restore = new int[n];
		modifieds = new Modified[N];
		modifiedN = 0;
		in = new int[n];
		out = new int[n];
		fill(in, -1);
		fill(out, -1);
		que = new int[n * 2];
		level = new int[n * 2];
		iter = new int[n * 2];
		packing = new ArrayList<int[]>();
		modTmp = new int[n];
	}
	
	FastSet used;
	
	int deg(int v) {
		Debug.check(x[v] < 0);
		int deg = 0;
		for (int u : adj[v]) if (x[u] < 0) deg++;
		return deg;
	}
	
	int restore[];
	
	void set(int v, int a) {
		Debug.check(x[v] < 0);
		crt += a;
		x[v] = a;
		restore[--rn] = v;
		if (a == 0) {
			for (int u : adj[v]) if (x[u] < 0) {
				x[u] = 1;
				crt++;
				restore[--rn] = u;
			}
		}
	}
	
	abstract class Modified {
		
		int add;
		int[] removed;
		int[] vs;
		int[][] oldAdj;
		
		Modified(int add, int[] removed, int[] vs, int[][] newAdj) {
			this.add = add;
			this.removed = removed;
			this.vs = vs;
			oldAdj = new int[vs.length][];
			crt += add;
			for (int i = 0; i < removed.length; i++) restore[--rn] = -1;
			for (int v : removed) {
				Debug.check(x[v] < 0);
				x[v] = 2;
			}
			for (int i = 0; i < vs.length; i++) {
				oldAdj[i] = adj[vs[i]];
				adj[vs[i]] = newAdj[i];
			}
		}
		
		Modified(int[] removed, int[] vs) {
			this.removed = removed;
			this.vs = vs;
		}
		
		void restore() {
			crt -= add;
			rn += removed.length;
			for (int v : removed) x[v] = -1;
			for (int i = 0; i < vs.length; i++) {
				adj[vs[i]] = oldAdj[i];
				int inV = in[vs[i]], outV = out[vs[i]];
				for (int u : adj[vs[i]]) {
					if (u == inV) inV = -1;
					if (u == outV) outV = -1;
				}
				if (inV >= 0) {
					out[in[vs[i]]] = -1;
					in[vs[i]] = -1;
				}
				if (outV >= 0) {
					in[out[vs[i]]] = -1;
					out[vs[i]] = -1;
				}
			}
		}
		
		abstract void reverse(int[] x);
		
	}
	
	class Fold extends Modified {
		
		Fold(int add, int[] removed, int[] vs, int[][] newAdj) {
			super(add, removed, vs, newAdj);
		}
		
		Fold(int[] removed, int[] vs) {
			super(removed, vs);
		}
		
		@Override
		void reverse(int[] x) {
			int k = removed.length / 2;
			if (x[vs[0]] == 0) {
				for (int i = 0; i < k; i++) x[removed[i]] = 1;
				for (int i = 0; i < k; i++) x[removed[k + i]] = 0;
			} else if (x[vs[0]] == 1) {
				for (int i = 0; i < k; i++) x[removed[i]] = 0;
				for (int i = 0; i < k; i++) x[removed[k + i]] = 1;
			}
		}
		
	}
	
	class Alternative extends Modified {
		
		int k;
		
		Alternative(int add, int[] removed, int[] vs, int[][] newAdj, int k) {
			super(add, removed, vs, newAdj);
			this.k = k;
		}
		
		Alternative(int[] removed, int[] vs, int k) {
			super(removed, vs);
			this.k = k;
		}
		
		@Override
		void reverse(int[] x) {
			boolean A0 = false, A1 = true;
			boolean B0 = false, B1 = true;
			for (int i = 0; i < k; i++) {
				if (x[vs[i]] == 0) A0 = true;
				if (x[vs[i]] != 1) A1 = false;
			}
			for (int i = k; i < vs.length; i++) {
				if (x[vs[i]] == 0) B0 = true;
				if (x[vs[i]] != 1) B1 = false;
			}
			if (A1 || B0) {
				for (int i = 0; i < removed.length / 2; i++) x[removed[i]] = 0;
				for (int i = removed.length / 2; i < removed.length; i++) x[removed[i]] = 1;
			} else if (B1 || A0) {
				for (int i = 0; i < removed.length / 2; i++) x[removed[i]] = 1;
				for (int i = removed.length / 2; i < removed.length; i++) x[removed[i]] = 0;
			}
		}
		
	}
	
	Modified[] modifieds;
	int modifiedN;
	
	int[] modTmp;
	
	void fold(int[] S, int[] NS) {
		Debug.check(NS.length == S.length + 1);
		int[] removed = new int[S.length * 2];
		for (int i = 0; i < S.length; i++) removed[i] = S[i];
		for (int i = 0; i < S.length; i++) removed[S.length + i] = NS[1 + i];
		int s = NS[0];
		used.clear();
		for (int v : S) used.add(v);
		int[] tmp = modTmp;
		int p = 0;
		for (int v : NS) {
			Debug.check(!used.get(v));
			for (int u : adj[v]) if (x[u] < 0 && used.add(u)) {
				tmp[p++] = u;
			}
		}
		int[][] newAdj = new int[p + 1][];
		newAdj[0] = copyOf(tmp, p);
		sort(newAdj[0]);
		int[] vs = new int[p + 1];
		vs[0] = s;
		used.clear();
		for (int v : S) used.add(v);
		for (int v : NS) used.add(v);
		for (int i = 0; i < newAdj[0].length; i++) {
			int v = newAdj[0][i];
			p = 0;
			boolean add = false;
			for (int u : adj[v]) if (x[u] < 0 && !used.get(u)) {
				if (!add && s < u) {
					tmp[p++] = s;
					add = true;
				}
				tmp[p++] = u;
			}
			if (!add) tmp[p++] = s;
			vs[1 + i] = v;
			newAdj[1 + i] = copyOf(tmp, p);
		}
		modifieds[modifiedN++] = new Fold(S.length, removed, vs, newAdj);
	}
	
	void alternative(int[] A, int[] B) {
		Debug.check(A.length == B.length);
		used.clear();
		for (int b : B) for (int u : adj[b]) if (x[u] < 0) used.add(u);
		for (int a : A) for (int u : adj[a]) if (x[u] < 0 && used.get(u)) set(u, 1);
		int p = 0, q = 0;
		int[] tmp = modTmp;
		used.clear();
		for (int b : B) used.add(b);
		for (int a : A) for (int u : adj[a]) if (x[u] < 0 && used.add(u)) tmp[p++] = u;
		int[] A2 = copyOf(tmp, p);
		sort(A2);
		p = 0;
		used.clear();
		for (int a : A) used.add(a);
		for (int b : B) for (int u : adj[b]) if (x[u] < 0 && used.add(u)) tmp[p++] = u;
		int[] B2 = copyOf(tmp, p);
		sort(B2);
		int[] removed = new int[A.length + B.length];
		for (int i = 0; i < A.length; i++) removed[i] = A[i];
		for (int i = 0; i < B.length; i++) removed[A.length + i] = B[i];
		int[] vs = new int[A2.length + B2.length];
		for (int i = 0; i < A2.length; i++) vs[i] = A2[i];
		for (int i = 0; i < B2.length; i++) vs[A2.length + i] = B2[i];
		int[][] newAdj = new int[vs.length][];
		used.clear();
		for (int a : A) used.add(a);
		for (int b : B) used.add(b);
		for (int i = 0; i < vs.length; i++) {
			int v = i < A2.length ? A2[i] : B2[i - A2.length];
			int[] C = i < A2.length ? B2 : A2;
			p = q = 0;
			for (int u : adj[v]) if (x[u] < 0 && !used.get(u)) {
				while (q < C.length && C[q] <= u) {
					if (used.get(C[q])) q++;
					else tmp[p++] = C[q++];
				}
				if (p == 0 || tmp[p - 1] != u) tmp[p++] = u;
			}
			while (q < C.length) {
				if (used.get(C[q])) q++;
				else tmp[p++] = C[q++];
			}
			newAdj[i] = copyOf(tmp, p);
		}
		modifieds[modifiedN++] = new Alternative(removed.length / 2, removed, vs, newAdj, A2.length);
	}
	
	void restore(int n) {
		while (rn < n) {
			int v = restore[rn];
			if (v >= 0) {
				crt -= x[v];
				x[v] = -1;
				rn++;
			} else {
				modifieds[--modifiedN].restore();
			}
		}
	}
	
	void reverse() {
		for (int i = modifiedN - 1; i >= 0; i--) {
			modifieds[i].reverse(y);
		}
	}
	
	int[] que, level, iter;
	
	boolean dinicDFS(int v) {
		while (iter[v] >= 0) {
			int u = adj[v][iter[v]--], w = in[u];
			if (x[u] >= 0) continue;
			if (w < 0 || level[v] < level[w] && iter[w] >= 0 && dinicDFS(w)) {
				in[u] = v;
				out[v] = u;
				return true;
			}
		}
		return false;
	}
	
	void updateLP() {
		try (Stat stat = new Stat("updateLP")) {
			for (int v = 0; v < n; v++) if (out[v] >= 0 && ((x[v] < 0) ^ (x[out[v]] < 0))) {
				in[out[v]] = -1;
				out[v] = -1;
			}
			for (;;) {
				used.clear();
				int qs = 0, qt = 0;
				for (int v = 0; v < n; v++) if (x[v] < 0 && out[v] < 0) {
					level[v] = 0;
					used.add(v);
					que[qt++] = v;
				}
				boolean ok = false;
				while (qs < qt) {
					int v = que[qs++];
					iter[v] = adj[v].length - 1;
					for (int u : adj[v]) if (x[u] < 0 && used.add(n + u)) {
						int w = in[u];
						if (w < 0) ok = true;
						else {
							level[w] = level[v] + 1;
							used.add(w);
							que[qt++] = w;
						}
					}
				}
				if (!ok) break;
				for (int v = n - 1; v >= 0; v--) if (x[v] < 0 && out[v] < 0) {
					dinicDFS(v);
				}
			}
		}
	}
	
	boolean lpReduction() {
		int oldn = rn;
		updateLP();
		try (Stat stat = new Stat("reduce_LP")) {
			for (int v = 0; v < n; v++) {
				if (x[v] < 0 && used.get(v) && !used.get(n + v)) set(v, 0);
			}
			used.clear();
			int p = 0;
			fill(iter, 0);
			for (int s = 0; s < n; s++) if (x[s] < 0 && used.add(s)) {
				int qt = 0;
				que[qt] = s;
				while (qt >= 0) {
					int v = que[qt], u = -1;
					if (v < n) {
						while (iter[v] < adj[v].length) {
							u = n + adj[v][iter[v]++];
							if (x[u - n] < 0 && used.add(u)) break;
							u = -1;
						}
					} else if (used.add(in[v - n])) {
						u = in[v - n];
					}
					if (u >= 0) {
						que[++qt] = u;
					} else {
						level[p++] = v;
						qt--;
					}
				}
			}
			used.clear();
			for (int i = p - 1; i >= 0; i--) if (used.add(level[i])) {
				int v = level[i];
				int qs = 0, qt = 0;
				que[qt++] = v;
				boolean ok = true;
				while (qs < qt) {
					v = que[qs++];
					if (used.get(v >= n ? (v - n) : (v + n))) ok = false;
					if (v >= n) {
						for (int u : adj[v - n]) if (x[u] < 0 && used.add(u)) {
							que[qt++] = u;
						}
					} else if (used.add(n + out[v])) {
						que[qt++] = n + out[v];
					}
				}
				//ok = false;
				if (ok) {
					for (int j = 0; j < qt; j++) {
						v = que[j];
						if (v >= n) set(v - n, 0);
					}
				}
			}
		}
		if (debug >= 3 && depth <= maxDepth && oldn != rn) debug("LP: %d -> %d%n", oldn, rn);
		if (oldn != rn) Stat.count("reduceN_LP", oldn - rn);
		return oldn != rn;
	}
	
	boolean deg1Reduction() {
		try (Stat stat = new Stat("reduce_deg1")) {
			int oldn = rn;
			int[] deg = iter;
			int qt = 0;
			used.clear();
			for (int v = 0; v < n; v++) if (x[v] < 0) {
				deg[v] = n == rn ? adj[v].length : deg(v);
				if (deg[v] <= 1) {
					que[qt++] = v;
					used.add(v);
				}
			}
			while (qt > 0) {
				int v = que[--qt];
				if (x[v] >= 0) continue;
				Debug.check(deg[v] <= 1);
				for (int u : adj[v]) if (x[u] < 0) {
					for (int w : adj[u]) if (x[w] < 0) {
						deg[w]--;
						if (deg[w] <= 1 && used.add(w)) que[qt++] = w;
					}
				}
				set(v, 0);
			}
			if (debug >= 3 && depth <= maxDepth && oldn != rn) debug("deg1: %d -> %d%n", oldn, rn);
			if (oldn != rn) Stat.count("reduceN_deg1", oldn - rn);
			return oldn != rn;
		}
	}
	
	boolean dominateReduction() {
		try (Stat stat = new Stat("reduce_dominate")) {
			int oldn = rn;
			for (int v = 0; v < n; v++) if (x[v] < 0) {
				used.clear();
				used.add(v);
				for (int u : adj[v]) if (x[u] < 0) used.add(u);
				loop : for (int u : adj[v]) if (x[u] < 0) {
					for (int w : adj[u]) if (x[w] < 0 && !used.get(w)) continue loop;
					set(v, 1);
					break;
				}
			}
			if (debug >= 3 && depth <= maxDepth && oldn != rn) debug("dominate: %d -> %d%n", oldn, rn);
			if (oldn != rn) Stat.count("reduceN_dominate", oldn - rn);
			return oldn != rn;
		}
	}
	
	boolean fold2Reduction() {
		try (Stat stat = new Stat("reduce_fold2")) {
			int oldn = rn;
			int[] tmp = level;
			loop : for (int v = 0; v < n; v++) if (x[v] < 0) {
				int p = 0;
				for (int u : adj[v]) if (x[u] < 0) {
					tmp[p++] = u;
					if (p > 2) continue loop;
				}
				if (p < 2) continue;
				for (int u : adj[tmp[0]]) if (u == tmp[1]) {
					set(v, 0);
					continue loop;
				}
				fold(new int[]{v}, copyOf(tmp, 2));
			}
			if (debug >= 3 && depth <= maxDepth && oldn != rn) debug("fold2: %d -> %d%n", oldn, rn);
			if (oldn != rn) Stat.count("reduceN_fold2", oldn - rn);
			return oldn != rn;
		}
	}
	
	boolean twinReduction() {
		try (Stat stat = new Stat("reduce_twin")) {
			int oldn = rn;
			int[] used = iter;
			int uid = 0;
			int[] NS = new int[3];
			for (int i = 0; i < n; i++) used[i] = 0;
			loop : for (int v = 0; v < n; v++) if (x[v] < 0 && deg(v) == 3) {
				int p = 0;
				for (int u : adj[v]) if (x[u] < 0) {
					NS[p++] = u;
					uid++;
					for (int w : adj[u]) if (x[w] < 0 && w != v) {
						if (p == 1) used[w] = uid;
						else if (used[w] == uid - 1) {
							used[w]++;
							if (p == 3 && deg(w) == 3) {
								uid++;
								for (int z : NS) used[z] = uid;
								boolean ind = true;
								for (int z : NS) for (int a : adj[z]) if (x[a] < 0 && used[a] == uid) ind = false;
								if (ind) {
									fold(new int[]{v, w}, NS.clone());
								} else {
									set(v, 0);
									set(w, 0);
								}
								continue loop;
							}
						}
					}
				}
			}
			if (debug >= 3 && depth <= maxDepth && oldn != rn) debug("twin: %d -> %d%n", oldn, rn);
			if (oldn != rn) Stat.count("reduceN_twin", oldn - rn);
			return oldn != rn;
		}
	}
	
	boolean funnelReduction() {
		try (Stat stat = new Stat("reduce_alternative")) {
			int oldn = rn;
			loop : for (int v = 0; v < n; v++) if (x[v] < 0) {
				used.clear();
				int[] tmp = level;
				int p = 0;
				for (int u : adj[v]) if (x[u] < 0 && used.add(u)) {
					tmp[p++] = u;
				}
				if (p <= 1) {
					set(v, 0);
					continue;
				}
				int u1 = -1;
				for (int i = 0; i < p; i++) {
					int d = 0;
					for (int u : adj[tmp[i]]) if (x[u] < 0 && used.get(u)) d++;
					if (d + 1 < p) {
						u1 = tmp[i];
						break;
					}
				}
				if (u1 < 0) {
					set(v, 0);
					continue;
				} else {
					int[] id = iter;
					for (int i = 0; i < p; i++) id[tmp[i]] = -1;
					for (int u : adj[u1]) if (x[u] < 0) id[u] = 0;
					int u2 = -1;
					for (int i = 0; i < p; i++) if (tmp[i] != u1 && id[tmp[i]] < 0) {
						u2 = tmp[i];
						break;
					}
					Debug.check(u2 >= 0);
					used.remove(u1);
					used.remove(u2);
					int d1 = 0, d2 = 0;
					for (int w : adj[u1]) if (x[w] < 0 && used.get(w)) d1++;
					for (int w : adj[u2]) if (x[w] < 0 && used.get(w)) d2++;
					if (d1 < p - 2 && d2 < p - 2) continue;
					for (int i = 0; i < p; i++) {
						int u = tmp[i];
						if (u == u1 || u == u2) continue;
						int d = 0;
						for (int w : adj[u]) if (x[w] < 0 && used.get(w)) d++;
						if (d < p - 3) {
							continue loop;
						}
					}
					int u = (d1 == p - 2) ? u2 : u1;
					alternative(new int[]{v}, new int[]{u});
				}
			}
			if (debug >= 3 && depth <= maxDepth && oldn != rn) debug("funnel: %d -> %d%n", oldn, rn);
			if (oldn != rn) Stat.count("reduceN_funnel", oldn - rn);
			return oldn != rn;
		}
	}
	
	boolean deskReduction() {
		try (Stat stat = new Stat("reduce_desk")) {
			int oldn = rn;
			int[] tmp = level;
			int[] nv = iter;
			for (int i = 0; i < n; i++) nv[i] = -1;
			loop : for (int v = 0; v < n; v++) if (x[v] < 0) {
				int d = 0;
				for (int u : adj[v]) if (x[u] < 0) {
					tmp[d++] = u;
					nv[u] = v;
					if (d > 4) break;
				}
				if (d == 3 || d == 4) {
					int d2 = 0;
					for (int i = 0; i < d; i++) {
						int a = deg(tmp[i]);
						if (a == 3 || a == 4) tmp[d2++] = tmp[i];
					}
					for (int i = 0; i < d2; i++) {
						int u1 = tmp[i];
						int sB1 = 0;
						used.clear();
						for (int w : adj[u1]) if (x[w] < 0 && w != v) {
							used.add(w);
							sB1++;
						}
						for (int j = i + 1; j < d2; j++) {
							int u2 = tmp[j];
							if (used.get(u2)) continue;
							int sB2 = 0;
							for (int w : adj[u2]) if (x[w] < 0 && w != v && !used.get(w)) sB2++;
							if (sB1 + sB2 <= 3) {
								for (int w : adj[u2]) if (x[w] < 0 && used.get(w) && nv[w] != v) {
									int d3 = deg(w);
									if (d3 == 3 || d3 == 4) {
										int sA = d - 2;
										for (int z : adj[w]) if (x[z] < 0 && z != u1 && z != u2 && nv[z] != v) {
											sA++;
										}
										if (sA <= 2) {
											alternative(new int[]{v, w}, new int[]{u1, u2});
											continue loop;
										}
									}
								}
							}
						}
					}
				}
			}
			if (debug >= 3 && depth <= maxDepth && oldn != rn) debug("desk: %d -> %d%n", oldn, rn);
			if (oldn != rn) Stat.count("reduceN_desk", oldn - rn);
			return oldn != rn;
		}
	}
	
	boolean unconfinedReduction() {
		try (Stat stat = new Stat("reduce_unconfined")) {
			int oldn = rn;
			int[] NS = level, deg = iter;
			for (int v = 0; v < n; v++) if (x[v] < 0) {
				used.clear();
				used.add(v);
				int p = 1, size = 0;
				for (int u : adj[v]) if (x[u] < 0) {
					used.add(u);
					NS[size++] = u;
					deg[u] = 1;
				}
				boolean ok = false;
				loop : while (!ok) {
					ok = true;
					for (int i = 0; i < size; i++) {
						int u = NS[i];
						if (deg[u] != 1) continue;
						int z = -1;
						for (int w : adj[u]) if (x[w] < 0 && !used.get(w)) {
							if (z >= 0) {
								z = -2;
								break;
							}
							z = w;
						}
						if (z == -1) {
							if (REDUCTION >= 3) {
								int[] qs = que;
								int q = 0;
								qs[q++] = 1;
								for (int w : adj[v]) if (x[w] < 0) qs[q++] = w;
								packing.add(copyOf(qs, q));
							}
							set(v, 1);
							break loop;
						} else if (z >= 0) {
							ok = false;
							used.add(z);
							p++;
							for (int w : adj[z]) if (x[w] < 0) {
								if (used.add(w)) {
									NS[size++] = w;
									deg[w] = 1;
								} else {
									deg[w]++;
								}
							}
						}
					}
				}
				if (x[v] < 0 && p >= 2) {
					used.clear();
					for (int i = 0; i < size; i++) used.add(NS[i]);
					int[] vs = que;
					for (int i = 0; i < size; i++) {
						vs[i] = vs[n + i] = -1;
						int u = NS[i];
						if (deg[u] != 2) continue;
						int v1 = -1, v2 = -1;
						for (int w : adj[u]) if (x[w] < 0 && !used.get(w)) {
							if (v1 < 0) v1 = w;
							else if (v2 < 0) v2 = w;
							else {
								v1 = v2 = -1;
								break;
							}
						}
						if (v1 > v2) {
							int t = v1;
							v1 = v2;
							v2 = t;
						}
						vs[i] = v1;
						vs[n + i] = v2;
					}
					loop : for (int i = 0; i < size; i++) if (vs[i] >= 0 && vs[n + i] >= 0) {
						int u = NS[i];
						used.clear();
						for (int w : adj[u]) if (x[w] < 0) used.add(w);
						for (int j = i + 1; j < size; j++) if (vs[i] == vs[j] && vs[n + i] == vs[n + j] && !used.get(NS[j])) {
							if (REDUCTION >= 3) {
								int[] qs = que;
								int q = 0;
								qs[q++] = 1;
								for (int w : adj[v]) if (x[w] < 0) qs[q++] = w;
								packing.add(copyOf(qs, q));
							}
							set(v, 1);
							Stat.count("reduceN_diamond");
							break loop;
						}
					}
				}
			}
			if (debug >= 3 && depth <= maxDepth && oldn != rn) debug("unconfined: %d -> %d%n", oldn, rn);
			if (oldn != rn) Stat.count("reduceN_unconfined", oldn - rn);
			return oldn != rn;
		}
	}
	
	int packingReduction() {
		try (Stat stat = new Stat("reduce_packing")) {
			int oldn = rn;
			int[] x2 = x.clone();
			int a = -1;
			for (int i = 0; i < packing.size(); i++) {
				if (a != rn) {
					for (int j = 0; j < N; j++) x2[j] = x[j];
					for (int j = modifiedN - 1; j >= 0; j--) modifieds[j].reverse(x2);
					a = rn;
				}
				int[] ps = packing.get(i);
				int max = ps.length - 1 - ps[0], sum = 0, size = 0;
				int[] S = level;
				for (int j = 1; j < ps.length; j++) {
					int v = ps[j];
					if (x2[v] < 0) S[size++] = v;
					if (x2[v] == 1) sum++;
				}
				if (sum > max) {
					Stat.count("reduceN_packingR");
					return -1;
				} else if (sum == max && size > 0) {
					int[] count = iter;
					used.clear();
					for (int j = 0; j < size; j++) {
						used.add(S[j]);
						count[S[j]] = -1;
					}
					for (int j = 0; j < size; j++) {
						for (int u : adj[S[j]]) if (x[u] < 0) {
							if (used.add(u)) {
								count[u] = 1;
							} else if (count[u] < 0) {
								return -1;
							} else {
								count[u]++;
							}
						}
					}
					for (int j = 0; j < size; j++) {
						for (int u : adj[S[j]]) if (x[u] < 0 && count[u] == 1) {
							int[] tmp = que;
							int p = 0;
							tmp[p++] = 1;
							for (int w : adj[u]) if (x[w] < 0 && !used.get(w)) {
								tmp[p++] = w;
							}
							packing.add(copyOf(tmp, p));
						}
					}
					for (int j = 0; j < size; j++) {
						if (S[j] == 1) return -1;
						Debug.check(x[S[j]] < 0);
						set(S[j], 0);
					}
				} else if (sum + size > max) {
					Debug.check(size >= 2);
					used.clear();
					for (int j = 0; j < size; j++) used.add(S[j]);
					for (int v : adj[S[0]]) if (x[v] < 0 && !used.get(v)) {
						int p = 0;
						for (int u : adj[v]) if (used.get(u)) p++;
						if (sum + p > max) {
							int[] qs = que;
							int q = 0;
							qs[q++] = 2;
							for (int u : adj[v]) if (x[u] < 0) qs[q++] = u;
							packing.add(copyOf(qs, q));
							set(v, 1);
							break;
						}
					}
				}
			}
			if (debug >= 3 && depth <= maxDepth && oldn != rn) debug("packing: %d -> %d%n", oldn, rn);
			if (oldn != rn) Stat.count("reduceN_packing", oldn - rn);
			return oldn != rn ? 1 : 0;
		}
	}
	
	void branching() {
		int oldLB = lb;
		int v = -1, dv = 0;
		int[] mirrors = que;
		int mirrorN = 0;
		try (Stat stat = new Stat("branching")) {
			if (BRANCHING == 0) {
				int p = rand.nextInt(rn);
				for (int i = 0; i < n; i++) if (x[i] < 0 && p-- == 0) v = i;
				dv = deg(v);
			} else if (BRANCHING == 1) {
				dv = n + 1;
				for (int u = 0; u < n; u++) if (x[u] < 0) {
					int deg = deg(u);
					if (dv > deg) {
						v = u;
						dv = deg;
					}
				}
			} else if (BRANCHING == 2) {
				dv = -1;
				long minE = 0;
				for (int u = 0; u < n; u++) if (x[u] < 0) {
					int deg = deg(u);
					if (dv > deg) continue;
					long e = 0;
					used.clear();
					for (int w : adj[u]) if (x[w] < 0) used.add(w);
					for (int w : adj[u]) if (x[w] < 0) {
						for (int w2 : adj[w]) if (x[w2] < 0 && used.get(w2)) e++;
					}
					if (dv < deg || dv == deg && minE > e) {
						dv = deg;
						minE = e;
						v = u;
					}
				}
			}
			int[] ps = iter;
			for (int i = 0; i < n; i++) ps[i] = -2;
			used.clear();
			used.add(v);
			for (int u : adj[v]) if (x[u] < 0) {
				used.add(u);
				ps[u] = -1;
			}
			for (int u : adj[v]) if (x[u] < 0) {
				for (int w : adj[u]) if (x[w] < 0 && used.add(w)) {
					int c1 = dv;
					for (int z : adj[w]) if (x[z] < 0 && ps[z] != -2) {
						ps[z] = w;
						c1--;
					}
					boolean ok = true;
					for (int u2 : adj[v]) if (x[u2] < 0 && ps[u2] != w) {
						int c2 = 0;
						for (int w2 : adj[u2]) if (x[w2] < 0 && ps[w2] != w && ps[w2] != -2) c2++;
						if (c2 != c1 - 1) {
							ok = false;
							break;
						}
					}
					if (ok) mirrors[mirrorN++] = w;
				}
			}
		}
		int pn = rn;
		int oldP = packing.size();
		if (REDUCTION >= 3) {
			int[] tmp = level;
			int p = 0;
			tmp[p++] = mirrorN > 0 ? 2 : 1;
			for (int u : adj[v]) if (x[u] < 0) tmp[p++] = u;
			packing.add(copyOf(tmp, p));
		}
		set(v, 1);
		for (int i = 0; i < mirrorN; i++) set(mirrors[i], 1);
		if (debug >= 2 && depth <= maxDepth) {
			if (mirrorN > 0) debug("branchMirror (%d, %d): 1%n", dv, mirrorN);
			else debug("branch (%d): 1%n", dv);
		}
		depth++;
		rec();
		while (packing.size() > oldP) packing.remove(packing.size() - 1);
		lb = oldLB;
		depth--;
		restore(pn);
		if (lb >= opt) return;
		nBranchings++;
		if (mirrorN == 0) {
			used.clear();
			used.add(v);
			for (int u : adj[v]) if (x[u] < 0) used.add(u);
			if (REDUCTION >= 3) {
				int[] ws = new int[n];
				fill(ws, -1);
				for (int u : adj[v]) if (x[u] < 0) {
					int[] tmp = level;
					int p = 0;
					tmp[p++] = 1;
					for (int w : adj[u]) if (x[w] < 0 && !used.get(w)) {
						tmp[p++] = w;
						ws[w] = u;
					}
					Debug.check(p >= 2);
					for (int u2 : adj[tmp[1]]) if (x[u2] < 0 && used.get(u2) && u2 != u) {
						int c = 0;
						for (int w : adj[u2]) if (x[w] < 0) {
							if (ws[w] == u) c++;
							else if (w == u || !used.get(w)) {
								c = -1;
								break;
							}
						}
						if (c == p - 1) {
							tmp[0] = 2;
							break;
						}
					}
					packing.add(copyOf(tmp, p));
				}
			}
		}
		set(v, 0);
		if (debug >= 2 && depth <= maxDepth) debug("branch (%d): 0%n", dv);
		depth++;
		rec();
		while (packing.size() > oldP) packing.remove(packing.size() - 1);
		lb = oldLB;
		depth--;
		restore(pn);
	}
	
	int lpLowerBound() {
		try (Stat stat = new Stat("lb_LP")) {
			return crt + (rn + 1) / 2;
		}
	}
	
	int cycleLowerBound() {
		try (Stat stat = new Stat("lb_cycle")) {
			int lb = crt;
			int[] id = iter;
			for (int i = 0; i < n; i++) id[i] = -1;
			int[] pos = que;
			int[] S = level, S2 = modTmp;
			for (int i = 0; i < n; i++) if (x[i] < 0 && id[i] < 0) {
				int v = i;
				int size = 0;
				do {
					Debug.check(id[v] < 0);
					id[v] = i;
					v = out[v];
					pos[v] = size;
					S[size++] = v;
				} while (v != i);
				boolean clique = true;
				for (int j = 0; j < size; j++) {
					v = S[j];
					int num = 0;
					for (int u : adj[v]) if (x[u] < 0 && id[u] == id[v]) num++;
					if (num != size - 1) {
						clique = false;
						break;
					}
				}
				if (clique) {
					lb += size - 1;
				} else {
					while (size >= 6) {
						int minSize = size, s = 0, t = size;
						for (int j = 0; j < size; j++) {
							used.clear();
							v = S[j];
							for (int u : adj[v]) if (x[u] < 0 && id[u] == id[v]) {
								used.add(u);
							}
							v = S[(j + 1) % size];
							for (int u : adj[v]) if (x[u] < 0 && id[u] == id[v]) {
								if (used.get(S[(pos[u] + 1) % size])) {
									int size2 = (pos[u] - j + size) % size;
									if (minSize > size2 && size2 % 2 != 0) {
										minSize = size2;
										s = (j + 1) % size;
										t = (pos[u] + 1) % size;
									}
								}
							}
						}
						if (minSize == size) break;
						int p = 0;
						for (int j = t; j != s; j = (j + 1) % size) {
							S2[p++] = S[j];
						}
						for (int j = s; j != t; j = (j + 1) % size) {
							id[S[j]] = n;
						}
						int[] S3 = S; S = S2; S2 = S3;
						size -= minSize;
						Debug.check(size == p);
						Debug.check(minSize > 1);
						lb += (minSize + 1) / 2;
						for (int j = 0; j < size; j++) pos[S[j]] = j;
					}
					Debug.check(size > 1);
					lb += (size + 1) / 2;
				}
			}
			return lb;
		}
	}
	
	int cliqueLowerBound() {
		try (Stat stat = new Stat("lb_clique")) {
			long[] ls = new long[rn];
			int k = 0;
			for (int i = 0; i < n; i++) if (x[i] < 0) ls[k++] = ((long)deg(i)) << 32 | i;
			sort(ls);
			int[] clique = que, size = level, tmp = iter;
			int need = crt;
			used.clear();
			for (int i = 0; i < rn; i++) {
				int v = (int)ls[i];
				int to = v, max = 0;
				for (int u : adj[v]) if (x[u] < 0 && used.get(u)) tmp[clique[u]] = 0;
				for (int u : adj[v]) if (x[u] < 0 && used.get(u)) {
					int c = clique[u];
					tmp[c]++;
					if (tmp[c] == size[c] && max < size[c]) {
						to = c;
						max = size[c];
					}
				}
				clique[v] = to;
				if (to != v) {
					size[to]++;
					need++;
				} else {
					size[v] = 1;
				}
				used.add(v);
			}
			return need;
		}
	}
	
	boolean decompose() {
		int[][] vss;
		try (Stat stat = new Stat("decompose")) {
			int[] id = level;
			int[] size = iter;
			int nC = 0;
			{
				for (int i = 0; i < n; i++) id[i] = -1;
				for (int s = 0; s < n; s++) if (x[s] < 0 && id[s] < 0) {
					nC++;
					int qs = 0, qt = 0;
					que[qt++] = s;
					id[s] = s;
					while (qs < qt) {
						int v = que[qs++];
						for (int u : adj[v]) if (x[u] < 0 && id[u] < 0) {
							id[u] = s;
							que[qt++] = u;
						}
					}
					size[s] = qt;
				}
			}
			if (nC <= 1 && (n <= 100 || n * SHRINK < rn)) return false;
			long[] cs = new long[nC];
			{
				int p = 0;
				for (int i = 0; i < n; i++) if (x[i] < 0 && id[i] == i) {
					cs[p++] = ((long)(size[i])) << 32 | i;
				}
				sort(cs);
			}
			vss = new int[nC][];
			int[] qs = new int[n];
			{
				for (int i = 0; i < nC; i++) {
					vss[i] = new int[size[(int)cs[i]]];
					qs[(int)cs[i]] = i;
				}
				int[] ps = new int[nC];
				for (int i = 0; i < n; i++) if (x[i] < 0) {
					int j = qs[id[i]];
					vss[j][ps[j]++] = i;
				}
			}
			for (int i = 0; i < n; i++) id[i] = -1;
			for (int i = 0; i < vss.length; i++) {
				int[] vs = vss[i];
				long[] ls = new long[vs.length];
				for (int j = 0; j < vs.length; j++) ls[j] = ((long)(n - deg(vs[j]))) << 32 | vs[j];
				sort(ls);
				for (int j = 0; j < vs.length; j++) vs[j] = (int)ls[j];
			}
		}
		int[] x2 = x.clone();
		for (int i = modifiedN - 1; i >= 0; i--) modifieds[i].reverse(x2);
		int[] size = new int[vss.length];
		for (int i = 0; i < vss.length; i++) size[i] = vss[i].length;
		int[] pos1 = new int[N];
		int[] pos2 = new int[N];
		ArrayList<int[]> packingB = new ArrayList<int[]>();
		{
			fill(pos1, -1);
			for (int i = 0; i < vss.length; i++) {
				for (int j = 0; j < vss[i].length; j++) {
					pos1[vss[i][j]] = i;
					pos2[vss[i][j]] = j;
				}
			}
			boolean[] need = new boolean[N];
			for (int i = 0; i < packing.size(); i++) {
				int[] ps = packing.get(i);
				int max = ps.length - 1 - ps[0], sum = 0, count = 0;
				for (int j = 1; j < ps.length; j++) {
					int v = ps[j];
					if (x2[v] < 0 || x2[v] == 2) {
						count++;
					}
					if (x2[v] == 1) sum++;
				}
				if (sum > max) return true;
				if (sum + count > max) {
					packingB.add(ps);
					for (int k = 1; k < ps.length; k++) {
						if (x2[ps[k]] == 2) need[ps[k]] = true;
					}
				}
			}
			for (int i = 0; i < modifiedN; i++) {
				boolean b = false;
				Modified mod = modifieds[i];
				for (int v : mod.removed) if (need[v]) b = true;
				if (b) {
					if (mod instanceof Fold) {
						if (x2[mod.vs[0]] == 2) need[mod.vs[0]] = true;
					} else {
						for (int v : mod.vs) if (x2[v] == 2) {
							need[v] = true;
						}
					}
				}
			}
			for (int i = modifiedN - 1; i >= 0; i--) {
				Modified mod = modifieds[i];
				boolean b = false;
				for (int v : mod.removed) if (need[v]) b = true;
				if (b) {
					if (mod instanceof Fold) {
						for (int v : mod.removed) {
							Debug.check(pos1[v] == -1);
							pos1[v] = pos1[mod.vs[0]];
							Debug.check(pos1[v] >= 0);
							pos2[v] = size[pos1[v]]++;
						}
					} else {
						int max = -1;
						for (int v : mod.vs) if (max < pos1[v]) max = pos1[v];
						Debug.check(max >= 0);
						for (int v : mod.removed) {
							Debug.check(pos1[v] == -1);
							pos1[v] = max;
							pos2[v] = size[pos1[v]]++;
						}
					}
				}
			}
			for (int i = 0; i < n; i++) {
				if ((x2[i] == 0 || x2[i] == 1) && pos1[i] >= 0) {
					Debug.print(i, n, x[i]);
					Debug.check(false);
				}
			}
		}
		VCSolver[] vcs = new VCSolver[vss.length];
		{
			for (int i = 0; i < vss.length; i++) {
				int[] vs = vss[i];
				size[i] += 2;
				int[][] adj2 = new int[vs.length][];
				for (int j = 0; j < vs.length; j++) {
					adj2[j] = new int[deg(vs[j])];
					int p = 0;
					for (int u : adj[vs[j]]) if (x[u] < 0) adj2[j][p++] = pos2[u];
					Debug.check(p == adj2[j].length);
					sort(adj2[j]);
				}
				vcs[i] = new VCSolver(adj2, size[i]);
				for (int j = 0; j < vs.length; j++) {
					if (in[vs[j]] >= 0 && pos1[in[vs[j]]] == i && pos2[in[vs[j]]] < vs.length) {
						vcs[i].in[j] = pos2[in[vs[j]]];
					}
					if (out[vs[j]] >= 0 && pos1[out[vs[j]]] == i && pos2[out[vs[j]]] < vs.length) {
						vcs[i].out[j] = pos2[out[vs[j]]];
					}
				}
				vcs[i].x[vcs[i].N - 2] = vcs[i].y[vcs[i].N - 2] = 0;
				vcs[i].x[vcs[i].N - 1] = vcs[i].y[vcs[i].N - 1] = 1;
			}
		}
		{
			for (int i = 0; i < packingB.size(); i++) {
				int[] ps = packingB.get(i);
				int maxID = -1;
				for (int j = 1; j < ps.length; j++) {
					int v = ps[j];
					if (x2[v] < 0 || x2[v] == 2) {
						maxID = Math.max(maxID, pos1[v]);
					}
				}
				vcs[maxID].packing.add(ps);
			}
		}
		{
			for (int i = 0; i < modifiedN; i++) {
				Modified mod = modifieds[i];
				int p = pos1[mod.removed[0]];
				if (p >= 0) vcs[p].modifieds[vcs[p].modifiedN++] = mod;
			}
		}
		int[][] vss2 = new int[vss.length][];
		{
			for (int i = 0; i < vss.length; i++) vss2[i] = new int[vcs[i].N - 2];
			for (int i = 0; i < N; i++) if (pos1[i] >= 0) vss2[pos1[i]][pos2[i]] = i;
		}
		int sum = crt;
		for (int i = 0; i < vss.length && opt > sum; i++) {
			VCSolver vc = vcs[i];
			{
				ArrayList<int[]> packing2 = new ArrayList<int[]>();
				for (int j = 0; j < vc.packing.size(); j++) {
					int[] ps = vc.packing.get(j);
					int[] tmp = level;
					int p = 0;
					tmp[p++] = ps[0];
					for (int k = 1; k < ps.length; k++) {
						int v = ps[k];
						if (pos1[v] == i) {
							tmp[p++] = pos2[v];
						} else {
							Debug.check(x2[v] == 0 || x2[v] == 1);
							if (x2[v] == 0) tmp[0]--;
						}
					}
					if (p - 1 < tmp[0]) return true;
					if (tmp[0] <= 0) continue;
					packing2.add(copyOf(tmp, p));
				}
				vc.packing = packing2;
			}
			{
				for (int j = 0; j < vc.modifiedN; j++) {
					Modified mod = vc.modifieds[j];
					int[] removed = new int[mod.removed.length];
					for (int k = 0; k < removed.length; k++) {
						int v = mod.removed[k];
						Debug.check(pos1[v] == i);
						removed[k] = pos2[v];
					}
					if (mod instanceof Fold) {
						int[] vs = new int[1];
						int v = mod.vs[0];
						if (pos1[v] == i) {
							vs[0] = pos2[v];
						} else {
							Debug.check(x2[v] == 0 || x2[v] == 1);
							vs[0] = vc.N - 2 + x2[v];
						}
						mod = new Fold(removed, vs);
					} else {
						int[] vs = new int[mod.vs.length];
						for (int k = 0; k < vs.length; k++) {
							int v = mod.vs[k];
							if (pos1[v] == i) {
								vs[k] = pos2[v];
							} else {
								Debug.check(x2[v] == 0 || x2[v] == 1);
								vs[k] = vc.N - 2 + x2[v];
							}
						}
						mod = new Alternative(removed, vs, ((Alternative)mod).k);
					}
					vc.modifieds[j] = mod;
				}
			}
			vc.depth = depth + (vss.length > 1 ? 1 : 0);
			if (debug >= 2 && depth <= maxDepth) {
				if (vss.length == 1) debug("shrink: %d -> %d (%d)%n", n, vcs[i].n, vcs[i].N);
				else debug("decompose: %d (%d)%n", vcs[i].n, vcs[i].N);
			}
			if (i + 1 == vss.length) {
				vc.opt = Math.min(vss[i].length, opt - sum);
			}
			vc.reverse();
			for (int j = 0; j < vc.N; j++) Debug.check(vc.y[j] == 0 || vc.y[j] == 1);
			vc.solve();
			sum += vc.opt;
			for (int j = 0; j < vc.N - 2; j++) {
				x2[vss2[i][j]] = vc.y[j];
				Debug.check(vc.y[j] == 0 || vc.y[j] == 1);
			}
		}
		if (opt > sum) {
			if (debug >= 2 && rootDepth <= maxDepth) debug("opt: %d -> %d%n", opt, sum);
			opt = sum;
			System.arraycopy(x, 0, y, 0, N);
			for (int i = 0; i < vss.length; i++) {
				for (int j = 0; j < vss[i].length; j++) y[vss[i][j]] = vcs[i].y[j];
			}
			reverse();
		}
		return true;
	}
	
	int lowerBound() {
		int type = 0, tmp;
		if (lb < crt) {
			lb = crt;
			type = 1;
		}
		if (LOWER_BOUND == 1 || LOWER_BOUND == 4) {
			tmp = cliqueLowerBound();
			if (lb < tmp) {
				lb = tmp;
				type = 4;
			}
		}
		if (LOWER_BOUND == 2 || LOWER_BOUND == 4) {
			tmp = lpLowerBound();
			if (lb < tmp) {
				lb = tmp;
				type = 2;
			}
		}
		if (LOWER_BOUND == 3 || LOWER_BOUND == 4) {
			tmp = cycleLowerBound();
			if (lb < tmp) {
				lb = tmp;
				type = 3;
			}
		}
		if (debug >= 2 && depth <= maxDepth) debug("lb: %d (%d), %d%n", lb, type, opt);
		return lb;
	}
	
	boolean reduce() {
		int oldn = rn;
		for (;;) {
			if (REDUCTION >= 0) deg1Reduction();
			if (n > 100 && n * SHRINK >= rn && !outputLP && decompose()) return true;
			if (REDUCTION >= 0 && REDUCTION < 2 && dominateReduction()) continue;
			if (REDUCTION >= 2 && unconfinedReduction()) continue;
			if (REDUCTION >= 1 && lpReduction()) continue;
			if (REDUCTION >= 3) {
				int r = packingReduction();
				if (r < 0) return true;
				if (r > 0) continue;
			}
			if (REDUCTION >= 1 && fold2Reduction()) continue;
			if (REDUCTION >= 2 && twinReduction()) continue;
			if (REDUCTION >= 2 && funnelReduction()) continue;
			if (REDUCTION >= 2 && deskReduction()) continue;
			break;
		}
		if (debug >= 2 && depth <= maxDepth && oldn != rn) debug("reduce: %d -> %d%n", oldn, rn);
		return false;
	}
	
	void rec() {
		if (REDUCTION < 3) Debug.check(packing.size() == 0);
		if (reduce()) return;
		if (lowerBound() >= opt) return;
		if (rn == 0) {
			if (debug >= 2 && rootDepth <= maxDepth) debug("opt: %d -> %d%n", opt, crt);
			opt = crt;
			System.arraycopy(x, 0, y, 0, N);
			reverse();
			return;
		}
		if (decompose()) return;
		branching();
	}
	
	void debug(String str, Object...os) {
		StringBuilder sb = new StringBuilder();
		Calendar c = Calendar.getInstance();
		sb.append(String.format("%02d:%02d:%02d  ", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND)));
		for (int i = 0; i < depth && i <= maxDepth; i++) sb.append(' ');
		System.err.print(sb);
		System.err.printf(str, os);
	}
	
	public int solve() {
		if (LOWER_BOUND >= 2 && REDUCTION <= 0 && !outputLP) {
			System.err.println("LP/cycle lower bounds require LP reduction.");
			Debug.check(false);
		}
		rootDepth = depth;
		if (outputLP) {
			if (REDUCTION < 0) {
				lpReduction();
			} else {
				reduce();
			}
			System.out.printf("%.1f%n", crt + rn / 2.0);
			return opt;
		}
		rec();
		if (debug >= 2 && depth <= maxDepth) debug("opt: %d%n", opt);
		return opt;
	}
	
}
