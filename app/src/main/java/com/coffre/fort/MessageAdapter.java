package com.coffre.fort;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    public interface OnMessageClickListener {
        void onMessageClick(VaultMessage message);
    }

    public interface SelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    private final Context context;
    private final OnMessageClickListener listener;
    private List<VaultMessage> messages = new ArrayList<>();
    private final Set<Long> selectedMessageIds = new HashSet<>();
    private SelectionChangeListener selectionChangeListener;

    public MessageAdapter(Context context, OnMessageClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setMessages(List<VaultMessage> messages) {
        this.messages = messages == null ? new ArrayList<>() : messages;
        selectedMessageIds.clear();
        notifySelectionChanged();
        notifyDataSetChanged();
    }

    public void setSelectionChangeListener(SelectionChangeListener selectionChangeListener) {
        this.selectionChangeListener = selectionChangeListener;
    }

    public List<VaultMessage> getSelectedMessages() {
        List<VaultMessage> selected = new ArrayList<>();
        for (VaultMessage message : messages) {
            if (selectedMessageIds.contains(message.getLocalId())) {
                selected.add(message);
            }
        }
        return selected;
    }

    public void clearSelection() {
        selectedMessageIds.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        VaultMessage message = messages.get(position);
        String address = TextUtils.isEmpty(message.getAddress())
                ? context.getString(R.string.sms_document_title_unknown)
                : message.getAddress();
        holder.addressTextView.setText(address);
        holder.dateTextView.setText(MessageFormatter.formatTimestamp(message.getDate()));
        holder.typeTextView.setText(getTypeLabel(message.getProviderType()));

        String body = TextUtils.isEmpty(message.getBody())
                ? context.getString(R.string.sms_email_empty_body_placeholder)
                : message.getBody();
        holder.bodyTextView.setText(body);

        holder.attachmentTextView.setVisibility(message.hasAttachments() ? View.VISIBLE : View.GONE);

        holder.selectCheckBox.setOnCheckedChangeListener(null);
        boolean isSelected = selectedMessageIds.contains(message.getLocalId());
        holder.selectCheckBox.setChecked(isSelected);
        holder.selectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedMessageIds.add(message.getLocalId());
            } else {
                selectedMessageIds.remove(message.getLocalId());
            }
            notifySelectionChanged();
        });

        holder.itemView.setOnClickListener(v -> listener.onMessageClick(message));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String getTypeLabel(String providerType) {
        if (MessageSyncManager.PROVIDER_MMS.equals(providerType)) {
            return context.getString(R.string.message_type_mms);
        }
        return context.getString(R.string.message_type_sms);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView addressTextView;
        final TextView dateTextView;
        final TextView bodyTextView;
        final TextView typeTextView;
        final TextView attachmentTextView;
        final CheckBox selectCheckBox;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            addressTextView = itemView.findViewById(R.id.messageAddressTextView);
            dateTextView = itemView.findViewById(R.id.messageDateTextView);
            bodyTextView = itemView.findViewById(R.id.messageBodyTextView);
            typeTextView = itemView.findViewById(R.id.messageTypeTextView);
            attachmentTextView = itemView.findViewById(R.id.messageAttachmentTextView);
            selectCheckBox = itemView.findViewById(R.id.messageSelectCheckBox);
        }
    }

    private void notifySelectionChanged() {
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedMessageIds.size());
        }
    }
}
