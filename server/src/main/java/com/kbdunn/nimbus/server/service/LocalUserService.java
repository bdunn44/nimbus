package com.kbdunn.nimbus.server.service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.api.network.util.HmacUtil;
import com.kbdunn.nimbus.common.async.EmailTransport;
import com.kbdunn.nimbus.common.exception.EmailConflictException;
import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.exception.UsernameConflictException;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.SMTPSettings;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.security.OAuthAPIService;
import com.kbdunn.nimbus.common.server.UserService;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.async.SMTPTransport;
import com.kbdunn.nimbus.server.dao.NimbusUserDAO;
import com.kbdunn.nimbus.server.security.PasswordHash;

public class LocalUserService implements UserService {
	
	private static final Logger log = LogManager.getLogger(LocalUserService.class.getName());
	private static final String TEMP_FILE_DIR = "temp";
	private static final int TEMP_PASSWORD_LENGTH = 8;
	private static final String TEMP_PASSWORD_CHARS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789";
	
	private LocalFileService fileService;
	private LocalStorageService storageService;
	private LocalOAuthService oAuthService;
	
	public LocalUserService() {  }
	
	public void initialize(NimbusContext container) {
		fileService = container.getFileService();
		storageService = container.getStorageService();
		oAuthService = container.getOAuthService();
	}
	
	@Override
	public  Integer getUserCount() {
		return NimbusUserDAO.getUserCount();
	}
	
	@Override
	public List<NimbusUser> getAllUsers() {
		return NimbusUserDAO.getAll();
	}
	
	@Override
	public NimbusUser getUserById(Long id) {
		if (id == null) throw new IllegalArgumentException("ID cannot be null!");
		return NimbusUserDAO.getById(id);
	}
	
	@Override
	public NimbusUser getUserByNameOrEmail(String name) {
		return NimbusUserDAO.getByDomainKey(name);	
	}
	
	// TODO: Implement all FileContainer methods?
	@Override
	public NimbusFile resolveRelativePath(NimbusUser user, StorageDevice device, String relativePath) {
		if (relativePath == null || relativePath.isEmpty() || relativePath.equals("/"))
			return getUserHomeFolder(user, device);
		return fileService.resolveRelativePath(getUserRootFolder(user, device), relativePath);
	}
	
	@Override
	public SMTPSettings getSmtpSettings(NimbusUser user) {
		return NimbusUserDAO.getSmtpSettings(user);
	}
	
	@Override
	public void updateSmtpSettings(SMTPSettings smtpSettings) {
		if (smtpSettings.getUserId() == null) throw new NullPointerException("User ID cannot be null.");
		if (smtpSettings.getUsername() == null) {
			// Can't be null, set to current email
			smtpSettings.setUsername(getUserById(smtpSettings.getUserId()).getEmail());
		}
		NimbusUserDAO.updateSmtpSettings(smtpSettings);
	}
	
	@Override
	public String getUserRootFolderPath(NimbusUser user, StorageDevice device) {
		if (device.getPath() == null) throw new NullPointerException("Storage device path cannot be null");
		if (user.getName() == null) throw new NullPointerException("User name cannot be null");
		return device.getPath() + "/NimbusUser-" + user.getName();
	}
	
	@Override
	public String getUserHomeFolderPath(NimbusUser user, StorageDevice device) {
		if (user == null) throw new NullPointerException("User cannot be null");
		if (device == null) throw new NullPointerException("Storage device cannot be null");
		return getUserRootFolderPath(user, device) + "/Home";
	}
	
	@Override
	public NimbusFile getUserRootFolder(NimbusUser user, StorageDevice device) {
		if (user == null) throw new NullPointerException("User cannot be null");
		if (device == null) throw new NullPointerException("Storage device cannot be null");
		return fileService.getFileByPath(getUserRootFolderPath(user, device), false);
	}
	
	@Override
	public NimbusFile getUserHomeFolder(NimbusUser user, StorageDevice device) {
		if (user == null) throw new NullPointerException("User cannot be null");
		if (device == null) throw new NullPointerException("Storage device cannot be null");
		return fileService.getFileByPath(getUserHomeFolderPath(user, device), false);
	}
	
	@Override
	public String getTempFolderPath(NimbusUser user, StorageDevice device) {
		return getUserRootFolderPath(user, device) + "/" + TEMP_FILE_DIR;
	}
	
	@Override
	public NimbusFile getTempFolder(NimbusUser user, StorageDevice device) {
		return fileService.getFileByPath(getTempFolderPath(user, device), false);
	}
	
	@Override
	public List<NimbusFile> getAllTempFolders(NimbusUser user) {
		List<NimbusFile> result = new ArrayList<NimbusFile>();
		for (StorageDevice d : storageService.getStorageDevicesAssignedToUser(user)) {
			if (storageService.storageDeviceIsAvailable(d)) {
				NimbusFile nf = getTempFolder(user, d);
				if (fileService.fileExistsOnDisk(nf)) result.add(nf);
			}
		}
		return result;
	}
	
