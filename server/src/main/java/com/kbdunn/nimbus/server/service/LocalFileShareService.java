package com.kbdunn.nimbus.server.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.Random;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.exception.ShareBlockNameConflictException;
import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.ShareBlockAccess;
import com.kbdunn.nimbus.common.model.ShareBlockFile;
import com.kbdunn.nimbus.common.model.ShareBlockRecipient;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.FileContainerService;
import com.kbdunn.nimbus.common.server.FileShareService;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.dao.ShareDAO;
import com.kbdunn.nimbus.server.dao.StorageDAO;

public class LocalFileShareService implements FileContainerService<ShareBlock>, FileShareService {
	
	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(LocalFileShareService.class.getName());
	protected static final int TOKEN_LENGTH = 10;
	
	private static final String SHARE_UPLOAD_DIRNAME = "Share Block Temp";
	private final Random RANDOM = new SecureRandom();
	private LocalUserService userService;
	private LocalFileService fileService;
	private LocalStorageService storageService;
	
	public LocalFileShareService() {  }
	
	public void initialize(NimbusContext container) {
		userService = container.getUserService();
		fileService = container.getFileService();
		storageService = container.getStorageService();
	}
	
	/*
	 * 
	 * SHARE BLOCKS
	 * 
	 */
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getShareBlockById(long)
	 */
	@Override
	public ShareBlock getShareBlockById(long id) {
		return ShareDAO.getShareBlockById(id);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getShareBlockByToken(java.lang.String)
	 */
	@Override
	public ShareBlock getShareBlockByToken(String token) {
		if (token == null || token.isEmpty()) throw new IllegalArgumentException("Token cannot be null or empty");
		return ShareDAO.getShareByToken(token);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getShareBlocks(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public List<ShareBlock> getShareBlocks(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return ShareDAO.getShareBlocksForUser(user.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getAccessibleShareBlocks(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public List<ShareBlock> getAccessibleShareBlocks(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return ShareDAO.getBlocksSharedWithUser(user.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#shareBlockNameExistsForUser(com.kbdunn.nimbus.common.bean.NimbusUser, java.lang.String, com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public boolean shareBlockNameExistsForUser(NimbusUser user, String name, ShareBlock ignoredShareBlock) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		if (ignoredShareBlock != null && ignoredShareBlock.getId() == null) throw new NullPointerException("Ignored ShareBlock ID cannot be null");
		Long ignore = ignoredShareBlock == null ? null : ignoredShareBlock.getId();
		return ShareDAO.shareNameExists(user.getId(), name, ignore);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#shareBlockNameExistsForUser(com.kbdunn.nimbus.common.bean.ShareBlock, java.lang.String)
	 */
	@Override
	public boolean shareBlockNameExistsForUser(ShareBlock ignoredShareBlock, String name) {
		if (ignoredShareBlock.getUserId() == null) throw new NullPointerException("ShareBlock User ID cannot be null");
		return ShareDAO.shareNameExists(ignoredShareBlock.getUserId(), name, ignoredShareBlock.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#save(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public boolean save(ShareBlock shareBlock) throws ShareBlockNameConflictException {
		if (shareBlock.getId() == null) return insertShareBlock(shareBlock);
		else return updateShareBlock(shareBlock);
	}
	
	private boolean insertShareBlock(ShareBlock shareBlock) {
		shareBlock.setToken(generateToken());
		if (!ShareDAO.insertShareBlock(shareBlock)) return false;
		ShareBlock dbo = ShareDAO.getShareByToken(shareBlock.getToken());
		shareBlock.setId(dbo.getId());
		shareBlock.setCreated(dbo.getCreated());
		shareBlock.setUpdated(dbo.getUpdated());
		return true;
	}
	
	private boolean updateShareBlock(ShareBlock shareBlock) throws ShareBlockNameConflictException {
		NimbusUser owner = userService.getUserById(shareBlock.getUserId());
		if (shareBlockNameExistsForUser(owner, shareBlock.getName(), shareBlock))
			throw new ShareBlockNameConflictException(shareBlock.getName());

		ShareBlock old = ShareDAO.getShareBlockById(shareBlock.getId());
		if (!shareBlock.getName().equals(old.getName())) {
			for (NimbusFile wrk : getAllShareBlockWorkingFolders(old)) {
				try {
					fileService.renameFile(wrk, shareBlock.getName());
				} catch (FileConflictException e) {
					// Folder could already exist, doesn't matter if it does
				}
			}
		}
		
		if (!ShareDAO.updateShareBlock(shareBlock)) return false;
		shareBlock.setUpdated(new Date()); // close enough
		
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#delete(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public boolean delete(ShareBlock shareBlock) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		return ShareDAO.deleteShareBlock(shareBlock.getId());
	}
	
	private String generateToken() {
		String characters = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789";
		String token = "";
		for (int i = 0; i < TOKEN_LENGTH; i++) {
			int index = (int) (RANDOM.nextDouble() * characters.length());
			token += characters.substring(index, index + 1);
		}
		if (ShareDAO.tokenExists(token)) 
			return generateToken();
		else 
			return token;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#incrementVisitCount(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public void incrementVisitCount(ShareBlock shareBlock) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		ShareDAO.incrementShareBlockVisitCount(shareBlock.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getShareBlockOwner(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public NimbusUser getShareBlockOwner(ShareBlock shareBlock) {
		if (shareBlock.getUserId() == null) throw new NullPointerException("ShareBlock User ID cannot be null");
		return userService.getUserById(shareBlock.getUserId());
	}
	
	/*
	 * 
	 * SHARE BLOCK FILES
	 * 
	 */


	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getShareBlockFiles(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public List<ShareBlockFile> getShareBlockFiles(ShareBlock shareBlock) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		List<ShareBlockFile> result = ShareDAO.getSharedFiles(shareBlock.getId());
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getShareBlockFileByPath(com.kbdunn.nimbus.common.bean.ShareBlock, java.lang.String)
	 */
	@Override
	public ShareBlockFile getShareBlockFileByPath(ShareBlock shareBlock, String path) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		ShareBlockFile dbo = ShareDAO.getSharedFile(shareBlock.getId(), path);
		if (dbo == null) {
			NimbusFile dbf = fileService.getFileByPath(path);
			if (dbf == null) return null;
			else return new ShareBlockFile(null, dbf.getId(), shareBlock.getId(), null, null, null);
		}
		else return dbo;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getShareBlockFile(com.kbdunn.nimbus.common.bean.ShareBlock, com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public ShareBlockFile getShareBlockFile(ShareBlock shareBlock, NimbusFile f) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		if (f.getPath() == null) throw new NullPointerException("NimbusFile path cannot be null");
		return ShareDAO.getSharedFile(shareBlock.getId(), f.getPath());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getShareBlockTempFolder(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public NimbusFile getShareBlockWorkingFolder(ShareBlock shareBlock) {
		if (shareBlock.getUserId() == null) throw new NullPointerException("ShareBlock User ID cannot be null");
		if (shareBlock.getName() == null) throw new NullPointerException("ShareBlock name cannot be null");
		NimbusUser owner = getShareBlockOwner(shareBlock);
		StorageDevice fav = getFavoredStorageDevice(shareBlock, owner);
		NimbusFile tmp = fileService.getFileByPath(userService.getUserHomeFolderPath(owner, fav) + "/" + SHARE_UPLOAD_DIRNAME + "/" + shareBlock.getName());
		if (!fileService.fileExistsOnDisk(tmp)) 
			fileService.createDirectory(tmp);
		else if (!tmp.isDirectory())
			throw new IllegalStateException("The share tmp path is a regular file!");
		return tmp;
	}
	
	private List<NimbusFile> getAllShareBlockWorkingFolders(ShareBlock shareBlock) {
		if (shareBlock.getName() == null) throw new NullPointerException("ShareBlock name cannot be null");
		List<NimbusFile> result = new ArrayList<NimbusFile>();
		NimbusUser owner = getShareBlockOwner(shareBlock);
		NimbusFile home = null;
		NimbusFile wrk = null;
		for (StorageDevice sd : storageService.getStorageDevicesAssignedToUser(owner)) {
			home = userService.getUserHomeFolder(owner, sd);
			wrk = fileService.getFileByPath(home.getPath() + "/" + SHARE_UPLOAD_DIRNAME + "/" + shareBlock.getName(), false);
			if (fileService.fileExistsOnDisk(wrk)) 
				result.add(wrk);
		}
		return result;
	}
	
	// Get Drive that stores the most files for the share
	// If no drive are stored, return a random user drive
	private StorageDevice getFavoredStorageDevice(ShareBlock b, NimbusUser owner) {
		Map<Long, Integer> c = new HashMap<Long, Integer>();
		List<NimbusFile> nfs = getContents(b);
		if (nfs.size() > 0) {
			for (NimbusFile nf : nfs) {
				if (!c.containsKey(nf.getStorageDeviceId())) {
					c.put(nf.getStorageDeviceId(), 1);
				} else { 
					c.put(nf.getStorageDeviceId(), c.get(nf.getStorageDeviceId()) + 1);
				}
			}
			Entry<Long, Integer> max = null;
			for (Entry<Long, Integer> entry : c.entrySet()) {
				if (max == null || entry.getValue() > max.getValue()) max = entry;
			}
			return StorageDAO.getById(max.getKey());
		} else {
			List<StorageDevice> userDevices = storageService.getStorageDevicesAssignedToUser(owner);
			if (userDevices.size() > 0) return userDevices.get(0);
			throw new IllegalStateException("User is not assigned to any storage devices");
		}
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getShareBlockRecycleBin(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	/*@Override
	public NimbusFile getShareBlockRecycleBin(ShareBlock shareBlock) {
		if (shareBlock.getUserId() == null) throw new NullPointerException("ShareBlock User ID cannot be null");
		NimbusFile del = fileService.getFileByPath(getShareBlockTempFolder(shareBlock).getPath() + "/" + RECYCLE_BIN_DIRNAME);
		if (!fileService.fileExistsOnDisk(del)) 
			fileService.createDirectory(del);
		else if (!del.isDirectory())
			throw new IllegalArgumentException("The share deleted path is a regular file!");
		return del;
	}*/
	
	
	/*private List<NimbusFile> getShareBlockTempNimbusFiles(ShareBlock shareBlock) {
		List<NimbusFile> tmpContents = new ArrayList<NimbusFile>();
		for (NimbusFile nf : getAllShareTempFolders(shareBlock)) {
			for (NimbusFile tmp : fileService.getContents(nf)) {
				if (tmp.getName().equals(RECYCLE_BIN_DIRNAME)) continue;
				tmpContents.add(tmp);
			}
		}
		return tmpContents;
	}
	
	private List<ShareBlockFile> getShareBlockTempFiles(ShareBlock shareBlock) {
		List<ShareBlockFile> result = new ArrayList<ShareBlockFile>();
		for (NimbusFile nf : getShareBlockTempNimbusFiles(shareBlock))
			result.add(new ShareBlockFile(null, nf.getId(), shareBlock.getId(), null, null, null));
		return result;
	}*/
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getContents(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public List<NimbusFile> getContents(ShareBlock container) {
		if (container.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		List<NimbusFile> result = ShareDAO.getSharedNimbusFiles(container.getId());

		return result;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getContents(com.kbdunn.nimbus.common.bean.ShareBlock, int, int)
	 */
	@Override
	public List<NimbusFile> getContents(ShareBlock container, int startIndex, int count) {
		throw new UnsupportedOperationException(); // TODO?
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getFolderContents(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public List<NimbusFile> getFolderContents(ShareBlock container) {
		List<NimbusFile> contents = new ArrayList<NimbusFile>();
		for (NimbusFile nf : getContents(container)) {
			if (nf.isDirectory())
				contents.add(nf);
		}
		return contents;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getFileContents(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public List<NimbusFile> getFileContents(ShareBlock container) {
		List<NimbusFile> contents = new ArrayList<NimbusFile>();
		for (NimbusFile nf : getContents(container)) {
			if (!nf.isDirectory()) 
				contents.add(nf);
		}
		return contents;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getImageContents(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public List<NimbusFile> getImageContents(ShareBlock container) {
		List<NimbusFile> contents = new ArrayList<NimbusFile>();
		for (NimbusFile nf : getContents(container)) {
			if (nf.isImage()) 
				contents.add(nf);
		}
		return contents;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getRelativePath(com.kbdunn.nimbus.common.bean.ShareBlock, com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public String getRelativePath(ShareBlock shareBlock, NimbusFile relativeFile) {
		for (NimbusFile nf : getContents(shareBlock)) {
			if (relativeFile.getPath().equals(nf.getPath())) {
				return relativeFile.getName();
			} else if (fileService.fileIsChildOf(relativeFile, nf)) {
				return nf.getName() + "/" + relativeFile.getPath().replace(nf.getPath() + "/", "");
			}
		}
		throw new IllegalArgumentException("The file is not contained within this share block. A relative path could not be built.");
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#resolveRelativePath(com.kbdunn.nimbus.common.bean.ShareBlock, java.lang.String)
	 */
	@Override
	public NimbusFile resolveRelativePath(ShareBlock container, String relativePath) {
		if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
		String rootFileName = null;
		String rootFolderRelativePath = null;
		
		if (relativePath.contains("/")) {
			rootFileName = relativePath.substring(0, relativePath.indexOf("/"));
			rootFolderRelativePath = relativePath.substring(rootFileName.length() + 1);
		} else {
			rootFileName = relativePath;
		}
		
		for (NimbusFile nf : getContents(container)) {
			if (nf.getName().equals(rootFileName)) {
				if (rootFolderRelativePath == null) return nf;
				else if (nf.isDirectory()) return fileService.resolveRelativePath(nf, rootFolderRelativePath);
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getRecursiveContentSize(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public long getRecursiveContentSize(ShareBlock container) {
		long size = 0;
		for (NimbusFile nf : getContents(container)) {
			if (nf.isDirectory())
				size += fileService.getRecursiveContentSize(nf);
			else
				size += nf.getSize();
		}
		return size;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getContentCount(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public int getContentCount(ShareBlock container) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getRecursiveContentCount(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public int getRecursiveContentCount(ShareBlock container) {
		int files = 0;
		for (NimbusFile nf : getContents(container)) {
			if (nf.isDirectory())
				files += fileService.getRecursiveContentCount(nf);
			else
				files++;
		}
		return files;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getFolderCount(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public int getFolderCount(ShareBlock shareBlock) {
		int i = 0;
		for (NimbusFile nf : getContents(shareBlock)) {
			if (nf.isDirectory()) i++;
		}
		return i;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getFileCount(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public int getFileCount(ShareBlock shareBlock) {
		int i = 0;
		for (NimbusFile nf : getContents(shareBlock)) {
			if (!nf.isDirectory()) {
				i++;
			} else {
				i += fileService.getRecursiveContentCount(nf);
			}
		}
		return i;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#saveShareBlockNimbusFiles(com.kbdunn.nimbus.common.bean.ShareBlock, java.util.List)
	 */
	@Override
	public boolean saveShareBlockNimbusFiles(ShareBlock shareBlock, List<NimbusFile> files) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		List<ShareBlockFile> sbfs = new ArrayList<ShareBlockFile>();
		for (NimbusFile nf : files) {
			ShareBlockFile sbf = getShareBlockFile(shareBlock, nf);
			if (sbf != null) sbfs.add(sbf);
			else sbfs.add(new ShareBlockFile(nf.getId(), shareBlock.getId(), null));
		}
		return saveShareBlockFiles(shareBlock, sbfs);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#saveShareBlockFiles(com.kbdunn.nimbus.common.bean.ShareBlock, java.util.List)
	 */
	@Override
	public boolean saveShareBlockFiles(ShareBlock shareBlock, List<ShareBlockFile> files) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		return ShareDAO.updateShareBlockFileContents(shareBlock.getId(), files);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#removeSharedFile(com.kbdunn.nimbus.common.bean.ShareBlockFile)
	 */
	@Override
	public boolean removeSharedFile(ShareBlockFile file) {
		if (file.getId() == null) throw new NullPointerException("ShareBlockFile ID cannot be null");
		return ShareDAO.deleteShareBlockFile(file.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#addFileToShareBlock(com.kbdunn.nimbus.common.bean.ShareBlock, com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public boolean addFileToShareBlock(ShareBlock shareBlock, NimbusFile file) {
		return addFileToShareBlock(shareBlock, file, null);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#addFileToShareBlock(com.kbdunn.nimbus.common.bean.ShareBlock, com.kbdunn.nimbus.common.bean.NimbusFile, java.lang.String)
	 */
	@Override
	public boolean addFileToShareBlock(ShareBlock shareBlock, NimbusFile file, String note) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		if (file.getId() == null) throw new NullPointerException("File ID cannot be null");
		if (getShareBlockFile(shareBlock, file) != null) return true;
		return ShareDAO.insertShareBlockFile(new ShareBlockFile(null, file.getId(), shareBlock.getId(), note, null, null));
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#removeFileFromShareBlock(com.kbdunn.nimbus.common.bean.ShareBlock, com.kbdunn.nimbus.common.bean.NimbusFile)
	 */
	@Override
	public boolean removeFileFromShareBlock(ShareBlock shareBlock, NimbusFile file) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		if (file.getId() == null) throw new NullPointerException("File ID cannot be null");
		ShareBlockFile sbf = ShareDAO.getSharedFile(shareBlock.getId(), file.getPath());
		if (sbf == null) return true; // doesn't exist
		return ShareDAO.deleteShareBlockFile(sbf.getId());
	}
	
	/*
	 * 
	 * SHARE BLOCK ACCESS
	 * 
	 */
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getShareBlockAccess(com.kbdunn.nimbus.common.bean.ShareBlock, com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public ShareBlockAccess getShareBlockAccess(ShareBlock shareBlock, NimbusUser user) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		ShareBlockAccess dbo = ShareDAO.getShareBlockAccess(shareBlock.getId(), user.getId());
		if (dbo == null) return new ShareBlockAccess(null, shareBlock.getId(), user.getId(), false, false, false, null, null);
		return dbo;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#saveShareBlockAccess(com.kbdunn.nimbus.common.bean.ShareBlock, java.util.List)
	 */
	@Override
	public boolean saveShareBlockAccess(ShareBlock shareBlock, List<ShareBlockAccess> access) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		return ShareDAO.updateShareBlockAccess(shareBlock.getId(), access);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#save(com.kbdunn.nimbus.common.bean.ShareBlockAccess)
	 */
	@Override
	public boolean save(ShareBlockAccess access) {
		if (access.getId() == null) return insertShareBlockAccess(access);
		else return updateShareBlockAccess(access);
	}
	
	private boolean insertShareBlockAccess(ShareBlockAccess access) {
		if (!ShareDAO.insertShareBlockAccess(access)) return false;
		ShareBlockAccess dbo = ShareDAO.getShareBlockAccess(access.getShareBlockId(), access.getUserId());
		access.setId(dbo.getId());
		access.setCreated(dbo.getCreated());
		access.setUpdated(dbo.getUpdated());
		return true;
	}
	
	private boolean updateShareBlockAccess(ShareBlockAccess access) {
		if (!ShareDAO.updateShareBlockAccess(access)) return false;
		access.setUpdated(new Date());
		return true;
	}
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getShareBlockAccess(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public List<ShareBlockAccess> getShareBlockAccess(ShareBlock shareBlock) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		return ShareDAO.getShareBlockAccess(shareBlock.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getUserAccessToShareBlock(com.kbdunn.nimbus.common.bean.NimbusUser, com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public ShareBlockAccess getUserAccessToShareBlock(NimbusUser user, ShareBlock shareBlock) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		if (user.equals(getShareBlockOwner(shareBlock))) {
			return getOwnerAccess(shareBlock);
		} else {
			return ShareDAO.getShareBlockAccess(shareBlock.getId(), user.getId());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getOwnerAccess(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public ShareBlockAccess getOwnerAccess(ShareBlock shareBlock) {
		return new ShareBlockAccess(null, shareBlock.getId(), shareBlock.getUserId(), 
				true, true, true, shareBlock.getCreated(), shareBlock.getUpdated());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#setAccess(com.kbdunn.nimbus.common.bean.ShareBlock, com.kbdunn.nimbus.common.bean.NimbusUser, boolean, boolean, boolean)
	 */
	@Override
	public boolean setAccess(ShareBlock shareBlock, NimbusUser user, boolean canCreate, boolean canUpdate, boolean canDelete) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		ShareBlockAccess access = ShareDAO.getShareBlockAccess(shareBlock.getId(), user.getId());
		if (access == null) 
			access = new ShareBlockAccess(null, shareBlock.getId(), shareBlock.getUserId(), null, null, null, null, null);
		access.setCanCreate(canCreate);
		access.setCanUpdate(canUpdate);
		access.setCanDelete(canDelete);
		return save(access);
	}
	
	/*
	 * 
	 * SHARE BLOCK RECIPIENTS
	 * 
	 */
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#getRecipients(com.kbdunn.nimbus.common.bean.ShareBlock)
	 */
	@Override
	public List<ShareBlockRecipient> getRecipients(ShareBlock shareBlock) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		return ShareDAO.getShareBlockRecipients(shareBlock.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#setRecipients(com.kbdunn.nimbus.common.bean.ShareBlock, java.util.List)
	 */
	@Override
	public boolean setRecipients(ShareBlock shareBlock, List<ShareBlockRecipient> recipients) {
		if (shareBlock.getId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		return ShareDAO.updateShareBlockRecipients(shareBlock.getId(), recipients);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#addRecipient(com.kbdunn.nimbus.common.bean.ShareBlockRecipient)
	 */
	@Override
	public boolean addRecipient(ShareBlockRecipient recipient) {
		if (recipient.getShareBlockId() == null) throw new NullPointerException("ShareBlock ID cannot be null");
		return ShareDAO.insertShareBlockRecipient(recipient);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalFileShareService#addRecipients(java.util.List)
	 */
	@Override
	public boolean addRecipients(List<ShareBlockRecipient> recipients) {
		return ShareDAO.insertShareBlockRecipients(recipients);
	}
}
