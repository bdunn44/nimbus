package com.kbdunn.nimbus.common.sync.interfaces;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import com.kbdunn.nimbus.common.sync.model.FileNode;

public interface FileManager {
	
	/**
	 * Add a file or a folder. Note that the file must already be in the predefined Hive2Hive folder. If the
	 * folder is not empty, containing files are <strong>not</strong> automatically added as well. The file
	 * must exist on the disk.
	 * 
	 * @param file The file or folder to add.
	 * @return A file add process.
	 * @throws NoPeerConnectionException If the peer is not connected to the network.
	 * @throws NoSessionException If no user has logged in.
	 * @throws IllegalArgumentException If the provided parameters are incorrect.
	 */
	Callable<Void> createAddProcess(File file) throws IllegalArgumentException;
	
	Callable<Void> createFolderAddProcess(List<File> folders) throws IllegalArgumentException;
	
	/**
	 * Delete a file / folder and all versions of that file from the network. This operation deletes also the
	 * file on disk. <strong>Note that this operation is irreversible.</strong> If the folder is not empty,
	 * all sub-files are deleted as well.
	 * 
	 * @param file The file or folder to delete.
	 * @return A file delete process.
	 * @throws NoPeerConnectionException If the peer is not connected to the network.
	 * @throws NoSessionException If no user has logged in.
	 * @throws IllegalArgumentException If the provided parameters are incorrect.
	 */
	Callable<Void> createDeleteProcess(File file) throws IllegalArgumentException;

	/**
	 * Update a file and create a new version. Folders cannot be updated.<br>
	 * 
	 * @param file The file to update.
	 * @return A file update process.
	 * @throws NoPeerConnectionException If the peer is not connected to the network.
	 * @throws NoSessionException If no user has logged in.
	 * @throws IllegalArgumentException If the provided parameters are incorrect.
	 */
	Callable<Void> createUpdateProcess(File file) throws IllegalArgumentException;

	/**
	 * Download a file that exists in the network and store it on the disk. If the file is a folder, a folder
	 * on disk is created, but containing files are not downloaded automatically.<br>
	 * <strong>Note:</strong>If the file on disk already exists, it will be overwritten.
	 * 
	 * @param file The file to be downloaded.
	 * @return A file download process.
	 * @throws NoPeerConnectionException If the peer is not connected to the network.
	 * @throws NoSessionException If no user has logged in.
	 * @throws IllegalArgumentException If the provided parameters are incorrect.
	 */
	Callable<Void> createDownloadProcess(File file) throws IllegalArgumentException;

	/**
	 * Move a file / folder from a given source to a given destination. This operation can also be used to
	 * rename a file, or moving and renaming it together. In case of moving a folder, sub-files are moved too.
	 * Note that this call does not perform any change on the file system.
	 * 
	 * @param source The full file path of the file to be moved.
	 * @param destination The full file path of the file destination.
	 * @return A file move process.
	 * @throws NoPeerConnectionException If the peer is not connected to the network.
	 * @throws NoSessionException If no user has logged in.
	 * @throws IllegalArgumentException If the provided parameters are incorrect.
	 */
	Callable<Void> createMoveProcess(File source, File destination) throws IllegalArgumentException;

	/**
	 * Get a tree of all files in the DHT of the currently logged in user. This must not necessary match
	 * with the file tree on disk because Hive2Hive only performs file operations at manual calls.
	 * 
	 * @return A file list process.
	 * @throws NoPeerConnectionException If the peer is not connected to the network.
	 * @throws NoSessionException If no user has logged in.
	 */
	Callable<FileNode> createFileListProcess();

	/**
	 * Subscribe all file event handlers of the given listener instance.
	 * <strong>Note:</strong> The listener needs to annotate the handlers with the @Handler annotation.
	 * 
	 * @param listener implementing the handler methods
	 */
	void subscribeFileEvents(FileEventListener listener);
}
