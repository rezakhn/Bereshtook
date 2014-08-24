package ir.blackgrape.bereshtook.util;

import ir.blackgrape.bereshtook.BereshtookApplication;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Decryptor {
	public static String convert(String encrypted) throws Exception {
		return doConvert(encrypted);
	}

	public static String doConvert(String encrypted) throws Exception{
		String seed = BereshtookApplication.SEED;
		byte[] rawKey = getRawKey(seed.getBytes());
		byte[] enc = toByte(encrypted);
		byte[] result = decrypt(enc, rawKey);
		return new String(result);
	}
	
	private static byte[] getRawKey(byte[] seed) throws Exception {
		SecretKey skey = new SecretKeySpec(seed, "AES");
		byte[] raw = skey.getEncoded();
		return raw;
	}

	public static byte[] toByte(String hexString) {
		int len = hexString.length() / 2;
		byte[] result = new byte[len];
		for (int i = 0; i < len; i++)
			result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2),
					16).byteValue();
		return result;
	}

	public static String toHex(byte[] buf) {
		if (buf == null)
			return "";
		StringBuffer result = new StringBuffer(2 * buf.length);
		for (int i = 0; i < buf.length; i++) {
			appendHex(result, buf[i]);
		}
		return result.toString();
	}

	private static byte[] decrypt(byte[] encrypted, byte[] raw) throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		byte[] decrypted = cipher.doFinal(encrypted);
		return decrypted;
	}

	private final static String HEX = "0123456789ABCDEF";

	private static void appendHex(StringBuffer sb, byte b) {
		sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
	}
}
