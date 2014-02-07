/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.prompts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoEvent;
import org.mozilla.gecko.R;
import org.mozilla.gecko.util.ThreadUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class Prompt implements OnClickListener, OnCancelListener, OnItemClickListener {
    private static final String LOGTAG = "GeckoPromptService";

    private String[] mButtons;
    private PromptInput[] mInputs;
    private AlertDialog mDialog;

    private final LayoutInflater mInflater;
    private final Context mContext;
    private PromptCallback mCallback;
    private String mGuid;

    private static boolean mInitialized = false;
    private static int mGroupPaddingSize;
    private static int mLeftRightTextWithIconPadding;
    private static int mTopBottomTextWithIconPadding;
    private static int mIconTextPadding;
    private static int mIconSize;
    private static int mInputPaddingSize;
    private static int mMinRowSize;
    private PromptListAdapter mAdapter;

    public Prompt(Context context, PromptCallback callback) {
        this(context);
        mCallback = callback;
    }

    private Prompt(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);

        if (!mInitialized) {
            Resources res = mContext.getResources();
            mGroupPaddingSize = (int) (res.getDimension(R.dimen.prompt_service_group_padding_size));
            mLeftRightTextWithIconPadding = (int) (res.getDimension(R.dimen.prompt_service_left_right_text_with_icon_padding));
            mTopBottomTextWithIconPadding = (int) (res.getDimension(R.dimen.prompt_service_top_bottom_text_with_icon_padding));
            mIconTextPadding = (int) (res.getDimension(R.dimen.prompt_service_icon_text_padding));
            mIconSize = (int) (res.getDimension(R.dimen.prompt_service_icon_size));
            mInputPaddingSize = (int) (res.getDimension(R.dimen.prompt_service_inputs_padding));
            mMinRowSize = (int) (res.getDimension(R.dimen.prompt_service_min_list_item_height));
            mInitialized = true;
        }
    }

    private View applyInputStyle(View view, PromptInput input) {
        // Don't add padding to color picker views
        if (input.canApplyInputStyle()) {
            view.setPadding(mInputPaddingSize, 0, mInputPaddingSize, 0);
        }
        return view;
    }

    public void show(JSONObject message) {
        processMessage(message);
    }


    public void show(String title, String text, PromptListItem[] listItems, boolean multipleSelection) {
        show(title, text, listItems, multipleSelection, null);
    }

    public void show(String title, String text, PromptListItem[] listItems, boolean multipleSelection, boolean[] selected) {
        ThreadUtils.assertOnUiThread();

        GeckoAppShell.getLayerView().abortPanning();

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }

        if (!TextUtils.isEmpty(text)) {
            builder.setMessage(text);
        }

        // Because lists are currently added through the normal Android AlertBuilder interface, they're
        // incompatible with also adding additional input elements to a dialog.
        if (listItems != null && listItems.length > 0) {
            addListItems(builder, listItems, multipleSelection, selected);
        } else if (!addInputs(builder)) {
            // If we failed to add any requested input elements, don't show the dialog
            return;
        }

        int length = mButtons == null ? 0 : mButtons.length;
        if (length > 0) {
            builder.setPositiveButton(mButtons[0], this);
            if (length > 1) {
                builder.setNeutralButton(mButtons[1], this);
                if (length > 2) {
                    builder.setNegativeButton(mButtons[2], this);
                }
            }
        }

        mDialog = builder.create();
        mDialog.setOnCancelListener(Prompt.this);
        mDialog.show();
    }

    public void setButtons(String[] buttons) {
        mButtons = buttons;
    }

    public void setInputs(PromptInput[] inputs) {
        mInputs = inputs;
    }

    /* Adds to a result value from the lists that can be shown in dialogs.
     *  Will set the selected value(s) to the button attribute of the
     *  object that's passed in. If this is a multi-select dialog, sets a
     *  selected attribute to an array of booleans.
     */
    private void addListResult(final JSONObject result, int which) {
        try {
            boolean[] selectedItems = mAdapter.getSelected();
            if (selectedItems != null) {
                JSONArray selected = new JSONArray();
                for (int i = 0; i < selectedItems.length; i++) {
                    if (selectedItems[i]) {
                        selected.put(i);
                    }
                }
                result.put("list", selected);
            } else {
                // Mirror the selected array from multi choice for consistency.
                JSONArray selected = new JSONArray();
                selected.put(which);
                result.put("list", selected);
                // Make the button be the index of the select item.
                result.put("button", which);
            }
        } catch(JSONException ex) { }
    }

    /* Adds to a result value from the inputs that can be shown in dialogs.
     * Each input will set its own value in the result.
     */
    private void addInputValues(final JSONObject result) {
        try {
            if (mInputs != null) {
                for (int i = 0; i < mInputs.length; i++) {
                    result.put(mInputs[i].getId(), mInputs[i].getValue());
                }
            }
        } catch(JSONException ex) { }
    }

    /* Adds the selected button to a result. This should only be called if there
     * are no lists shown on the dialog, since they also write their results to the button
     * attribute.
     */
    private void addButtonResult(final JSONObject result, int which) {
        int button = -1;
        switch(which) {
            case DialogInterface.BUTTON_POSITIVE : button = 0; break;
            case DialogInterface.BUTTON_NEUTRAL  : button = 1; break;
            case DialogInterface.BUTTON_NEGATIVE : button = 2; break;
        }
        try {
            result.put("button", button);
        } catch(JSONException ex) { }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        ThreadUtils.assertOnUiThread();
        JSONObject ret = new JSONObject();
        try {
            ListView list = mDialog.getListView();
            addButtonResult(ret, which);
            addInputValues(ret);

            if (list != null || mAdapter.getSelected() != null) {
                addListResult(ret, which);
            }
        } catch(Exception ex) {
            Log.i(LOGTAG, "Error building return: " + ex);
        }

        if (dialog != null) {
            dialog.dismiss();
        }

        finishDialog(ret);
    }

    /* Adds a set of list items to the prompt. This can be used for either context menu type dialogs, checked lists,
     * or multiple selection lists. If mAdapter.selected is set in the prompt before addListItems is called, the items will be
     * shown with "checkmarks" on their left side.
     *
     * @param builder
     *        The alert builder currently building this dialog.
     * @param listItems
     *        The items to add.
     * @param multipleSelection
     *        If true, and mAdapter.getSelected is defined to be a non-zero-length list, the list will show checkmarks on the
     *        left and allow multiple selection. 
     * @param selected
     *        A boolean array indicating whether a rows is selected or not. If this is a single select list, only the first true element will be used.
    */
    private void addListItems(AlertDialog.Builder builder, PromptListItem[] listItems, boolean multipleSelection, boolean[] selected) {
        if (selected != null && selected.length > 0) {
            if (multipleSelection) {
                addMultiSelectList(builder, listItems, selected);
            } else {
                addSingleSelectList(builder, listItems, selected);
            }
        } else {
            addMenuList(builder, listItems);
        }
    }

    /* Shows a multi-select list with checkmarks on the side. Android doesn't support using an adapter for
     * multi-choice lists by default so instead we insert our own custom list so that we can do fancy things
     * to the rows like disabling/indenting them.
     *
     * @param builder
     *        The alert builder currently building this dialog.
     * @param listItems
     *        The items to add.
     */
    private void addMultiSelectList(AlertDialog.Builder builder, PromptListItem[] listItems, boolean[] selected) {
        mAdapter = new PromptListAdapter(mContext, R.layout.select_dialog_multichoice, listItems);
        mAdapter.setSelected(selected);

        ListView listView = (ListView) mInflater.inflate(R.layout.select_dialog_list, null);
        listView.setOnItemClickListener(this);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(mAdapter);

        builder.setInverseBackgroundForced(true);
        builder.setView(listView);
    }

    /* Shows a single-select list with radio boxes on the side.
     *
     * @param builder
     *        the alert builder currently building this dialog.
     * @param listItems
     *        The items to add.
     */
    private void addSingleSelectList(AlertDialog.Builder builder, PromptListItem[] listItems, boolean[] selected) {
        mAdapter = new PromptListAdapter(mContext, R.layout.select_dialog_singlechoice, listItems);
        // For single select, we only maintain a single index of the selected row
        int selectedIndex = -1;
        for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
                selectedIndex = i;
                break;
            }
        }

        builder.setSingleChoiceItems(mAdapter, selectedIndex, this);
    }

    /* Shows a single-select list.
     *
     * @param builder
     *        the alert builder currently building this dialog.
     * @param listItems
     *        The items to add.
     */
    private void addMenuList(AlertDialog.Builder builder, PromptListItem[] listItems) {
        mAdapter = new PromptListAdapter(mContext, android.R.layout.simple_list_item_1, listItems);
        builder.setAdapter(mAdapter, this);
    }


    /* Wraps an input in a linearlayout. We do this so that we can set padding that appears outside the background
     * drawable for the view.
     */
    private View wrapInput(final PromptInput input) {
        final LinearLayout linearLayout = new LinearLayout(mContext);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        applyInputStyle(linearLayout, input);

        linearLayout.addView(input.getView(mContext));

        return linearLayout;
    }

    /* Add the requested input elements to the dialog.
     *
     * @param builder
     *        the alert builder currently building this dialog.
     * @return 
     *         return true if the inputs were added successfully. This may fail
     *         if the requested input is compatible with this Android verison
     */
    private boolean addInputs(AlertDialog.Builder builder) {
        int length = mInputs == null ? 0 : mInputs.length;
        if (length == 0) {
            return true;
        }

        try {
            View root = null;
            boolean scrollable = false; // If any of the innuts are scrollable, we won't wrap this in a ScrollView

            if (length == 1) {
                root = wrapInput(mInputs[0]);
                scrollable |= mInputs[0].getScrollable();
            } else if (length > 1) {
                LinearLayout linearLayout = new LinearLayout(mContext);
                linearLayout.setOrientation(LinearLayout.VERTICAL);

                for (int i = 0; i < length; i++) {
                    View content = wrapInput(mInputs[i]);
                    linearLayout.addView(content);
                    scrollable |= mInputs[i].getScrollable();
                }

                root = linearLayout;
            }

            if (scrollable) {
                builder.setView(root);
            } else {
                ScrollView view = new ScrollView(mContext);
                view.addView(root);
                builder.setView(view);
            }
        } catch(Exception ex) {
            Log.e(LOGTAG, "Error showing prompt inputs", ex);
            // We cannot display these input widgets with this sdk version,
            // do not display any dialog and finish the prompt now.
            cancelDialog();
            return false;
        }

        return true;
    }

    /* AdapterView.OnItemClickListener
     * Called when a list item is clicked
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ThreadUtils.assertOnUiThread();
        mAdapter.toggleSelected(position);
    }

    /* @DialogInterface.OnCancelListener
     * Called when the user hits back to cancel a dialog. The dialog will close itself when this
     * ends. Setup the correct return values here.
     *
     * @param aDialog
     *          A dialog interface for the dialog that's being closed.
     */
    @Override
    public void onCancel(DialogInterface aDialog) {
        ThreadUtils.assertOnUiThread();
        cancelDialog();
    }

    /* Called in situations where we want to cancel the dialog . This can happen if the user hits back,
     *  or if the dialog can't be created because of invalid JSON.
     */
    private void cancelDialog() {
        JSONObject ret = new JSONObject();
        try {
            ret.put("button", -1);
        } catch(Exception ex) { }
        addInputValues(ret);
        finishDialog(ret);
    }

    /* Called any time we're closing the dialog to cleanup and notify listeners that the dialog
     * is closing.
     */
    public void finishDialog(JSONObject aReturn) {
        mInputs = null;
        mButtons = null;
        mDialog = null;
        try {
            aReturn.put("guid", mGuid);
        } catch(JSONException ex) { }

        // poke the Gecko thread in case it's waiting for new events
        GeckoAppShell.sendEventToGecko(GeckoEvent.createNoOpEvent());

        if (mCallback != null) {
            mCallback.onPromptFinished(aReturn.toString());
        }
        mGuid = null;
    }

    /* Handles parsing the initial JSON sent to show dialogs
     */
    private void processMessage(JSONObject geckoObject) {
        String title = geckoObject.optString("title");
        String text = geckoObject.optString("text");
        mGuid = geckoObject.optString("guid");

        mButtons = getStringArray(geckoObject, "buttons");

        JSONArray inputs = getSafeArray(geckoObject, "inputs");
        mInputs = new PromptInput[inputs.length()];
        for (int i = 0; i < mInputs.length; i++) {
            try {
                mInputs[i] = PromptInput.getInput(inputs.getJSONObject(i));
            } catch(Exception ex) { }
        }

        PromptListItem[] menuitems = PromptListItem.getArray(geckoObject.optJSONArray("listitems"));
        boolean multiple = geckoObject.optBoolean("multiple");
        show(title, text, menuitems, multiple, getBooleanArray(geckoObject, "selected"));
    }

    private static JSONArray getSafeArray(JSONObject json, String key) {
        try {
            return json.getJSONArray(key);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static String[] getStringArray(JSONObject aObject, String aName) {
        JSONArray items = getSafeArray(aObject, aName);
        int length = items.length();
        String[] list = new String[length];
        for (int i = 0; i < length; i++) {
            try {
                list[i] = items.getString(i);
            } catch(Exception ex) { }
        }
        return list;
    }

    private static boolean[] getBooleanArray(JSONObject aObject, String aName) {
        JSONArray items = new JSONArray();
        try {
            items = aObject.getJSONArray(aName);
        } catch(Exception ex) { return null; }
        int length = items.length();
        boolean[] list = new boolean[length];
        for (int i = 0; i < length; i++) {
            try {
                list[i] = items.getBoolean(i);
            } catch(Exception ex) { }
        }
        return list;
    }

    public interface PromptCallback {
        public void onPromptFinished(String jsonResult);
    }
}
