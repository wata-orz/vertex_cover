import static java.lang.Math.*;
import static java.util.Arrays.*;

import java.io.*;
import java.util.*;

import tc.wata.debug.*;
import tc.wata.io.*;
import tc.wata.util.*;
import tc.wata.util.SetOpt.*;

public class Main {
	
	@Option(abbr = 'r', usage = "0: deg1+dominance+fold2, 1:LP, 2:unconfined+twin+funnel+desk, 3:packing")
	public static int reduction = 3;
	
	@Option(abbr = 'l', usage = "0: nothing, 1:clique, 2:LP, 3:cycle, 4:all")
	public static int lb = 4;
	
	@Option(abbr = 'b', usage = "0:random, 1:mindeg, 2:maxdeg")
	public static int branching = 2;
	
	@Option(abbr = 'o')
	public static boolean outputLP = false;
	
	@Option(abbr = 'p', usage = "Print the minimum vertex cover. The size of VC is in the first line. Each of the following lines contains the vertex ID.")
	public static boolean printVC = false;
	
	@Option(abbr = 'd')
	public static int debug = 0;
	
	int[] vertexID;
	int[][] adj;
	
	void read(String file) {
		if (file.endsWith(".dat")) {
			GraphIO io = new GraphIO();
			io.read(new File(file));
			adj = io.adj;
			vertexID = new int[adj.length];
			for (int i = 0; i < adj.length; i++) vertexID[i] = i;
		} else {
			GraphConverter conv = new GraphConverter();
			conv.file = file;
			conv.type = "snap";
			conv.undirected = true;
			conv.sorting = true;
			try {
				conv.read();
			} catch (Exception e) {
				conv = new GraphConverter();
				conv.file = file;
				conv.type = "dimacs";
				conv.undirected = true;
				conv.sorting = true;
				try {
					conv.read();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
			adj = conv.adj;
			vertexID = conv.vertexID;
		}
	}
	
	void run(String file) {
		System.err.println("reading the input graph...");
		read(file);
		if (debug > 0) Stat.setShutdownHook();
		int m = 0;
		for (int i = 0; i < adj.length; i++) m += adj[i].length;
		m /= 2;
		System.err.printf("n = %d, m = %d%n", adj.length, m);
		VCSolver vc = new VCSolver(adj, adj.length);
		VCSolver.nBranchings = 0;
		VCSolver.REDUCTION = reduction;
		VCSolver.LOWER_BOUND = lb;
		VCSolver.BRANCHING = branching;
		VCSolver.outputLP = outputLP;
		VCSolver.debug = debug;
		long start, end;
		try (Stat stat = new Stat("solve")) {
			start = System.currentTimeMillis();
			vc.solve();
			end = System.currentTimeMillis();
		}
		System.err.printf("opt = %d, time = %.3f%n", vc.opt, 1e-3 * (end - start));
		read(file);
		int sum = 0;
		for (int i = 0; i < adj.length; i++) {
			sum += vc.y[i];
			Debug.check(vc.y[i] == 0 || vc.y[i] == 1);
			for (int j : adj[i]) Debug.check(vc.y[i] + vc.y[j] >= 1);
		}
		Debug.check(sum == vc.opt);
		if (debug > 0) {
			System.err.printf("%d\t%d\t%d\t%.3f\t%d%n", adj.length, m, vc.opt, 1e-3 * (end - start), VCSolver.nBranchings);
		}
		if (printVC) {
			System.out.println(sum);
			for (int i = 0; i < adj.length; i++) if (vc.y[i] > 0) {
				System.out.println(vertexID[i]);
			}
		}
	}
	
	void debug(Object...os) {
		System.err.println(deepToString(os));
	}
	
	public static void main(String[] args) {
		Main main = new Main();
		args = SetOpt.setOpt(main, args);
		main.run(args[0]);
	}
}
