package com.kbdunn.nimbus.common.util;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

public abstract class AbstractCommandLineUtility {
	
	protected static void exitWithError(String message) {
		outln(message);
		System.exit(1);
	}
	
	protected static String readln() {
		String line = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			line = br.readLine();
		} catch (IOException ioe) {
			exitWithError("Error reading user input! Please try again.");
		}
		return line;
	}
	
	protected static String readPassword() {
		Console c = System.console();
		if (c == null) {
			out(" [console echo turned on]");
			return readln();
		}
		return new String(c.readPassword());
	}
	
	protected static Integer numeric(String s) {
		return Integer.valueOf(s);
	}
	
	protected static boolean affirmative(String s) {
		return s.toUpperCase().equals("Y");
	}
	
	protected static void outln() {
		System.out.println();
	}
		
	protected static void out(String out) {
		System.out.print(out);
	}
	
	protected static void outln(String out) {
		System.out.println(out);
	}

	protected static void setLogLevel() {
		LogManager.getLogger("org.hsqldb").setLevel(Level.OFF);
		LogManager.getLogger("com.kbdunn").setLevel(Level.OFF);
		LogManager.getRootLogger().setLevel(Level.OFF);
	}
	
	protected int promptListSelection(List<String> optionCaptions) {
		if (optionCaptions.isEmpty()) return -1;
		Integer selected = null;
		while (selected == null) {
			for (int i=0; i < optionCaptions.size(); i++)
				outln("  " + (i+1) + ". " + optionCaptions.get(i));
			
			out("Enter your selection [1-" + optionCaptions.size() + "]: ");
			Integer n = numeric(readln());
			if (n == null || !(n > 0 && n <= optionCaptions.size())) outln("Please enter a valid number.");
			else selected = n-1;
		}
		return selected;
	}
}
