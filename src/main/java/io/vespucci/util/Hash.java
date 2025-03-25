package io.vespucci.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import androidx.annotation.NonNull;
import io.vespucci.exception.OperationFailedException;

/**
 * Provides a simple interface for string hashing.
 * 
 * @author Jan
 */
public final class Hash {

    /**
     * Private constructor to stop instantiation
     */
    private Hash() {
        // private
    }

    /**
     * Hashes a string with SHA256
     * 
     * @param str the string to hash
     * @return a hexadecimal representation of the SHA-256 hash
     */
    public static String sha256(@NonNull String str) {
        try {
            return toHex(MessageDigest.getInstance("SHA-256").digest(str.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new OperationFailedException("Your Java is broken", e);
        }
    }

    /**
     * Converts a byte array to lowercase hexadecimal (without separators)
     * 
     * @param data a byte array to convert
     * @return the hex string representing the data in the byte array
     */
    public static String toHex(@NonNull byte[] data) {
        StringBuilder buf = new StringBuilder(data.length * 2);
        for (byte b : data) {
            buf.append(String.format("%02x", b & 0xFF));
        }
        return buf.toString();
    }
}
