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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListActivity;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class represents the primary engine of managing the
 * master-detail UI pattern, to allow the activity to
 * inherit from whatever it needs to. Each activity using
 * this library for managing the master-detail behavior
 * needs an instance of a subclass of this class, properly
 * configured for how you want the master-detail pattern to
 * be managed.
 * 
 * @param <T>
 *          the Java class representing your collection of
 *          model data (e.g., ArrayList&lt;Restaurant&gt;,
 *          Cursor)
 */
abstract public class MasterDetailHelper<T> implements
    PagerListAdapter.Assistant, OnItemLongClickListener,
    MultiChoiceModeListener {
  /**
   * Override this method to return your model object
   * collection. The data type is up to you. For this
   * specific class, this model collection is solely for
   * state management in configuration changes, as
   * MasterDetailHelper will ensure that the model
   * collection is retained. You can retrieve your instance
   * of your model collection via getModelCollection().
   * 
   * @return the collection of model objects to be displayed
   *         in the associated activity
   */
  abstract protected T buildModelCollection();

  /**
   * Override this to provide the PagerAdapter that
   * represents the detail pages to be shown. By default,
   * this will also drive the list of items to be shown in
   * the master list. Note that the pages may not
   * necessarily be shown in a ViewPager, as depending on
   * screen size and the associated strategy, it may be that
   * the pages are displayed individually, outside of a
   * ViewPager.
   * 
   * @param fm
   *          a FragmentManager, if you need one, for your
   *          PagerAdapter
   * @return a PagerAdapter that will provide the pages to
   *         be displayed as the detail for your models in
   *         your collection
   */
  abstract protected PagerAdapter buildPagerAdapter(FragmentManager fm);

  static private final String STATE_CHECKED=
      "com.commonsware.cwac.masterdetail.STATE_CHECKED";
  static private final String STATE_SHOW_DETAIL=
      "com.commonsware.cwac.masterdetail.STATE_SHOW_DETAIL";
  static private final String STATE_PAGER_ID=
      "com.commonsware.cwac.masterdetail.STATE_PAGER_ID";
  static private final String STATE_MC=
      "com.commonsware.cwac.masterdetail.STATE_MC";
  static private final AtomicInteger sNextGeneratedId=
      new AtomicInteger(1);
  private PagerAdapter pages=null;
  private MasterDetailStrategy strategy=null;
  private ModelCollectionFragment<T> modelCollectionFragment=null;
  private int pagerId=-1;
  private ActionMode activeMode=null;
  private Activity host=null;
  private ListView lv=null;

  /**
   * Initializes the master-detail UI. This should be called
   * from onCreate() of the activity that is implementing
   * the master-detail pattern.
   * 
   * @param host
   *          the activity implementing the master-detail
   *          pattern
   * @param state
   *          the Bundle passed into the activity's
   *          onCreate() method
   */
  @SuppressWarnings("unchecked")
  public void onCreate(Activity host, Bundle state) {
    this.host=host;

    if (state != null) {
      pagerId=state.getInt(STATE_PAGER_ID, -1);
    }

    if (pagerId == -1) {
      pagerId=generateViewId(); // must have an ID to
                                // work
    }

    modelCollectionFragment=
        (ModelCollectionFragment<T>)getFragmentManager().findFragmentByTag(getModelFragmentTag());

    if (modelCollectionFragment == null) {
      modelCollectionFragment=
          new ModelCollectionFragment<T>().modelCollection(buildModelCollection());

      getFragmentManager().beginTransaction()
                          .add(modelCollectionFragment,
                               getModelFragmentTag()).commit();
    }

    int minDip=getMinimumDipWidthForDualPane();

    if (getResources().getConfiguration().screenWidthDp >= minDip
        || getResources().getConfiguration().screenHeightDp >= minDip) {
      strategy=new MasterDetailStrategy.DualPane(this, pagerId);
    }
    else {
      strategy=new MasterDetailStrategy.SinglePane(this, pagerId);
    }

    pages=buildPagerAdapter(getFragmentManager());
    host.setContentView(strategy.getContentView(pages));
    lv=(ListView)host.findViewById(android.R.id.list);

    ListAdapter adapter=buildListAdapter();

    adapter.registerDataSetObserver(masterObserver);
    setListAdapter(adapter);

    if (offerActionMode()) {
      getListView().setOnItemLongClickListener(this);
    }

    getListView().setChoiceMode(getDefaultChoiceMode());

    if (state != null) {
      if (state.getBoolean(STATE_MC, false)) {
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(this);
      }
      else {
        int position=state.getInt(STATE_CHECKED, -1);

        if (position > -1 && state.getBoolean(STATE_SHOW_DETAIL, false)) {
          showDetail(position);
        }
        else {
          strategy.clearDetail();
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.commonsware.cwac.masterdetail.PagerListAdapter.
   * Assistant#getView(int, android.view.View,
   * android.view.ViewGroup)
   */
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (convertView == null) {
      int layout=android.R.layout.simple_list_item_activated_1;
      // isActivatedStyle()
      // ? android.R.layout.simple_list_item_activated_1
      // : android.R.layout.simple_list_item_1;

      convertView=getLayoutInflater().inflate(layout, parent, false);
    }

    ((TextView)convertView).setText(pages.getPageTitle(position));

    return(convertView);
  }

  /**
   * Handler for action bar setup. The activity's
   * onCreateOptionsMenu() should call this one as part of
   * its processing.
   * 
   * @param menu
   *          the Menu object passed into the activity's
   *          onCreateOptionsMenu()
   * @return true, indicating that we are done with the
   *         event
   */
  public boolean onCreateOptionsMenu(Menu menu) {
    return(true);
  }

  /**
   * Handler for action bar item clicks. The activity's
   * onOptionsItemSelected() should call this one as part of
   * its processing.
   * 
   * @param item
   *          the MenuItem passed into the activity's
   *          onOptionsItemSelected()
   * @return false, indicating that we did not consume the
   *         event
   */
  public boolean onOptionsItemSelected(MenuItem item) {
    return(false);
  }

  /**
   * Handler for clicks on list items in the master view of
   * the activity. The activity's onListItemClick() (or the
   * equivalent) should call this one as part of its
   * processing.
   * 
   * @param l
   *          the ListView that the user tapped upon
   * @param v
   *          the row View that the user tapped upon
   * @param position
   *          the position in the adapter of the row that
   *          the user tapped upon
   * @param id
   *          the id of the row that the user tapped upon
   */
  public void onListItemClick(ListView l, View v, int position, long id) {
    onDetailSelected(position);
  }

  /**
   * Handler for saving state on configuration changes, etc.
   * The activity's onSaveInstanceState() should call this
   * one as part of its processing.
   * 
   * @param state
   *          Bundle of state to hold onto
   */
  public void onSaveInstanceState(Bundle state) {
    if (getListView().getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE_MODAL) {
      state.putBoolean(STATE_MC, true);
    }
    else if (strategy.isActivatedStyle()) {
      state.putInt(STATE_CHECKED,
                   getListView().getCheckedItemPosition());
    }

    state.putBoolean(STATE_SHOW_DETAIL, strategy.isDetailShowing());
    state.putInt(STATE_PAGER_ID, pagerId);
  }

  /**
   * Handler for BACK button presses. The activity's
   * onBackPressed() implementation should pass control to
   * this one as part of its processing.
   * 
   * @return true if we consumed the event, false otherwise
   */
  public boolean onBackPressed() {
    if (strategy.isMasterShowing()) {
      return(false);
    }
    if (!strategy.isActivatedStyle()) {
      getListView().clearChoices();
    }

    strategy.showMaster();

    return(true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * android.widget.AdapterView.OnItemLongClickListener#
   * onItemLongClick(android.widget.AdapterView,
   * android.view.View, int, long)
   */
  @Override
  public boolean onItemLongClick(AdapterView<?> parent, View view,
                                 int position, long id) {
    getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    getListView().setMultiChoiceModeListener(this);
    getListView().setItemChecked(position, true);

    return(true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * android.view.ActionMode.Callback#onCreateActionMode
   * (android.view.ActionMode, android.view.Menu)
   */
  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    activeMode=mode;

    int actionModeResource=getActionModeResource();

    if (actionModeResource != -1) {
      MenuInflater inflater=getMenuInflater();

      inflater.inflate(actionModeResource, menu);
    }

    return(true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * android.view.ActionMode.Callback#onPrepareActionMode
   * (android.view.ActionMode, android.view.Menu)
   */
  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    onItemCheckedStateChanged(mode, -1, -1, false);

    return(false);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * android.view.ActionMode.Callback#onActionItemClicked
   * (android.view.ActionMode, android.view.MenuItem)
   */
  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    activeMode.finish();

    return(true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * android.view.ActionMode.Callback#onDestroyActionMode
   * (android.view.ActionMode)
   */
  @Override
  public void onDestroyActionMode(ActionMode mode) {
    activeMode=null;

    getListView().post(new Runnable() {
      @Override
      public void run() {
        getListView().setChoiceMode(getDefaultChoiceMode());
        getListView().setAdapter(getListView().getAdapter());
        strategy.clearDetail();
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * android.widget.AbsListView.MultiChoiceModeListener#
   * onItemCheckedStateChanged(android.view.ActionMode, int,
   * long, boolean)
   */
  @Override
  public void onItemCheckedStateChanged(ActionMode mode, int position,
                                        long id, boolean checked) {
    if (getListView().getCheckedItemCount() == 0) {
      strategy.clearDetail();
    }
    else if (getListView().getCheckedItemCount() == 1) {
      if (checked && position > -1) {
        strategy.showDetailMultipleChoice(position);
      }
      else {
        strategy.clearDetail();
      }
    }
    else {
      strategy.showDetailMultipleChoice(getListView().getCheckedItemPositions());
    }
  }

  /**
   * Returns the activity that is hosting this helper. Use
   * this if you need a Context for something from your
   * helper subclass.
   * 
   * @return the activity that is hosting this helper
   */
  public Activity getHost() {
    return(host);
  }

  /**
   * Convenience method to retrieve the Resources from the
   * hosting activity
   * 
   * @return the Resources for this activity
   */
  Resources getResources() {
    return(host.getResources());
  }

  /**
   * Override this if you want something else as the
   * "empty view" for the master, in case the list is empty.
   * The default implementation uses a simple ProgressBar.
   * Note that the empty view will be centered within the
   * master. Also note that the empty view should not have a
   * parent, as it will be added to a parent by the
   * framework.
   * 
   * @return a View to serve as the "empty view" for the
   *         master list
   */
  protected View buildListEmptyView() {
    return(new ProgressBar(getHost()));
  }

  /**
   * Override this if you want something else as the
   * "empty view" for the detail. This is shown when there
   * is no detail selected, in dual-pane mode. It is not
   * used in single-pane mode. The default implementation is
   * an empty View, and so the detail area is blank when
   * nothing is selected. You might elect to use this for
   * instructions, summary information, etc. Note that the
   * empty view will be sized to fill the detail area. Also
   * note that the empty view should not have a parent, as
   * it will be added to a parent by the framework.
   * 
   * @return a View to serve as the "empty view" for the
   *         detail area
   */
  protected View buildDetailEmptyView() {
    View result=new View(getHost());

    // result.setBackgroundColor(Color.MAGENTA);

    return(result);
  }

  /**
   * Override this if you want something else as the view
   * shown in the detail area when there are multiple
   * selections made in the dual-pane mode. You are passed
   * the positions in your ViewPager/model collection
   * representing the selected items. This is not used in
   * single pane mode. The default implementation is an
   * empty View, so nothing will be shown by default. You
   * might use this to show aggregate information about the
   * selected items. Note that the empty view will be sized
   * to fill the detail area. Also note that the empty view
   * should not have a parent, as it will be added to a
   * parent by the framework.
   * 
   * @param positions
   *          the items selected by the user
   * @return the View to show in the detail area for the
   *         multiple selections
   */
  protected View buildDetailMultipleChoiceView(SparseBooleanArray positions) {
    View result=new View(getHost());

    // result.setBackgroundColor(Color.CYAN);

    return(result);
  }

  /**
   * Override this if you want to replace the ListAdapter
   * used for the master area. The default is to build a
   * ListAdapter based on the contents of the ViewPager,
   * with one row for each page, whose content is the title
   * (getPageTitle()) of that page. You can extend
   * PagerListAdapter or provide your own. If you do provide
   * your own, bear in mind that the positions need to match
   * (e.g., the first position of your ListAdapter must be
   * for the content that will be shown in the detail area
   * for the first position in the ViewPager). Otherwise,
   * your users will be rather confused.
   * 
   * @return
   */
  protected ListAdapter buildListAdapter() {
    return(new PagerListAdapter(pages, this));
  }

  /**
   * Override this if you want to get control when the user
   * has chosen something in the master. Please chain to the
   * superclass if you do, to allow normal detail processing
   * to proceed.
   * 
   * @param position
   *          the selected item
   */
  protected void onDetailSelected(int position) {
    showDetail(position);
  }

  /**
   * Override this to determine the "dividing line" between
   * when the helper uses single-pane mode versus dual-pane
   * mode. The default is 720dip.
   * 
   * @return the number of density-independent pixels, where
   *         if the device is wider than this value, use
   *         dual-pane mode instead of single-pane mode
   */
  protected int getMinimumDipWidthForDualPane() {
    return(720);
  }

  /**
   * Override this to supply the tag to be used for the
   * model fragment that retains your model collection
   * across configuration changes. The default value is
   * something that is unlikely to collide with any other
   * tag that you may be using.
   * 
   * @return some unique value to be used as the fragment
   *         tag for the fragment retaining the model
   */
  protected String getModelFragmentTag() {
    return("OMG-YOU-BETTER-NOT-USE-THIS-TAG-FOR-ANYTHING-ELSE");
  }

  /**
   * Call this to retrieve your model collection that you
   * supplied via buildModelCollection().
   * 
   * @return whatever your model collection is
   */
  public T getModelCollection() {
    return(modelCollectionFragment.modelCollection());
  }

  /**
   * Override this to supply the int value of a menu
   * resource (R.menu.something_or_another) that you wish to
   * use for the action mode.
   * 
   * @return a menu resource value
   */
  protected int getActionModeResource() {
    return(-1);
  }

  /**
   * Convenience method to access the master ListView.
   * 
   * @return the master ListView
   */
  protected ListView getListView() {
    return(lv);
  }

  /**
   * Convenience method to get a LayoutInflater for use in
   * inflating empty views, etc.
   * 
   * @return a LayoutInflater
   */
  protected LayoutInflater getLayoutInflater() {
    return(host.getLayoutInflater());
  }

  /**
   * Convenience method to get the PagerAdapter that you
   * supplied via buildPagerAdapter().
   * 
   * @return your very own PagerAdapter
   */
  protected PagerAdapter getPagerAdapter() {
    return(pages);
  }

  /**
   * Override this to control whether or not an action mode
   * should be offered to the user via a long-click on a row
   * in the master. The default value is false, meaning
   * that an action mode will not be offered. Return true
   * if you would like an action mode, in which case you
   * will also need to override getActionModeResource() to
   * return the menu resource to use. 
   * 
   * @return true or false
   */
  protected boolean offerActionMode() {
    return(false);
  }

  void showDetail(int position) {
    strategy.showDetail(position);
  }

  void setActivatedItem(int position) {
    if (strategy.isActivatedStyle()) {
      getListView().setItemChecked(position, true);
    }
  }

  FragmentManager getFragmentManager() {
    return(host.getFragmentManager());
  }

  void setListAdapter(ListAdapter adapter) {
    if (host instanceof ListActivity) {
      ((ListActivity)host).setListAdapter(adapter);
    }
    else {
      lv.setAdapter(adapter);
    }
  }

  MenuInflater getMenuInflater() {
    return(host.getMenuInflater());
  }

  int getDefaultChoiceMode() {
    return(strategy.isActivatedStyle() ? AbsListView.CHOICE_MODE_SINGLE
        : AbsListView.CHOICE_MODE_NONE);
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  static int generateViewId() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      return(View.generateViewId());
    }

    // following from AOSP

    for (;;) {
      final int result=sNextGeneratedId.get();
      // aapt-generated IDs have the high byte nonzero;
      // clamp to the range under that.
      int newValue=result + 1;
      if (newValue > 0x00FFFFFF)
        newValue=1; // Roll over to 1, not 0.
      if (sNextGeneratedId.compareAndSet(result, newValue)) {
        return result;
      }
    }
  }

  private DataSetObserver masterObserver=new DataSetObserver() {
    @Override
    public void onChanged() {
      final int current=getListView().getCheckedItemPosition();

      getListView().post(new Runnable() {
        @Override
        public void run() {
          if (current >= 0) {
            if (strategy.isDetailShowing()) {
              onDetailSelected(current);
            }
          }
        }
      });
    }

    @Override
    public void onInvalidated() {
      onChanged();
    }
  };

  /**
   * This class is public, because the fragment framework
   * requires it. However, this class is part of the
   * internal implementation of MasterDetailHelper and is
   * not designed to be used by others.
   * 
   * Specifically, this class is a simple model fragment,
   * used to retain the model collection across
   * configuration changes.
   * 
   * @param <T>
   *          the Java class representing your collection of
   *          model data (e.g., ArrayList&lt;Restaurant&gt;,
   *          Cursor)
   */
  public static class ModelCollectionFragment<T> extends Fragment {
    T modelCollection=null;

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity host) {
      super.onAttach(host);

      setRetainInstance(true);
    }

    /**
     * @return the model collection
     */
    T modelCollection() {
      return(modelCollection);
    }

    /**
     * @param model
     *          the model collection to be retained
     * @return the model collection (for fluent interface)
     */
    ModelCollectionFragment<T> modelCollection(T model) {
      this.modelCollection=model;

      return(this);
    }
  }
}
