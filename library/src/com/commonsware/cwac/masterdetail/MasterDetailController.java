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

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.commonsware.cwac.pager.ArrayPagerAdapter;
import com.commonsware.cwac.pager.PageDescriptor;
import com.commonsware.cwac.pager.SimplePageDescriptor;

/**
 * Subclass of MasterDetailHelper, designed for cases where
 * your model collection is a List of models, and you would
 * like to enable automatic management of the action bar and
 * action modes, for common operations like add and remove,
 * and automatic creation of a PagerAdapter wrapped around
 * your model collection.
 * 
 * @param <T>
 *          the type of your model, where your model
 *          collection is a List of this type
 */
abstract public class MasterDetailController<T> extends
    MasterDetailHelper<List<T>> {
  /**
   * Given a model, returns a unique identifying tag for
   * this model. This tag will be used to identify the
   * fragment associated with this model. Other than being
   * unique (and not null), the actual value is immaterial.
   * 
   * @param model
   *          the model whose tag we need
   * @return the aforementioned tag
   */
  protected abstract String getModelTag(T model);

  /**
   * Given a model's tag, returns a newly-constructed
   * Fragment for that model, to be shown in the detail
   * area. This will be added to a ViewPager in single-pane
   * mode.
   * 
   * @param tag
   *          identifier of the model
   * @return the fragment to show as the detail for this
   *         model
   */
  protected abstract Fragment buildFragmentForTag(String tag);

  /**
   * Implement this method to return a new model object, in
   * response to the user requesting to add a new model. You
   * do not have to add it to the model collection. You do,
   * however, have to take care of anything else, including
   * arranging to persist this model, if appropriate.
   * 
   * @return an empty, but initialized, model object
   */
  abstract protected T createNewModel();

  /**
   * Implement this method to handle all aspects of removing
   * this model, in response to a user request to remove it.
   * The exception is that you do not need to remove it from
   * the model collection -- that is handled for you.
   * However, this is your chance to arrange to update your
   * backing store (file, database, "the cloud", whatever)
   * to reflect the fact that this model is gone.
   * 
   * Note that a future update to this library will support
   * a simple undo operation, akin to what you see in Gmail
   * when you delete conversations. The removeModel() method
   * will not be called until that undo opportunity has
   * passed.
   * 
   * @param model
   *          the model object to be removed from existence
   */
  abstract protected void removeModel(T model);

  /**
   * Constructor for a MasterDetailController
   * 
   * @param options
   *          a MasterDetailOptions.Controller providing
   *          configuration information for this helper
   */
  public MasterDetailController(MasterDetailController.Options options) {
    super(options);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.commonsware.cwac.masterdetail.MasterDetailHelper
   * #buildPagerAdapter(android.app.FragmentManager)
   */
  @Override
  protected PagerAdapter buildPagerAdapter(FragmentManager fm) {
    ArrayList<PageDescriptor> pages=new ArrayList<PageDescriptor>();
    List<T> model=getModelCollection();

    for (int i=0; i < model.size(); i++) {
      pages.add(new SimplePageDescriptor(getModelTag(model.get(i)),
                                         getModelTitle(model.get(i))));
    }

    return(new ModelPagerAdapter(getFragmentManager(), pages));
  }

  /**
   * Override this to provide a title for this model object.
   * This will be used, by default, for the contents of the
   * rows in the master ListView. The default implementation
   * calls toString() on the model object, so you are
   * welcome to override that instead if you prefer
   * 
   * @param model
   *          the model whose title we need
   * @return the title of the model
   */
  protected String getModelTitle(T model) {
    return(model.toString());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.commonsware.cwac.masterdetail.MasterDetailHelper
   * #onCreateActionMode(android.view.ActionMode,
   * android.view.Menu)
   */
  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    updateActionModeTitle(mode);

    return(super.onCreateActionMode(mode, menu));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.commonsware.cwac.masterdetail.MasterDetailHelper
   * #onItemCheckedStateChanged(android.view.ActionMode,
   * int, long, boolean)
   */
  @Override
  public void onItemCheckedStateChanged(ActionMode mode, int position,
                                        long id, boolean checked) {
    updateActionModeTitle(mode);

    super.onItemCheckedStateChanged(mode, position, id, checked);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.commonsware.cwac.masterdetail.MasterDetailHelper
   * #onCreateOptionsMenu(android.view.Menu)
   */
  public boolean onCreateOptionsMenu(Menu menu) {
    if (getOptionsMenuResource() >= 0) {
      getMenuInflater().inflate(getOptionsMenuResource(), menu);
    }

    return(true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.commonsware.cwac.masterdetail.MasterDetailHelper
   * #onOptionsItemSelected(android.view.MenuItem)
   */
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == getAddMenuId()) {
      add();
      return(true);
    }

    return(false);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.commonsware.cwac.masterdetail.MasterDetailHelper
   * #onActionItemClicked(android.view.ActionMode,
   * android.view.MenuItem)
   */
  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    if (item.getItemId() == getRemoveMenuId()) {
      removeChecked();
    }

    return(super.onActionItemClicked(mode, item));
  }

  /**
   * @return the title to go in the action mode when we are
   *         in multiple-choice mode and the action mode is
   *         displayed
   */
  protected String getActionModeTitle() {
    return(null);
  }

  /**
   * @return the subtitle to go in the action mode when we
   *         are in multiple-choice mode and the action mode
   *         is displayed
   */
  protected String getActionModeSubtitle() {
    return(null);
  }

  @SuppressWarnings("unchecked")
  void add() {
    T model=createNewModel();

    SimplePageDescriptor desc=
        new SimplePageDescriptor(getModelTag(model),
                                 getModelTitle(model));

    getModelCollection().add(model);
    ((ModelPagerAdapter)getPagerAdapter()).add(desc);

    final int position=getModelCollection().size() - 1;

    setActivatedItem(position);

    getListView().post(new Runnable() {
      @Override
      public void run() {
        onDetailSelected(position);
      }
    });
  }

  @SuppressWarnings("unchecked")
  void removeChecked() {
    SparseBooleanArray checked=getListView().getCheckedItemPositions();

    ArrayList<Integer> positions=new ArrayList<Integer>();

    for (int i=0; i < checked.size(); i++) {
      if (checked.valueAt(i)) {
        positions.add(checked.keyAt(i));
      }
    }

    Collections.sort(positions, Collections.reverseOrder());

    for (int position : positions) {
      removeModel(getModelCollection().get(position));
      getModelCollection().remove(position);
      ((ModelPagerAdapter)getPagerAdapter()).remove(position);
    }

    getListView().clearChoices();
    ((ModelPagerAdapter)getPagerAdapter()).notifyDataSetChanged();
  }

  void updateActionModeTitle(ActionMode mode) {
    mode.setTitle(getActionModeTitle());
    mode.setSubtitle(getActionModeSubtitle());
  }

  /**
   * @return a menu resource (e.g.,
   *         R.menu.something_or_another) that will be
   *         inflated into the action bar
   */
  private int getOptionsMenuResource() {
    return(((MasterDetailController.Options)options).optionsMenuResource);
  }

  /**
   * @return the ID of a menu item (e.g., R.id.add) that
   *         represents the "add" operation
   */
  private int getAddMenuId() {
    return(((MasterDetailController.Options)options).addMenuId);
  }

  /**
   * @return the ID of a menu item (e.g., R.id.remove) that
   *         represents the "remove" operation
   */
  private int getRemoveMenuId() {
    return(((MasterDetailController.Options)options).removeMenuId);
  }

  class ModelPagerAdapter extends ArrayPagerAdapter<Fragment> {
    public ModelPagerAdapter(FragmentManager fragmentManager,
                             ArrayList<PageDescriptor> descriptors) {
      super(fragmentManager, descriptors);
    }

    @Override
    protected Fragment createFragment(PageDescriptor desc) {
      return(buildFragmentForTag(desc.getFragmentTag()));
    }
  }

  /**
   * Class for supplying configuration information to a
   * MasterDetailController.
   */
  public static class Options extends MasterDetailOptions {
    int optionsMenuResource=-1;
    int addMenuId=-1;
    int removeMenuId=-1;
  
    /**
     * @param resource
     *          a menu resource (e.g.,
     *          R.menu.something_or_another) that will be
     *          inflated into the action bar
     * @return the options object
     */
    public Options optionsMenuResource(int resource) {
      this.optionsMenuResource=resource;
  
      return(this);
    }
  
    /**
     * @param resource
     *          the ID of a menu item (e.g., R.id.add) that
     *          represents the "add" operation
     * @return the options object
     */
    public Options addMenuId(int resource) {
      this.addMenuId=resource;
  
      return(this);
    }
  
    /**
     * @param resource
     *          the ID of a menu item (e.g., R.id.remove)
     *          that represents the "remove" operation
     * @return the options object
     */
    public Options removeMenuId(int resource) {
      this.removeMenuId=resource;
  
      return(this);
    }
  }
}
