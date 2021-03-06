/*
 * Copyright (c) 2017 Reto Inderbitzin (mail@indr.ch)
 *
 * For the full copyright and license information, please view
 * the LICENSE file that was distributed with this source code.
 */

package ch.indr.threethreefive.pages.radio;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.List;

import ch.indr.threethreefive.Fake;
import ch.indr.threethreefive.R;
import ch.indr.threethreefive.TtfRobolectricTestCase;
import ch.indr.threethreefive.data.network.ApiClient;
import ch.indr.threethreefive.libs.PageItem;

import static ch.indr.threethreefive.services.UiModeManager.UI_MODE_BUTTONS;
import static org.mockito.Mockito.verify;

public class CountryStationsPageTests extends TtfRobolectricTestCase {

  private ApiClient apiClient;

  @Override public void setUp() throws Exception {
    super.setUp();
    setMode(UI_MODE_BUTTONS);
    this.apiClient = appModule().apiClient(context());
  }

  @Test(expected = IllegalArgumentException.class)
  public void onCreate_withoutCountryIdInBundle_throws() {
    try {
      createPage(new Bundle());
    } catch (Exception ex) {
      assertTrue(ex.getMessage().contains("key countryId"));
      throw ex;
    }
  }

  @Test
  public void onCreate_withCountryIdInBundle_setsTitle() {
    final Bundle bundle = new Bundle();
    bundle.putString("countryId", "Dreamland");
    final CountryStationsPage page = createPage(bundle);

    assertEquals("Dreamland", page.getTitle());
  }

  @Test
  public void onStart_getStationsByCountry() {
    final CountryStationsPage page = createPage();

    page.onStart();

    verify(apiClient).getStationsByCountry("Dreamland", page);
  }

  @Test
  public void onRequestSuccess_withNullResponse_noStationsFoundError() {
    final CountryStationsPage page = createPage();

    page.onRequestSuccess(null);

    final List<PageItem> pageItems = page.getPageItems();
    assertEquals(getString(R.string.no_stations_found_error), pageItems.get(1).getTitle());
  }

  @Test
  public void onRequestSuccess_withEmptyResponse_noStationsFound() {
    final CountryStationsPage page = createPage();

    page.onRequestSuccess(Fake.stations(0));

    final List<PageItem> pageItems = page.getPageItems();
    assertEquals(getString(R.string.no_stations_found), pageItems.get(1).getTitle());
  }

  @Test
  public void onRequestSuccess_with15Stations_show15Stations() {
    final CountryStationsPage page = createPage();

    page.onRequestSuccess(Fake.stations(15));

    final List<PageItem> pageItems = page.getPageItems();
    assertEquals(1 + 15, pageItems.size());
  }

  @Test
  public void onRequestSuccess_with50Stations_shows15StationsAndShowAll() {
    final CountryStationsPage page = createPage();

    page.onRequestSuccess(Fake.stations(50));

    final List<PageItem> pageItems = page.getPageItems();
    assertEquals(1 + 15 + 1, pageItems.size());
    assertEquals(getString(R.string.show_all_stations), pageItems.get(pageItems.size() - 1).getTitle());
  }

  @Test
  public void onRequestSuccess_with51Stations_shows15StationsAndShowMore() {
    final CountryStationsPage page = createPage();

    page.onRequestSuccess(Fake.stations(51));

    final List<PageItem> pageItems = page.getPageItems();
    assertEquals(1 + 15 + 1, pageItems.size());
    assertEquals(getString(R.string.show_more_stations), pageItems.get(pageItems.size() - 1).getTitle());
  }

  @NonNull private CountryStationsPage createPage() {
    final Bundle bundle = new Bundle();
    bundle.putString("countryId", "Dreamland");
    return createPage(bundle);
  }

  @NonNull private CountryStationsPage createPage(@NonNull final Bundle bundle) {
    final CountryStationsPage page = new CountryStationsPage(environment());
    page.onCreate(context(), Uri.parse("/radio/countries/Fantasia"), bundle);
    return page;
  }

}
