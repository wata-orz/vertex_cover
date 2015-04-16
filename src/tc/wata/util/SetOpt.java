package tc.wata.util;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * -ABBR VAL / -ABBRVAL / --NAME VAL / --NAME=VAL という形式の文字列をパースしてOptionアノテーションの指定されたpublicフィールドに値をセットする．<br>
 * 使える値の種類はint,long,double,boolean,String．<br>
 * NAMEは曖昧性が無い限り短縮可能．
 * booleanの場合はデフォルト値を反転させる．<br>
 * -- から先は全てオプションではない実引数として扱う．
 */
public class SetOpt {
	
	private SetOpt() {
	}
	
	/**
	 * オプションの指定
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Option {
		/**
		 * 短縮形の名前．指定しない場合短縮形なし．
		 */
		char abbr() default 0;
		/**
		 * オプション名．指定しない場合，変数名がそのまま使われる．
		 */
		String name() default "";
		/**
		 * 説明文
		 */
		String usage() default "";
		/**
		 * 必須かどうか
		 */
		boolean required() default false;
	}
	
	/**
	 * 文字列を元に指定された型のオブジェクトを生成．
	 */
	public static Object createObject(Class<?> type, String val) {
		Object obj;
		if (type == int.class) obj = Integer.parseInt(val);
		else if (type == long.class) obj = Long.parseLong(val);
		else if (type == double.class) obj = Double.parseDouble(val);
		else if (type == boolean.class) obj = Boolean.parseBoolean(val);
		else if (type == String.class) obj = val;
		else throw new IllegalArgumentException("Unspported type: " + type);
		return obj;
	}
	
	private static String getName(Field f) {
		Option opt = f.getAnnotation(Option.class);
		if (opt.name() != null && opt.name().length() > 0) return opt.name();
		return f.getName();
	}
	
	private static Field getField(Object obj, String name) {
		Field res = null;
		ArrayList<String> list = new ArrayList<>();
		if ("help".startsWith(name)) list.add("help");
		for (Field f : obj.getClass().getFields()) if (f.isAnnotationPresent(Option.class)) {
			String id = getName(f);
			if (id.equals(name)) {
				return f;
			} else if (id.startsWith(name)) {
				list.add(id);
				res = f;
			}
		}
		if (res == null) {
			if (list.size() > 0) return null;
			System.err.printf("No such option: '--%s'%n", name);
			System.exit(1);
		} else if (list.size() > 1) {
			StringBuilder str = new StringBuilder();
			for (String s : list) {
				if (str.length() > 0) str.append(", ");
				str.append(s);
			}
			System.err.printf("Ambiguous option: '--%s' (%s)%n", name, str);
			System.exit(1);
		}
		return res;
	}
	
	private static Field getAbbrField(Object obj, char abbr) {
		for (Field f : obj.getClass().getDeclaredFields()) if (f.isAnnotationPresent(Option.class)) {
			Option opt = f.getAnnotation(Option.class);
			if (opt.abbr() == abbr) {
				return f;
			}
		}
		System.err.printf("No such option: '-%c'%n" + abbr);
		System.exit(1);
		return null;
	}
	
	/**
	 * 引数を元に，オブジェクトのフィールドに値を設定する．
	 * @return オプション以外の引数リストを返す
	 */
	public static String[] setOpt(Object obj, String...args) {
		Set<Field> used = new HashSet<>();
		ArrayList<String> ret = new ArrayList<>();
		try {
			for (int i = 0; i < args.length; i++) {
				String s = args[i];
				if (s.equals("--")) {
					for (int j = i + 1; j < args.length; j++) {
						ret.add(args[j]);
					}
					break;
				} else if (!s.startsWith("-")) {
					ret.add(s);
				} else if (s.startsWith("--")) {
					String name, val = null;
					if (s.indexOf('=') >= 0) {
						name = s.substring(2, s.indexOf('='));
						val = s.substring(s.indexOf('=') + 1);
					} else {
						name = s.substring(2);
					}
					Field f = getField(obj, name);
					if (f == null) {
						showUsage(obj);
						System.exit(1);
					} else if (f.getType().equals(boolean.class)) {
						if (val != null) {
							System.err.printf("The option '--%s' cannot not take arguments%n", name);
							System.exit(1);
						}
						f.set(obj, !(boolean)f.get(obj));
					} else {
						if (val == null) {
							if (i + 1 >= args.length) {
								System.err.printf("The option '--%s' requires an argument%n", name);
								System.exit(1);
							}
							val = args[++i];
						}
						try {
							f.set(obj, createObject(f.getType(), val));
						} catch (Exception e) {
							System.err.printf("Illegal argument for the option '--%s': %s%n", name, val);
							System.exit(1);
						}
					}
					used.add(f);
				} else if (s.length() >= 2) {
					char abbr = s.charAt(1);
					String val = s.substring(2);
					Field f = getAbbrField(obj, abbr);
					if (f == null) {
						showUsage(obj);
						System.exit(1);
					} else if (f.getType().equals(boolean.class)) {
						if (!val.isEmpty()) {
							System.err.printf("The option '-%c' cannot not take arguments%n", abbr);
							System.exit(1);
						}
						f.set(obj, !(boolean)f.get(obj));
					} else {
						if (val.isEmpty()) {
							if (i + 1 >= args.length) {
								System.err.printf("The option '-%c' requires an argument%n", abbr);
								System.exit(1);
							}
							val = args[++i];
						}
						try {
							f.set(obj, createObject(f.getType(), val));
						} catch (Exception e) {
							System.err.printf("Illegal argument for the option '-%c': %s%n", abbr, val);
							System.exit(1);
						}
					}
					used.add(f);
				} else {
					System.err.printf("No such option: '-'%n");
					System.exit(1);
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		for (Field f : obj.getClass().getFields()) if (f.isAnnotationPresent(Option.class)) {
			Option opt = f.getAnnotation(Option.class);
			if (opt.required() && !used.contains(f)) {
				System.err.printf("The required option '--%s' is not specified.%n", getName(f));
				System.exit(1);
			}
		}
		return ret.toArray(new String[0]);
	}
	
	/**
	 * Optionアノテーションが付与されたフィールドの一覧を表示する．
	 */
	public static void showUsage(Object obj) {
		try {
			System.err.println("Options:");
			for (Field f : obj.getClass().getFields()) if (f.isAnnotationPresent(Option.class)) {
				Option opt = f.getAnnotation(Option.class);
				System.err.print("  ");
				if (opt.abbr() != 0) System.err.printf("-%c, ", opt.abbr());
				else System.err.print("    ");
				System.err.printf("--%s <%s>(%s)", getName(f), f.getType().getSimpleName(), opt.required() ? "required" : f.get(obj));
				System.err.println("\t" + opt.usage());
			}
			System.err.println("      --help\tShow this usage");
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
}
