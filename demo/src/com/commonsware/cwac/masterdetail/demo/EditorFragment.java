/***
  Copyright (c) 2013 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
  
  From _The Busy Coder's Guide to Android Development_
    http://commonsware.com/Android
 */

package com.commonsware.cwac.masterdetail.demo;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class EditorFragment extends
    ContractFragment<EditorFragment.Contract> implements TextWatcher {
  private static final String KEY_KEY="k";

  static EditorFragment newInstance(String key) {
    EditorFragment frag=new EditorFragment();
    Bundle args=new Bundle();

    args.putString(KEY_KEY, key);
    frag.setArguments(args);

    return(frag);
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
    View result=inflater.inflate(R.layout.editor, container, false);
    EditText editor=(EditText)result.findViewById(R.id.editor);

    editor.setHint(getModel().toString());
    editor.setText(getModel().getText());
    editor.addTextChangedListener(this);

    return(result);
  }

  private Note getModel() {
    return(getContract().getNote(getArguments().getString(KEY_KEY)));
  }

  interface Contract {
    Note getNote(String key);
  }

  @Override
  public void afterTextChanged(Editable arg0) {
    getModel().setText(arg0.toString());
  }

  @Override
  public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                int arg3) {
    // no-op
  }

  @Override
  public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                            int arg3) {
    // no-op
  }
}