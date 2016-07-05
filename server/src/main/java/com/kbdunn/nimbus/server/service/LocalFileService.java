package com.kbdunn.nimbus.server.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.model.FileConflict;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.Song;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.model.Video;
import com.kbdunn.nimbus.common.server.FileContainerService;
import com.kbdunn.nimbus.common.server.FileService;
import com.kbdunn.nimbus.common.util.FileUtil;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.dao.NimbusFileDAO;
import com.kbdunn.nimbus.server.util.ZipCompress;

public class LocalFileService implements FileContainerService<NimbusFile>, FileService  {
	
	private static final Logger log = LogManager.getLogger(LocalFileService.class.getName());
	private static final String MUSIC_FORMATS = "mp3 wav ogg m4a wma";
	// Unsupported for now: aac, ac3, avi, flac
	private static final String VIDEO_FORMATS = "mp4 webm wmv flv ogv";
	// Unsupported for now: ??
	private static final String IMAGE_FORMATS = "jpg jpeg jpe gif bmp png tif tiff";
	
	private LocalMediaLibraryService mediaLibraryService;
	private LocalUserService userService;
	private LocalStorageService storageService;
	private FileSyncService syncService;
	
	public LocalFileService() {  }
	
	public void initialize(NimbusContext container) {
		mediaLibraryService = container.getMediaLibraryService();
		userService = container.getUserService();
		storageService = container.getStorageService();
		syncService = container.getFileSyncService();
	}
	
	/* Used to copy a NimbusFile object */
	@Override
	public NimbusFile getFileCopy(NimbusFile nf) {
		return new NimbusFile(nf.getId(), nf.getUserId(), nf.getStorageDeviceId(), nf.getPath(), nf.isDirectory(), nf.getSize(), nf.isSong(), 
				nf.isVideo(), nf.isImage(), nf.isReconciled(), nf.getLastReconciled(), nf.isLibraryRemoved(), nf.getMd5(), nf.getLastHashed(),
				nf.getLastModified(), nf.getCreated(), nf.getUpdated());
	}
	
	private void setFileAttributes(NimbusFile nf) {
		log.trace("setFileAttributes() called for " + nf.getPath());
		
		Path p = Paths.get(nf.getPath());
		if (!Files.exists(p, LinkOption.NOFOLLOW_LINKS)) {
			nf.setDirectory(null);
		} else {
		    try {
		    	nf.setLastModified(FileUtil.getLastModifiedTime(nf));
				nf.setDirectory(Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS));
				if (nf.isDirectory()) nf.setSize(0l);
				else nf.setSize(Files.size(p));
			} catch (IOException e) {
				log.error("Error setting file attributes", e);
			}
		}
		
		nf.setSong(false);
		nf.setVideo(false);
		nf.setImage(false);
		nf.setReconciled(false);
	    
	    if (!nf.isDirectory()) {
			String ext = nf.getFileExtension();
			if (!ext.isEmpty()) {
				if (MUSIC_FORMATS.contains(ext.toLowerCase()))
			    	nf.setSong(true);
				else if (VIDEO_FORMATS.contains(ext.toLowerCase()))
			    	nf.setVideo(true);
				else if (IMAGE_FORMATS.contains(ext.toLowerCase()))
					nf.setImage(true);
			}
	    }
		
