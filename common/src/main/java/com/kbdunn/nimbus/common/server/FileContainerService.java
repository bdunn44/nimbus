package com.kbdunn.nimbus.common.server;

import java.util.List;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.NimbusFile;

public interface FileContainerService<T extends FileContainer> {

	List<NimbusFile> getContents(T container);

	List<NimbusFile> getContents(T container, int startIndex, int count); // LazyQueryContainer

	List<NimbusFile> getFolderContents(T container);

	List<NimbusFile> getFileContents(T container);

	List<NimbusFile> getImageContents(T container);

	String getRelativePath(T container, NimbusFile file);

	NimbusFile resolveRelativePath(T container, String relativePath);

	long getRecursiveContentSize(T container);

	int getContentCount(T container);

	int getRecursiveContentCount(T container);

}