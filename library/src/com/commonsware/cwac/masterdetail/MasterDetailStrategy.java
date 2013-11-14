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

import android.app.Activity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import com.mobidevelop.widget.SplitPaneLayout;

abstract class MasterDetailStrategy {
  abstract View getContentView(PagerAdapter pages);

  protected MasterDetailHelper<?> helper=null;
  protected int pagerId=-1;

  MasterDetailStrategy(MasterDetailHelper<?> helper, int pagerId) {
    this.helper=helper;
    this.pagerId=pagerId;
  }

  void showMaster() {
    // default no-op
  }

  void clearDetail() {
    // default no-op
  }

  void showDetailMultipleChoice(int position) {
    // default no-op
  }

  void showDetailMultipleChoice(SparseBooleanArray positions) {
    // default no-op
  }

  boolean isMasterShowing() {
    return(true); // default always showing
  }

  boolean isDetailShowing() {
    return(true); // default always showing
  }

  boolean isActivatedStyle() {
    return(true); // should show list context
  }

  void showDetail(int position) {
    // no-op by default
  }

  ListView buildListView(Activity host) {
    ListView result=new ListView(host);

    result.setId(android.R.id.list);
    result.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

    return(result);
  }

  ViewPager buildViewPager(Activity host, PagerAdapter pages) {
    ViewPager pager=new ViewPager(host);

    pager.setId(pagerId);
    pager.setAdapter(pages);

    return(pager);
  }

  static class SinglePane extends MasterDetailStrategy implements
      OnPageChangeListener {
    private ListView master=null;
    private ViewPager detail=null;

    SinglePane(MasterDetailHelper<?> helper, int pagerId) {
      super(helper, pagerId);
    }

    @Override
    View getContentView(PagerAdapter pages) {
      FrameLayout result=new FrameLayout(helper.getHost());

      master=buildListView(helper.getHost());
      result.addView(master,
                     new FrameLayout.LayoutParams(
                                                  FrameLayout.LayoutParams.MATCH_PARENT,
                                                  FrameLayout.LayoutParams.MATCH_PARENT));

      View listEmptyView=helper.buildListEmptyView();

      result.addView(listEmptyView,
                     new FrameLayout.LayoutParams(
                                                  FrameLayout.LayoutParams.WRAP_CONTENT,
                                                  FrameLayout.LayoutParams.WRAP_CONTENT,
                                                  Gravity.CENTER));
      master.setEmptyView(listEmptyView);

      detail=buildViewPager(helper.getHost(), pages);
      detail.setOnPageChangeListener(this);
      showMaster();
      result.addView(detail,
                     new FrameLayout.LayoutParams(
                                                  FrameLayout.LayoutParams.MATCH_PARENT,
                                                  FrameLayout.LayoutParams.MATCH_PARENT));

      return(result);
    }

    @Override
    void showMaster() {
      master.setVisibility(View.VISIBLE);
      detail.setVisibility(View.GONE);
    }

    @Override
    void showDetail(int position) {
      master.setVisibility(View.GONE);
      detail.setVisibility(View.VISIBLE);
      detail.setCurrentItem(position, false);
    }

    @Override
    boolean isMasterShowing() {
      return(master.getVisibility() == View.VISIBLE);
    }

    @Override
    boolean isDetailShowing() {
      return(detail.getVisibility() == View.VISIBLE);
    }

    @Override
    boolean isActivatedStyle() {
      return(false);
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
      // no-op
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
      // no-op
    }

    @Override
    public void onPageSelected(int position) {
      helper.setActivatedItem(position);
    }
  }

  static class DualPane extends MasterDetailStrategy {
    private PagerAdapter detailSource=null;
    private ViewGroup detailTarget=null;
    private Object handle=null;
    private int lastPosition=-1;
    private View detailEmptyView=null;
    private View detailMultiChoice=null;

    DualPane(MasterDetailHelper<?> helper, int pagerId) {
      super(helper, pagerId);
    }

    @Override
    View getContentView(PagerAdapter pages) {
      detailEmptyView=helper.buildDetailEmptyView();

      SplitPaneLayout result=new SplitPaneLayout(helper.getHost());

      result.setOrientation(SplitPaneLayout.ORIENTATION_HORIZONTAL);
      result.setSplitterDrawable(helper.getResources()
                                       .getDrawable(R.drawable.divider_horizontal));

      FrameLayout listFrame=new FrameLayout(helper.getHost());
      ListView list=buildListView(helper.getHost());

      listFrame.addView(list,
                        new FrameLayout.LayoutParams(
                                                     FrameLayout.LayoutParams.MATCH_PARENT,
                                                     FrameLayout.LayoutParams.MATCH_PARENT));

      View listEmptyView=helper.buildListEmptyView();

      listFrame.addView(listEmptyView,
                        new FrameLayout.LayoutParams(
                                                     FrameLayout.LayoutParams.WRAP_CONTENT,
                                                     FrameLayout.LayoutParams.WRAP_CONTENT,
                                                     Gravity.CENTER));
      list.setEmptyView(listEmptyView);

      result.addView(listFrame,
                     new ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT));

      detailSource=pages;
      detailTarget=new FrameLayout(helper.getHost());

      // must have an ID to work for FragmentPagerAdapter
      detailTarget.setId(pagerId);
      detailTarget.addView(detailEmptyView,
                           new FrameLayout.LayoutParams(
                                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                                        Gravity.CENTER));

      result.addView(detailTarget,
                     new ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT));
      result.setSplitterPositionPercent(.3f);

      return(result);
    }

    @Override
    void showDetail(int position) {
      clearMultiChoice();
      detailEmptyView.setVisibility(View.GONE);
      detailSource.startUpdate(detailTarget);

      if (lastPosition > -1) {
        detailSource.destroyItem(detailTarget, lastPosition, handle);
      }

      lastPosition=position;

      handle=detailSource.instantiateItem(detailTarget, position);
      detailSource.finishUpdate(detailTarget);
    }

    @Override
    void clearDetail() {
      clearDetail(true);
    }

    @Override
    void showDetailMultipleChoice(int position) {
      showDetail(position);
    }

    @Override
    void showDetailMultipleChoice(SparseBooleanArray positions) {
      clearDetail(false);
      detailMultiChoice=helper.buildDetailMultipleChoiceView(positions);
      detailTarget.addView(detailMultiChoice,
                           new FrameLayout.LayoutParams(
                                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                                        FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void clearDetail(boolean showEmptyView) {
      clearMultiChoice();
      detailSource.startUpdate(detailTarget);

      if (lastPosition > -1) {
        detailSource.destroyItem(detailTarget, lastPosition, handle);
      }

      lastPosition=-1;

      detailSource.finishUpdate(detailTarget);

      if (showEmptyView) {
        detailEmptyView.setVisibility(View.VISIBLE);
      }
    }

    private void clearMultiChoice() {
      if (detailMultiChoice != null) {
        detailTarget.removeView(detailMultiChoice);
      }
    }
  }
}