	    if (nf.getPath().indexOf("/NimbusUser-") == -1) {
	    	return;
	    	//throw new IllegalArgumentException("The file is not in a user's directory! Path does not contain the string '/NimbusUser-'");
	    }
	    String dPath = nf.getPath().substring(0, nf.getPath().indexOf("/NimbusUser-"));
	    String uName = nf.getPath().replace(dPath + "/NimbusUser-", "");
	    uName = uName.indexOf("/") == -1 ? uName : uName.substring(0, uName.indexOf("/"));
		StorageDevice d = storageService.getStorageDeviceByPath(dPath);
		if (d != null) nf.setStorageDeviceId(d.getId());
	    NimbusUser user = userService.getUserByNameOrEmail(uName);
		if (user != null) nf.setUserId(user.getId());
	}
	
	@Override
	public NimbusFile getFileById(long id) {
		return NimbusFileDAO.getById(id);
	}
	
	@Override
	public NimbusFile getFileByPath(Path fullPath) {
		return getFileByPath(fullPath.toAbsolutePath().toString().replace("\\", "/").replace("C:", ""));
	}
	
	@Override
	public NimbusFile getFileByPath(String fullPath) {
		return getFileByPath(fullPath, false);
	}
	
	@Override
	public NimbusFile getFileByPath(String fullPath, boolean reconcile) {
		NimbusFile nf = NimbusFileDAO.getByPath(fullPath);
		if (nf == null) {
			nf = new NimbusFile(null, null, null, fullPath, null, null, null, null, null, null, null, null, null, null, null, null, null);
			setFileAttributes(nf);
		}
		if (reconcile) reconcile(nf);
		return nf;
	}
	
	@Override
	public long getTotalFileCount() {
		return NimbusFileDAO.getTotalFileCount();
	}
	
	// TODO: Move to system service?
	@Override
	public long getTotalFileSize() {
		return NimbusFileDAO.getTotalFileSize();
	}
	
	@Override
	public StorageDevice getStorageDevice(NimbusFile file) {
		if (file.getPath() == null) throw new NullPointerException("File path cannot be null");
		if (file.getStorageDeviceId() != null)
			return storageService.getStorageDeviceById(file.getStorageDeviceId());
		String path = file.getPath();
		if (path.indexOf("/NimbusUser-") == -1) throw new IllegalArgumentException("File path must contain '/NimbusUser-'");
		return storageService.getStorageDeviceByPath(path.substring(0, path.indexOf("/NimbusUser-")));
	}
	
	@Override
	public boolean fileExistsOnDisk(NimbusFile file) {
		return Files.exists(Paths.get(file.getPath()));
	}
	
	@Override
	public Date getLastModifiedDate(NimbusFile file) {
		try {
			if (fileExistsOnDisk(file))
				return FileUtil.getLastModifiedDate(file);
			else
				return file.getUpdated();
		} catch (IOException e) {
			log.error(e, e);
			return null;
		}
	}
	
	@Override
	public String getFileSizeString(NimbusFile file) {
		if (file.isDirectory()) {
			return getRecursiveFileSizeString(file);
		}
		return StringUtil.toHumanSizeString(file.getSize());
	}
	
	private String getRecursiveFileSizeString(NimbusFile file) {
		Long size = file.isDirectory() ? getRecursiveContentSize(file) : file.getSize();
		return StringUtil.toHumanSizeString(size);
	}
	
	@Override
	public long getRecursiveContentSize(NimbusFile file) {
		if (!file.isDirectory()) return file.getSize();
		return NimbusFileDAO.getRecursiveFileSize(file);
	}
	
	// Get the parent NimbusFile
	@Override
	public NimbusFile getParentFile(NimbusFile file) {
		return getFileByPath(Paths.get(file.getPath()).getParent());
	}

	@Override
	public int getContentCount(NimbusFile folder) {
		if (!folder.isDirectory()) return 0;
		if (!folder.isReconciled() || getStorageDevice(folder).isAutonomous()) reconcileFolder(folder);
		return NimbusFileDAO.getContentCount(folder);
	}
	
	// Does not account for reconciliation. It's a best guess
	@Override
	public int getRecursiveContentCount(NimbusFile folder) {
		if (!folder.isDirectory()) return 0;
		return NimbusFileDAO.getRecursiveChildCount(folder);
	}
	
	// Check if folder has any children in the least expensive way
	@Override
	public boolean folderHasContents(NimbusFile folder) {
		if (!folder.isDirectory()) return false;
		if (folder.isReconciled() && !getStorageDevice(folder).isAutonomous()) {
			return NimbusFileDAO.fileHasChildren(folder);
		} else {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(folder.getPath()))) {
				return stream.iterator().hasNext();
			} catch (IOException | DirectoryIteratorException e) {
			    log.error(e, e);
			}
		}
		
	    return false;
	}
	
	// Check if folder has any child folders in the least expensive way
	@Override
	public boolean folderContentsContainsFolder(NimbusFile folder) {
		if (!folder.isDirectory()) throw new IllegalArgumentException("Can't get contents of a regular file");
		if (folder.isReconciled() && !getStorageDevice(folder).isAutonomous()) {
			return NimbusFileDAO.fileHasChildFolders(folder);
		} else {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(folder.getPath()))) {
				for (Path p : stream) {
					if (Files.isDirectory(p)) return true;
				}
			} catch (IOException | DirectoryIteratorException e) {
			    log.error(e, e);
			}
		}
		
	    return false;
	}
	
	// Determines if the argument is a child of this file
	@Override
	public boolean fileIsChildOf(NimbusFile child, NimbusFile parent) {
		if (!parent.isDirectory()) return false;
		String cp = child.getPath().toUpperCase();
		if (child.isDirectory() && !cp.endsWith("/")) cp += "/";
		String pp = parent.getPath().toUpperCase();
		if (!pp.endsWith("/")) pp += "/";
		return cp.startsWith(pp) && !cp.equals(pp);
	}
	
	// Methods to retrieve folder contents
	// If the folder is reconciled (in sync with database), then the database is queried (fast)
	// Otherwise the file system is checked (slow)
	@Override
	public List<NimbusFile> getContents(NimbusFile folder) {
		log.debug("Getting all contents of " + folder);
		if (!folder.isDirectory()) throw new IllegalArgumentException("Can't get contents of a regular file");
		if (!folder.isReconciled() || getStorageDevice(folder).isAutonomous()) reconcileFolder(folder);
		return NimbusFileDAO.getContents(folder);
	}

	@Override
	public List<NimbusFile> getContents(NimbusFile folder, int startIndex, int count) {
		log.debug("Getting top " + count + " contents of " + folder + " from start index " + startIndex);
		if (!folder.isDirectory()) throw new IllegalArgumentException("Can't get contents of a regular file");
		if (!folder.isReconciled() || getStorageDevice(folder).isAutonomous()) reconcileFolder(folder);
		return NimbusFileDAO.getContents(folder, startIndex, count);
	}
	
	@Override
	public List<NimbusFile> getFileContents(NimbusFile folder) {
		log.debug("Getting file contents of " + folder);
		if (!folder.isDirectory()) throw new IllegalArgumentException("Can't get contents of a regular file");
		if (!folder.isReconciled() || getStorageDevice(folder).isAutonomous()) reconcileFolder(folder);
		return NimbusFileDAO.getFileContents(folder);
	}

	@Override
	public List<NimbusFile> getFolderContents(NimbusFile folder) {
		log.debug("Getting folder contents of " + folder);
		if (!folder.isDirectory()) throw new IllegalArgumentException("Can't get contents of a regular file");
		if (!folder.isReconciled() || getStorageDevice(folder).isAutonomous()) reconcileFolder(folder);
		return NimbusFileDAO.getFolderContents(folder);
	}

	@Override
	public List<NimbusFile> getImageContents(NimbusFile folder) {
		log.debug("Getting image contents of " + folder);
		if (!folder.isDirectory()) throw new IllegalArgumentException("Can't get contents of a regular file");
		if (!folder.isReconciled() || getStorageDevice(folder).isAutonomous()) reconcileFolder(folder);
		return NimbusFileDAO.getImageContents(folder);
	}
	
	// Get contents from the file system
	private List<NimbusFile> getContentsFromDisk(NimbusFile folder) {
		if (!folder.isDirectory() || !fileExistsOnDisk(folder)) 
			throw new IllegalArgumentException("Cannot get contents for a path that is not a directory or does not exist");
		
		List<NimbusFile> contents = new ArrayList<NimbusFile>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(folder.getPath()))) {
		    for (Path p: stream) {
		        contents.add(getFileByPath(p));
		    }
		} catch (IOException | DirectoryIteratorException e) {
		    log.error(e, e);
		}
		
		return contents;
	}
	
	// Get recursive contents of a folder
	@Override
	public List<NimbusFile> getRecursiveFolderContents(NimbusFile folder) {
		List<NimbusFile> result = new ArrayList<NimbusFile>();
		if (folder.isDirectory()) {
			for (NimbusFile f : getContents(folder)) {
				result.add(f);
				if (f.isDirectory()) result.addAll(getRecursiveFolderContents(f));
			}
		}
		return result;
	}
	
	// Returns null if the file doesn't exist or canonical path doesn't start with this file's path
	@Override
	public NimbusFile resolveRelativePath(NimbusFile folder, String relativePath) {
		log.debug("Resolving relative path " + relativePath + " for root " + folder);
		log.debug("Storage device for folder is " + getStorageDevice(folder));
		
		if (folder == null) throw new NullPointerException("Folder cannot be null");
		if (relativePath == null) throw new NullPointerException("Relative Path cannot be null");
		//if (!folder.isDirectory()) throw new IllegalArgumentException("Folder is not a directory!");
		if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
		
		boolean autonomous = getStorageDevice(folder).isAutonomous();
		if (autonomous || !folder.isReconciled()) reconcileFolder(folder); // reconcile the root
		if (relativePath.indexOf("/") != -1) { // reconcile each step in-between, if needed
			String walker = folder.getPath();
			String[] steps = relativePath.split("/");
			NimbusFile walkerFile = null;
			for (int i = 0; i < steps.length-1; i++) {
				walker += "/" + steps[i];
				walkerFile = getFileByPath(walker);
				//if (!fileExistsOnDisk(walkerFile) || !walkerFile.isDirectory()) return null;
				if ((autonomous || !walkerFile.isReconciled()) && walkerFile.isDirectory()) 
					reconcileFolder(walkerFile);
			}
		}
		NimbusFile relativeFile = getFileByPath(folder.getPath() + "/" + relativePath);
		if (relativeFile.getId() == null // new file
				|| (fileExistsOnDisk(relativeFile) && fileIsChildOf(relativeFile, folder))) { // existing file
			return relativeFile;
		}
		return null;
	}
	
	@Override
	public String getRelativePath(NimbusFile root, NimbusFile file) {
		if (fileIsChildOf(file, root))
			return file.getPath().substring(root.getPath().length() + 1);
		else
			return null;
	}
	
	@Override
	public boolean save(NimbusFile file) {
		return save(file, null, false, null);
	}
	
	public boolean save(NimbusFile file, String originationId) {
		return save(file, null, false, originationId);
	}
	
	public boolean save(NimbusFile file, NimbusFile copySource, boolean moved, String originationId) {
		if (file.getUserId() == null) throw new NullPointerException("User ID cannot be null");
		if (file.getStorageDeviceId() == null) throw new NullPointerException("Drive ID cannot be null");
		// Check instantiation, work with a temp file
		// At the end be sure everything is up-to-date
		NimbusFile tmpfile = checkInstantiation(file);
		boolean success = false;
		if (file instanceof Song) success = mediaLibraryService.save((Song) file, copySource, moved, originationId);
		/*if (file.isSong()) return mediaLibraryService.save((Song) checkInstantiation(file));
		if (file instanceof Video) return mediaLibraryService.save((Video) file);
		if (file.isVideo()) return mediaLibraryService.save((Video) checkInstantiation(file));*/
		if (file.getId() == null) success = insert(file, copySource, originationId);
		else success = update(file, copySource, moved, originationId);
		if (tmpfile != file) {
			// checkInstantiation() did it's job. Update the original object
			file.setId(tmpfile.getId());
			file.setCreated(tmpfile.getCreated());
			file.setUpdated(tmpfile.getUpdated());
		}
		return success;
	}
	
	// Create a record in the database
	boolean insert(NimbusFile file, NimbusFile copySource, String originationId) {
		log.trace("Inserting file in database " + file);
		if (!NimbusFileDAO.insert(file)) return false;
		NimbusFile dbf = NimbusFileDAO.getByPath(file.getPath());
		file.setId(dbf.getId());
		file.setCreated(dbf.getCreated());
		file.setUpdated(dbf.getUpdated());
		if (copySource == null) {
			try {
				syncService.publishFileAddEvent(file, originationId);
			} catch (IOException e) {
				log.error("Error publishing file add event", e);
			}
		} else {
			try {
				syncService.publishFileCopyEvent(copySource, file, originationId);
			} catch (IOException e) {
				log.error("Error publishing file copy event", e);
			}
		}
		return true;
	}
	
	// Update a record in the database
	// Should surpress the file event only if the file was moved
	boolean update(NimbusFile file, NimbusFile copySource, boolean moved, String originationId) {
		log.trace("Updating file in database " + file);
		if (copySource == null) {
			if (!file.isDirectory()) {
				// Publish file update - sync service takes care of hashing, etc.
				syncService.publishFileUpdateEvent(file, originationId);
			}
		} else {
			// Send copy/move event
			if (moved) {
				try {
					syncService.publishFileMoveEvent(copySource, file, originationId);
				} catch (IOException e) {
					log.error("Error publishing file move event", e);
				}
			} else {
				try {
					// Don't think this is needed... Copied files are inserted
					syncService.publishFileCopyEvent(copySource, file, originationId);
				} catch (IOException e) {
					log.error("Error publishing file copy event", e);
				}
			}
		}
		if (!NimbusFileDAO.update(file)) return false;
		file.setUpdated(new Date()); // close enough
		return true;
	}
	
	// Delete file or folder. Folder delete is recursive
	@Override
	public boolean delete(NimbusFile file) {
		return delete(file, null);
	}
	
	public boolean delete(NimbusFile file, String originationId) {
		if (file.isDirectory()) {
			reconcileFolder(file);
			for (NimbusFile child : getContents(file)) {
				if (!delete(child, originationId)) return false;
			}
		}
		try {
			syncService.awaitHashJobsFinished(Collections.singletonList(file)); // Cancel isn't working
			Files.delete(Paths.get(file.getPath()));
			if (NimbusFileDAO.delete(file)) {
				// Publish file delete event
				syncService.publishFileDeleteEvent(file, originationId);
				return true;
			}
		} catch (IOException e) {
			log.error("Error deleting file", e);
			return false;
		}
		log.warn("Uncaught error occurred while deleting file " + file);
		return false;
	}
	
	@Override
	public boolean createDirectory(NimbusFile folder) {
		return createDirectory(folder, null);
	}
	
	public boolean createDirectory(NimbusFile folder, String originationId) {
		if (Files.exists(Paths.get(folder.getPath()))) return true;
		if (!fileExistsOnDisk(getParentFile(folder))) {
			if (!createDirectory(getParentFile(folder), originationId)) 
				return false;
		}
		
		try {
			Files.createDirectory(Paths.get(folder.getPath()));
		} catch (IOException e) {
			log.error(e, e);
			return false;
		}
		folder.setDirectory(true);
		folder.setSize(0l);
		return save(folder, originationId);
	}
	
	@Override
	public boolean touchFile(NimbusFile file) {
		try {
			syncService.awaitHashJobsFinished(Collections.singletonList(file));
			Path fp = Paths.get(file.getPath());
			if (Files.exists(fp)) {
				Files.setLastModifiedTime(fp, FileTime.fromMillis(System.currentTimeMillis()));
				file.setUpdated(new Date());
			} else {
				if (!fp.toFile().createNewFile()) return false;
				setFileAttributes(file);
			}
		} catch (IOException e) {
			log.error(e, e);
			return false;
		}
		return save(file);
	}
	
	// Rename a file or folder.
	@Override
	public NimbusFile renameFile(NimbusFile file, String newName) throws FileConflictException {
		if (newName.contains("/")) throw new IllegalArgumentException("Name cannot contain a slash character");
		
		String newPath = getParentFile(file).getPath() + "/" + newName;
		// Check for file extension change
		if (!file.getFileExtension().equals(StringUtil.getFileExtensionFromPath(newPath))) {
			// check if file is still a song/video/image
			setFileAttributes(file); 
		}
		return moveFile(file, newPath);
	}
	
	// Check for an invalid copy/move location
	@Override
	public boolean fileMoveDestinationIsValid(NimbusFile sourceFile, NimbusFile destinationFolder) {
		return !sourceFile.equals(destinationFolder) && !fileIsChildOf(destinationFolder, sourceFile);
	}
	
	@Override
	public NimbusFile moveFileTo(NimbusFile file, NimbusFile targetFolder) throws FileConflictException {
		return moveFileTo(file, targetFolder, null);
	}
	
	public NimbusFile moveFileTo(NimbusFile file, NimbusFile targetFolder, String originationId) throws FileConflictException {
		return moveFile(file, targetFolder.getPath() + "/" + file.getName(), originationId);
	}
	
	@Override
	public NimbusFile moveFile(NimbusFile file, String fullPath) throws FileConflictException {
		return moveFile(file, fullPath, null);
	}
	
	public NimbusFile moveFile(NimbusFile file, String fullPath, String originationId) throws FileConflictException {
		return copyOrMoveFile(file, fullPath, originationId, true);
	}
	
	@Override
	public NimbusFile copyFileTo(NimbusFile file, NimbusFile targetFolder) throws FileConflictException, IllegalArgumentException {
		return copyFileTo(file, targetFolder, null);
	}
	
	public NimbusFile copyFileTo(NimbusFile file, NimbusFile targetFolder, String originationId) throws FileConflictException, IllegalArgumentException {
		return copyFile(file, targetFolder.getPath() + "/" + file.getName(), originationId);
	}
	
	public NimbusFile copyFile(NimbusFile file, String fullPath, String originationId) throws FileConflictException, IllegalArgumentException {
		return copyOrMoveFile(file, fullPath, originationId, false);
	}
	
	private NimbusFile copyOrMoveFile(NimbusFile file, String fullPath, String originationId, boolean move) throws FileConflictException, IllegalArgumentException {
		NimbusFile target = getFileByPath(fullPath);
		NimbusFile targetFolder = getParentFile(target);
		
		if (!targetFolder.isDirectory() || !fileExistsOnDisk(targetFolder))
			throw new IllegalArgumentException("Cannot " + (move ? "move" : "copy") + " file to a path which is not a directory or does not exist");
		
		List<FileConflict> conflicts = checkConflicts(target, targetFolder);
		if (!conflicts.isEmpty())
			throw new FileConflictException(conflicts);
		
		log.info((move ? "Moving" : "Copying") + " file " + file + " to " + target);
		syncService.awaitHashJobsFinished(Arrays.asList(file, target));
		
		if (file.isDirectory()) {
			log.debug("Performing recursive folder " + (move ? "move" : "copy"));
			target.setDirectory(true);
			if (!fileExistsOnDisk(target)) {
				// Create on disk - will do DB save later
				try {
					Files.createDirectory(Paths.get(target.getPath()));
				} catch (IOException e) {
					log.error("Error encountered while creating target directory " + target.getPath(), e);
					return null;
				}
				//createDirectory(target);
			}
			
			for (NimbusFile child: getContents(file)) {
				if (move) {
					moveFileTo(child, target);
				} else {
					copyFileTo(child, target);
				}
			}
			
			if (move) {
				// Delete the move source folder on disk
				try {
					Files.delete(Paths.get(file.getPath()));
				} catch (IOException e) {
					log.error("Error deleting source of folder move: " + file, e);
					return null;
				}
			}
		} else {
			try {
				if (move) {
					Files.move(Paths.get(file.getPath()), Paths.get(target.getPath()));
				} else {
					Files.copy(Paths.get(file.getPath()), Paths.get(target.getPath()));
				}
			} catch (IOException e) {
				log.error("Error " + (move ? "moving" : "copying") + " file", e);
				return null;
			}
		}
		
		setFileAttributes(target);
		if (move) target.setId(file.getId()); // Save the ID in case of move to maintain FKs to Shares, etc.
		target.setUserId(file.getUserId());
		target.setMd5(file.getMd5());
		target.setLastHashed(file.getLastHashed());
		save(target, file, move, originationId);
		return target;
	}
	
	// FOLDER CONFLICTS ARE NOT CONFLICTS!!
	@Override
	public List<FileConflict> checkConflicts(NimbusFile source, NimbusFile targetFolder) {
		List<FileConflict> conflicts = new ArrayList<FileConflict>();
		
		NimbusFile target = getFileByPath(targetFolder.getPath() + "/" + source.getName());
		if (source.isDirectory()) {
			target.setDirectory(true);
			List<NimbusFile> children = getContents(source);
			for (NimbusFile child: children) {
				conflicts.addAll(checkConflicts(child, target));
			}
		}
		if (fileExistsOnDisk(target) && !target.isDirectory()) conflicts.add(new FileConflict(source, target));
		
		return conflicts;
	}
	
	public boolean batchMove(List<NimbusFile> sources, NimbusFile targetFolder, List<FileConflict> conflictResolutions) {
		return processBatchCopyOrMove(sources, targetFolder, conflictResolutions, true, null);
	}

	@Override
	public boolean batchCopy(List<NimbusFile> sources, NimbusFile targetFolder, List<FileConflict> conflictResolutions) {
		return processBatchCopyOrMove(sources, targetFolder, conflictResolutions, false, null);
	}
	
	// Recursively copies or moves source files
	// Conflict resolution map needs to contain resolution for ALL children.
	boolean processBatchCopyOrMove(List<NimbusFile> sources, NimbusFile targetFolder, 
			List<FileConflict> conflictResolutions, boolean move, String originationId) {
		log.debug("Processing batch " + (move ? "move" : "copy") + " with conflict resolutions");
		
		// Build list of sources with conflicts
		List<NimbusFile> conflictedSources = new ArrayList<NimbusFile>();
		for (FileConflict fc : conflictResolutions)
			conflictedSources.add(fc.getSource());
		
		// Build complete (recursive) list of conflict-free sources
		List<NimbusFile> conflictFreeSources = new ArrayList<NimbusFile>();
		for (NimbusFile source : sources) {
			// Add recursive contents
			if (source.isDirectory()) {
				for (NimbusFile sourceChild : getRecursiveFolderContents(source)) {
					if (!conflictedSources.contains(sourceChild))
						conflictFreeSources.add(sourceChild);
				}
			} else {
				if (!conflictedSources.contains(source))
					conflictFreeSources.add(source);
			}
		}
		
		// Process conflict-free copies
		log.trace("Target folder is: " + targetFolder.getPath());
		for (NimbusFile f : conflictFreeSources) {
			try {
				if (move) {
					moveFileTo(f, targetFolder, originationId);
				} else {
					copyFileTo(f, targetFolder, originationId);
				}
			} catch (IllegalArgumentException | FileConflictException e) {
				log.error(e, e);
				return false;
			}
		}
		
		// Process conflicts
		for (FileConflict fc : conflictResolutions) {
			if (fc.getResolution().equals(FileConflict.Resolution.IGNORE)) continue;
			try {
				NimbusFile resolution = fc.getResolution() == FileConflict.Resolution.COPY ? getFileConflictResolution(fc) : fc.getTarget();
				syncService.awaitHashJobsFinished(Collections.singletonList(fc.getSource()));
				syncService.awaitHashJobsFinished(Collections.singletonList(resolution));
				log.debug("Copy Source: " + fc.getSource().getPath() + ", resolution: " + resolution.getPath());
				if (move) {
					moveFile(fc.getSource(), resolution.getPath(), originationId);
				} else {
					copyFile(fc.getSource(), resolution.getPath(), originationId);
				}
			} catch (Exception e) {
				log.error("Error processing file conflict resolution", e);
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean reconcileFolder(NimbusFile folder) {
		return reconcileFolder(folder, null, false, null);
	}
	
	public boolean reconcileFolder(NimbusFile folder, NimbusFile copySource, boolean moved, String originationId) {
		if (!folder.isDirectory()) throw new IllegalArgumentException("Argument must be a directory");
		
		// Reconcile a maximum of once every 5 seconds
		long ts = System.currentTimeMillis();
		if (folder.getId() != null) {
			long lr = NimbusFileDAO.getLastReconciled(folder.getId());
			if (ts - lr < 5000) { 
				return true;
			}
		}
		
		log.debug("Reconciling folder " + folder);
		List<NimbusFile> fullContents = getContentsFromDisk(folder); // sets attributes if needed
		
		for (NimbusFile dbf : NimbusFileDAO.getContents(folder)) {
			if (!fullContents.contains(dbf)) {
				log.debug("Deleting bad file in database " + dbf);
				if (!NimbusFileDAO.delete(dbf)) return false;
			}
		}
		
		for (NimbusFile child : fullContents) {
			child = checkInstantiation(child);
			save(child, copySource, moved, originationId);
			//if (child.isDirectory() && recursive) reconcileFolder(child, true);
		}
		
		folder.setReconciled(true);
		folder.setLastReconciled(ts);
		return save(folder, copySource, moved, originationId);
	}
	
	// Synchronizes files that exist on disk with the database
	@Override
	public boolean reconcile(NimbusFile nf) {
		return reconcile(nf, null, false, null);
	}
	
	public boolean reconcile(NimbusFile nf, NimbusFile copySource, boolean moved, String originationId) {
		log.trace("Reconciling file " + nf);
		
		setFileAttributes(nf);
		//nf = checkInstantiation(nf); // This is done in save()
		
		if (nf.isDirectory()) {
			return reconcileFolder(nf, copySource, moved, originationId);
		} else {
			 return save(nf, copySource, moved, originationId);
		}
	}

	// Check that the file is correctly instantiated and set attributes if not
	private NimbusFile checkInstantiation(NimbusFile nf) {
		if (nf.isSong() && !(nf instanceof Song)) {
			log.debug("Converting file to Song");
			nf = new Song(nf);
			mediaLibraryService.setSongAttributes((Song) nf);
		} else if (nf.isVideo() && !(nf instanceof Video)) {
			log.debug("Converting file to Video");
			nf = new Video(nf);
			mediaLibraryService.setVideoAttributes((Video) nf);
		}
		return nf;
	}

	@Override
	public NimbusFile getFileConflictResolution(FileConflict conflict) {
		NimbusFile source = conflict.getSource();
		NimbusFile target = conflict.getTarget();
		
		Pattern fileRegex = Pattern.compile("^(.*?)( \\((\\d+)\\))?(\\..*)?$");
		//Pattern nameRegex = Pattern.compile("^([ a-zA-Z0-9]+)\\(([0-9]+)\\).*$");
		Matcher fileMatcher = fileRegex.matcher(source.getName());
		String sourceFirst = null;
		String sourceExt = null;
		if (fileMatcher.matches()) {
			sourceFirst = fileMatcher.group(1);
			sourceExt = fileMatcher.group(4);
		}
		Pattern sourceRegex = Pattern.compile("^" 
				+ Pattern.quote(sourceFirst) + "( \\((\\d+)\\))" + Pattern.quote(sourceExt) + "$");
		Integer maxCopyNo = 0;
		for (NimbusFile f : getContents(getParentFile(target))) {
			Matcher sourceMatcher = sourceRegex.matcher(f.getName());
			if (sourceMatcher.matches()) {
				int copyNo = Integer.valueOf(sourceMatcher.group(2));
				maxCopyNo = copyNo > maxCopyNo ? copyNo : maxCopyNo;
			}
		}
		maxCopyNo++;
		return getFileByPath(getParentFile(target).getPath() + "/" + sourceFirst + " (" + maxCopyNo + ")" + sourceExt);
		/*String targetName = target.getName();
		
		String copyPath = target.getParent() + "/";
		Matcher nameMatcher = nameRegex.matcher(targetName);
		if (nameMatcher.matches()) {
			Integer copyNo = Integer.valueOf(nameMatcher.group(1)) + 1;
			System.out.println("Capture group: " + nameMatcher.group(1) + " equals: " + copyNo);
			copyPath += targetName.substring(0, nameMatcher.start(1) - 1);
			copyPath += "(" + copyNo + ")";
			copyPath += targetName.substring(nameMatcher.end(1) + 1);
		} else {
			int endIndex = targetName.lastIndexOf(".");
			if (endIndex == -1) {
				copyPath += targetName + " (1)";
			} else {
				copyPath += targetName.substring(0, endIndex);
				copyPath += " (1)";
				copyPath += targetName.substring(endIndex);
			}
		}
		return new NimbusFile(copyPath);*/
	}

	@Override
	public InputStream getZipComressedInputStream(List<NimbusFile> contents) {
		return new ZipCompress(contents).getInputStream();
	}
}
