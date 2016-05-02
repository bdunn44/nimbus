package com.kbdunn.nimbus.server.service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import com.kbdunn.nimbus.common.model.Album;
import com.kbdunn.nimbus.common.model.Artist;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.Playlist;
import com.kbdunn.nimbus.common.model.Song;
import com.kbdunn.nimbus.common.model.Video;
import com.kbdunn.nimbus.common.server.MediaLibraryService;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.dao.MediaLibraryDAO;
import com.kbdunn.nimbus.server.dao.NimbusFileDAO;

public class LocalMediaLibraryService implements MediaLibraryService {
	
	private static final Logger log = LogManager.getLogger(LocalMediaLibraryService.class.getName());
	
	private LocalFileService fileService;
	
	public LocalMediaLibraryService() {  }
	
	public void initialize(NimbusContext container) {
		fileService = container.getFileService();
	}
	
	/*
	 * 
	 * Songs
	 * 
	 */
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#setSongAttributes(com.kbdunn.nimbus.common.bean.Song)
	 */
	@Override
	public void setSongAttributes(Song song) {
		log.debug("Setting song attributes for " + song);
		
		// JAudioTagger
		try {
			AudioFile f = AudioFileIO.read(new File(song.getPath()));
			String art = "";
			String alb = "";
			String tit = "";
			String yr = null;
			Integer trk = null;
			Integer len = 0;

			AudioHeader header = f.getAudioHeader();
			Tag tag = f.getTag();
			if (tag != null) {
				art = tag.getFirst(FieldKey.ARTIST);
				alb = tag.getFirst(FieldKey.ALBUM);
				tit = tag.getFirst(FieldKey.TITLE);
				yr = tag.getFirst(FieldKey.YEAR);
				try {
					trk = Integer.parseInt(tag.getFirst(FieldKey.TRACK));
				} catch (NumberFormatException nfe) {  }
			}
			if (header != null) {
				len = header.getTrackLength();
			}
			
			song.setArtist(art);
			song.setAlbum(alb);
			song.setTitle(tit);
			if (song.getTitle() == null || song.getTitle().isEmpty()) song.setTitle(song.getName());
			song.setAlbumYear(yr);
			song.setTrackNumber(trk);
			song.setLength(len);
			//album.addSong(this);
			//artist.addAlbum(album);
		} catch (Exception e) {
			log.warn(e.getMessage());
		} finally {
			
		}
		
		// BeagleBuddy
		/*MP3 mp3 = null;
		try {
			mp3 = new MP3(song.getPath());
			
			if (mp3.getAudioDuration() == 0)       // if the length of the song hasn't been specified,
				mp3.setAudioDuration();             // then calculate it from the mpeg audio frames
		} catch (IOException e) {
			if (!e.getMessage().contains("An ID3v2.2 tag was found in the mp3 file but that version is not currently supported"))
				log.error(e.getMessage());
		} finally {
			String art = null;
			String alb = null;
			String tit = null;
			String yr = null;
			Integer trk = null;
			Integer len = null;
			
			if (mp3 != null) {
				log.debug("audio duration.....: " + mp3.getAudioDuration()                  + " s\n"      +
				"audio size.........: " + mp3.getAudioSize()                      + " bytes\n"  +
				"album..............: " + mp3.getAlbum()                          + "\n"        +
				"artist.............: " + mp3.getBand()                           + "\n"        +
				"contributing artist: " + mp3.getLeadPerformer()                  + "\n"        +
				"lyrics by..........: " + mp3.getLyricsBy()                       + "\n"        +
				"music by...........: " + mp3.getMusicBy()                        + "\n"        +
				"picture............: " + mp3.getPicture(PictureType.FRONT_COVER) + "\n"        +
				"publisher..........: " + mp3.getPublisher()                      + "\n"        +
				"rating.............: " + mp3.getRating()                         + "\n"        +
				"title..............: " + mp3.getTitle()                          + "\n"        +
				"track #............: " + mp3.getTrack()                          + "\n"        +
				"year recorded......: " + mp3.getYear()                           + "\n"        +
				"lyrics.............: " + mp3.getLyrics()                         + "\n");
				art = mp3.getBand();
				alb = mp3.getAlbum();
				tit = mp3.getTitle();
				yr = String.valueOf(mp3.getYear());
				trk = mp3.getTrack();
				len = mp3.getAudioDuration();
			}
			
			song.setArtist(art);
			song.setAlbum(alb);
			song.setTitle(tit);
			song.setAlbumYear(yr);
			song.setTrackNumber(trk);
			song.setLength(len);
		}*/
	}
	
