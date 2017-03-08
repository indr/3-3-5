package ch.indr.threethreefive.navigation;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import ch.indr.threethreefive.ThreeThreeFiveApp;
import ch.indr.threethreefive.AppComponent;
import ch.indr.threethreefive.favorites.FavoritesStore;
import ch.indr.threethreefive.libs.Environment;
import ch.indr.threethreefive.libs.PageCommand;
import ch.indr.threethreefive.libs.PageItem;
import ch.indr.threethreefive.libs.PageItemsBuilder;
import ch.indr.threethreefive.libs.PageLink;
import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import timber.log.Timber;

import static ch.indr.threethreefive.libs.rx.transformers.Transfomers.takeWhen;

public abstract class Page implements PageType {

  private Context context;

  private Environment environment;
  private FavoritesStore favoritesStore;

  private Uri pageUri;

  // Page lifecycle state
  private State state = State.New;

  private final BehaviorSubject<String> pageTitle = BehaviorSubject.create();

  // Set default value to `null`, which indicates the page is loading items
  private final BehaviorSubject<List<PageItem>> pageItems = BehaviorSubject.create((List<PageItem>) null);

  private final BehaviorSubject<PageItem> pageItem = BehaviorSubject.create();

  // Set default parent page link to HomePage
  private final BehaviorSubject<PageLink> parentPageLink = BehaviorSubject.create(PageLink.HomePage);

  private final PublishSubject<Object> selectNextPageItem = PublishSubject.create();

  private final PublishSubject<Object> selectPreviousPageItem = PublishSubject.create();

  private final PublishSubject<PageCommand> pageCommand = PublishSubject.create();

  public Page(Environment environment) {
    this.environment = environment;
    this.favoritesStore = environment.favoritesStore();
  }

  protected PageLink getCurrentPageLink() {
    return new PageLink(getPageUri(), getTitle());
  }

  @Override public boolean getIsRootPage() {
    return getParentPageUri() == null;
  }

  @Override public Uri getPageUri() {
    return pageUri;
  }

  @Override public Uri getParentPageUri() {
    final PageLink value = parentPageLink.getValue();
    return value == null ? null : value.getUri();
  }

  @Deprecated
  protected Environment environment() {
    return environment;
  }

  @Override public Observable<String> pageTitle() {
    return pageTitle;
  }

  @Override public Observable<List<PageItem>> pageItems() {
    return pageItems;
  }

  @Override public Observable<PageItem> pageItem() {
    return pageItem;
  }

  @Override public Observable<PageLink> parentPageLink() {
    return parentPageLink;
  }

  @Override public Observable<PageCommand> pageCommand() {
    return pageCommand;
  }

  public void selectNextPageItem() {
    selectNextPageItem.onNext(null);
  }

  public void selectPreviousPageItem() {
    selectPreviousPageItem.onNext(null);
  }

  protected void setParentPageLink(final @Nullable PageLink link) {
    parentPageLink.onNext(link);
  }

  protected void setTitle(final @Nullable CharSequence title) {
    if (title != null) {
      setTitle(title.toString());
    } else {
      setTitle("");
    }
  }

  protected void setTitle(final @NonNull String title) {
    Timber.d("setTitle %s, %s", title, this.toString());
    pageTitle.onNext(title);
  }

  protected void setTitle(final int resourceId) {
    this.setTitle(this.getContext().getResources().getString(resourceId));
  }

  public final @Nullable String getTitle() {
    return pageTitle.getValue();
  }

  public void onCreate(@NonNull Context context, Uri uri, Bundle bundle) {
    if (state != State.New) {
      throw new IllegalStateException("Page is not in state new");
    }
    setState(State.Created);

    this.context = context;
    this.pageUri = uri;

    final BehaviorSubject<Integer> pageItemIdx = BehaviorSubject.create(0);

    // Current/focused page item
    Observable.combineLatest(pageItems, pageItemIdx, (items, idx) -> {
      if (items == null || idx < 0 || idx >= items.size()) return null;
      return items.get(idx);
    }).subscribe(pageItem);

    // Step right, focus next page item
    pageItemIdx
        .compose(takeWhen(selectNextPageItem))
        .map(idx -> idx + 1)
        .withLatestFrom(pageItems, (idx, items) -> {
          if (items == null) return 0;
          return idx > items.size() - 1 ? 0 : idx;
        })
        .subscribe(pageItemIdx);

    // Step left, focus previous page item
    pageItemIdx.compose(takeWhen(selectPreviousPageItem))
        .map(idx -> idx - 1)
        .withLatestFrom(pageItems, (idx, items) -> {
          if (items == null) return 0;
          return idx < 0 ? items.size() - 1 : idx;
        })
        .subscribe(pageItemIdx);
  }

  public void onStart() {
    if (state != State.Created) {
      throw new IllegalStateException("Page is not in state created: " + state);
    }
    setState(State.Started);
  }

  public void onStop() {
    if (state != State.Started && state != State.Paused) {
      throw new IllegalStateException("Page is not in state started, paused: " + state);
    }
    setState(State.Stopped);
  }

  @CallSuper
  public void onResume() {
    if (state != State.Started && state != State.Paused) {
      throw new IllegalStateException("Page is not in state started, paused: " + state);
    }
    setState(State.Resumed);
  }

  @CallSuper
  public void onPause() {
    if (state != State.Resumed) {
      throw new IllegalStateException("Page is not in state resumed: " + state);
    }
    setState(State.Paused);
  }

  public void onDestroy() {
    if (state != State.Stopped) {
      throw new IllegalStateException("Page is not in state stopped: " + state);
    }
    setState(State.Destroyed);
  }

  protected Context getContext() {
    return context;
  }

  protected @NonNull ThreeThreeFiveApp application() {
    return (ThreeThreeFiveApp) context.getApplicationContext();
  }

  protected @NonNull AppComponent component() {
    return application().component();
  }

  protected void handle(Exception e) {
    setError(e);
    e.printStackTrace();
  }

  protected void handle(String message) {
    setError("Error: " + message);
  }

  protected PageItemsBuilder pageItemsBuilder() {
    return new PageItemsBuilder(this.context.getResources(), this.favoritesStore);
  }

  protected void setPageItems(final @NonNull PageItemsBuilder builder) {
    setPageItems(builder.build());
  }

  protected void setPageItems(final @NonNull List<PageItem> items) {
    pageItems.onNext(items);
  }

  protected void setLoading() {
    pageItems.onNext(null);
  }

  private void setError(final @NonNull Exception error) {
    setError(error.getMessage());
  }

  private void setError(final String message) {
    final List<PageItem> items = pageItemsBuilder()
        .addText(message)
        .build();

    setPageItems(items);
  }

  private void setState(State state) {
    Timber.d("New state " + state.name());
    this.state = state;
  }

  public State getState() {
    return state;
  }

  public enum State {
    New,
    Created,
    Started,
    Resumed,
    Paused,
    Stopped,
    Destroyed
  }
}