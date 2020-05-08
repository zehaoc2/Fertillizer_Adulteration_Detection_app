package edu.illinois.fertilizeradulterationdetection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ItemView>{
    private ArrayList<Store> instances;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);

        void onDeleteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public static class ItemView extends RecyclerView.ViewHolder {
        public TextView storeName;
        public Button storeSelectButton;

        public ItemView(View itemView, final OnItemClickListener listener) {
            super(itemView);
            storeName = itemView.findViewById(R.id.store_name);
//            storeSelectButton = itemView.findViewById(R.id.store_button);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    public GalleryAdapter(ArrayList<Store> list) {
        instances = list;
    }

    @Override
    public ItemView onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_item, parent, false);
        ItemView evh = new ItemView(v, mListener);
        return evh;
    }

    @Override
    public void onBindViewHolder(ItemView holder, int position) {
        Store currentItem = instances.get(position);

        holder.storeName.setText(currentItem.getName());
    }

    @Override
    public int getItemCount() {
        return instances.size();
    }
}