	@Override
	public String getSyncRootFolderPath(NimbusUser user) {
		final StorageDevice rootDevice = storageService.getSyncRootStorageDevice(user);
		if (rootDevice == null) return null;
		return getUserHomeFolderPath(user, rootDevice);
	}
	
	@Override
	public NimbusFile getSyncRootFolder(NimbusUser user) {
		final String path = getSyncRootFolderPath(user);
		if (path == null) return null;
		return fileService.getFileByPath(path);
	}
	
	/*public Boolean userHasDuplicateNaturalKey(NimbusUser user, boolean ignoreSelf) {
		return userHasDuplicateEmail(user, ignoreSelf) || userHasDuplicateName(user, ignoreSelf);
	}*/
	
	@Override
	public Boolean hasDuplicateEmail(NimbusUser user) {
		if (user.getEmail() == null) throw new NullPointerException("User email cannot be null");
		return NimbusUserDAO.isDuplicateEmail(user.getEmail(), user.getId());
	}
	
	@Override
	public Boolean hasDuplicateName(NimbusUser user) {
		if (user.getName() == null) throw new NullPointerException("User name cannot be null");
		return NimbusUserDAO.isDuplicateName(user.getName(), user.getId());
	}
	
	@Override
	public String generateTemporaryPassword() {
		SecureRandom random = new SecureRandom();
		
		String pw = "";
		for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
			int index = (int) (random.nextDouble() * TEMP_PASSWORD_CHARS.length());
			pw += TEMP_PASSWORD_CHARS.substring(index, index + 1);
		}
		return pw;
	}
	
	@Override
	public String getDigestedPassword(String clearTextPassword) {
		String hash = "";
		try {
			hash = PasswordHash.createHash(clearTextPassword);
		} catch (NoSuchAlgorithmException e) {
			log.error(e, e);
		} catch (InvalidKeySpecException e) {
			log.error(e, e);
		}
		return hash;
	}

	@Override
	public boolean validatePassword(String clearTextPassword, String passwordDigest) {
		try {
			return PasswordHash.validatePassword(clearTextPassword, passwordDigest);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			log.error(e, e);
			return false;
		}
	}
	
	@Override
	public void resetApiToken(NimbusUser user) throws UsernameConflictException, EmailConflictException, FileConflictException {
		user.setApiToken(HmacUtil.generateSecretKey());
		save(user);
	}
	
	@Override
	public boolean save(NimbusUser user) throws UsernameConflictException, EmailConflictException, FileConflictException {
		if (user.getName() == null || user.getName().isEmpty() 
				|| user.getEmail() == null || user.getEmail().isEmpty())
			throw new NullPointerException("User's name and email must be set");
		
		if (hasDuplicateName(user)) throw new UsernameConflictException(user.getName());
		else if (hasDuplicateEmail(user)) throw new EmailConflictException(user.getEmail());
		
		if (user.getApiToken() == null) user.setApiToken(HmacUtil.generateSecretKey());
		
		if (user.getId() == null)  return insert(user);
		return update(user);
	}
	
	private boolean insert(NimbusUser user) {
		log.trace("Inserting user in database " + user);
		if (!NimbusUserDAO.insert(user)) return false;
		NimbusUser dbu = NimbusUserDAO.getByDomainKey(user.getName());
		user.setId(dbu.getId());
		user.setCreated(dbu.getCreated());
		user.setUpdated(dbu.getUpdated());
		return true;
	}
	
	private boolean update(NimbusUser user) throws FileConflictException {
		log.trace("Updating user in database " + user);
		user.setPasswordTemporary(false); // Clear temp pw - if it's still really the temp then they can keep it
		
		NimbusUser old = NimbusUserDAO.getById(user.getId());
		
		// Check for name change
		if (!old.getName().equals(user.getName())) {
			log.debug("Renaming user's home folder(s)");
			for (StorageDevice d : storageService.getStorageDevicesAssignedToUser(user)) {
				if (storageService.storageDeviceIsAvailable(d)) {
					NimbusFile oldRoot = getUserRootFolder(old, d);
					if (fileService.fileExistsOnDisk(oldRoot) 
							&& fileService.renameFile(oldRoot, StringUtil.getFileNameFromPath(getUserRootFolderPath(user, d))) != null) {
							return false;
					}
				}
			}
		}
		
		return NimbusUserDAO.update(user);
	}
	
	@Override
	public boolean delete(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		log.trace("Deleting user from database " + user);
		return NimbusUserDAO.delete(user.getId());
	}

	@Override
	public EmailTransport getEmailTransport(NimbusUser user) {
		if (user == null) throw new IllegalArgumentException("User cannot be null");
		if (user.getEmailServiceName() == null) {
			// SMTP Email Transport
			final SMTPSettings smtp = getSmtpSettings(user);
			if (smtp == null || smtp.noAttributesSet()) return null;
			return new SMTPTransport(smtp);
		} else {
			final OAuthAPIService service = oAuthService.getOAuthAPIService(user, user.getEmailServiceName());
			if (!(service instanceof EmailTransport)) throw new IllegalStateException("The OAuth service does not support email");
			return (EmailTransport) service;
		}
	}
}
