package ir.bereshtook.androidclient.util.chat;

import ir.bereshtook.androidclient.R;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.style.DynamicDrawableSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;


public final class MessageUtils {

    public static void convertSmileys(Context context, Spannable text, int size) {
        // remove all of our spans first
        SmileyImageSpan[] oldSpans = text.getSpans(0, text.length(), SmileyImageSpan.class);
        for (int i = 0; i < oldSpans.length; i++)
            text.removeSpan(oldSpans[i]);

        int len = text.length();
        int skip;
        for (int i = 0; i < len; i += skip) {
            skip = 0;
            int icon = 0;
            char c = text.charAt(i);
            if (Emoji.isSoftBankEmoji(c)) {
                try {
                    icon = Emoji.getSoftbankEmojiResource(c);
                    skip = 1;
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    // skip code
                }
            }

            if (icon == 0) {
                // softbank encoding not found, try extracting a code point
                int unicode = Character.codePointAt(text, i);
                // calculate skip count if not previously set
                if (skip == 0)
                    skip = Character.charCount(unicode);

                // avoid looking up if unicode < 0xFF
                if (unicode > 0xff)
                    icon = Emoji.getEmojiResource(context, unicode);
            }

            if (icon > 0) {
                // set emoji span
                SmileyImageSpan span = new SmileyImageSpan(context, icon, size);
                text.setSpan(span, i, i+skip, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE /* | Spannable.SPAN_COMPOSING*/);
            }
        }
    }
    
    public static QuickAction smileysPopup(Context context, AdapterView.OnItemClickListener listener) {
        QuickAction act = new QuickAction(context, R.layout.popup_smileys);

        ImageAdapter adapter = new ImageAdapter(context, Emoji.emojiGroups);
        act.setGridAdapter(adapter, listener);
        return act;
    }
    
    private static int getDensityPixel(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
    
    private static final class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private int[][] mTheme;

        public ImageAdapter(Context c, int[][] theme) {
            mContext = c;
            mTheme = theme;
        }

        public int getCount() {
            return mTheme[0].length;
        }

        /** Actually not used. */
        public Object getItem(int position) {
            int icon = Emoji.getEmojiResource(mContext, mTheme[0][position]);
            return mContext.getResources().getDrawable(icon);
        }

        public long getItemId(int position) {
            return mTheme[0][position];
        }

        public boolean hasStableIds() {
            return true;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);
                int size = getDensityPixel(mContext, SmileyImageSpan.SIZE_DIALOG/*+8*/);
                imageView.setLayoutParams(new GridView.LayoutParams(size, size));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                int padding = getDensityPixel(mContext, 4);
                imageView.setPadding(padding, padding, padding, padding);
            } else {
                imageView = (ImageView) convertView;
            }

            int icon = Emoji.getEmojiResource(mContext, mTheme[0][position]);
            imageView.setImageResource(icon);
            return imageView;
        }
    }
    
    
    public static final class SmileyImageSpan extends DynamicDrawableSpan {
        public static final int SIZE_DIALOG = 40;
        public static final int SIZE_EDITABLE = 24;
        public static final int SIZE_LISTITEM = 18;

        private final Context mContext;
        private final int mResourceId;
        private final int mSize;
        private Drawable mDrawable;

        public SmileyImageSpan(Context context, int resourceId, int size) {
            super(ALIGN_BOTTOM);
            mContext = context;
            mResourceId = resourceId;
            mSize = size;
        }

        public Drawable getDrawable() {
            Drawable drawable = null;

            if (mDrawable != null) {
                drawable = mDrawable;
            }
            else {
                try {
                    drawable = mContext.getResources().getDrawable(mResourceId);
                    int size = getDensityPixel(mContext, mSize);
                    drawable.setBounds(0, 0, size, size);
                } catch (Exception e) {
                    Log.e("sms", "Unable to find resource: " + mResourceId);
                }
            }

            return drawable;
        }
    }

}
