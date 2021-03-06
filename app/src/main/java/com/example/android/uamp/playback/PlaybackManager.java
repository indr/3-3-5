/*
 * Copyright (c) 2016 Reto Inderbitzin (mail@indr.ch)
 *
 * For the full copyright and license information, please view
 * the LICENSE file that was distributed with this source code.
 */

package com.example.android.uamp.playback;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import ch.indr.threethreefive.R;
import ch.indr.threethreefive.libs.MediaIdHelper;
import timber.log.Timber;

/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
public class PlaybackManager implements PlaybackType.Callback {

  // Action to thumbs up a media item
  private static final String CUSTOM_ACTION_THUMBS_UP = "com.example.android.uamp.THUMBS_UP";

  private static final boolean AUTO_REPEAT = false;

  private QueueManager mQueueManager;
  private Resources mResources;
  private PlaybackType mPlayback;
  private PlaybackServiceCallback mServiceCallback;
  private MediaSessionCallback mMediaSessionCallback;

  // TODO: Remove PlaybackType from constructor
  public PlaybackManager(PlaybackServiceCallback serviceCallback, Resources resources,
                         QueueManager queueManager, PlaybackType playback) {
    mServiceCallback = serviceCallback;
    mResources = resources;
    mQueueManager = queueManager;
    mMediaSessionCallback = new MediaSessionCallback();
    mPlayback = playback;
    mPlayback.setCallback(this);
  }

  public PlaybackType getPlayback() {
    return mPlayback;
  }

  public MediaSessionCompat.Callback getMediaSessionCallback() {
    return mMediaSessionCallback;
  }

  /**
   * Handle a request to play music
   */
  public void handlePlayRequest() {
    handlePlayRequest(PlaybackStateCompat.STATE_STOPPED);
  }

  /**
   * Handle a request to play music with desired state between the end of last media and new playback
   */
  public void handlePlayRequest(int state) {
    Timber.d("handlePlayRequest: mState=" + mPlayback.getState());
    MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentQueueItem();
    if (currentMusic != null) {
      mServiceCallback.onPlaybackStart();
      mPlayback.play(currentMusic, state);
    } else {
      handleStopRequest(null);
      mPlayback.setState(PlaybackStateCompat.STATE_NONE);
      updatePlaybackState(null);
    }
  }

  /**
   * Handle a request to pause music
   */
  public void handlePauseRequest() {
    Timber.d("handlePauseRequest: mState=" + mPlayback.getState());
    if (mPlayback.isPlaying()) {
      mPlayback.pause();
      mServiceCallback.onPlaybackPause();
    }
  }

  /**
   * Handle a request to stop music
   *
   * @param withError Error message in case the stop has an unexpected cause. The error
   *                  message will be set in the PlaybackState and will be visible to
   *                  MediaController clients.
   */
  public void handleStopRequest(String withError) {
    Timber.d("handleStopRequest: mState=" + mPlayback.getState() + " error=" + withError);
    mPlayback.stop(true);
    mServiceCallback.onPlaybackStop();
    updatePlaybackState(withError);

    if (mQueueManager.getCurrentQueueItem() == null) {
      mPlayback.setState(PlaybackStateCompat.STATE_NONE);
      updatePlaybackState(null);
    }
  }


  /**
   * Update the current media player state, optionally showing an error message.
   *
   * @param error if not null, error message to present to the user.
   */
  public void updatePlaybackState(String error) {
    Timber.d("updatePlaybackState, playback state=" + mPlayback.getState());
    long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
    if (mPlayback != null && mPlayback.isConnected()) {
      position = mPlayback.getCurrentStreamPosition();
    }

    //noinspection ResourceType
    PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
        .setActions(getAvailableActions());

    setCustomAction(stateBuilder);
    int state = mPlayback.getState();

    // If there is an error message, send it to the playback state:
    if (error != null) {
      // Error states are really only supposed to be used for errors that cause playback to
      // stop unexpectedly and persist until the user takes action to fix it.
      stateBuilder.setErrorMessage(error);
      state = PlaybackStateCompat.STATE_ERROR;
    }
    //noinspection ResourceType
    stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

    // Set the activeQueueItemId if the current index is valid.
    MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentQueueItem();
    if (currentMusic != null) {
      stateBuilder.setActiveQueueItemId(currentMusic.getQueueId());
    }

    mServiceCallback.onPlaybackStateUpdated(stateBuilder.build());

    if (state == PlaybackStateCompat.STATE_PLAYING ||
        state == PlaybackStateCompat.STATE_PAUSED) {
      mServiceCallback.onNotificationRequired();
    }
  }

