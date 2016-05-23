package com.kbdunn.nimbus.common.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.NimbusFile;

public class HashUtil {

	private static final Logger log = LogManager.getLogger(HashUtil.class);
	private static final String HASH_ALGORITHM = "MD5";

	private HashUtil() {
		// only static methods
	}

	/**
	 * Generates a MD5 hash of a given data
	 *
	 * @param data to calculate the MD5 hash over it
	 * @return the md5 hash
	 */
	public static byte[] hash(byte[] data) {
		try {
			MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
			digest.update(data, 0, data.length);
			return digest.digest();
		} catch (NoSuchAlgorithmException e) {
			log.error("Invalid hash algorithm " + HASH_ALGORITHM, e);
			return new byte[0];
		}
	}
	
	public static byte[] hash(NimbusFile file) throws IOException {
		return hash(new File(file.getPath()));
	}

	/**
	 * Generates a MD5 hash of an input stream (can take a while)
	 *
	 * @param file
	 * @return the hash of the file
	 * @throws IOException
	 */
	public static byte[] hash(File file) throws IOException {
		if (file == null) {
			throw new IllegalArgumentException("File cannot be null");
		} else if (file.isDirectory()) {
			throw new IllegalArgumentException("Cannot hash a directory");
		} else if (!file.exists()) {
			throw new IOException("File does not exist");
		}

		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(HASH_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			log.error("Invalid hash algorithm " + HASH_ALGORITHM, e);
			return new byte[0];
		}

		FileInputStream fis;
		try {
			// open the stream
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			log.error("File " + file + " not found to generate the hash", e);
			return new byte[0];
		}

		DigestInputStream dis = new DigestInputStream(fis, digest);
		try {
			byte[] buffer = new byte[1024];
			int numRead;
			do {
				numRead = dis.read(buffer);
			} while (numRead != -1);
		} finally {
			if (dis != null) {
				dis.close();
			}

			if (fis != null) {
				fis.close();
			}
		}

		return digest.digest();
	}

	/**
	 * Compares if the file md5 matches a given md5 hash
	 *
	 * @param file
	 * @param expectedMD5
	 * @return <code>true</code> if the file has the expected hash
	 * @throws IOException
	 */
	public static boolean compare(File file, byte[] expectedMD5) throws IOException {
		if (!file.exists() && (expectedMD5 == null || expectedMD5.length == 0)) {
			// both do not exist
			return true;
		} else if (file.isDirectory()) {
			// directories always match
			return true;
		}

		byte[] md5Hash = HashUtil.hash(file);
		return compare(md5Hash, expectedMD5);
	}

	/**
	 * Compares if the given md5 matches another md5 hash. This method works symmetrically and is not
	 * dependent on the parameter order
	 *
	 * @param md5 the hash to test
	 * @param expectedMD5 the expected md5 hash
	 * @return <code>true</code> if the hashes match
	 */
	public static boolean compare(byte[] md5, byte[] expectedMD5) {
		return Arrays.equals(md5, expectedMD5);
	}
}