package net.xkor.hierarchyviewerplus.library;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    public void scanHierarchy(View rootView, OutputStream outputStream) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        ScanResult scanResult = scan(rootView, zipOutputStream);

        ZipEntry entry = new ZipEntry("layers.json");
        zipOutputStream.putNextEntry(entry);
        zipOutputStream.write(new Gson().toJson(scanResult).getBytes());
        zipOutputStream.closeEntry();
    }

    private ScanResult scan(View view, ZipOutputStream zipOutputStream) throws IOException {
        ArrayList<ScanResult> childs = null;
        String imageName;

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            ArrayList<ViewState> states = new ArrayList<>();
            childs = new ArrayList<>();
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child.getVisibility() == View.VISIBLE)
                    childs.add(scan(child, zipOutputStream));
                states.add(new ViewState(child));
                ViewState.INVISIBLE.restore(child);
            }

            imageName = paintView(view, zipOutputStream);

            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                states.get(i).restore(child);
            }
        } else {
            imageName = paintView(view, zipOutputStream);
        }

        return new ScanResult(view, imageName, childs);
    }

    private String paintView(View view, ZipOutputStream zipOutputStream) throws IOException {
        String imageName = view.getClass().getName();
        if (view.getId() > 0) {
            imageName += "." + view.getContext().getResources().getResourceEntryName(view.getId());
        }
        imageName += "." + view.hashCode() + ".png";

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        ZipEntry entry = new ZipEntry(imageName);
        zipOutputStream.putNextEntry(entry);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOutputStream);
        zipOutputStream.closeEntry();

        return imageName;
    }

    private static class ViewState {
        public static final ViewState INVISIBLE = new ViewState();
        private Object mViewFlags;
        private Object mCurrentAnimation;

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

    private static class ScanResult implements Serializable {
        private int id;
        private String name;
        private String type;
        private String imageName;
        private Rect rect;
        private ArrayList<ScanResult> childs;

        private ScanResult(View view, String imageName, ArrayList<ScanResult> childs) {
            this.id = view.getId();
            try {
                this.name = view.getContext().getResources().getResourceEntryName(view.getId());
            } catch (Exception ignored) {
                this.name = "id:" + view.getId();
            }
            this.type = view.getClass().getName();
            this.imageName = imageName;
            this.rect = new Rect();
            view.getDrawingRect(this.rect);
            this.childs = childs;
        }
    }
}
