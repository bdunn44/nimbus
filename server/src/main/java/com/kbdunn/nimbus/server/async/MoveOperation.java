package com.kbdunn.nimbus.server.async;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
		LocalFileService fileService = NimbusContext.instance().getFileService();
		long start = System.nanoTime();
		boolean succeeded = true;
		if (resolutions == null) {
			float increment = .8f / sources.size();
			for (NimbusFile source : sources) {
				succeeded = fileService.moveFileTo(source, targetFolder) != null ? succeeded : false;
				setProgress(getProgress() + increment);
			}
		} else {
			setProgress(.2f);
			succeeded = fileService.batchMove(sources, targetFolder, resolutions);
			setProgress(.8f);
		}
		long end = System.nanoTime();
		long msDuration = TimeUnit.NANOSECONDS.toMillis(end - start);
		if (msDuration < 500) {
			// Force this to take at least 500ms to avoid missed UI updates (push)
			Thread.sleep(500 - msDuration);
		}
		super.setSucceeded(succeeded);
		super.setProgress(1f);
	}
}
