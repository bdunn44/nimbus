package com.kbdunn.nimbus.server.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
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
	
	public LocalFileService() {  }
	
	public void initialize(NimbusContext container) {
		mediaLibraryService = container.getMediaLibraryService();
		userService = container.getUserService();
		storageService = container.getStorageService();
	}
	
	/* Used to copy a NimbusFile object */
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getFileCopy(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public NimbusFile getFileCopy(NimbusFile nf) {
		return new NimbusFile(nf.getId(), nf.getUserId(), nf.getStorageDeviceId(), nf.getPath(), nf.isDirectory(), nf.getSize(), nf.isSong(), 
				nf.isVideo(), nf.isImage(), nf.isReconciled(), nf.getLastReconciled(), nf.isLibraryRemoved(), nf.getCreated(), nf.getUpdated());
	}
	
	private void setFileAttributes(NimbusFile nf) {
		log.trace("setFileAttributes() called for " + nf.getPath());
		
		Path p = Paths.get(nf.getPath());
		if (!Files.exists(p, LinkOption.NOFOLLOW_LINKS)) {
			nf.setDirectory(null);
		} else {
		    try {
				nf.setDirectory(Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS));
				if (nf.isDirectory()) nf.setSize(0l);
				else nf.setSize(Files.size(p));
			} catch (IOException e) {
				log.error(e, e);
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
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getFileById(long)
	 */
	@Override
	public NimbusFile getFileById(long id) {
		return NimbusFileDAO.getById(id);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getFileByPath(java.nio.file.Path)
	 */
	@Override
	public NimbusFile getFileByPath(Path fullPath) {
		return getFileByPath(fullPath.toAbsolutePath().toString().replace("\\", "/").replace("C:", ""));
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getFileByPath(java.lang.String)
	 */
	@Override
	public NimbusFile getFileByPath(String fullPath) {
		return getFileByPath(fullPath, false);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getFileByPath(java.lang.String)
	 */
	@Override
	public NimbusFile getFileByPath(String fullPath, boolean reconcile) {
		NimbusFile nf = NimbusFileDAO.getByPath(fullPath);
		if (nf == null) {
			nf = new NimbusFile(null, null, null, fullPath, null, null, null, null, null, null, null, null, null, null);
			setFileAttributes(nf);
		}
		if (reconcile) reconcile(nf);
		return nf;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getTotalFileCount()
	 */
	@Override
	public long getTotalFileCount() {
		return NimbusFileDAO.getTotalFileCount();
	}
	
	// TODO: Move to system service?
	@Override
	public long getTotalFileSize() {
		return NimbusFileDAO.getTotalFileSize();
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getStorageDevice(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public StorageDevice getStorageDevice(NimbusFile file) {
		if (file.getPath() == null) throw new NullPointerException("File path cannot be null");
		if (file.getStorageDeviceId() != null)
			return storageService.getStorageDeviceById(file.getStorageDeviceId());
		String path = file.getPath();
		if (path.indexOf("/NimbusUser-") == -1) throw new IllegalArgumentException("File path must contain '/NimbusUser-'");
		return storageService.getStorageDeviceByPath(path.substring(0, path.indexOf("/NimbusUser-")));
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#fileExistsOnDisk(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public boolean fileExistsOnDisk(NimbusFile file) {
		return Files.exists(Paths.get(file.getPath()));
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getLastModifiedDate(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public Date getLastModifiedDate(NimbusFile file) {
		try {
			if (fileExistsOnDisk(file))
				return new Date(Files.getLastModifiedTime(Paths.get(file.getPath()), LinkOption.NOFOLLOW_LINKS).toMillis());
			else
				return file.getUpdated();
		} catch (IOException e) {
			log.error(e, e);
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getFileSizeString(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
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
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getRecursiveContentSize(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public long getRecursiveContentSize(NimbusFile file) {
		if (!file.isDirectory()) return file.getSize();
		return NimbusFileDAO.getRecursiveFileSize(file);
	}
	
	// Get the parent NimbusFile
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getParentFile(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public NimbusFile getParentFile(NimbusFile file) {
		return getFileByPath(Paths.get(file.getPath()).getParent());
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getContentCount(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public int getContentCount(NimbusFile folder) {
		if (!folder.isDirectory()) return 0;
		if (!folder.isReconciled() || getStorageDevice(folder).isAutonomous()) reconcileFolder(folder);
		return NimbusFileDAO.getContentCount(folder);
	}
	
	// Does not account for reconciliation. It's a best guess
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getRecursiveContentCount(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public int getRecursiveContentCount(NimbusFile folder) {
		if (!folder.isDirectory()) return 0;
		return NimbusFileDAO.getRecursiveChildCount(folder);
	}
	
	// Check if folder has any children in the least expensive way
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#folderHasContents(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
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
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#folderContentsContainsFolder(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
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
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#fileIsChildOf(com.kbdunn.nimbus.common.bean.NimbusFile, com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
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
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getContents(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public List<NimbusFile> getContents(NimbusFile folder) {
		log.debug("Getting all contents of " + folder);
		if (!folder.isDirectory()) throw new IllegalArgumentException("Can't get contents of a regular file");
		if (!folder.isReconciled() || getStorageDevice(folder).isAutonomous()) reconcileFolder(folder);
		return NimbusFileDAO.getContents(folder);
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getContents(com.kbdunn.nimbus.common.bean.NimbusFile, int, int)
	 */
	@Override
	public List<NimbusFile> getContents(NimbusFile folder, int startIndex, int count) {
		log.debug("Getting top " + count + " contents of " + folder + " from start index " + startIndex);
		if (!folder.isDirectory()) throw new IllegalArgumentException("Can't get contents of a regular file");
		if (!folder.isReconciled() || getStorageDevice(folder).isAutonomous()) reconcileFolder(folder);
		return NimbusFileDAO.getContents(folder, startIndex, count);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getFileContents(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public List<NimbusFile> getFileContents(NimbusFile folder) {
		log.debug("Getting file contents of " + folder);
		if (!folder.isDirectory()) throw new IllegalArgumentException("Can't get contents of a regular file");
		if (!folder.isReconciled() || getStorageDevice(folder).isAutonomous()) reconcileFolder(folder);
		return NimbusFileDAO.getFileContents(folder);
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getFolderContents(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public List<NimbusFile> getFolderContents(NimbusFile folder) {
		log.debug("Getting folder contents of " + folder);
		if (!folder.isDirectory()) throw new IllegalArgumentException("Can't get contents of a regular file");
		if (!folder.isReconciled() || getStorageDevice(folder).isAutonomous()) reconcileFolder(folder);
		return NimbusFileDAO.getFolderContents(folder);
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getImageContents(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
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
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getRecursiveFolderContents(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
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
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#resolveRelativePath(com.kbdunn.nimbus.common.bean.NimbusFile, java.lang.String)
	 */
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
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#getRelativePath(com.kbdunn.nimbus.common.bean.NimbusFile, com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public String getRelativePath(NimbusFile root, NimbusFile file) {
		if (fileIsChildOf(file, root))
			return file.getPath().substring(root.getPath().length() + 1);
		else
			return null;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#save(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public boolean save(NimbusFile file) {
		if (file.getUserId() == null) throw new NullPointerException("User ID cannot be null");
		if (file.getStorageDeviceId() == null) throw new NullPointerException("Drive ID cannot be null");
		if (file instanceof Song) return mediaLibraryService.save((Song) file);
		if (file.isSong()) return mediaLibraryService.save(new Song(file));
		if (file.getId() == null) return insert(file);
		return update(file);
	}
	
	// Create a record in the database
	boolean insert(NimbusFile file) {
		log.trace("Inserting file in database " + file);
		if (!NimbusFileDAO.insert(file)) return false;
		NimbusFile dbf = NimbusFileDAO.getByPath(file.getPath());
		file.setId(dbf.getId());
		file.setCreated(dbf.getCreated());
		file.setUpdated(dbf.getUpdated());
		return true;
	}
	
	// Update a record in the database
	boolean update(NimbusFile file) {
		log.trace("Updating file in database " + file);
		if (!NimbusFileDAO.update(file)) return false;
		file.setUpdated(new Date()); // close enough
		return true;
	}
	
	// Delete file or folder. Folder delete is recursive
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#delete(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public boolean delete(NimbusFile file) {		
		if (file.isDirectory()) {
			for (NimbusFile child: getContents(file)) 
				if (!delete(child)) return false;
		}
		try {
			Files.delete(Paths.get(file.getPath()));
			return NimbusFileDAO.delete(file);
		} catch (IOException e) {
			log.error(e, e);
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#createDirectory(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public boolean createDirectory(NimbusFile folder) {
		if (Files.exists(Paths.get(folder.getPath()))) return true;
		if (!fileExistsOnDisk(getParentFile(folder))) {
			if (!createDirectory(getParentFile(folder))) 
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
		return save(folder);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#touchFile(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public boolean touchFile(NimbusFile file) {
		try {
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
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#renameFile(com.kbdunn.nimbus.common.bean.NimbusFile, java.lang.String)
	 */
	@Override
	public boolean renameFile(NimbusFile file, String newName) throws FileConflictException {
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
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#fileMoveDestinationIsValid(com.kbdunn.nimbus.common.bean.NimbusFile, com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public boolean fileMoveDestinationIsValid(NimbusFile sourceFile, NimbusFile destinationFolder) {
		return !sourceFile.equals(destinationFolder) && !fileIsChildOf(destinationFolder, sourceFile);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#moveFileTo(com.kbdunn.nimbus.common.bean.NimbusFile, com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public boolean moveFileTo(NimbusFile file, NimbusFile targetFolder) throws FileConflictException {
		return moveFile(file, targetFolder.getPath() + "/" + file.getName());
	}
	
	// Move a file or folder
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#moveFile(com.kbdunn.nimbus.common.bean.NimbusFile, java.lang.String)
	 */
	@Override
	public boolean moveFile(NimbusFile file, String fullPath) throws FileConflictException {
		NimbusFile target = getFileByPath(fullPath);
		
		if (fileExistsOnDisk(target))
			throw new FileConflictException(new FileConflict(file, target));
		
		log.debug("Moving file " + file + " to " + target);
		
		try {
			Files.move(Paths.get(file.getPath()), Paths.get(target.getPath()));
		} catch (IOException e) {
			log.error(e, e);
			return false;
		}
		
		file.setPath(target.getPath());
		return save(file);
	}
	
	// Copies this file to a target folder. Returns the new copy
	// TODO: resolve file path?
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#copyFileTo(com.kbdunn.nimbus.common.bean.NimbusFile, com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public NimbusFile copyFileTo(NimbusFile file, NimbusFile targetFolder) throws FileConflictException, IllegalArgumentException {
		if (!targetFolder.isDirectory() || !fileExistsOnDisk(targetFolder))
			throw new IllegalArgumentException("Cannot copy file to a path which is not a directory or does not exist");
		
		List<FileConflict> conflicts = checkConflicts(file, targetFolder);
		if (!conflicts.isEmpty())
			throw new FileConflictException(conflicts);

		log.debug("Copying file " + file + " to " + targetFolder);
		NimbusFile copy = getFileByPath(targetFolder.getPath() + "/" + file.getName());
		
		if (file.isDirectory()) {
			log.debug("Performing recursive folder copy");
			copy.setDirectory(true);
			if (!fileExistsOnDisk(copy)) createDirectory(copy);
			
			for (NimbusFile child: getContents(file)) 
				copyFileTo(child, copy);
			
		} else {
			try {
				Files.copy(Paths.get(file.getPath()), Paths.get(copy.getPath()), StandardCopyOption.COPY_ATTRIBUTES);
			} catch (IOException e) {
				log.error(e, e);
				return null;
			}
		}
		
		setFileAttributes(copy);
		save(copy);
		return copy;
	}
	
	// FOLDER CONFLICTS ARE NOT CONFLICTS!!
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#checkConflicts(com.kbdunn.nimbus.common.bean.NimbusFile, com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
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
	
	// Recursively copies source files. Conflict resolution map needs to contain resolution for ALL children.
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#batchCopy(java.util.List, com.kbdunn.nimbus.common.bean.NimbusFile, java.util.List)
	 */
	@Override
	public boolean batchCopy(List<NimbusFile> sources, NimbusFile targetFolder, List<FileConflict> conflictResolutions) {
		log.debug("Processing batch copy with conflict resolutions");
		
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
				copyFileTo(f, targetFolder);
			} catch (IllegalArgumentException | FileConflictException e) {
				log.error(e, e);
				return false;
			}
		}
		
		// Process conflicts
		for (FileConflict fc : conflictResolutions) {
			try {
				if (fc.getResolution() == FileConflict.Resolution.COPY) {
					NimbusFile resolution = getFileConflictResolution(fc);
					Files.copy(Paths.get(fc.getSource().getPath()), Paths.get(resolution.getPath()), StandardCopyOption.COPY_ATTRIBUTES);
					reconcile(resolution);
				} else if (fc.getResolution() == FileConflict.Resolution.REPLACE) {
					Files.copy(Paths.get(fc.getSource().getPath()), Paths.get(fc.getTarget().getPath()), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
					reconcile(fc.getTarget());
				}
			} catch (Exception e) {
				log.error(e, e);
				return false;
			}
		}
		
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#reconcileFolder(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public boolean reconcileFolder(NimbusFile folder) {
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
			save(child);
			//if (child.isDirectory() && recursive) reconcileFolder(child, true);
		}
		
		folder.setReconciled(true);
		folder.setLastReconciled(ts);
		return save(folder);
	}
	
	// Synchronizes files that exist on disk with the database
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileService#reconcile(com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public boolean reconcile(NimbusFile nf) {
		log.trace("Reconciling file " + nf);
		
		setFileAttributes(nf);
		nf = checkInstantiation(nf);
		
		if (nf.isDirectory()) {
			return reconcileFolder(nf);
		} else {
			 return save(nf);
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
				+ sourceFirst + "( \\((\\d+)\\))" + sourceExt + "$");
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
