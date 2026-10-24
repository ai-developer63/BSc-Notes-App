package app.nepaliapp.bscnotes.dashboard.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.nepaliapp.bscnotes.R;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerViewMessages;
    private EditText etMessageInput;
    private Button btnSend;

    private List<String> messageList;
    private RecyclerView.Adapter<MessageViewHolder> messageAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Initialize views
        recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);
        etMessageInput = view.findViewById(R.id.etMessageInput);
        btnSend = view.findViewById(R.id.btnSend);

        messageList = new ArrayList<>();

        // Setup RecyclerView
        setupRecyclerView();

        // Handle Send Button click
        btnSend.setOnClickListener(v -> {
            String messageText = etMessageInput.getText().toString().trim();
            if (!messageText.isEmpty()) {
                messageList.add(messageText);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                etMessageInput.getText().clear();
            }
        });

        return view;
    }

    private void setupRecyclerView() {
        messageAdapter = new RecyclerView.Adapter<MessageViewHolder>() {
            @NonNull
            @Override
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TextView textView = new TextView(parent.getContext());
                textView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                textView.setPadding(24, 16, 24, 16);
                textView.setTextSize(16f);
                textView.setTextColor(Color.BLACK);
                return new MessageViewHolder(textView);
            }

            @Override
            public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
                ((TextView) holder.itemView).setText(messageList.get(position));
            }

            @Override
            public int getItemCount() {
                return messageList.size();
            }
        };

        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    // ViewHolder class
    private static class MessageViewHolder extends RecyclerView.ViewHolder {
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}