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
import javax.vecmath.TexCoord2f;
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
//        universe.getViewer().getView().setDepthBufferFreezeTransparent(false);
//        universe.getViewer().getView().setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);
        universe.getViewer().getView().setProjectionPolicy(View.PARALLEL_PROJECTION);
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
        OrderedGroup orderedGroup = new OrderedGroup();
        childGroup.addChild(orderedGroup);

        level = 0f;
        loadView(layers, zipFile, orderedGroup);
    }

    private float level;

    private void loadView(ScanResult scanResult, ZipFile zipFile, Group group) throws IOException {
        Appearance appearance = createTexture(scanResult.getImageName(), zipFile);
        Rect rect = scanResult.getRect();

        QuadArray plane = new QuadArray(4, QuadArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);
        plane.setCoordinate(0, new Point3f(rect.left / mul, -rect.top / mul, level));
        plane.setCoordinate(1, new Point3f(rect.right / mul, -rect.top / mul, level));
        plane.setCoordinate(2, new Point3f(rect.right / mul, -rect.bottom / mul, level));
        plane.setCoordinate(3, new Point3f(rect.left / mul, -rect.bottom / mul, level));
        plane.setTextureCoordinate(0, 0, new TexCoord2f(0f, 1f));
        plane.setTextureCoordinate(0, 1, new TexCoord2f(1f, 1f));
        plane.setTextureCoordinate(0, 2, new TexCoord2f(1f, 0f));
        plane.setTextureCoordinate(0, 3, new TexCoord2f(0f, 0f));

        group.addChild(new Shape3D(plane, appearance));

        if (scanResult.getChilds() != null) {
//            Transform3D transform = new Transform3D();
//            transform.setTranslation(new Vector3f(0f, 0f, 0.01f));
//            TransformGroup childGroup = new TransformGroup(transform);
////            OrderedGroup orderedGroup = new OrderedGroup();
//            group.addChild(childGroup);
////            childGroup.addChild(orderedGroup);
            level += 0.01f;
            for (ScanResult childScanResult : scanResult.getChilds())
                loadView(childScanResult, zipFile, group);
            level -= 0.01f;
        }
    }

    private Appearance createTexture(String fileName, ZipFile zipFile) throws IOException {
        Image sourceImage = ImageIO.read(zipFile.getInputStream(zipFile.getEntry(fileName)));
        if (sourceImage == null)
            System.out.println("Image could not be loaded from " + fileName);

        TextureLoader loader = new TextureLoader(sourceImage, "RGBA", this.mainPanel.getRootPane());
        ImageComponent2D image = loader.getImage();
//        Texture texture = loader.getTexture();

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

//        RenderingAttributes renderAttribs = new RenderingAttributes();
//        renderAttribs.setDepthBufferWriteEnable(true);
//        appearance.setRenderingAttributes(renderAttribs);

        TextureAttributes textureAttributes = new TextureAttributes();
//        textureAttributes.setTextureMode(TextureAttributes.COMBINE);
//        textureAttributes.setCombineRgbMode(TextureAttributes.COMBINE_REPLACE);
//        textureAttributes.setCombineAlphaScale(TextureAttributes.COMBINE_MODULATE);
        textureAttributes.setTextureMode(TextureAttributes.MODULATE);
        appearance.setTextureAttributes(textureAttributes);

        TransparencyAttributes transparencyAttributes = new TransparencyAttributes(TransparencyAttributes.NICEST, 0f);
        appearance.setTransparencyAttributes(transparencyAttributes);

        appearance.setTexture(texture);

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
