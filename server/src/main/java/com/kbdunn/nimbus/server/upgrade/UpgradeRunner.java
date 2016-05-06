package com.kbdunn.nimbus.server.upgrade;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.time.LocalDateTime;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.kbdunn.nimbus.common.model.NimbusVersion;
import com.kbdunn.nimbus.common.util.AbstractCommandLineUtility;

public class UpgradeRunner extends AbstractCommandLineUtility {

	public static void main(String[] args) {
		String srcDirPath = args[0];
		String tgtDirPath = args[1];
		File upgradeSrcDir = null;
		File upgradeTgtDir = null;
		if (srcDirPath == null || srcDirPath.isEmpty() || !(upgradeSrcDir = new File(srcDirPath)).isDirectory()) {
			System.out.println("Invalid upgrade source path " + srcDirPath);
			System.exit(1);
		}
		if (tgtDirPath == null || tgtDirPath.isEmpty() || !(upgradeTgtDir = new File(tgtDirPath)).isDirectory()) {
			System.out.println("Invalid upgrade target path " + srcDirPath);
			System.exit(1);
		}
		if (upgradeSrcDir.getAbsolutePath().equals(upgradeTgtDir.getAbsolutePath())) {
			System.out.println("I can't upgrade myself!");
			System.exit(1);
		}
		try {
			new UpgradeRunner(upgradeSrcDir, upgradeTgtDir).upgrade();
		} catch (IllegalArgumentException e) {
			System.out.println("Error detecting Nimbus version " + e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Error performing upgrade! " + e.getMessage());
			System.exit(1);
		}
	}
	
	private static final FileFilter nimbusJarFilter = new RegexFileFilter("^nimbus.*\\.jar$");
	private static final FileFilter nimbusScriptFilter = new RegexFileFilter("^.*\\.sh$");
	
	private final File upgradeSrcDir, upgradeTgtDir, logFile;
	private final NimbusVersion srcVersion, tgtVersion;
	
	private UpgradeRunner(File upgradeSrcDir, File upgradeTgtDir) throws NumberFormatException, IllegalArgumentException, IOException {
		this.upgradeSrcDir = upgradeSrcDir;
		this.upgradeTgtDir = upgradeTgtDir;
		this.srcVersion = NimbusVersion.fromNimbusInstallation(upgradeSrcDir);
		this.tgtVersion = NimbusVersion.fromNimbusInstallation(upgradeTgtDir);
		this.logFile = new File(upgradeTgtDir, "logs/upgrades.log");
	}
	
	private void upgrade() throws IOException {
		try {
			if (!UpgradeRunner.checkUpgradeable(srcVersion) || !UpgradeRunner.checkUpgradeable(tgtVersion)) {
				exitWithError("The source or target Nimbus installation is older than 0.6.1 and cannot be upgraded.");
			}
			if (srcVersion.compareTo(tgtVersion) < 1) {
				exitWithError("The version you are upgrading to is equal to or lower than the current installation."
						+ " (" + tgtVersion + " -> " + srcVersion + ")");
			}
			outln("Nimbus installation at '" + upgradeTgtDir.getAbsolutePath() 
					+ "' will be upgraded from version " + tgtVersion + " to " + srcVersion);
			out("Do you want to proceed? [Y/N]: ");
			boolean go = affirmative(readln());
			if (!go) System.exit(0);
			
			// Always do this stuff, regardless of source/target version
			writeUpgradeLogEntry("Nimbus " + tgtVersion + " -> " + srcVersion + " upgrade started");
			replaceNimbusJars();
			replaceStaticResources();
			replaceNimbusScripts();
			replaceVersionFile();
			writeUpgradeLogEntry("Nimbus " + tgtVersion + " -> " + srcVersion + " upgrade suceeded!");
		} catch (Exception e) {
			if (logFile != null && logFile.exists()) {
				writeUpgradeLogEntry(ExceptionUtils.getFullStackTrace(e));
			}
			throw e;
		}
	}
	
	private void replaceVersionFile() throws IOException {
		File src = new File(upgradeSrcDir, "logs/version.txt");
		File tgt = new File(upgradeTgtDir, "logs/version.txt");
		writeUpgradeLogEntry("Copying " + src + " to " + tgt);
		FileUtils.copyFile(src, tgt);
	}
	
	private void replaceNimbusJars() throws IOException {
		outLog("Upgrading Nimbus libraries... ");
		File srcLib = new File(upgradeSrcDir, "lib");
		File tgtLib = new File(upgradeTgtDir, "lib");
		replaceFiles(srcLib, tgtLib, nimbusJarFilter);
		outln("Done");
	}
	
	private void replaceNimbusScripts() throws IOException {
		outLog("Upgrading scripts... ");
		replaceFiles(upgradeSrcDir, upgradeTgtDir, nimbusScriptFilter);
		outln("Done");
		outln("NOTE: Nimbus startup & installation scripts may have changed. "
				+ "You may need to execute them again.");
	}
	
	private void replaceStaticResources() throws IOException {
		outLog("Upgrading static resources... ");
		File src = new File(upgradeSrcDir, "static");
		File tgt = new File(upgradeTgtDir, "static");
		writeUpgradeLogEntry("\tDeleting " + tgt);
		FileUtils.deleteDirectory(tgt);
		writeUpgradeLogEntry("\tCopying " + src + " to " + tgt);
		FileUtils.copyDirectory(src, tgt);
		outln("Done");
	}
	
	private void writeUpgradeLogEntry(String entry) throws IOException {
		FileUtils.writeStringToFile(logFile, LocalDateTime.now().toString() + ": " + entry + "\n", true); 
	}
	
	private void outLog(String out) throws IOException {
		super.out(out);
		writeUpgradeLogEntry(out);
	}
	
	private void replaceFiles(File srcDir, File tgtDir, FileFilter filter) throws IOException {
		// Delete old files
		for (File tgtFile : tgtDir.listFiles(filter)) {
			writeUpgradeLogEntry("\tDeleting " + tgtFile);
			FileUtils.forceDelete(tgtFile);
		}
		// Copy new files
		for (File srcFile : srcDir.listFiles(filter)) {
			writeUpgradeLogEntry("\tCopying " + srcFile + " to " + tgtDir);
			FileUtils.copyFileToDirectory(srcFile, tgtDir);
		}
	}
	
	private static boolean checkUpgradeable(NimbusVersion version) {
		return version.getMajor() > 0 
				|| (version.getMajor() == 0 && version.getMinor() > 6)
				|| (version.getMajor() == 0 && version.getMinor() == 6 && version.getDot() == 1);
	}
}
