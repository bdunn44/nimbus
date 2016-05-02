package com.kbdunn.nimbus.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public abstract class StringUtil {

	public static String toDateString(Date date) {
		return new SimpleDateFormat().format(date);
	}
	
	public static String toHumanDateString(Date date) {
		throw new UnsupportedOperationException("Not implemented!"); // TODO
	}
	
	public static String toHumanSizeString(final long bytes) {
		if (bytes < 1024) return bytes + " B";
		int e = (int) (Math.log(bytes)/Math.log(1024));
		char pre = "KMGTPE".charAt(e-1);
		String f = e > 2 ? "%.1f %sB" : "%.0f %siB";
		return String.format(f, bytes/Math.pow(1024, e), pre);
	}
	
	public static String toHumanPercentage(float value) {
		if (value <= 1) value *= 100;
		return String.format("%,.0f", value) + "%";
	}
	
	public static String getFileNameFromPath(String path) {
		if (path.contains("/")) return path.substring(path.lastIndexOf("/") + 1);
		else return path;
	}
	
	public static String getFileExtensionFromPath(String path) {
		int d = path.lastIndexOf('.');
		int s = path.lastIndexOf("/");
		if (d > -1 && (s == -1 || d > s)) 
		    return path.substring(d+1);
		return "";
	}
	
	public static String decodeUtf8(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");		
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	public static String encodeUtf8(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");		
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	// Encodes a relative or full URI to UTF-8
	public static String encodeUriUtf8(String path) {
		String encoded = "";
		try {
			if (path.contains("/")) {
				for (String dir : path.split(Pattern.quote("/")))
					encoded += "/" + URLEncoder.encode(dir, "UTF-8");
			} else {
				encoded = URLEncoder.encode(path, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			return null;
		}
		
		// Trim leading slash if necessary
		if (encoded.startsWith("/"))
			return encoded.substring(1);
		else
			return encoded;
	}
	
	// Decodes a relative or full URI to UTF-8
	public static String decodeUriUtf8(String path) {
		String decoded = "";
		try {
			if (path.contains("/")) {
				for (String dir : path.split(Pattern.quote("/")))
					decoded += "/" + URLDecoder.decode(dir, "UTF-8");
			} else {
				decoded = URLDecoder.decode(path, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			return null;
		}
		
		// Trim leading slash if necessary
		if (decoded.startsWith("/"))
			return decoded.substring(1);
		else
			return decoded;
	}
	
	public static String toDurationString(Integer seconds) {
		if (seconds == null || seconds == 0) return "0:00";
		String l = String.valueOf(seconds/60) + ":";
		l += String.valueOf(seconds%60).length() == 1 ? "0" : "";
		l += String.valueOf(seconds%60);
		return  l;
	}
	
	public static boolean isLong(String s) {
		try {
			Long.parseLong(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public static boolean stringContainsInvalidCharacters(String input, String invalidCharacters) {
		for (int i=0; i < invalidCharacters.length(); i++) {
			if (input.contains(String.valueOf(invalidCharacters.charAt(i)))) {
				return true;
			}
		}
		return false;
	}
}
