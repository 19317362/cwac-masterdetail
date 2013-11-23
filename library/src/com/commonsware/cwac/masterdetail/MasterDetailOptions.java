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

/**
 * Class for providing configuration options to a
 * MasterDetailHelper. Use MasterDetailOptions.Controller
 * for providing options to a MasterDetailController.
 */
public class MasterDetailOptions {
  int actionModeResource=-1;
  String modelFragmentTag=
      "OMG-YOU-BETTER-NOT-USE-THIS-TAG-FOR-ANYTHING-ELSE";
  int dualPaneWidthDip=720;

  /**
   * Call this to supply the int value of a menu resource
   * (R.menu.something_or_another) that you wish to use for
   * the action mode.
   * 
   * @param resource
   *          a menu resource value
   * @return the options object
   */
  public MasterDetailOptions actionMode(int resource) {
    this.actionModeResource=resource;

    return(this);
  }

  /**
   * Call this to supply the tag to be used for the model
   * fragment that retains your model collection across
   * configuration changes. The default value is something
   * that is unlikely to collide with any other tag that you
   * may be using.
   * 
   * @param tag
   *          some unique value to be used as the fragment
   *          tag for the fragment retaining the model
   * @return the options object
   */
  public MasterDetailOptions modelFragmentTag(String tag) {
    this.modelFragmentTag=tag;

    return(this);
  }

  /**
   * Call this to determine the "dividing line" between when
   * the helper uses single-pane mode versus dual-pane mode.
   * The default is 720dip.
   * 
   * @param dip
   *          the number of density-independent pixels,
   *          where if the device is wider than this value,
   *          use dual-pane mode instead of single-pane mode
   * @return the options object
   */
  public MasterDetailOptions dualPaneWidthDip(int dip) {
    this.dualPaneWidthDip=dip;

    return(this);
  }
}
