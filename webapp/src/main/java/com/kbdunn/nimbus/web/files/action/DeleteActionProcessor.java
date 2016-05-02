package com.kbdunn.nimbus.web.files.action;

import java.util.List;

import com.kbdunn.nimbus.common.model.NimbusFile;

public interface DeleteActionProcessor {
	public void handleDelete();
	public void processDelete(List<NimbusFile> targetFiles);
}
