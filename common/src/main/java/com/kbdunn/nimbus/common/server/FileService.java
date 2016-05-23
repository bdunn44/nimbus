package com.kbdunn.nimbus.common.server;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.model.FileConflict;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.StorageDevice;

public interface FileService {

	/* Used to copy a NimbusFile object */
	NimbusFile getFileCopy(NimbusFile nf);

	NimbusFile getFileById(long id);

	NimbusFile getFileByPath(Path fullPath);

	NimbusFile getFileByPath(String fullPath);

	NimbusFile getFileByPath(String fullPath, boolean reconcile);

	long getTotalFileCount();

	StorageDevice getStorageDevice(NimbusFile file);

	boolean fileExistsOnDisk(NimbusFile file);

	Date getLastModifiedDate(NimbusFile file);

	String getFileSizeString(NimbusFile file);

	long getRecursiveContentSize(NimbusFile file);

	// Get the parent NimbusFile
	NimbusFile getParentFile(NimbusFile file);

	int getContentCount(NimbusFile folder);

	// Does not account for reconciliation. It's a best guess
	int getRecursiveContentCount(NimbusFile folder);

	// Check if folder has any children in the least expensive way
	boolean folderHasContents(NimbusFile folder);

	// Check if folder has any child folders in the least expensive way
	boolean folderContentsContainsFolder(NimbusFile folder);

	// Determines if the argument is a child of this file
	boolean fileIsChildOf(NimbusFile child, NimbusFile parent);

	// Methods to retrieve folder contents
	// If the folder is reconciled (in sync with database), then the database is queried (fast)
	// Otherwise the file system is checked (slow)
	List<NimbusFile> getContents(NimbusFile folder);

	List<NimbusFile> getContents(NimbusFile folder, int startIndex, int count);

	List<NimbusFile> getFileContents(NimbusFile folder);

	List<NimbusFile> getFolderContents(NimbusFile folder);

	List<NimbusFile> getImageContents(NimbusFile folder);

	// Get recursive contents of a folder
	List<NimbusFile> getRecursiveFolderContents(NimbusFile folder);

	// Returns null if the file doesn't exist or canonical path doesn't start with this file's path
	NimbusFile resolveRelativePath(NimbusFile folder, String relativePath);

	String getRelativePath(NimbusFile root, NimbusFile file);

	NimbusFile getFileConflictResolution(FileConflict conflict);
	
	boolean save(NimbusFile file);

	// Delete file or folder. Folder delete is recursive
	boolean delete(NimbusFile file);

	boolean createDirectory(NimbusFile folder);

	boolean touchFile(NimbusFile file);

	// Rename a file or folder.
	NimbusFile renameFile(NimbusFile file, String newName) throws FileConflictException;

	// Check for an invalid copy/move location
	boolean fileMoveDestinationIsValid(NimbusFile sourceFile, NimbusFile destinationFolder);

	NimbusFile moveFileTo(NimbusFile file, NimbusFile targetFolder) throws FileConflictException;

	// Move a file or folder
	NimbusFile moveFile(NimbusFile file, String fullPath) throws FileConflictException;

	// Copies this file to a target folder. Returns the new copy
	// TODO: resolve file path?
	NimbusFile copyFileTo(NimbusFile file, NimbusFile targetFolder)
			throws FileConflictException, IllegalArgumentException;

	// FOLDER CONFLICTS ARE NOT CONFLICTS!!
	List<FileConflict> checkConflicts(NimbusFile source, NimbusFile targetFolder);

	// Recursively copies source files. Conflict resolution map needs to contain resolution for ALL children.
	boolean batchCopy(List<NimbusFile> sources, NimbusFile targetFolder, List<FileConflict> conflictResolutions);

	boolean reconcileFolder(NimbusFile folder);

	// Synchronizes files that exist on disk with the database
	boolean reconcile(NimbusFile nf);

	InputStream getZipComressedInputStream(List<NimbusFile> contents);

	long getTotalFileSize();
}