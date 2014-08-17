package ir.blackgrape.bereshtook.util;

public class StringUtil {
	private static char[] arabicChars = {'٠','١','٢','٣','٤','٥','٦','٧','٨','٩'};
	
	public static String convertToPersian(String str){
		StringBuilder builder = new StringBuilder();
		for(int i=0;i<str.length();i++)
			builder.append(arabicChars[(int)(str.charAt(i))-48]);
		return builder.toString();
	}
}
