package com.kbdunn.nimbus.server.upgrade;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.kbdunn.nimbus.common.model.NimbusVersion;
import com.kbdunn.nimbus.common.util.AbstractCommandLineUtility;
import com.kbdunn.nimbus.server.upgrade.annotations.RequireUpgradeScript;
import com.kbdunn.nimbus.server.upgrade.exceptions.ScriptVersionException;
import com.kbdunn.nimbus.server.upgrade.runners.BaseUpgrader;
import com.kbdunn.nimbus.server.upgrade.runners.Upgrader;
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
			UpgradeRunner runner = null;
			try {
				runner = new UpgradeRunner(upgradeSrcDir, upgradeTgtDir);
			} catch (IllegalArgumentException e) {
				System.out.println("Error detecting Nimbus version " + e.getMessage());
				System.exit(1);
			}
			runner.start();
		} catch (ScriptVersionException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			System.out.println("Error performing upgrade! " + e.getMessage());
			System.exit(2);
		}
	}
	
	private int scriptVersion = -1;
	private final File upgradeSrcDir, upgradeTgtDir, logFile;
	private final NimbusVersion newVersion, oldVersion;
	
	private UpgradeRunner(File upgradeSrcDir, File upgradeTgtDir) throws NumberFormatException, IllegalArgumentException, IOException {
		this.upgradeSrcDir = upgradeSrcDir;
		this.upgradeTgtDir = upgradeTgtDir;
		this.newVersion = NimbusVersion.fromNimbusInstallation(upgradeSrcDir);
		this.oldVersion = NimbusVersion.fromNimbusInstallation(upgradeTgtDir);
		this.logFile = new File(upgradeTgtDir, "logs/upgrades.log");
	}
	
	private void start() throws Exception {
		try {
			// Read script version set by the upgrade script
			String scriptVersion = System.getProperty("script.version");
			if (scriptVersion != null && !scriptVersion.isEmpty()) {
				this.scriptVersion = Integer.valueOf(scriptVersion);
			}
			// Check for upgradeable source & target versions
			if (!UpgradeRunner.checkUpgradeableVersion(newVersion) || !UpgradeRunner.checkUpgradeableVersion(oldVersion)) {
				exitWithError("The source or target Nimbus installation is older than 0.6.1 and cannot be upgraded."
						+ " (" + oldVersion + " -> " + newVersion + ")");
			}
			// Check for an upgrade in the right direction
			if (newVersion.compareTo(oldVersion) < 1) {
				exitWithError("The version you are upgrading to is equal to or lower than the current installation."
						+ " (" + oldVersion + " -> " + newVersion + ")");
			}
			outln("The Nimbus installation at '" + upgradeTgtDir.getAbsolutePath() 
					+ "' will be upgraded from version " + oldVersion + " to " + newVersion);
			out("Do you want to proceed? [Y/N]: ");
			boolean go = affirmative(readln());
			if (!go) System.exit(0);
			writeUpgradeLogEntry("Nimbus " + oldVersion + " -> " + newVersion + " upgrade started");
			doUpgrade();
			writeUpgradeLogEntry("Nimbus " + oldVersion + " -> " + newVersion + " upgrade suceeded!");
		} catch (Exception e) {
			if (logFile != null && logFile.exists()) {
				writeUpgradeLogEntry(ExceptionUtils.getFullStackTrace(e));
			}
			throw e;
		}
	}

	private void doUpgrade() throws IOException, SQLException {
		// Get the correct upgrader
		Upgrader upgrader = null;
		if (oldVersion.equalsIgnoreBuild(new NimbusVersion(0,6,1))
				&& newVersion.equalsIgnoreBuild(new NimbusVersion(0,6,2))) {
			upgrader = new V061To062Upgrader(this);
		} else {
			upgrader = new BaseUpgrader(this);
		}
		
		// Check annotations for additional preconditions
		RequireUpgradeScript requireScript = upgrader.getClass().getAnnotation(RequireUpgradeScript.class);
		if (requireScript != null) {
			int minVersion = requireScript.minVersion();
			if (this.scriptVersion < minVersion) throw new ScriptVersionException(minVersion);
		}
		
		// Do the upgrade
		upgrader.doUpgrade();
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

	private static boolean checkUpgradeableVersion(NimbusVersion version) {
		return version.getMajor() > 0 
				|| (version.getMajor() == 0 && version.getMinor() > 6)
				|| (version.getMajor() == 0 && version.getMinor() == 6 && version.getDot() >= 1);
	}
}
