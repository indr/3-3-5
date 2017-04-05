/*
 * Copyright (c) 2017 Reto Inderbitzin (mail@indr.ch)
 *
 * For the full copyright and license information, please view
 * the LICENSE file that was distributed with this source code.
 */

package ch.indr.threethreefive.libs;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ch.indr.threethreefive.commands.ActionCommand;
import ch.indr.threethreefive.commands.ToggleFavorite;
import rx.functions.Action0;
import rx.functions.Action1;

public class PageItemsBuilder {

  private final Resources resources;
  private final Environment environment;

  private final List<PageItem> items = new ArrayList<>();

  public PageItemsBuilder(@NonNull Context context, @NonNull Environment environment) {
    this.resources = context.getResources();
    this.environment = environment;
  }

  public PageItemsBuilder add(final @NonNull PageItem item) {
    items.add(item);
    return this;
  }

  public PageItemsBuilder addAction(final @NonNull String title, Action0 action) {
    return add(new ActionCommand(title, action));
  }

  public PageItemsBuilder addAction(final @NonNull String title, Action1<Environment> action) {
    return add(new ActionCommand(title, action));
  }

  public PageItemsBuilder addLink(final @NonNull Uri uri, final @NonNull String title) {
    items.add(new PageLink(uri, title));
    return this;
  }

  public PageItemsBuilder addLink(final @NonNull Uri uri, final @NonNull String title,
                                  final @Nullable String subtitle, final @NonNull String description) {
    items.add(new PageLink(uri, title, subtitle, description));
    return this;
  }

  public PageItemsBuilder addLink(final @NonNull Uri uri, final @NonNull String title,
                                  final @Nullable String subtitle, final @NonNull String description,
                                  final @Nullable String iconUri) {
    items.add(new PageLink(uri, title, subtitle, description, iconUri));
    return this;
  }

  public PageItemsBuilder addText(final @NonNull String title) {
    items.add(new TextItem(title));
    return this;
  }

  public PageItemsBuilder addText(final @NonNull String title, final @Nullable String subtitle, final @NonNull String description) {
    items.add(new TextItem(title, subtitle, description));
    return this;
  }

  public PageItemsBuilder addToggleFavorite(PageLink pageLink) {
    items.add(new ToggleFavorite(environment.favoritesStore(), pageLink));
    return this;
  }

  public List<PageItem> build() {
    return items;
  }
}
