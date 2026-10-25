package app.nepaliapp.bscnotes.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class NoticeBlockAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_HTML_TEXT = 1;
    private static final int TYPE_IMAGE = 2;
    private static final int TYPE_LINK_CARD = 3;
    private static final int TYPE_ACTION_BUTTON = 4;

    private final Context context;
    private final JSONObject headerMeta;
    private final List<JSONObject> blocks;

    public NoticeBlockAdapter(Context context, JSONObject headerMeta, List<JSONObject> blocks) {
        this.context = context;
        this.headerMeta = headerMeta;
        this.blocks = blocks;
    }

    @Override
    public int getItemCount() {
        return 1 + blocks.size(); // 0 is the Notice Header, 1..N are the contentBlocks
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        JSONObject block = blocks.get(position - 1);
        String type = block.optString("type", "html").toLowerCase(Locale.ROOT);
        switch (type) {
            case "image":
                return TYPE_IMAGE;
            case "link_card":
                return TYPE_LINK_CARD;
            case "action_button":
                return TYPE_ACTION_BUTTON;
            default:
                return TYPE_HTML_TEXT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(createHeaderLayout());
        } else if (viewType == TYPE_IMAGE) {
            return new ImageBlockViewHolder(createImageBlockLayout());
        } else if (viewType == TYPE_LINK_CARD) {
            return new LinkCardViewHolder(createLinkCardLayout());
        } else if (viewType == TYPE_ACTION_BUTTON) {
            return new ActionButtonViewHolder(createButtonLayout());
        } else {
            return new TextBlockViewHolder(createTextBlockLayout());
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position == 0 && holder instanceof HeaderViewHolder) {
            HeaderViewHolder h = (HeaderViewHolder) holder;
            h.tvTitle.setText(headerMeta.optString("heading", "Notice"));
            h.tvSub.setText(headerMeta.optString("subtext", ""));
            JSONObject badge = headerMeta.optJSONObject("badge");
            if (badge != null) {
                h.tvBadge.setText(badge.optString("text", "NOTICE"));
                try {
                    h.tvBadge.setTextColor(Color.parseColor(badge.optString("textColor", "#E11D48")));
                    GradientDrawable pill = new GradientDrawable();
                    pill.setColor(Color.parseColor(badge.optString("bgColor", "#FFE4E6")));
                    pill.setCornerRadius(dp(6));
                    h.tvBadge.setBackground(pill);
                } catch (Exception ignored) {}
            }
            return;
        }

        JSONObject block = blocks.get(position - 1);
        if (holder instanceof TextBlockViewHolder) {
            TextBlockViewHolder th = (TextBlockViewHolder) holder;
            String html = block.optString("value", "");
            th.textView.setText(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT));
        } else if (holder instanceof ImageBlockViewHolder) {
            ImageBlockViewHolder ih = (ImageBlockViewHolder) holder;
            String imgUrl = block.optString("value", "");
            String caption = block.optString("caption", "");
            Glide.with(context).load(imgUrl).into(ih.imageView);
            if (!caption.trim().isEmpty()) {
                ih.tvCaption.setVisibility(View.VISIBLE);
                ih.tvCaption.setText(caption);
            } else {
                ih.tvCaption.setVisibility(View.GONE);
            }
            ih.imageView.setOnClickListener(v -> openUrlSafely(imgUrl));
        } else if (holder instanceof LinkCardViewHolder) {
            LinkCardViewHolder lh = (LinkCardViewHolder) holder;
            lh.tvTitle.setText(block.optString("title", "Official Notice Link"));
            lh.tvSubtitle.setText(block.optString("subtitle", "Tap to open in browser"));
            String url = block.optString("url", "");
            lh.card.setOnClickListener(v -> openUrlSafely(url));
        } else if (holder instanceof ActionButtonViewHolder) {
            ActionButtonViewHolder bh = (ActionButtonViewHolder) holder;
            bh.button.setText("📄 " + block.optString("value", "Open Attachment"));
            String url = block.optString("url", "");
            bh.button.setOnClickListener(v -> openUrlSafely(url));
        }
    }

    private void openUrlSafely(String url) {
        if (url == null || url.trim().isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout createHeaderLayout() {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.setPadding(0, 0, 0, dp(16));

        TextView badge = new TextView(context);
        badge.setId(android.R.id.text1);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badge.setLayoutParams(badgeLp);

        TextView title = new TextView(context);
        title.setId(android.R.id.title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setPadding(0, dp(10), 0, dp(4));

        TextView sub = new TextView(context);
        sub.setId(android.R.id.summary);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sub.setTextColor(Color.parseColor("#64748B"));

        View divider = new View(context);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divLp.topMargin = dp(14);
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(Color.parseColor("#E2E8F0"));

        root.addView(badge);
        root.addView(title);
        root.addView(sub);
        root.addView(divider);
        return root;
    }

    private TextView createTextBlockLayout() {
        TextView tv = new TextView(context);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        tv.setLayoutParams(lp);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTextColor(Color.parseColor("#1E293B"));
        tv.setLineSpacing(dp(4), 1.0f);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        return tv;
    }

    private LinearLayout createImageBlockLayout() {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(16);
        box.setLayoutParams(lp);

        ImageView iv = new ImageView(context);
        iv.setId(android.R.id.icon);
        iv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        iv.setAdjustViewBounds(true);

        TextView caption = new TextView(context);
        caption.setId(android.R.id.text2);
        caption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        caption.setTextColor(Color.parseColor("#64748B"));
        caption.setGravity(Gravity.CENTER_HORIZONTAL);
        caption.setPadding(0, dp(6), 0, 0);

        box.addView(iv);
        box.addView(caption);
        return box;
    }

    private MaterialCardView createLinkCardLayout() {
        MaterialCardView card = new MaterialCardView(context);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        card.setLayoutParams(lp);
        card.setRadius(dp(12));
        card.setCardBackgroundColor(Color.parseColor("#F8FAFC"));
        card.setStrokeColor(Color.parseColor("#CBD5E1"));
        card.setStrokeWidth(dp(1));

        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView title = new TextView(context);
        title.setId(android.R.id.text1);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#2563EB"));

        TextView subtitle = new TextView(context);
        subtitle.setId(android.R.id.text2);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subtitle.setTextColor(Color.parseColor("#64748B"));

        inner.addView(title);
        inner.addView(subtitle);
        card.addView(inner);
        return card;
    }

    private MaterialButton createButtonLayout() {
        MaterialButton btn = new MaterialButton(context);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        btn.setLayoutParams(lp);
        btn.setAllCaps(false);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#2563EB"));
        btn.setCornerRadius(dp(10));
        return btn;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge, tvTitle, tvSub;
        HeaderViewHolder(View v) {
            super(v);
            tvBadge = v.findViewById(android.R.id.text1);
            tvTitle = v.findViewById(android.R.id.title);
            tvSub = v.findViewById(android.R.id.summary);
        }
    }

    static class TextBlockViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        TextBlockViewHolder(TextView v) { super(v); this.textView = v; }
    }

    static class ImageBlockViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvCaption;
        ImageBlockViewHolder(View v) {
            super(v);
            imageView = v.findViewById(android.R.id.icon);
            tvCaption = v.findViewById(android.R.id.text2);
        }
    }

    static class LinkCardViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvTitle, tvSubtitle;
        LinkCardViewHolder(MaterialCardView v) {
            super(v);
            card = v;
            tvTitle = v.findViewById(android.R.id.text1);
            tvSubtitle = v.findViewById(android.R.id.text2);
        }
    }

    static class ActionButtonViewHolder extends RecyclerView.ViewHolder {
        MaterialButton button;
        ActionButtonViewHolder(MaterialButton v) { super(v); this.button = v; }
    }
}