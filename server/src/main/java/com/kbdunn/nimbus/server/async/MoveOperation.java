package com.kbdunn.nimbus.server.async;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.kbdunn.nimbus.common.async.AsyncConfiguration;
import com.kbdunn.nimbus.common.model.FileConflict;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.service.LocalFileService;

public class MoveOperation extends CopyOperation {

	public MoveOperation(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder) {
		super(config, Collections.singletonList(source), targetFolder);
		super.getConfiguration().setName(generateName("Moving"));
	}
	
	public MoveOperation(AsyncConfiguration config, List<NimbusFile> sources, NimbusFile targetFolder) {
		super(config, sources, targetFolder);
		super.getConfiguration().setName(generateName("Moving"));
	}
	
	public MoveOperation(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder, List<FileConflict> resolutions) {
		super(config, Collections.singletonList(source), targetFolder, resolutions);
		super.getConfiguration().setName(generateName("Moving"));
	}
	
	public MoveOperation(AsyncConfiguration config, List<NimbusFile> sources, NimbusFile targetFolder, List<FileConflict> resolutions) {
		super(config, sources, targetFolder, resolutions);
		super.getConfiguration().setName(generateName("Moving"));
	}
	
	@Override
	public void doOperation() throws Exception {
		super.doOperation(); // copy files
		if (!super.succeeded()) {
			return;
		}
		LocalFileService fileService = NimbusContext.instance().getFileService();
		for (NimbusFile nf : sources) {
			if (!fileService.delete(nf)) {
				throw new IOException("Unable to delete file " + nf);
			}
		}
		super.setProgress(1f);
	}
}
