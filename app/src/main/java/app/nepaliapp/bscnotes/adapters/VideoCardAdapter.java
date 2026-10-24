package app.nepaliapp.bscnotes.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import app.nepaliapp.bscnotes.R;

public class VideoCardAdapter extends RecyclerView.Adapter<VideoCardAdapter.ViewHolder> {

    private final Context context;
    private JSONArray array;
    private final OnVideoClickListener listener;

    public interface OnVideoClickListener {
        void onVideoClick(JSONObject video);
    }

    public VideoCardAdapter(Context context, JSONArray array, OnVideoClickListener listener) {
        this.context = context;
        this.array = (array != null) ? array : new JSONArray();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tutorial_video_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject object = array.optJSONObject(position);
        if (object == null) return;

        String title = object.optString("title", "Tutorial Video");
        String rawDesc = object.optString("description", "");
        String plainDesc = Html.fromHtml(rawDesc, Html.FROM_HTML_MODE_COMPACT).toString().trim();

        String thumbUrl = object.optString("thumbnailUrl", object.optString("image", ""));

        holder.title.setText(title);
        if (!plainDesc.isEmpty()) {
            holder.subTitle.setVisibility(View.VISIBLE);
            holder.subTitle.setText(plainDesc);
        } else {
            holder.subTitle.setVisibility(View.GONE);
        }

        Glide.with(holder.thumbnailView.getContext())
                .load(thumbUrl)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .into(holder.thumbnailView);

        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVideoClick(object);
            }
        });
    }

    public void updateData(JSONArray newArray) {
        this.array = (newArray != null) ? newArray : new JSONArray();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return array.length();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailView;
        TextView title, subTitle, author;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.videoTitle);
            subTitle = itemView.findViewById(R.id.videoSubTitle);
            author = itemView.findViewById(R.id.videoAuthor);
            thumbnailView = itemView.findViewById(R.id.videoThumbnail);
            cardView = itemView.findViewById(R.id.videoCardID);
        }
    }
}