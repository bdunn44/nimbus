package com.kbdunn.nimbus.server.upgrade;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.kbdunn.nimbus.common.model.NimbusVersion;
import com.kbdunn.nimbus.common.util.AbstractCommandLineUtility;
import com.kbdunn.nimbus.server.upgrade.runners.BaseUpgrader;
import com.kbdunn.nimbus.server.upgrade.runners.V061To062Upgrader;

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
		} catch (Exception e) {
			System.out.println("Error performing upgrade! " + e.getMessage());
			System.exit(2);
		}
	}
	
	private final File upgradeSrcDir, upgradeTgtDir, logFile;
	private final NimbusVersion newVersion, oldVersion;
	
	private UpgradeRunner(File upgradeSrcDir, File upgradeTgtDir) throws NumberFormatException, IllegalArgumentException, IOException {
		this.upgradeSrcDir = upgradeSrcDir;
		this.upgradeTgtDir = upgradeTgtDir;
		this.newVersion = NimbusVersion.fromNimbusInstallation(upgradeSrcDir);
		this.oldVersion = NimbusVersion.fromNimbusInstallation(upgradeTgtDir);
		this.logFile = new File(upgradeTgtDir, "logs/upgrades.log");
	}
	
	private void upgrade() throws Exception {
		try {
			if (!UpgradeRunner.checkUpgradeable(newVersion) || !UpgradeRunner.checkUpgradeable(oldVersion)) {
				exitWithError("The source or target Nimbus installation is older than 0.6.1 and cannot be upgraded."
						+ " (" + oldVersion + " -> " + newVersion + ")");
			}
			if (newVersion.compareTo(oldVersion) < 1) {
				exitWithError("The version you are upgrading to is equal to or lower than the current installation."
						+ " (" + oldVersion + " -> " + newVersion + ")");
			}
			outln("Nimbus installation at '" + upgradeTgtDir.getAbsolutePath() 
					+ "' will be upgraded from version " + oldVersion + " to " + newVersion);
			out("Do you want to proceed? [Y/N]: ");
			boolean go = affirmative(readln());
			if (!go) System.exit(0);
			writeUpgradeLogEntry("Nimbus " + oldVersion + " -> " + newVersion + " upgrade started");
			runUpgrade();
			writeUpgradeLogEntry("Nimbus " + oldVersion + " -> " + newVersion + " upgrade suceeded!");
		} catch (Exception e) {
			if (logFile != null && logFile.exists()) {
				writeUpgradeLogEntry(ExceptionUtils.getFullStackTrace(e));
			}
			throw e;
		}
	}
	
	private void runUpgrade() throws IOException, SQLException {
		if (oldVersion.equalsIgnoreBuild(new NimbusVersion(0,6,1))
				&& newVersion.equalsIgnoreBuild(new NimbusVersion(0,6,2))) {
			new V061To062Upgrader(this).doUpgrade();
		} else {
			new BaseUpgrader(this).doUpgrade();
		}
		replaceVersionFile();
	}

	public File getUpgradeSrcDir() {
		return upgradeSrcDir;
	}

	public File getUpgradeTgtDir() {
		return upgradeTgtDir;
	}

	private void replaceVersionFile() throws IOException {
		File src = new File(upgradeSrcDir, "logs/version.txt");
		File tgt = new File(upgradeTgtDir, "logs/version.txt");
		writeUpgradeLogEntry("Copying " + src + " to " + tgt);
		FileUtils.copyFile(src, tgt);
	}
	
	public void writeUpgradeLogEntry(String entry) throws IOException {
		FileUtils.writeStringToFile(logFile, LocalDateTime.now().toString() + ": " + entry + "\n", true); 
	}
	
	public void outLog(String out) throws IOException {
		super.out(out);
		writeUpgradeLogEntry(out);
	}
	
	private static boolean checkUpgradeable(NimbusVersion version) {
		return version.getMajor() > 0 
				|| (version.getMajor() == 0 && version.getMinor() > 6)
				|| (version.getMajor() == 0 && version.getMinor() == 6 && version.getDot() >= 1);
	}
}
