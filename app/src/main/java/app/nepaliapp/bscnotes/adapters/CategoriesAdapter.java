package app.nepaliapp.bscnotes.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity;
import app.nepaliapp.bscnotes.dashboard.fragments.ChatFragment;
import app.nepaliapp.bscnotes.dashboard.fragments.CourseFragment;
import app.nepaliapp.bscnotes.dashboard.fragments.HomeFragment;
import app.nepaliapp.bscnotes.dashboard.fragments.UnlockFragment;
import app.nepaliapp.bscnotes.dashboard.fragments.VideosFragment;
// TODO: Import other Tier 2 sub-fragments (e.g., ShopFragment, ShecmatricCompaniesFragment) as needed

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.ViewHolder> {

    private final Context context;
    private final JSONArray array;

    public CategoriesAdapter(Context context, JSONArray array) {
        this.context = context;
        this.array = array;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.categories_model, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject jsonObject = array.getJSONObject(position);
            Glide.with(context)
                    .load(jsonObject.optString("icon"))
                    .into(holder.icons);

            holder.txtView.setText(jsonObject.optString("name"));
            holder.cardView.setOnClickListener(v -> {
                boolean clickable = jsonObject.optBoolean("enable", true);
                if (clickable) {
                    changeByCategoryClick(jsonObject.optString("name"));
                } else {
                    Toast.makeText(context, "Not available in your country", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return array.length();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icons;
        TextView txtView;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icons = itemView.findViewById(R.id.imageView5);
            txtView = itemView.findViewById(R.id.txtIcon);
            cardView = itemView.findViewById(R.id.categoriesCards);
        }
    }

    /**
     * Opens a Tier 2 Deep Sub-Fragment (Stacked on BackStack)
     */
    private void openSubScreen(Fragment fragment) {
        if (context instanceof DashBoardManagerActivity) {
            ((DashBoardManagerActivity) context).openSubFragment(fragment);
        }
    }

    /**
     * Switches to one of the 5 Tier 1 Root Bottom-Nav Tabs
     */
    private void navigateToRootTab(int menuTabId) {
        if (context instanceof DashBoardManagerActivity) {
            ((DashBoardManagerActivity) context).navigateToBaseTab(menuTabId);
        }
    }

    private void changeByCategoryClick(String clickedName) {
        switch (clickedName.toLowerCase()) {
            case "course":
                // Tier 1 Root Tab: Courses
                navigateToRootTab(R.id.nav_courses);
                break;

            case "unlock":
                // Tier 1 Root Tab: Unlock / Tools
                navigateToRootTab(R.id.nav_unlock);
                break;

            case "videos":
                // Tier 1 Root Tab: Videos
                navigateToRootTab(R.id.nav_videos);
                break;

            case "chat":
                // Tier 1 Root Tab: Chat
                navigateToRootTab(R.id.nav_chat);
                break;


            default:
                // Fallback to Home Root Tab or show toast
                navigateToRootTab(R.id.nav_home);
                break;
        }
    }
}