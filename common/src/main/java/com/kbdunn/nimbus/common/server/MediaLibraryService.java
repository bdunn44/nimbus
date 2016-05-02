package com.kbdunn.nimbus.common.server;

import java.util.List;

import com.kbdunn.nimbus.common.model.Album;
import com.kbdunn.nimbus.common.model.Artist;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.Playlist;
import com.kbdunn.nimbus.common.model.Song;
import com.kbdunn.nimbus.common.model.Video;

public interface MediaLibraryService {

	void setSongAttributes(Song song);

	boolean save(Song song);

	List<Song> getSongs(NimbusUser user);

	List<Song> getSongs(NimbusUser user, int startIndex, int count);

	int getSongCount(NimbusUser user);

	void setVideoAttributes(Video video);

	List<Video> getVideos(NimbusUser user);

	List<Video> getVideos(NimbusUser user, int startIndex, int count);

	int getVideoCount(NimbusUser user);

	List<Playlist> getPlaylists(NimbusUser user);

	List<Playlist> getPlaylists(NimbusUser user, int startIndex, int count);

	int getPlaylistCount(NimbusUser user);

	Playlist getPlaylistByName(NimbusUser owner, String name);

	boolean save(Playlist playlist);

	boolean delete(Playlist playlist);

	boolean setPlaylistSongs(Playlist playlist, List<Song> songs);

	List<Song> getPlaylistSongs(Playlist playlist);

	List<Song> getPlaylistSongs(Playlist playlist, int startIndex, int count);

	int getPlaylistSongCount(Playlist playlist);

	Album getAlbumByName(NimbusUser user, String artistName, String albumName);

	List<Album> getAlbums(NimbusUser user);

	List<Album> getAlbums(NimbusUser user, int startIndex, int count);

	int getAlbumCount(NimbusUser user);

	List<Song> getAlbumSongs(NimbusUser owner, Album album);

	List<Song> getAlbumSongs(NimbusUser owner, Album album, int startIndex, int count);

	int getAlbumSongCount(NimbusUser owner, Album album);

	Artist getArtistByName(NimbusUser owner, String name);

	List<Artist> getArtists(NimbusUser user);

	List<Artist> getArtists(NimbusUser user, int startIndex, int count);

	int getArtistCount(NimbusUser user);

	List<Album> getArtistAlbums(NimbusUser owner, Artist artist);

	List<Song> getArtistSongs(NimbusUser owner, Artist artist);

	List<Song> getArtistSongs(NimbusUser owner, Artist artist, int startIndex, int count);

	int getArtistSongCount(NimbusUser owner, Artist artist);

}