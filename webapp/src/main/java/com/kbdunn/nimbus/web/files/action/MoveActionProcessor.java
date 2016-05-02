package com.kbdunn.nimbus.web.files.action;

import java.util.List;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.NimbusFile;

public interface MoveActionProcessor {
	public void handleMove();
	public void processMove(List<NimbusFile> sourceFiles, NimbusFile targetFolder);
	public FileContainer getRootContainer();
}
