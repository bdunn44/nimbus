package com.kbdunn.nimbus.server.upgrade.runners;

import java.io.IOException;
import java.sql.SQLException;

import com.kbdunn.nimbus.server.upgrade.UpgradeRunner;

public class V061To062Upgrader extends BaseUpgrader {

	public V061To062Upgrader(UpgradeRunner utility) {
		super(utility);
	}
	
	@Override
	public void doUpgrade() throws IOException, SQLException {
		super.doUpgrade();
		super.getUpgradeUtil().runSql(
				"ALTER TABLE NIMBUS.FILE ADD MD5 VARCHAR(32)",
				"ALTER TABLE NIMBUS.FILE ADD LAST_HASHED BIGINT",
				"ALTER TABLE NIMBUS.FILE ADD LAST_MODIFIED BIGINT",
				"ALTER TABLE NIMBUS.USER_STORAGE ADD SYNC_ROOT BOOLEAN",
				"ALTER TABLE NIMBUS.USER DROP COLUMN HMAC_KEY");
		super.getUpgradeUtil().addLinesToConfigFile("nimbus.ssl.https.port", 
				"",
				"# Use this setting if the externally exposed SSL port is something other than nimbus.ssl.https.port. This ensures the redirect",
				"# from HTTP -> HTTPS works correctly in case, for example, you run SSL on port 8443 and have a proxy to redirect port 443 to 8443.",
				"nimbus.ssl.https.external.port=");
	}
}
