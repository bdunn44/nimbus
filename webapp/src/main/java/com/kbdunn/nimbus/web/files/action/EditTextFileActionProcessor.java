package com.kbdunn.nimbus.web.files.action;

import com.kbdunn.nimbus.common.model.NimbusFile;

public interface EditTextFileActionProcessor {
	public void handleOpenTextEditor();
	public void handleOpenTextEditor(NimbusFile file);
}
