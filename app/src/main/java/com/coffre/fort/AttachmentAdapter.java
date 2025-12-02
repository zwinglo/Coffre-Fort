package com.coffre.fort;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AttachmentAdapter extends RecyclerView.Adapter<AttachmentAdapter.AttachmentViewHolder> {

    public interface OnAttachmentClickListener {
        void onAttachmentClick(MessageAttachment attachment);
    }

    private List<MessageAttachment> attachments = new ArrayList<>();
    private final OnAttachmentClickListener listener;

    public AttachmentAdapter(OnAttachmentClickListener listener) {
        this.listener = listener;
    }

    public void setAttachments(List<MessageAttachment> attachments) {
        this.attachments = attachments == null ? new ArrayList<>() : attachments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AttachmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attachment, parent, false);
        return new AttachmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttachmentViewHolder holder, int position) {
        MessageAttachment attachment = attachments.get(position);
        String fileName = new File(attachment.getFilePath()).getName();
        holder.nameTextView.setText(fileName);
        holder.infoTextView.setText(holder.itemView.getContext().getString(
                R.string.attachment_info_format,
                attachment.getContentType(),
                humanReadableSize(attachment.getSizeBytes())));
        holder.openButton.setOnClickListener(v -> listener.onAttachmentClick(attachment));
    }

    @Override
    public int getItemCount() {
        return attachments.size();
    }

    private String humanReadableSize(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int unitIndex = (int) (Math.log10(bytes) / Math.log10(1024));
        if (unitIndex >= units.length) {
            unitIndex = units.length - 1;
        }
        double scaled = bytes / Math.pow(1024, unitIndex);
        return String.format(java.util.Locale.US, "%.1f %s", scaled, units[unitIndex]);
    }

    static class AttachmentViewHolder extends RecyclerView.ViewHolder {
        final TextView nameTextView;
        final TextView infoTextView;
        final Button openButton;

        AttachmentViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.attachmentNameTextView);
            infoTextView = itemView.findViewById(R.id.attachmentInfoTextView);
            openButton = itemView.findViewById(R.id.attachmentOpenButton);
        }
    }
}
