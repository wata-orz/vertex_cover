package tc.wata.data;

/**
 * 最大要素数固定の高速Set．全操作O(1).
 */
public class FastSet {
	
	int[] used;
	int uid = 1;
	
	public FastSet(int n) {
		used = new int[n];
	}
	
	/**
	 * 初期化する
	 */
	public void clear() {
		uid++;
		if (uid < 0) {
			for (int i = 0; i < used.length; i++) used[i] = 0;
			uid = 1;
		}
	}
	
	/**
	 * 要素iを加える．
	 * @return iが含まれていなければtrue
	 */
	public boolean add(int i) {
		boolean res = used[i] != uid;
		used[i] = uid;
		return res;
	}
	
	/**
	 * 要素iを取り除く．
	 */
	public void remove(int i) {
		used[i] = uid - 1;
	}
	
	/**
	 * 要素iが含まれているか．
	 */
	public boolean get(int i) {
		return used[i] == uid;
	}
	
}
