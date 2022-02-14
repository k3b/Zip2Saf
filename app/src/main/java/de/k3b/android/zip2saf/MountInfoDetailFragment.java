package de.k3b.android.zip2saf;

import android.content.ClipData;
import android.os.Bundle;
import android.view.DragEvent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import de.k3b.android.zip2saf.data.MountInfo;
import de.k3b.android.zip2saf.data.MountInfoRepository;
import de.k3b.android.zip2saf.databinding.FragmentMountinfoDetailBinding;

/**
 * A fragment representing a single Mount Info detail screen.
 * This fragment is either contained in a {@link MountInfoListFragment}
 * in two-pane mode (on larger screen devices) or self-contained
 * on handsets.
 */
public class MountInfoDetailFragment extends Fragment {
    MountInfoRepository repository = MountInfoRepository.getInstance();

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The data content this fragment is presenting.
     */
    private MountInfo mountInfo;
    private CollapsingToolbarLayout detailToolbar;
    private TextView tvMountinfoDetail;

    private final View.OnDragListener dragListener = (v, event) -> {
        if (event.getAction() == DragEvent.ACTION_DROP) {
            ClipData.Item clipDataItem = event.getClipData().getItemAt(0);
            mountInfo = repository.getById(clipDataItem.getText().toString());
            updateContent();
        }
        return true;
    };

    private FragmentMountinfoDetailBinding binding;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MountInfoDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            if (arguments.containsKey(ARG_ITEM_ID)) {
                // Load the data content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                mountInfo = repository.getById(arguments.getString(ARG_ITEM_ID));
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentMountinfoDetailBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        detailToolbar = rootView.findViewById(R.id.toolbar_layout);
        tvMountinfoDetail = binding.mountinfoDetail;

        // Show the data content as text in a TextView & in the toolbar if available.
        updateContent();
        rootView.setOnDragListener(dragListener);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateContent() {
        if (mountInfo != null) {
            tvMountinfoDetail.setText(mountInfo.uri + "\n" + mountInfo.details);
            if (detailToolbar != null) {
                detailToolbar.setTitle(mountInfo.zipId);
            }
        }
    }
}