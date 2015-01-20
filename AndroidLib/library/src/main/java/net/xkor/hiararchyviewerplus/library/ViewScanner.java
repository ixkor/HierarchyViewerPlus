package net.xkor.hiararchyviewerplus.library;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ViewScanner {

    private static Field field_mViewFlags;
    private static Field field_mCurrentAnimation;

    static {
        try {
            field_mViewFlags = View.class.getDeclaredField("mViewFlags");
            field_mViewFlags.setAccessible(true);
            field_mCurrentAnimation = View.class.getDeclaredField("mCurrentAnimation");
            field_mCurrentAnimation.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }
    }

    private JSONObject scan(View view) throws JSONException {
        String viewName = view.getClass().getName();
        if (view.getId() > 0) {
            viewName += "." + view.getContext().getResources().getResourceEntryName(view.getId());
        }
        viewName += view.hashCode();
        JSONObject result = new JSONObject();
        result.put("name", viewName);

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            ArrayList<ViewState> states = new ArrayList<>();
            JSONArray childs = new JSONArray();
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child.getVisibility() == View.VISIBLE)
                    childs.put(scan(child));
                states.add(new ViewState(child));
                ViewState.INVISIBLE.restore(child);
            }

            result.put("childs", childs);
            paintView(view, viewName);

            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                states.get(i).restore(child);
            }
        } else {
            paintView(view);
        }

        return result;
    }

    private Bitmap paintView(View view) {
        //view.destroyDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, )
        Log.d("imgPath", MediaStore.Images.Media.insertImage(view.getContext().getContentResolver(), bitmap, view.getClass().getName() + view.getId(), null));
        return bitmap;
    }

    private static class ViewState {
        private Object mViewFlags;
        private Object mCurrentAnimation;

        public static final ViewState INVISIBLE = new ViewState();

        private ViewState() {
            mViewFlags = View.INVISIBLE;
            mCurrentAnimation = null;
        }

        public ViewState(View view) {
            try {
                mViewFlags = field_mViewFlags.get(view);
                mCurrentAnimation = field_mCurrentAnimation.get(view);
            } catch (IllegalAccessException ignored) {
            }
        }

        public void restore(View view) {
            try {
                field_mViewFlags.set(view, mViewFlags);
                field_mCurrentAnimation.set(view, mCurrentAnimation);
            } catch (IllegalAccessException ignored) {
            }
        }
    }
}
