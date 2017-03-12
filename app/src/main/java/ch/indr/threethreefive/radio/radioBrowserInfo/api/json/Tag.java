/*
 * Copyright (c) 2016 Reto Inderbitzin (mail@indr.ch)
 *
 * For the full copyright and license information, please view
 * the LICENSE file that was distributed with this source code.
 */

package ch.indr.threethreefive.radio.radioBrowserInfo.api.json;

import com.google.api.client.util.Key;

import java.util.Comparator;

public class Tag {

  @Key private String name;

  @Key private String value;

  @Key private String stationcount;

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public int getStationCount() {
    return stationcount == null ? 0 : Integer.parseInt(stationcount);
  }

  public static class NameComparator implements Comparator<Tag> {
    @Override public int compare(Tag tag1, Tag tag2) {
      return tag1.getName().compareTo(tag2.getName());
    }
  }

  public static class StationCountComparator implements Comparator<Tag> {
    @Override public int compare(Tag tag1, Tag tag2) {
      return tag2.getStationCount() - tag1.getStationCount();
    }
  }

}
