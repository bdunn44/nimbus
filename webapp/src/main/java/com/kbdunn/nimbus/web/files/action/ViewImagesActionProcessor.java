package com.kbdunn.nimbus.web.files.action;

import com.kbdunn.nimbus.common.model.NimbusFile;

public interface ViewImagesActionProcessor {
	public void handleViewImages();
	public void handleViewImage(NimbusFile image);
}
