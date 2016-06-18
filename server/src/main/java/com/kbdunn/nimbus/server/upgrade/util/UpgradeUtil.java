package com.kbdunn.nimbus.server.upgrade.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import com.kbdunn.nimbus.server.jdbc.JdbcHelper;
import com.kbdunn.nimbus.server.upgrade.UpgradeRunner;

public class UpgradeUtil {

	private static final FileFilter allFilesFilter = new RegexFileFilter("^.*$");
	private static final FileFilter nimbusJarFilter = new RegexFileFilter("^nimbus.*\\.jar$");
	private static final FileFilter nimbusScriptFilter = new RegexFileFilter("^.*\\.sh$");
	
	private final UpgradeRunner runner;
	
	public UpgradeUtil(UpgradeRunner runner) { 
		this.runner = runner;
	}
	
	public void replaceNimbusJars() throws IOException {
		runner.outLog("Upgrading Nimbus libraries... ");
		File srcLib = new File(runner.getUpgradeSrcDir(), "lib");
		File tgtLib = new File(runner.getUpgradeTgtDir(), "lib");
		replaceFiles(srcLib, tgtLib, nimbusJarFilter);
		outln("Done");
	}
	
	public void replaceLibFiles() throws IOException {
		runner.outLog("Replacing library files... ");
		File srcLib = new File(runner.getUpgradeSrcDir(), "lib");
		File tgtLib = new File(runner.getUpgradeTgtDir(), "lib");
		replaceFiles(srcLib, tgtLib, allFilesFilter);
		outln("Done");
	}
	
	public void replaceNimbusScripts() throws IOException {
		runner.outLog("Upgrading scripts... ");
		replaceFiles(runner.getUpgradeSrcDir(), runner.getUpgradeTgtDir(), nimbusScriptFilter);
		outln("Done");
		outln("NOTE: Nimbus installation scripts may have changed. "
				+ "It is strongly recommended to run them again.");
	}
	
	public void replaceStaticResources() throws IOException {
		runner.outLog("Upgrading static resources... ");
		File src = new File(runner.getUpgradeSrcDir(), "static");
		File tgt = new File(runner.getUpgradeTgtDir(), "static");
		runner.writeUpgradeLogEntry("\tDeleting " + tgt);
		FileUtils.deleteDirectory(tgt);
		runner.writeUpgradeLogEntry("\tCopying " + src + " to " + tgt);
		FileUtils.copyDirectory(src, tgt);
		outln("Done");
	}
	
	public void replaceFiles(File srcDir, File tgtDir, FileFilter filter) throws IOException {
		// Delete old files
		for (File tgtFile : tgtDir.listFiles(filter)) {
			runner.writeUpgradeLogEntry("\tDeleting " + tgtFile);
			FileUtils.forceDelete(tgtFile);
		}
		// Copy new files
		for (File srcFile : srcDir.listFiles(filter)) {
			runner.writeUpgradeLogEntry("\tCopying " + srcFile + " to " + tgtDir);
			FileUtils.copyFileToDirectory(srcFile, tgtDir);
		}
	}
	
	public void runSql(String... statements) throws SQLException, IOException {
		runner.outLog("Upgrading the database... ");
		LogManager.getLogger("hsqldb.db").setLevel(Level.WARN);
		try (Connection con = JdbcHelper.createConnection()) {
			for (String statement : statements) {
				runner.writeUpgradeLogEntry("\tExecuting SQL: " + statement);
				con.createStatement().execute(statement);
			}
			con.createStatement().execute("CHECKPOINT DEFRAG");
			con.createStatement().execute("DISCONNECT");
		}
		outln("Done");
	}
	
	public void addLinesToConfigFile(String afterProperty, String... newLines) throws IOException {
		runner.outLog("Adding new lines to nimbus.properties... ");
		File file = new File(runner.getUpgradeTgtDir(), "conf/nimbus.properties");
		List<String> oldLines = FileUtils.readLines(file);
		int idx = oldLines.size() - 1;
		if (afterProperty != null && !afterProperty.isEmpty()) {
			for (int i = 0; i < oldLines.size(); i++) {
				if (oldLines.get(i).startsWith(afterProperty)) {
					idx = i + 1;
				}
			}
		}
		oldLines.addAll(idx, Arrays.asList(newLines));
		for (String line : newLines) runner.writeUpgradeLogEntry("\tAdded config line: " + line);
		FileUtils.deleteQuietly(file);
		FileUtils.writeLines(file, oldLines);
		outln("Done");
	}
	
	private static void outln(String out) {
		System.out.println(out);
	}
}
