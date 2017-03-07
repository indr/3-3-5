/*
 * Copyright (c) 2016 Reto Inderbitzin (mail@indr.ch)
 *
 * For the full copyright and license information, please view
 * the LICENSE file that was distributed with this source code.
 */

package ch.indr.threethreefive.libs.rx.transformers;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import rx.Observable;

public final class CombineLatestPairTransformer<S, T> implements Observable.Transformer<S, Pair<S, T>> {
  @NonNull private final Observable<T> second;

  public CombineLatestPairTransformer(final @NonNull Observable<T> second) {
    this.second = second;
  }

  @Override
  @NonNull public Observable<Pair<S, T>> call(final @NonNull Observable<S> first) {
    return Observable.combineLatest(first, second, Pair::new);
  }
}
