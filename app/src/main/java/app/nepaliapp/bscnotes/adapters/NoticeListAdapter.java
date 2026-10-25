package app.nepaliapp.bscnotes.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.List;

import app.nepaliapp.bscnotes.R;

public class NoticeListAdapter extends RecyclerView.Adapter<NoticeListAdapter.NoticeViewHolder> {

    public interface OnNoticeClickListener {
        void onNoticeClicked(JSONObject noticeObj);
    }

    private final List<JSONObject> noticeList;
    private final OnNoticeClickListener listener;

    public NoticeListAdapter(List<JSONObject> noticeList, OnNoticeClickListener listener) {
        this.noticeList = noticeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice_card, parent, false);
        return new NoticeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        JSONObject obj = noticeList.get(position);
        holder.tvHeading.setText(obj.optString("heading", "Notice"));
        holder.tvSubtext.setText(obj.optString("subtext", "Tap to read full notice"));

        JSONObject badge = obj.optJSONObject("badge");
        if (badge != null) {
            holder.tvBadge.setText(badge.optString("text", "NOTICE"));
            try {
                int textColor = Color.parseColor(badge.optString("textColor", "#E11D48"));
                int bgColor = Color.parseColor(badge.optString("bgColor", "#FFE4E6"));
                holder.tvBadge.setTextColor(textColor);

                GradientDrawable pill = new GradientDrawable();
                pill.setColor(bgColor);
                pill.setCornerRadius(14f);
                holder.tvBadge.setBackground(pill);
            } catch (Exception ignored) {}
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNoticeClicked(obj);
        });
    }

    @Override
    public int getItemCount() {
        return noticeList.size();
    }

    static class NoticeViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge, tvHeading, tvSubtext;

        public NoticeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadge = itemView.findViewById(R.id.tvItemNoticeBadge);
            tvHeading = itemView.findViewById(R.id.tvItemNoticeHeading);
            tvSubtext = itemView.findViewById(R.id.tvItemNoticeSubtext);
        }
    }
}