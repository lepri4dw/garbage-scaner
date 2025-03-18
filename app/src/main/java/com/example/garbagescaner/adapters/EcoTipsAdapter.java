package com.example.garbagescaner.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.garbagescaner.R;
import com.example.garbagescaner.models.EcoTip;

import java.util.List;

public class EcoTipsAdapter extends RecyclerView.Adapter<EcoTipsAdapter.ViewHolder> {

    private final List<EcoTip> tips;
    private final Context context;

    public EcoTipsAdapter(Context context, List<EcoTip> tips) {
        this.context = context;
        this.tips = tips;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_eco_tip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EcoTip tip = tips.get(position);

        holder.tvTitle.setText(tip.getTitle());
        holder.tvContent.setText(tip.getContent());
        holder.ivImage.setImageResource(tip.getImageRes());
    }

    @Override
    public int getItemCount() {
        return tips.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;
        TextView tvContent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivTipImage);
            tvTitle = itemView.findViewById(R.id.tvTipTitle);
            tvContent = itemView.findViewById(R.id.tvTipContent);
        }
    }
}