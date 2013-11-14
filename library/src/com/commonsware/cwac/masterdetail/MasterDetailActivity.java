/***
  Copyright (c) 2013 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.commonsware.cwac.masterdetail;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

/**
 * An activity that supports the master-detail UI pattern.
 * 
 * If you need to inherit from something else, copy the code
 * from here into your activity class, or clone this class
 * to create one that inherits from what you need.
 * 
 * Otherwise, feel free to inherit from this class directly.
 * 
 * @param <T>
 *          the Java class representing your collection of
 *          model data (e.g., ArrayList&lt;Restaurant&gt;,
 *          Cursor)
 */
abstract public class MasterDetailActivity<T> extends ListActivity {
  /**
   * Override this method to provide your configured
   * instance of a MasterDetailHelper. MasterDetailActivity
   * will hold onto this instance for you, which you can
   * retrieve via getHelper(), so you do not need to
   * maintain your own data member for it.
   * 
   * @return a configured MasterDetailHelper instance
   */
  abstract protected MasterDetailHelper<T> buildMasterDetailHelper();

  private MasterDetailHelper<T> helper=null;

  /*
   * (non-Javadoc)
   * 
   * @see android.app.Activity#onCreate(android.os.Bundle)
   */
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);

    helper=buildMasterDetailHelper();
    helper.onCreate(this, state);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * android.app.ListActivity#onListItemClick(android.widget
   * .ListView, android.view.View, int, long)
   */
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    getHelper().onListItemClick(l, v, position, id);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * android.app.Activity#onSaveInstanceState(android.os
   * .Bundle)
   */
  @Override
  public void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);

    getHelper().onSaveInstanceState(state);
  }

  /*
   * (non-Javadoc)
   * 
   * @see android.app.Activity#onBackPressed()
   */
  @Override
  public void onBackPressed() {
    if (!getHelper().onBackPressed()) {
      super.onBackPressed();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * android.app.Activity#onCreateOptionsMenu(android.view
   * .Menu)
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return(getHelper().onCreateOptionsMenu(menu));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * android.app.Activity#onOptionsItemSelected(android.
   * view.MenuItem)
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return(getHelper().onOptionsItemSelected(item));
  }

  /**
   * Getter for the activity's configured MasterDetailHelper
   * instance. The value is set in onCreate() via a call to
   * buildMasterDetailHelper(), so there is no corresponding
   * setter.
   * 
   * @return
   */
  protected MasterDetailHelper<T> getHelper() {
    return(helper);
  }
}
