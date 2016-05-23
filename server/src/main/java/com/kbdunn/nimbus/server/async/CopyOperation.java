package com.kbdunn.nimbus.server.async;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.kbdunn.nimbus.common.async.AsyncConfiguration;
import com.kbdunn.nimbus.common.model.FileConflict;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.service.LocalFileService;

public class CopyOperation extends AsyncServerOperation {

	protected List<NimbusFile> sources;
	protected NimbusFile targetFolder;
	protected List<FileConflict> resolutions;

	public CopyOperation(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder) {
		super(config);
		this.sources = Collections.singletonList(source);
		this.targetFolder = targetFolder;
		super.getConfiguration().setName(generateName("Copying"));
	}
	
	public CopyOperation(AsyncConfiguration config, List<NimbusFile> sources, NimbusFile targetFolder) {
		super(config);
		this.sources = sources;
		this.targetFolder = targetFolder;
		super.getConfiguration().setName(generateName("Copying"));
	}
	
	public CopyOperation(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder, List<FileConflict> resolutions) {
		super(config);
		this.sources = Collections.singletonList(source);
		this.targetFolder = targetFolder;
		this.resolutions = resolutions;
		super.getConfiguration().setName(generateName("Copying"));
	}
	
	public CopyOperation(AsyncConfiguration config, List<NimbusFile> sources, NimbusFile targetFolder, List<FileConflict> resolutions) {
		super(config);
		this.sources = sources;
		this.targetFolder = targetFolder;
		this.resolutions = resolutions;
		super.getConfiguration().setName(generateName("Copying"));
	}
	
	protected String generateName(String action) {
		return action 
				+ (sources.size() == 1 && sources.get(0).isDirectory() ? " folder" : " file")
				+ (sources.size() > 1 ? "s" : "")
				+ " to "
				+ targetFolder.getName();
	}
	
	@Override
	public void doOperation() throws Exception {
		LocalFileService fileService = NimbusContext.instance().getFileService();
		long start = System.nanoTime();
		boolean succeeded = true;
		if (resolutions == null) {
			float increment = .8f / sources.size();
			for (NimbusFile source : sources) {
				succeeded = fileService.copyFileTo(source, targetFolder) != null ? succeeded : false;
				setProgress(getProgress() + increment);
			}
		} else {
			setProgress(.2f);
			succeeded = fileService.batchCopy(sources, targetFolder, resolutions);
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