  private void setCustomAction(PlaybackStateCompat.Builder stateBuilder) {
    MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentQueueItem();
    if (currentMusic == null) {
      return;
    }
    // Set appropriate "Favorite" icon on Custom action:
    String mediaId = currentMusic.getDescription().getMediaId();
    if (mediaId == null) {
      return;
    }
    String musicId = MediaIdHelper.extractMusicIDFromMediaID(mediaId);
    int favoriteIcon = isFavorite(musicId) ?
        R.drawable.ic_star_on : R.drawable.ic_star_off;
    Timber.d("updatePlaybackState, setting Favorite custom action of music " + musicId + " current favorite=" + isFavorite(musicId));
    Bundle customActionExtras = new Bundle();
    stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
        CUSTOM_ACTION_THUMBS_UP, mResources.getString(R.string.favorite), favoriteIcon)
        .setExtras(customActionExtras)
        .build());
  }

  private boolean isFavorite(String musicId) {
    return true;
  }

  private long getAvailableActions() {
    long actions =
        PlaybackStateCompat.ACTION_PLAY_PAUSE |
            PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
            PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
    if (mPlayback.isPlaying()) {
      actions |= PlaybackStateCompat.ACTION_PAUSE;
    } else {
      actions |= PlaybackStateCompat.ACTION_PLAY;
    }
    return actions;
  }

  /**
   * Implementation of the Playback.Callback interface
   */
  @Override
  public void onCompletion() {
    // The media player finished playing the current song, so we go ahead
    // and start the next.

    if (mQueueManager.skipQueuePosition(1, AUTO_REPEAT)) {
      handlePlayRequest(PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM);
      mQueueManager.updateMetadata();
    } else {
      // If skipping was not possible, we stop and release the resources:
      handleStopRequest(null);
    }
  }

  @Override
  public void onPlaybackStatusChanged(int state) {
    updatePlaybackState(null);
  }

  @Override
  public void onError(String error) {
    updatePlaybackState(error);
  }

  private class MediaSessionCallback extends MediaSessionCompat.Callback {
    @Override
    public void onPlay() {
      Timber.d("onPlay");
      if (mQueueManager.getCurrentQueueItem() == null) {
        notifyOptionNotAvailable(R.string.speech_can_not_play_empty_queue);
        return;
      }
      handlePlayRequest();
    }

    @Override
    public void onSkipToQueueItem(long queueId) {
      Timber.d("onSkipToQueueItem: " + queueId);
      mQueueManager.setCurrentQueueItem(queueId);
      mQueueManager.updateMetadata();
    }

    @Override
    public void onSeekTo(long position) {
      Timber.d("onSeekTo: " + position);
      mPlayback.seekTo((int) position);
    }

    @Override
    public void onPause() {
      Timber.d("onPause, current state=" + mPlayback.getState());
      handlePauseRequest();
    }

    @Override
    public void onStop() {
      Timber.d("onStop %s", this.toString());
      Timber.d("onStop. current state=" + mPlayback.getState());
      handleStopRequest(null);
    }

    @Override
    public void onSkipToNext() {
      Timber.d("onSkipToNext %s", this.toString());
      if (mQueueManager.skipQueuePosition(1)) {
        handlePlayRequest(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);
      } else {
        handleStopRequest("Cannot skip");
        notifyOptionNotAvailable(R.string.speech_can_not_skip_to_next);
      }
      mQueueManager.updateMetadata();
    }

    @Override
    public void onSkipToPrevious() {
      Timber.d("onSkipToPrevious %s", this.toString());

      if (mPlayback.canSeek() && mPlayback.getCurrentStreamPosition() >= 2000) {
        mPlayback.seekTo(0);
      } else if (mQueueManager.skipQueuePosition(-1)) {
        handlePlayRequest(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);
      } else {
        handleStopRequest("Cannot skip");
        notifyOptionNotAvailable(R.string.speech_can_not_skip_to_previous);
      }
      mQueueManager.updateMetadata();
    }

    @Override public void onRewind() {
      Timber.d("onRewind %s", this.toString());
      mPlayback.seekTo(mPlayback.getCurrentStreamPosition() - 5000, PlaybackStateCompat.STATE_REWINDING);

      if (!mPlayback.canSeek()) {
        notifyOptionNotAvailable(R.string.speech_can_not_seek_rewind);
      }
    }

    @Override public void onFastForward() {
      Timber.d("onFastForward %s", this.toString());
      mPlayback.seekTo(mPlayback.getCurrentStreamPosition() + 5000, PlaybackStateCompat.STATE_FAST_FORWARDING);

      if (!mPlayback.canSeek()) {
        notifyOptionNotAvailable(R.string.speech_can_not_seek_fast_forward);
      }
    }
  }

  private void notifyOptionNotAvailable(int resourceId) {
    final Bundle bundle = new Bundle();
    bundle.putInt("resourceId", resourceId);
    mServiceCallback.onCustomEvent("option_not_available", bundle);
  }


  public interface PlaybackServiceCallback {
    void onNotificationRequired();

    void onPlaybackStart();

    void onPlaybackPause();

    void onPlaybackStop();

    void onPlaybackStateUpdated(PlaybackStateCompat newState);

    void onCustomEvent(@NonNull String event, @Nullable Bundle extras);
  }
}
