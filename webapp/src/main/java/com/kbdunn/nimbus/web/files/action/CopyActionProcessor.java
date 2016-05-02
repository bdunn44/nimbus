package com.kbdunn.nimbus.web.files.action;

import java.util.List;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.NimbusFile;

public interface CopyActionProcessor {
	public void handleCopy();
	public void processCopy(List<NimbusFile> sourceFiles, NimbusFile targetFolder);
	public FileContainer getRootContainer();
}