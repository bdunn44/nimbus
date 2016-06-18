package com.kbdunn.nimbus.server.upgrade.runners;

import java.io.IOException;
import java.sql.SQLException;

import com.kbdunn.nimbus.server.upgrade.UpgradeRunner;
import com.kbdunn.nimbus.server.upgrade.util.UpgradeUtil;

public class BaseUpgrader implements Upgrader {
	
	private UpgradeRunner upgradeRunner;
	private UpgradeUtil upgradeUtil;
	
	public BaseUpgrader(UpgradeRunner utility) {
		this.upgradeRunner = utility;
		this.upgradeUtil = new UpgradeUtil(utility);
	}
	
	UpgradeRunner getUpgradeRunner() {
		return upgradeRunner;
	}
	
	UpgradeUtil getUpgradeUtil() {
		return upgradeUtil;
	}
	
	// These are upgrade tasks that should always be done, regardless of version
	// This is also run for build version upgrades
	@Override
	public void doUpgrade() throws IOException, SQLException {
		upgradeUtil.replaceLibFiles();
		upgradeUtil.replaceStaticResources();
		upgradeUtil.replaceNimbusScripts();
	}
}
