package com.ducktapedapps.updoot.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ducktapedapps.updoot.R;
import com.ducktapedapps.updoot.databinding.FragmentCommentsBinding;
import com.ducktapedapps.updoot.model.LinkData;
import com.ducktapedapps.updoot.ui.adapters.CommentsAdapter;
import com.ducktapedapps.updoot.utils.SwipeUtils;
import com.ducktapedapps.updoot.viewModels.CommentsVM;

import static com.ducktapedapps.updoot.BR.linkdata;

public class commentsFragment extends Fragment {
    private static final String TAG = "commentsFragment";
    private FragmentCommentsBinding binding;
    private CommentsVM viewModel;
    private CommentsAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_comments, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        if (getArguments() != null) {
            LinkData data = commentsFragmentArgs.fromBundle(getArguments()).getSubmissionData();
            binding.setVariable(linkdata, data);
            setUpRecyclerView();
            setUpViewModel(data);
        }
        return binding.getRoot();
    }

    private void setUpViewModel(LinkData data) {
        viewModel = new ViewModelProvider(this).get(CommentsVM.class);
        binding.setCommentsViewModel(viewModel);

        viewModel.loadComments(data.getSubredditName(), data.getId());

        viewModel.getAllComments().observe(this, commentDataList -> adapter.submitList(commentDataList));
    }

    private void setUpRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(commentsFragment.this.getContext());
        adapter = new CommentsAdapter(new ClickHandler());
        RecyclerView recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        new ItemTouchHelper(new SwipeUtils(commentsFragment.this.getContext(), new SwipeUtils.swipeActionCallback() {
            @Override
            public void performSlightLeftSwipeAction(int adapterPosition) {
            }

            @Override
            public void performSlightRightSwipeAction(int adapterPosition) {
            }

            @Override
            public void performLeftSwipeAction(int adapterPosition) {
            }

            @Override
            public void performRightSwipeAction(int adapterPosition) {
            }
        })).attachToRecyclerView(recyclerView);
    }

    public class ClickHandler {
        public void onClick(int index, boolean isExpanded) {
            if (!isExpanded)
                viewModel.toggleChildrenVisibility(index);
        }
    }

}