package com.coffre.fort;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<Document> documents;
    private OnDocumentClickListener listener;

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
        private TextView titleTextView;
        private TextView categoryTextView;
        private TextView contentTextView;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.documentTitleTextView);
            categoryTextView = itemView.findViewById(R.id.documentCategoryTextView);
            contentTextView = itemView.findViewById(R.id.documentContentTextView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDocumentClick(documents.get(position));
                }
            });
        }

        public void bind(Document document) {
            titleTextView.setText(document.getTitle());
            categoryTextView.setText(document.getCategory());
            contentTextView.setText(document.getContent());
        }
    }
}
