/*
 * Copyright (c) 2016 Reto Inderbitzin (mail@indr.ch)
 *
 * For the full copyright and license information, please view
 * the LICENSE file that was distributed with this source code.
 */

package ch.indr.threethreefive.music.pages;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.List;

import ch.indr.threethreefive.R;
import ch.indr.threethreefive.libs.Environment;
import ch.indr.threethreefive.libs.PageItemsBuilder;
import ch.indr.threethreefive.libs.PageUris;
import ch.indr.threethreefive.music.MusicStore;
import ch.indr.threethreefive.navigation.Page;

public class ArtistsPage extends Page {

  public ArtistsPage(Environment environment) {
    super(environment);
  }

  @Override public void onCreate(@NonNull Context context, @NonNull Uri uri, Bundle bundle) {
    super.onCreate(context, uri, bundle);

    setTitle("Artists");

    final PageItemsBuilder builder = pageItemsBuilder();
    final MusicStore musicStore = new MusicStore(getContext());

    final List<MusicStore.Artist> artists = musicStore.queryArtists(null);
    for (MusicStore.Artist artist : artists) {
      builder.addLink(PageUris.makeArtistUri(artist.getId()), artist.getName(), makeSubtitle(artist), makeDescription(artist));
    }

    setPageItems(builder);
  }

  private String makeDescription(MusicStore.Artist artist) {
    return artist.getName() + ", " + makeSubtitle(artist);
  }

  private String makeSubtitle(MusicStore.Artist artist) {
    return getResources().getQuantityString(R.plurals.music_albums, artist.getNumberOfAlbums(), artist.getNumberOfAlbums()) +
        ", " +
        getResources().getQuantityString(R.plurals.music_tracks, artist.getNumberOfTracks(), artist.getNumberOfTracks());
  }
}
