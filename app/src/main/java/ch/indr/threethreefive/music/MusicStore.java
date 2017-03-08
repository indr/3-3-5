/*
 * Copyright (c) 2017 Reto Inderbitzin (mail@indr.ch)
 *
 * For the full copyright and license information, please view
 * the LICENSE file that was distributed with this source code.
 */

package ch.indr.threethreefive.music;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

public class MusicStore {
  private static final String TAG = "MusicStore";

  private final Context context;
  private HashMap<String, String> albumArts;

  public MusicStore(Context context) {
    this.context = context;
  }

  public Artist getArtistById(String artistId) {
    ArrayList<Artist> artists = queryArtists("_id=" + artistId);
    if (artists.size() < 1) return null;

    return artists.get(0);
  }

  public ArrayList<Artist> queryArtists(String selection) {
    ArrayList<Artist> result = new ArrayList<>();
    Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
    Cursor cursor = getContentResolver().query(uri, new String[]{"_id", "artist"},
        selection, null, "artist");
    if (cursor != null && cursor.moveToFirst()) {
      do {
        result.add(new Artist(
            cursor.getString(0),
            cursor.getString(1)));
      }
      while (cursor.moveToNext());
      cursor.close();
    }
    return result;
  }

  public ArrayList<Album> findAlbumsByArtistId(String artistId) {
    return queryAlbums("artist_id=" + artistId);
  }

  public Album getAlbumById(String albumId) {
    ArrayList<Album> albums = queryAlbums("_id=" + albumId);
    if (albums.size() < 1) return null;

    return albums.get(0);
  }

  public ArrayList<Album> queryAlbums(String selection) {
    ArrayList<Album> albums = new ArrayList<>();

    Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
    Cursor cursor = getContentResolver().query(uri,
        new String[]{"_id", "album", "artist", "artist_id"},
        selection, null, "album");

    if (cursor != null && cursor.moveToFirst()) {
      do {
        albums.add(new Album(
            cursor.getString(0),
            cursor.getString(1),
            cursor.getString(2),
            cursor.getString(3)));
      }
      while (cursor.moveToNext());
      cursor.close();
    }
    return albums;
  }

  public Song getSongById(String songId) {
    ArrayList<Song> songs = querySongs("_id=" + songId, null);
    if (songs.size() < 1) return null;

    return songs.get(0);
  }

  public ArrayList<Song> getSongsByAlbumId(String albumId) {
    return querySongs("album_id=" + albumId, null);
  }

  public ArrayList<Song> getSongsByArtistId(String artistId) {
    return querySongs("artist_id=" + artistId, "album, track, title");
  }

  public ArrayList<Song> querySongs(String selection, String sortOrder) {
    sortOrder = sortOrder != null ? sortOrder : "track, title";
    ArrayList<Song> songs = new ArrayList<>();

    Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    Cursor cursor = getContentResolver().query(uri,
        new String[]{"_id", "title", "artist", "artist_id", "album", "album_id", "_data", "duration"},
        selection, null, sortOrder);

    if (cursor != null && cursor.moveToFirst()) {
      do {
        final String albumArt = getAlbumArtByAlbumId(cursor.getString(5));
        songs.add(new Song(
            cursor.getString(0),
            cursor.getString(1),
            cursor.getString(2),
            cursor.getString(3),
            cursor.getString(4),
            cursor.getString(5),
            cursor.getString(6),
            cursor.getLong(7),
            albumArt
        ));
      }
      while (cursor.moveToNext());
      cursor.close();
    }
    return songs;
  }

  public List<Genre> queryGenres() {
    List<Genre> genres = new ArrayList<>();

    Uri uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
    Cursor cursor = getContentResolver().query(uri,
        new String[]{"name"},
        null, null, "name");

    if (cursor != null && cursor.moveToFirst()) {
      do {
        genres.add(new Genre(
            cursor.getString(0)
        ));
      }
      while (cursor.moveToNext());
      cursor.close();
    }
    return genres;
  }

  private String getAlbumArtByAlbumId(String albumId) {
    if (this.albumArts == null) {
      this.albumArts = new HashMap<String, String>();
      Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
      Cursor cursor = getContentResolver().query(uri,
          new String[]{"_id", MediaStore.Audio.AlbumColumns.ALBUM_ART},
          null, null, null);

      if (cursor != null && cursor.moveToFirst()) {
        do {
          albumArts.put(
              cursor.getString(0),
              cursor.getString(1)
          );
        }
        while (cursor.moveToNext());
        cursor.close();
      }
    }

    return albumArts.get(albumId);
  }

  private ContentResolver getContentResolver() {
    return this.context.getContentResolver();
  }

  public List<Song> getSongsByGenreName(String genre) {
    Timber.w("TODO: getSongsByGenreName %s", this.toString());
    return new ArrayList<>();
  }

  public class Artist {
    private final String id;
    private final String name;

    public Artist(String id, String name) {
      this.id = id;
      this.name = name;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }

  public class Album {
    private final String id;
    private final String name;
    private final String artist;
    private final String artistId;

    public Album(String id, String name, String artist, String artistId) {
      this.id = id;
      this.name = name;
      this.artist = artist;
      this.artistId = artistId;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getArtist() {
      return artist;
    }

    public String getArtistId() {
      return artistId;
    }
  }

  public class Song {
    private final String id;
    private final String name;
    private final String artist;
    private final String artistId;
    private final String album;
    private final String albumId;
    private final String data;
    private final long duration;
    private final String albumArt;

    public Song(String id, String name, String artist, String artistId, String album, String albumId, String data, long duration, String albumArt) {
      this.id = id;
      this.name = name;
      this.artist = artist;
      this.artistId = artistId;
      this.album = album;
      this.albumId = albumId;
      this.data = data;
      this.duration = duration;
      this.albumArt = albumArt;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getArtist() {
      return artist;
    }

    public String getAlbum() {
      return album;
    }

    public String getArtistId() {
      return artistId;
    }

    public String getAlbumId() {
      return albumId;
    }

    public String getData() {
      return data;
    }

    public long getDuration() {
      return duration;
    }

    public String getAlbumArt() {
      return albumArt;
    }
  }

  public class Genre {
    private final String name;

    public Genre(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}

