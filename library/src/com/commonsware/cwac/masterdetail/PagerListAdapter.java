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

import android.support.v4.view.PagerAdapter;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * A ListAdapter whose contents come from a PagerAdapter.
 * Basically, each page is represented by an element in the
 * list. This is useful in cases where you want to show the
 * pages in a list, such as in the master portion of a
 * master-detail UI.
 */
public class PagerListAdapter extends BaseAdapter {
  private PagerAdapter pages=null;
  private Assistant binder=null;

  /**
   * Constructor for a PagerListAdapter. Work to generate
   * the views is delegated to a PagerListAdapter.Assistant
   * implementation.
   * 
   * @param pages
   *          the PagerAdapter to be converted into a
   *          ListAdapter
   * @param binder
   *          the Assistant to create views for the list
   */
  public PagerListAdapter(PagerAdapter pages, Assistant binder) {
    this.pages=pages;
    this.binder=binder;

    pages.registerDataSetObserver(new Observer());
  }

  /*
   * (non-Javadoc)
   * 
   * @see android.widget.Adapter#getCount()
   */
  @Override
  public int getCount() {
    return(pages.getCount());
  }

  /*
   * (non-Javadoc)
   * 
   * @see android.widget.Adapter#getItem(int)
   */
  @Override
  public Object getItem(int position) {
    return(position);
  }

  /*
   * (non-Javadoc)
   * 
   * @see android.widget.Adapter#getItemId(int)
   */
  @Override
  public long getItemId(int position) {
    return(position);
  }

  /*
   * (non-Javadoc)
   * 
   * @see android.widget.Adapter#getView(int,
   * android.view.View, android.view.ViewGroup)
   */
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    return(binder.getView(position, convertView, parent));
  }

  private class Observer extends DataSetObserver {
    @Override
    public void onChanged() {
      notifyDataSetChanged();
    }

    @Override
    public void onInvalidated() {
      notifyDataSetInvalidated();
    }
  }

  /**
   * Interface for a view binder-style "assistant" that will
   * return the View to be shown for a given position within
   * the PagerAdapter.
   */
  public interface Assistant {
    View getView(int position, View convertView, ViewGroup parent);
  }
}
