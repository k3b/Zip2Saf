package de.k3b.android.zip2saf;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import de.k3b.android.zip2saf.data.MountInfo;
import de.k3b.android.zip2saf.databinding.FragmentMountinfoListBinding;
import de.k3b.android.zip2saf.databinding.MountinfoListContentBinding;

import de.k3b.android.zip2saf.data.MountInfoRepository;

/**
 * A fragment representing a list of Mounted Zip Files. This fragment
 * has different presentations for handset and larger screen devices. On
 * handsets, the fragment presents a list of items, which when touched,
 * lead to a {@link MountInfoDetailFragment} representing
 * item details. On larger screens, the Navigation controller presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class MountInfoListFragment extends Fragment {

    /**
     * Method to intercept global key events in the
     * item list fragment to trigger keyboard shortcuts
     * Currently provides a toast when Ctrl + Z and Ctrl + F
     * are triggered
     */
    ViewCompat.OnUnhandledKeyEventListenerCompat unhandledKeyEventListenerCompat = (v, event) -> {
        if (event.getKeyCode() == KeyEvent.KEYCODE_Z && event.isCtrlPressed()) {
            Toast.makeText(
                    v.getContext(),
                    "Undo (Ctrl + Z) shortcut triggered",
                    Toast.LENGTH_LONG
            ).show();
            return true;
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_F && event.isCtrlPressed()) {
            Toast.makeText(
                    v.getContext(),
                    "Find (Ctrl + F) shortcut triggered",
                    Toast.LENGTH_LONG
            ).show();
            return true;
        }
        return false;
    };

    private FragmentMountinfoListBinding binding;

    private MountInfoRepository repository = MountInfoRepository.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMountinfoListBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.addOnUnhandledKeyEventListener(view, unhandledKeyEventListenerCompat);

        RecyclerView recyclerView = binding.mountinfoList;

        // Leaving this not using view binding as it relies on if the view is visible the current
        // layout configuration (layout, layout-sw600dp)
        View itemDetailFragmentContainer = view.findViewById(R.id.mountinfo_detail_nav_container);

        setupRecyclerView(recyclerView, itemDetailFragmentContainer);
    }

    private void setupRecyclerView(
            RecyclerView recyclerView,
            View itemDetailFragmentContainer
    ) {

        recyclerView.setAdapter(new MountInfoListItemRecyclerViewAdapter(
                repository,
                itemDetailFragmentContainer
        ));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class MountInfoListItemRecyclerViewAdapter
            extends RecyclerView.Adapter<MountInfoListItemRecyclerViewAdapter.ViewHolder> {

        private final MountInfoRepository mRepository;
        private final View mItemDetailFragmentContainer;

        MountInfoListItemRecyclerViewAdapter(MountInfoRepository repository,
                                             View itemDetailFragmentContainer) {
            this.mRepository = repository;
            mItemDetailFragmentContainer = itemDetailFragmentContainer;
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

            holder.itemView.setTag(mountInfo);
            holder.itemView.setOnClickListener(itemView -> {
                MountInfo item =
                        (MountInfo) itemView.getTag();
                Bundle arguments = new Bundle();
                arguments.putString(MountInfoDetailFragment.ARG_ITEM_ID, item.zipId);
                if (mItemDetailFragmentContainer != null) {
                    Navigation.findNavController(mItemDetailFragmentContainer)
                            .navigate(R.id.fragment_mountinfo_detail, arguments);
                } else {
                    Navigation.findNavController(itemView).navigate(R.id.show_mountinfo_detail, arguments);
                }
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

            ViewHolder(MountinfoListContentBinding binding) {
                super(binding.getRoot());
                mZipIdView = binding.zipId;
            }

        }
    }
}