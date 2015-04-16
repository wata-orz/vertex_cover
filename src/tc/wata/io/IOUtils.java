package tc.wata.io;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.FileChannel.MapMode;
import java.util.*;

/**
 * 入出力用ユーティリティ
 */
public class IOUtils {
	
	private IOUtils() {
	}
	
	/**
	 * ファイルの読み込み用バッファを取得する．
	 */
	public static ByteBuffer getReadBuffer(File file) {
		try (RandomAccessFile f = new RandomAccessFile(file, "r")) {
			return f.getChannel().map(MapMode.READ_ONLY, 0, file.length());
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * ファイルの読み込み用バッファを取得する．
	 */
	public static ByteBuffer getReadBuffer(String file) {
		return getReadBuffer(new File(file));
	}
	
	/**
	 * ファイルの書き込み用バッファを取得する．
	 */
	public static ByteBuffer getWriteBuffer(File file, long size) {
		try (RandomAccessFile f = new RandomAccessFile(file, "rw")) {
			f.setLength(size);
			return f.getChannel().map(MapMode.READ_WRITE, 0, file.length());
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * ファイルの書き込み用バッファを取得する．
	 */
	public static ByteBuffer getWriteBuffer(String file, long size) {
		return getWriteBuffer(new File(file), size);
	}
	
	/**
	 * ファイルを全て読み込む．<br>
	 * ファイルが存在しない場合はnullを返す．
	 */
	public static String readAll(File file) {
		try (InputStream in = new FileInputStream(file)) {
			byte[] bs = new byte[(int)file.length()];
			int p = 0, len;
			while ((len = in.read(bs, p, bs.length - p)) >= 0) p += len;
			if (p != bs.length) throw new RuntimeException();
			return new String(bs);
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * ファイルを全て読み込む．<br>
	 * ファイルが存在しない場合はnullを返す．
	 */
	public static String read(String file) {
		return readAll(new File(file));
	}
	
	/**
	 * ファイルを一行ずつ読み込む．<br>
	 * ファイルが存在しない場合はnullを返す．
	 */
	public static String[] readLines(File file) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			ArrayList<String> res = new ArrayList<>();
			String line;
			while ((line = in.readLine()) != null) res.add(line);
			return res.toArray(new String[0]);
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * ファイルを一行ずつ読み込む．<br>
	 * ファイルが存在しない場合はnullを返す．
	 */
	public static String[] readLines(String file) {
		return readLines(new File(file));
	}
	
	/**
	 * ファイルに書きだす．
	 */
	public static void write(File file, String str) {
		try (OutputStream out = new FileOutputStream(file)) {
			out.write(str.getBytes());
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * ファイルに書きだす．
	 */
	public static void write(String file, String str) {
		write(new File(file), str);
	}
	
	/**
	 * ファイルをコピーする．
	 */
	public static void copy(File from, File to) {
		try (
			FileInputStream fin = new FileInputStream(from);
			FileChannel in = fin.getChannel();
			FileOutputStream fout = new FileOutputStream(to);
			FileChannel out = fout.getChannel()
		) {
			in.transferTo(0, from.length(), out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * ディレクトリを再帰的に削除． (rm -r)
	 */
	public static void deleteRec(File path) {
		if (path.isDirectory()) {
			for (String child : path.list()) {
				deleteRec(new File(path, child));
			}
		}
		if (!path.delete()) throw new RuntimeException("Failed to delete: " + path.getAbsolutePath());
	}
	
}
