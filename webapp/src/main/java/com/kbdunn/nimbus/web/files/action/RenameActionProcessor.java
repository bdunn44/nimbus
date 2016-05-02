package com.kbdunn.nimbus.web.files.action;

import com.kbdunn.nimbus.common.model.NimbusFile;

public interface RenameActionProcessor {
	public void handleRename();
	public void processRename(NimbusFile file, String newName);
}
