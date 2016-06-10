package com.kbdunn.nimbus.server.upgrade.runners;

import java.io.IOException;
import java.sql.SQLException;

public interface Upgrader {
	void doUpgrade() throws IOException, SQLException;
}
