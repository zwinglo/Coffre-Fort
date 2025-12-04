package com.coffre.fort;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<Document> documents;
    private OnDocumentClickListener listener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    public interface OnDocumentClickListener {
        void onDocumentClick(Document document);
    }

    public DocumentAdapter(OnDocumentClickListener listener) {
        this.documents = new ArrayList<>();
        this.listener = listener;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        Document document = documents.get(position);
        holder.bind(document);
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {
        private final View categoryIndicatorView;
        private final TextView titleTextView;
        private final TextView categoryTextView;
        private final TextView timestampTextView;
        private final TextView contentTextView;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryIndicatorView = itemView.findViewById(R.id.categoryIndicatorView);
            titleTextView = itemView.findViewById(R.id.documentTitleTextView);
            categoryTextView = itemView.findViewById(R.id.documentCategoryTextView);
            timestampTextView = itemView.findViewById(R.id.documentTimestampTextView);
            contentTextView = itemView.findViewById(R.id.documentContentTextView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDocumentClick(documents.get(position));
                }
            });
        }

        public void bind(Document document) {
            Context context = itemView.getContext();
            titleTextView.setText(document.getTitle());

            String category = CategoryUtils.normalizeCategory(context, document.getCategory());
            categoryTextView.setText(category);

            int colorRes = getColorForCategory(context, category);
            int color = ContextCompat.getColor(context, colorRes);
            categoryTextView.setTextColor(color);
            categoryIndicatorView.setBackgroundColor(color);

            String formattedDate = dateFormat.format(new Date(document.getTimestamp()));
            timestampTextView.setText(context.getString(R.string.document_list_timestamp, formattedDate));

            if (document.hasAttachment()) {
                String attachmentName = document.getAttachmentName();
                if (TextUtils.isEmpty(attachmentName)) {
                    attachmentName = context.getString(R.string.unknown_file);
                }
                contentTextView.setText(context.getString(R.string.attachment_summary_short, attachmentName));
            } else if (!TextUtils.isEmpty(document.getContent())) {
                contentTextView.setText(document.getContent());
            } else {
                contentTextView.setText(R.string.no_preview_available);
            }
        }
    }

    private int getColorForCategory(Context context, String category) {
        if (category == null) {
            return R.color.category_other_color;
        }
        if (category.equals(context.getString(R.string.category_text))) {
            return R.color.category_text_color;
        } else if (category.equals(context.getString(R.string.category_images))) {
            return R.color.category_images_color;
        } else if (category.equals(context.getString(R.string.category_media))) {
            return R.color.category_media_color;
        } else if (CategoryUtils.isMessageCategory(context, category)) {
            return R.color.category_messages_color;
        }
        return R.color.category_other_color;
    }
}
