/*
 * Copyright (c) 2017 Reto Inderbitzin (mail@indr.ch)
 *
 * For the full copyright and license information, please view
 * the LICENSE file that was distributed with this source code.
 */

package ch.indr.threethreefive.data.network.radioBrowser.transformers;

import com.octo.android.robospice.request.listener.RequestListener;

import java.util.List;

import ch.indr.threethreefive.data.network.radioBrowser.model.Genre;
import ch.indr.threethreefive.data.network.radioBrowser.model.GenresBuilder;
import ch.indr.threethreefive.data.network.radioBrowser.model.Tag;
import ch.indr.threethreefive.libs.net.ResponseTransformer;

public class TagsToGenresTransformer extends ResponseTransformer<Tag[], List<Genre>> {

  public TagsToGenresTransformer(RequestListener<List<Genre>> listener) {
    super(listener);
  }

  @Override public void onRequestSuccess(Tag[] tags) {
    if (tags == null) {
      listener.onRequestSuccess(null);
      return;
    }

    final List<Genre> genres = GenresBuilder.getGenres(tags);
    listener.onRequestSuccess(genres);
  }
}
