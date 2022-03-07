/*
    Copyright (C) 2022 k3b

    This file is part of de.k3b.android.zip2saf (https://github.com/k3b/Zip2Saf/)

    This program is free software: you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
    FOR A PARTICULAR PURPOSE. See the GNU General Public License
    for more details.

    You should have received a copy of the GNU General Public License along with
    this program. If not, see <http://www.gnu.org/licenses/>
    */

package de.k3b.android.zip2saf;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import de.k3b.android.zip2saf.databinding.MountinfoListContentBinding;
import de.k3b.zip2saf.data.MountInfo;
import de.k3b.zip2saf.data.MountInfoRepository;

public class MountInfoListItemRecyclerViewAdapter
        extends RecyclerView.Adapter<MountInfoListItemRecyclerViewAdapter.ViewHolder> {

    private final MountInfoRepository mRepository;
    private final OnClickHandler onClickHandler;

    MountInfoListItemRecyclerViewAdapter(MountInfoRepository repository, OnClickHandler onClickHandler) {
        this.mRepository = repository;
        this.onClickHandler = onClickHandler;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        MountinfoListContentBinding binding =
                MountinfoListContentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final MountInfo mountInfo = getByPosition(position);
        holder.mZipIdView.setText(mountInfo.zipId);

        try {
            holder.mFullPath.setText(URLDecoder.decode(mountInfo.uri, StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            holder.mFullPath.setText(mountInfo.uri);
        }

        holder.itemView.setTag(mountInfo);
        holder.itemView.setOnClickListener(itemView -> {
            onClickHandler.onListClick((MountInfo) itemView.getTag());
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /*
             * Context click listener to handle Right click events
             * from mice and trackpad input to provide a more native
             * experience on larger screen devices
             */
            holder.itemView.setOnContextClickListener(v -> {
                MountInfo item =
                        (MountInfo) holder.itemView.getTag();
                Toast.makeText(
                        holder.itemView.getContext(),
                        "Context click of item " + item.zipId,
                        Toast.LENGTH_LONG
                ).show();
                return true;
            });
        }
        holder.itemView.setOnLongClickListener(v -> {
            // Setting the item id as the clip data so that the drop target is able to
            // identify the id of the content
            ClipData.Item clipItem = new ClipData.Item(mountInfo.zipId);
            ClipData dragData = new ClipData(
                    ((MountInfo) v.getTag()).uri,
                    new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                    clipItem
            );

            if (Build.VERSION.SDK_INT >= 24) {
                v.startDragAndDrop(
                        dragData,
                        new View.DragShadowBuilder(v),
                        null,
                        0
                );
            } else {
                v.startDrag(
                        dragData,
                        new View.DragShadowBuilder(v),
                        null,
                        0
                );
            }
            return true;
        });
    }

    @NonNull
    private MountInfo getByPosition(int position) {
        MountInfo mountInfo = mRepository.getByPosition(position);
        if (mountInfo == null) mountInfo = MountInfo.EMPTY;
        return mountInfo;
    }

    @Override
    public int getItemCount() {
        return mRepository.getCount();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        // final TextView mPositionView;
        final TextView mZipIdView;
        final TextView mFullPath;

        ViewHolder(MountinfoListContentBinding binding) {
            super(binding.getRoot());
            mZipIdView = binding.zipId;
            mFullPath = binding.fullPath;
        }

    }

    public interface OnClickHandler {
        void onListClick(MountInfo item);
    }
}
