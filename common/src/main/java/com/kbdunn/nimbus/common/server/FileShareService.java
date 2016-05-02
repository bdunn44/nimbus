package com.kbdunn.nimbus.common.server;

import java.util.List;

import com.kbdunn.nimbus.common.exception.ShareBlockNameConflictException;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.ShareBlockAccess;
import com.kbdunn.nimbus.common.model.ShareBlockFile;
import com.kbdunn.nimbus.common.model.ShareBlockRecipient;

public interface FileShareService {

	ShareBlock getShareBlockById(long id);

	ShareBlock getShareBlockByToken(String token);

	List<ShareBlock> getShareBlocks(NimbusUser user);

	List<ShareBlock> getAccessibleShareBlocks(NimbusUser user);

	boolean shareBlockNameExistsForUser(NimbusUser user, String name, ShareBlock ignoredShareBlock);

	boolean shareBlockNameExistsForUser(ShareBlock ignoredShareBlock, String name);

	boolean save(ShareBlock shareBlock) throws ShareBlockNameConflictException;

	boolean delete(ShareBlock shareBlock);

	void incrementVisitCount(ShareBlock shareBlock);

	NimbusUser getShareBlockOwner(ShareBlock shareBlock);

	List<ShareBlockFile> getShareBlockFiles(ShareBlock shareBlock);

	ShareBlockFile getShareBlockFileByPath(ShareBlock shareBlock, String path);

	ShareBlockFile getShareBlockFile(ShareBlock shareBlock, NimbusFile f);

	NimbusFile getShareBlockWorkingFolder(ShareBlock shareBlock);

	//NimbusFile getShareBlockRecycleBin(ShareBlock shareBlock);

	List<NimbusFile> getContents(ShareBlock container);

	List<NimbusFile> getContents(ShareBlock container, int startIndex, int count);

	List<NimbusFile> getFolderContents(ShareBlock container);

	List<NimbusFile> getFileContents(ShareBlock container);

	List<NimbusFile> getImageContents(ShareBlock container);

	String getRelativePath(ShareBlock shareBlock, NimbusFile relativeFile);

	NimbusFile resolveRelativePath(ShareBlock container, String relativePath);

	long getRecursiveContentSize(ShareBlock container);

	int getContentCount(ShareBlock container);

	int getRecursiveContentCount(ShareBlock container);

	int getFolderCount(ShareBlock shareBlock);

	int getFileCount(ShareBlock shareBlock);

	boolean saveShareBlockNimbusFiles(ShareBlock shareBlock, List<NimbusFile> files);

	boolean saveShareBlockFiles(ShareBlock shareBlock, List<ShareBlockFile> files);

	boolean removeSharedFile(ShareBlockFile file);

	boolean addFileToShareBlock(ShareBlock shareBlock, NimbusFile file);

	boolean addFileToShareBlock(ShareBlock shareBlock, NimbusFile file, String note);

	boolean removeFileFromShareBlock(ShareBlock shareBlock, NimbusFile file);

	ShareBlockAccess getShareBlockAccess(ShareBlock shareBlock, NimbusUser user);

	boolean saveShareBlockAccess(ShareBlock shareBlock, List<ShareBlockAccess> access);

	boolean save(ShareBlockAccess access);

	List<ShareBlockAccess> getShareBlockAccess(ShareBlock shareBlock);

	ShareBlockAccess getUserAccessToShareBlock(NimbusUser user, ShareBlock shareBlock);

	ShareBlockAccess getOwnerAccess(ShareBlock shareBlock);

	boolean setAccess(ShareBlock shareBlock, NimbusUser user, boolean canCreate, boolean canUpdate, boolean canDelete);

	List<ShareBlockRecipient> getRecipients(ShareBlock shareBlock);

	boolean setRecipients(ShareBlock shareBlock, List<ShareBlockRecipient> recipients);

	boolean addRecipient(ShareBlockRecipient recipient);

	boolean addRecipients(List<ShareBlockRecipient> recipients);

}