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

package com.commonsware.cwac.masterdetail.demo;

import android.annotation.SuppressLint;
import android.app.Fragment;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import com.commonsware.cwac.masterdetail.MasterDetailController;

public class NoteHelper extends MasterDetailController<Note> {
  @Override
  protected ArrayList<Note> buildModelCollection() {
    ArrayList<Note> result=new ArrayList<Note>();

    result.add(new Note(buildKey(), buildTitle()));

    return(result);
  }

  @Override
  protected String getModelTag(Note model) {
    return(model.getKey());
  }

  @Override
  protected Fragment buildFragmentForTag(String tag) {
    return(EditorFragment.newInstance(tag));
  }

  @Override
  protected int getRemoveMenuId() {
    return(R.id.remove);
  }
  
  @Override
  protected int getActionModeResource() {
    return(R.menu.action_mode);
  }

  @SuppressLint("DefaultLocale")
  @Override
  protected String getActionModeTitle() {
    return(String.format("Modifying %d item(s)",
                         getListView().getCheckedItemCount()));
  }
  
  @Override
  protected int getOptionsMenuResource() {
    return(R.menu.actions);
  }
  
  @Override
  protected int getAddMenuId() {
    return(R.id.add);
  }
  
  @Override
  protected Note createNewModel() {
    return(new Note(buildKey(), buildTitle()));
  }
  
  @Override
  protected void removeModel(Note model) {
    // TODO
  }

  public Note getNote(String key) {
    for (Note note : getModelCollection()) {
      if (key.equals(note.getKey())) {
        return(note);
      }
    }

    return(null);
  }

  static String buildKey() {
    return(UUID.randomUUID().toString());
  }

  static String buildTitle() {
    return(new Date().toString());
  }
}
