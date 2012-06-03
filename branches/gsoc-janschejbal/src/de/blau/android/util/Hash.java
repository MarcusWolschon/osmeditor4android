package de.blau.android.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class Hash {
	public static String sha256(String str) {
		try {
			return toHex(MessageDigest.getInstance("SHA-256").digest(str.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Your Java is broken", e);
		}
		
	}
	
	public static String toHex(byte[] data) {
		StringBuffer buf = new StringBuffer(data.length*2);
		for (byte b : data) {
			buf.append(String.format("%02x", b & 0xFF));
		}
		return buf.toString();
	}
	
	
}
