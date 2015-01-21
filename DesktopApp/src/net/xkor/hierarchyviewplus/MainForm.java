package net.xkor.hierarchyviewplus;

import com.google.gson.Gson;
import com.sun.j3d.exp.swing.JCanvas3D;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;

import javax.imageio.ImageIO;
import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.zip.ZipFile;

/**
 * Created by xkor on 20.01.15.
 */
public class MainForm {
    private static final float mul = 2000f;
    private JPanel mainPanel;
    private JCanvas3D c3d;
    private JButton button1;
    private JPanel container;
    private SimpleUniverse universe;

    public static void main(String[] args) {
        JFrame frame = new JFrame("MainForm");
        final MainForm form = new MainForm();
        frame.setContentPane(form.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        form.createScene();
    }

    private void createScene() {
        universe = new SimpleUniverse(c3d.getOffscreenCanvas3D());

        BranchGroup branch = new BranchGroup();

        // Make a changeable 3D transform
        TransformGroup trans = new TransformGroup();
        trans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        branch.addChild(trans);

        // Make a shape
//        ColorCube demo = new ColorCube(0.4);
//        trans.addChild(demo);

        // Make a behavor to spin the shape
        Alpha spinAlpha = new Alpha(-1, 40000);
        RotationInterpolator spinner = new RotationInterpolator(spinAlpha, trans);
        spinner.setSchedulingBounds(new BoundingSphere(new Point3d(), 1000.0));
        trans.addChild(spinner);

        try {
            loadHierarchy(trans);
        } catch (IOException e) {
            e.printStackTrace();
        }

        branch.compile();
        universe.getViewingPlatform().setNominalViewingTransform();
        universe.addBranchGraph(branch);
    }

    private void loadHierarchy(TransformGroup group) throws IOException {
        ZipFile zipFile = new ZipFile("/home/xkor/Dropbox/HierarchyViewerPlus.zip");
        InputStream layersStream = zipFile.getInputStream(zipFile.getEntry("layers.json"));
        ScanResult layers = new Gson().fromJson(new InputStreamReader(layersStream), ScanResult.class);

        Rect rect = layers.getRect();
        Transform3D transform = new Transform3D();
        transform.setTranslation(new Vector3f(-rect.right / mul / 2, rect.bottom / mul / 2, 0f));
        TransformGroup childGroup = new TransformGroup(transform);
        group.addChild(childGroup);

        loadView(layers, zipFile, childGroup);
    }

    private void loadView(ScanResult scanResult, ZipFile zipFile, TransformGroup group) throws IOException {
        Appearance appearance = createTexture(scanResult.getImageName(), zipFile);
        Rect rect = scanResult.getRect();

        QuadArray plane = new QuadArray(4, QuadArray.COORDINATES | GeometryArray.COLOR_4);
        plane.setCoordinate(0, new Point3f(rect.left / mul, -rect.top / mul, 0f));
        plane.setCoordinate(1, new Point3f(rect.right / mul, -rect.top / mul, 0f));
        plane.setCoordinate(2, new Point3f(rect.right / mul, -rect.bottom / mul, 0f));
        plane.setCoordinate(3, new Point3f(rect.left / mul, -rect.bottom / mul, 0f));

        group.addChild(new Shape3D(plane, appearance));

        if (scanResult.getChilds() != null) {
            Transform3D transform = new Transform3D();
            transform.setTranslation(new Vector3f(0f, 0f, 0.1f));
            TransformGroup childGroup = new TransformGroup(transform);
            group.addChild(childGroup);

            for (ScanResult childScanResult : scanResult.getChilds())
                loadView(childScanResult, zipFile, childGroup);
        }
    }

    private Appearance createTexture(String fileName, ZipFile zipFile) throws IOException {
        Image sourceImage = ImageIO.read(zipFile.getInputStream(zipFile.getEntry(fileName)));
        if (sourceImage == null)
            System.out.println("Image could not be loaded from " + fileName);

        TextureLoader loader = new TextureLoader(sourceImage, "RGBA", this.mainPanel);
        ImageComponent2D image = loader.getImage();

        if (image == null)
            System.out.println("Texture could not be loaded from " + fileName);

        Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA, image.getWidth(), image.getHeight());
        texture.setImage(0, image);
        texture.setEnable(true);
        texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
        texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);

        Appearance appearance = new Appearance();
        PolygonAttributes polyAttributes = new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0f);
        appearance.setPolygonAttributes(polyAttributes);
        appearance.setTexture(texture);

        TextureAttributes textureAttributes = new TextureAttributes();
        textureAttributes.setTextureMode(TextureAttributes.REPLACE);
        appearance.setTextureAttributes(textureAttributes);

//        TransparencyAttributes transparencyAttributes = new TransparencyAttributes(TransparencyAttributes.BLENDED, 1f);
        TransparencyAttributes transparencyAttributes = new TransparencyAttributes(TransparencyAttributes.NONE, 0f);
        appearance.setTransparencyAttributes(transparencyAttributes);

        return appearance;
    }

    private static class ScanResult implements Serializable {
        private int id;
        private String name;
        private String type;
        private String imageName;
        private Rect rect;
        private ArrayList<ScanResult> childs;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getImageName() {
            return imageName;
        }

        public Rect getRect() {
            return rect;
        }

        public ArrayList<ScanResult> getChilds() {
            return childs;
        }
    }

    public static class Rect {
        public float left;
        public float top;
        public float right;
        public float bottom;
    }
}
