package de.blau.android.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides a simple interface for string hashing.
 * @author Jan
 */
public abstract class Hash {
	/**
	 * Hashes a string with SHA256
	 * @param str the string to hash
	 * @return a hexadecimal representation of the SHA-256 hash
	 */
	public static String sha256(String str) {
		try {
			return toHex(MessageDigest.getInstance("SHA-256").digest(str.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Your Java is broken", e);
		}
		
	}
	
	/**
	 * Converts a byte array to lowercase hexadecimal (without separators)
	 * @param data a byte array to convert
	 * @return the hex string representing the data in the byte array
	 */
	public static String toHex(byte[] data) {
		StringBuffer buf = new StringBuffer(data.length*2);
		for (byte b : data) {
			buf.append(String.format("%02x", b & 0xFF));
		}
		return buf.toString();
	}
	
	
}
