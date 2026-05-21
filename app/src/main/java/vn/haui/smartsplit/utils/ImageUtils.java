package vn.haui.smartsplit.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtils {

    /**
     * Converts a Uri to a compressed Base64 string.
     * Keeps the maximum dimension (width or height) to at most maxDimension.
     */
    public static String convertUriToBase64(ContentResolver resolver, Uri uri, int maxDimension) {
        try {
            InputStream input = resolver.openInputStream(uri);
            if (input == null) return null;
            
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            if (bitmap == null) return null;

            // Resize bitmap to keep it under 1MB Firestore limit and load fast
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxDimension || height > maxDimension) {
                float ratio = (float) width / height;
                if (width > height) {
                    width = maxDimension;
                    height = (int) (width / ratio);
                } else {
                    height = maxDimension;
                    width = (int) (height * ratio);
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            // Compress to JPEG with 70% quality
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            byte[] bytes = outputStream.toByteArray();
            
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads an image from a path (either a HTTP URL, a content Uri string, or a Base64 data string)
     * using Glide into an ImageView.
     */
    public static void loadImage(Context context, String path, ImageView imageView, int placeholderRes) {
        if (path == null || path.isEmpty()) {
            if (placeholderRes != 0) {
                imageView.setImageResource(placeholderRes);
            }
            return;
        }

        if (path.startsWith("data:image/jpeg;base64,") || path.startsWith("data:image/png;base64,")) {
            try {
                String base64Data = path.substring(path.indexOf(",") + 1);
                byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
                Glide.with(context)
                        .asBitmap()
                        .load(decodedString)
                        .placeholder(placeholderRes)
                        .into(imageView);
            } catch (Exception e) {
                e.printStackTrace();
                if (placeholderRes != 0) {
                    imageView.setImageResource(placeholderRes);
                }
            }
        } else {
            // Fallback for regular URLs or local file paths
            Glide.with(context)
                    .load(path)
                    .placeholder(placeholderRes)
                    .into(imageView);
        }
    }
}