	private void saveId3(Song song) {
		// BeagleBuddy
		/*MP3 mp3 = null;
		try {
			mp3 = new MP3(song.getPath());
		} catch (IOException e) {
			if (!e.getMessage().contains("An ID3v2.2 tag was found in the mp3 file but that version is not currently supported"))
				log.error(e.getMessage());
		} finally {
			if (mp3 != null) {
				if (song.getArtist() != null && !song.getArtist().equals(Artist.UNKNOWN)) 
					mp3.setBand(song.getArtist());
				if (song.getAlbum() != null && !song.getAlbum().equals(Album.UNKNOWN)) 
					mp3.setAlbum(song.getAlbum());
				if (song.getTitle() != null && !song.getTitle().equals(song.getName())) 
					mp3.setTitle(song.getTitle());
				if (song.getTrackNumber() != null)
					mp3.setTrack(song.getTrackNumber());
				try {
					Integer y = Integer.valueOf(song.getAlbumYear());
					if (y != null && !y.equals(0)) mp3.setYear(y);
				} catch (NumberFormatException e) {
					// Ignore
				}
				try {
					mp3.save();
				} catch (IllegalStateException | IOException e) {
					log.error(e, e);
				}
				//mp3.setMusicType(Genre.HARD_ROCK);
				//mp3.setAudioDuration(310);
				//mp3.setRating(240);
				//mp3.setLyrics("I'm rolling thunder\npouring rain.\nI'm coming on like a hurricane\n...");
				//mp3.setPicture(PictureType.FRONT_COVER, new File("c:/images/ac_dc/back_in_black.jpg"));
			}
		}*/
		
		// JAudioTagger
		try {
			AudioFile f = AudioFileIO.read(new File(song.getPath()));
			Tag tag = f.getTag();
			tag.setField(FieldKey.ARTIST, song.getArtist());
			tag.setField(FieldKey.ALBUM, song.getAlbum());
			tag.setField(FieldKey.TITLE, song.getTitle());
			tag.setField(FieldKey.TRACK, String.valueOf(song.getTrackNumber()));
			tag.setField(FieldKey.YEAR, song.getAlbumYear());
			f.commit();
		} catch (CannotReadException | IOException | TagException
				| ReadOnlyFileException | InvalidAudioFrameException | CannotWriteException e) {
			log.warn(e.getMessage());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#save(com.kbdunn.nimbus.common.bean.Song)
	 */
	@Override
	public boolean save(Song song) {
		if (song.getUserId() == null) throw new NullPointerException("User ID cannot be null");
		if (song.getStorageDeviceId() == null) throw new NullPointerException("Drive ID cannot be null");
		saveId3(song); // Don't stop if this fails
		if (song.getId() == null) {
			if (!fileService.insert(song)) return false;
			Song dbs = (Song) NimbusFileDAO.getByPath(song.getPath());
			song.setId(dbs.getId());
			song.setCreated(dbs.getCreated());
			song.setUpdated(dbs.getUpdated());
		} else {
			if (!fileService.update(song)) return false;
			song.setUpdated(new Date()); // close enough
		}
		
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getSongs(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public List<Song> getSongs(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserSongs(user.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getSongs(com.kbdunn.nimbus.common.bean.NimbusUser, int, int)
	 */
	@Override
	public List<Song> getSongs(NimbusUser user, int startIndex, int count) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserSongs(user.getId(), startIndex, count);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getSongCount(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public int getSongCount(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserSongCount(user.getId());
	}
	
	/*
	 * 
	 * Videos
	 * 
	 */
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#setVideoAttributes(com.kbdunn.nimbus.common.bean.Video)
	 */
	@Override
	public void setVideoAttributes(Video video) {
		// Would be nice to set length...
		if (video.getTitle() == null || video.getTitle().isEmpty()) { // always null right now
			video.setTitle(video.getName());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getVideos(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public List<Video> getVideos(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserVideos(user.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getVideos(com.kbdunn.nimbus.common.bean.NimbusUser, int, int)
	 */
	@Override
	public List<Video> getVideos(NimbusUser user, int startIndex, int count) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserVideos(user.getId(), startIndex, count);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getVideoCount(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public int getVideoCount(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserVideoCount(user.getId());
	}
	
	/*
	 * 
	 * Playlists
	 * 
	 */
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getPlaylists(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public List<Playlist> getPlaylists(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserPlaylists(user.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getPlaylists(com.kbdunn.nimbus.common.bean.NimbusUser, int, int)
	 */
	@Override
	public List<Playlist> getPlaylists(NimbusUser user, int startIndex, int count) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserPlaylists(user.getId(), startIndex, count);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getPlaylistCount(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public int getPlaylistCount(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserPlaylistCount(user.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getPlaylistByName(com.kbdunn.nimbus.common.bean.NimbusUser, java.lang.String)
	 */
	@Override
	public Playlist getPlaylistByName(NimbusUser owner, String name) {
		if (owner.getId() == null) throw new NullPointerException("Owner ID cannot be null");
		if (name == null) throw new NullPointerException("Playlist name cannot be null");
		return MediaLibraryDAO.getPlaylistByName(owner.getId(), name);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#save(com.kbdunn.nimbus.common.bean.Playlist)
	 */
	@Override
	public boolean save(Playlist playlist) {
		if (playlist.getId() == null) return insert(playlist);
		else return update(playlist);
	}
	
	private boolean insert(Playlist playlist) {
		if (!MediaLibraryDAO.insertPlaylist(playlist)) return false;
		Playlist dbp = MediaLibraryDAO.getPlaylistByName(playlist.getUserId(), playlist.getName());
		playlist.setId(dbp.getId());
		playlist.setCreated(dbp.getCreated());
		playlist.setUpdated(dbp.getUpdated());
		return true;
	}
	
	private boolean update(Playlist playlist) {
		if (!MediaLibraryDAO.updatePlaylist(playlist)) return false;
		playlist.setUpdated(new Date()); // close enough
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#delete(com.kbdunn.nimbus.common.bean.Playlist)
	 */
	@Override
	public boolean delete(Playlist playlist) {
		if (playlist.getId() == null) throw new NullPointerException("Playlist ID cannot be null");
		return MediaLibraryDAO.deletePlaylist(playlist.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#setPlaylistSongs(com.kbdunn.nimbus.common.bean.Playlist, java.util.List)
	 */
	@Override
	public boolean setPlaylistSongs(Playlist playlist, List<Song> songs) {
		if (playlist.getId() == null) throw new NullPointerException("Playlist ID cannot be null");
		if (!MediaLibraryDAO.deletePlaylistSongs(playlist.getId())) return false;
		return MediaLibraryDAO.insertPlaylistSongs(playlist.getId(), songs);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getPlaylistSongs(com.kbdunn.nimbus.common.bean.Playlist)
	 */
	@Override
	public List<Song> getPlaylistSongs(Playlist playlist) {
		if (playlist.getId() == null) throw new NullPointerException("Playlist ID cannot be null");
		return MediaLibraryDAO.getPlaylistSongs(playlist.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getPlaylistSongs(com.kbdunn.nimbus.common.bean.Playlist, int, int)
	 */
	@Override
	public List<Song> getPlaylistSongs(Playlist playlist, int startIndex, int count) {
		if (playlist.getId() == null) throw new NullPointerException("Playlist ID cannot be null");
		return MediaLibraryDAO.getPlaylistSongs(playlist.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getPlaylistSongCount(com.kbdunn.nimbus.common.bean.Playlist)
	 */
	@Override
	public int getPlaylistSongCount(Playlist playlist) {
		if (playlist.getId() == null) throw new NullPointerException("Playlist ID cannot be null");
		return MediaLibraryDAO.getPlaylistSongCount(playlist.getId());
	}
	
	/*
	 * 
	 * Albums
	 * 
	 */
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getAlbumByName(com.kbdunn.nimbus.common.bean.NimbusUser, java.lang.String, java.lang.String)
	 */
	@Override
	public Album getAlbumByName(NimbusUser user, String artistName, String albumName) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		if (artistName == null) throw new NullPointerException("Artist name cannot be null");
		if (albumName == null) throw new NullPointerException("Album name cannot be null");
		return MediaLibraryDAO.getAlbumByName(user.getId(), artistName, albumName);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getAlbums(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public List<Album> getAlbums(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserAlbums(user.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getAlbums(com.kbdunn.nimbus.common.bean.NimbusUser, int, int)
	 */
	@Override
	public List<Album> getAlbums(NimbusUser user, int startIndex, int count) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserAlbums(user.getId(), startIndex, count);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getAlbumCount(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public int getAlbumCount(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserAlbumCount(user.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getAlbumSongs(com.kbdunn.nimbus.common.bean.NimbusUser, com.kbdunn.nimbus.common.bean.Album)
	 */
	@Override
	public List<Song> getAlbumSongs(NimbusUser owner, Album album) {
		if (owner.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getAlbumSongs(owner.getId(), album);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getAlbumSongs(com.kbdunn.nimbus.common.bean.NimbusUser, com.kbdunn.nimbus.common.bean.Album, int, int)
	 */
	@Override
	public List<Song> getAlbumSongs(NimbusUser owner, Album album, int startIndex, int count) {
		if (owner.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getAlbumSongs(owner.getId(), album, startIndex, count);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getAlbumSongCount(com.kbdunn.nimbus.common.bean.NimbusUser, com.kbdunn.nimbus.common.bean.Album)
	 */
	@Override
	public int getAlbumSongCount(NimbusUser owner, Album album) {
		if (owner.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getAlbumSongCount(owner.getId(), album);
	}
	
	/*
	 * 
	 * Artists
	 * 
	 */
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getArtistByName(com.kbdunn.nimbus.common.bean.NimbusUser, java.lang.String)
	 */
	@Override
	public Artist getArtistByName(NimbusUser owner, String name) {
		if (owner.getId() == null) throw new NullPointerException("Owner ID cannot be null");
		if (name == null) throw new NullPointerException("Artist name cannot be null");
		return MediaLibraryDAO.getArtistByName(owner.getId(), name);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getArtists(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public List<Artist> getArtists(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserArtists(user.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getArtists(com.kbdunn.nimbus.common.bean.NimbusUser, int, int)
	 */
	@Override
	public List<Artist> getArtists(NimbusUser user, int startIndex, int count) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserArtists(user.getId(), startIndex, count);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getArtistCount(com.kbdunn.nimbus.common.bean.NimbusUser)
	 */
	@Override
	public int getArtistCount(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getUserArtistCount(user.getId());
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getArtistAlbums(com.kbdunn.nimbus.common.bean.NimbusUser, com.kbdunn.nimbus.common.bean.Artist)
	 */
	@Override
	public List<Album> getArtistAlbums(NimbusUser owner, Artist artist) {
		if (owner.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getArtistAlbums(owner.getId(), artist);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getArtistSongs(com.kbdunn.nimbus.common.bean.NimbusUser, com.kbdunn.nimbus.common.bean.Artist)
	 */
	@Override
	public List<Song> getArtistSongs(NimbusUser owner, Artist artist) {
		if (owner.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getArtistSongs(owner.getId(), artist);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getArtistSongs(com.kbdunn.nimbus.common.bean.NimbusUser, com.kbdunn.nimbus.common.bean.Artist, int, int)
	 */
	@Override
	public List<Song> getArtistSongs(NimbusUser owner, Artist artist, int startIndex, int count) {
		if (owner.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getArtistSongs(owner.getId(), artist, startIndex, count);
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.core.service.LocalMediaLibraryService#getArtistSongCount(com.kbdunn.nimbus.common.bean.NimbusUser, com.kbdunn.nimbus.common.bean.Artist)
	 */
	@Override
	public int getArtistSongCount(NimbusUser owner, Artist artist) {
		if (owner.getId() == null) throw new NullPointerException("User ID cannot be null");
		return MediaLibraryDAO.getArtistSongCount(owner.getId(), artist);
	}
	
	// Builds a list of Artists based on files stored in the database
	// Artist objects will contain all child albums and song contents
	/*public static List<Artist> derive(NimbusUser owner) {
		log.info("Deriving Artist and Album list from file metadata...");
		List<Artist> artists = new ArrayList<Artist>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HSQLConnectionPool.getCurrentConnection();
			ps = con.prepareStatement(
					"SELECT ID, N_USER_ID, ARTIST, ALBUM, TRACK_NO "
					+ "FROM N_FILE WHERE IS_SONG "
					+ (owner == null ? "" : "AND N_USER_ID = ? ")
					+ "AND EXISTS (SELECT 0 FROM N_DRIVE d WHERE N_FILE.N_DRIVE_ID = d.ID AND d.CONNECTED AND d.MOUNTED)"
					+ "ORDER BY 2, 3, 4, 5;");
			if (owner != null)
				ps.setLong(1, owner.getId());
			
			rs = ps.executeQuery();
			
			Artist artist = null;
			Album album = null;
			Song song = null;
			
			// Loop through songs
			while (rs.next()) {
				
				// Aggregate Artists
				if (artist == null 
						|| !artist.getUserId().equals(rs.getLong("N_USER_ID")) 
						|| !artist.getName().equals(rs.getString("ARTIST"))) {
					
					if (owner == null) owner = NimbusUserDAO.getById(rs.getLong("N_USER_ID"));
					
					// Get from DB
					artist = MediaLibraryDAO.getArtistByName(rs.getString("ARTIST"), owner.getId());
					// If not in DB, create
					if (artist == null) {
						artist = new Artist(null, owner.getId(), rs.getString("ARTIST"), null, null);
					}
					
					// Add to list
					log.debug("Adding Artist " + artist.getName());
					artists.add(artist);
				}
				
				// Aggregate Albums
				if (album == null 
						|| !album.getUserId().equals(artist.getUserId())
						|| !album.getArtistName().equals(artist.getName()) 
						|| !album.getName().equals(rs.getString("ALBUM"))) {

					// Get from DB
					album = MediaLibraryDAO.getAlbumByName(rs.getString("ALBUM"), owner.getId(), artist.getId());
					// If not in DB, create
					
					// Add to list
					log.debug("Adding Album " + album.getName() + " - " + albumArtistName);
					artist.addAlbum(album);
				}
				
				// Build contents -- NFiles only need ID attribute set for MediaLibraryDAO.createContents()
				song = new Song(NimbusFileDAO.getById(rs.getLong("ID")));
				album.addSongToPlaylist(song);
			}
			
			log.info("Done building Artist and Album list.");
		} catch (SQLException e) { log.error(e, e); } 
		finally {
			try {
				HSQLConnectionPool.releaseCurrentConnection(con);
				rs.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
		
		return artists;
	}*/
}
