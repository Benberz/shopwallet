package com.shopwallet.ituchallenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Custom adapter for managing an expandable list view in an Android application.
 * This adapter is responsible for providing views for both group headers and child items.
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private final Context _context;
    private final List<String> _listDataHeader; // List of header titles
    private final HashMap<String, List<String>> _listDataChild; // Mapping of header titles to child items

    /**
     * Constructs an ExpandableListAdapter.
     *
     * @param context          The context in which the adapter is operating.
     * @param listDataHeader   List of group headers.
     * @param listChildData    Mapping of each header to its corresponding list of child items.
     */
    public ExpandableListAdapter(Context context, List<String> listDataHeader,
                                 HashMap<String, List<String>> listChildData) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        // Retrieve the child item based on group and child position
        return Objects.requireNonNull(this._listDataChild.get(this._listDataHeader.get(groupPosition)))
                .get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        // Return the ID of the child item
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final String childText = (String) getChild(groupPosition, childPosition);

        if (convertView == null) {
            // Inflate the layout for child items if it doesn't exist
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_item, null);
        }

        // Set the text for the child item view
        TextView txtListChild = (TextView) convertView
                .findViewById(R.id.lblListItem);
        txtListChild.setText(childText);

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        // Return the number of children under a specific group
        return Objects.requireNonNull(this._listDataChild.get(this._listDataHeader.get(groupPosition)))
                .size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        // Return the group header at the specified position
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        // Return the number of groups
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        // Return the ID of the group
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            // Inflate the layout for group headers if it doesn't exist
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.group_item, null);
        }

        // Set the text and icon for the group header view
        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        ImageView imgExpand = (ImageView) convertView.findViewById(R.id.imgExpand);
        lblListHeader.setText(headerTitle);

        // Set the expand/collapse icon based on whether the group is expanded or not
        if (isExpanded) {
            imgExpand.setImageResource(R.drawable.ic_baseline_expand_less_24);
        } else {
            imgExpand.setImageResource(R.drawable.ic_baseline_expand_more_24);
        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        // Return whether the adapter's IDs are stable across changes
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        // Return whether the child item is selectable
        return true;
    }
}