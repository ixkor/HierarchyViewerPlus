package net.xkor.hierarchyviewplus;

import com.sun.j3d.exp.swing.JCanvas3D;
import com.sun.j3d.utils.universe.SimpleUniverse;

import javax.swing.*;

/**
 * Created by xkor on 20.01.15.
 */
public class MainForm {
    private JPanel mainPanel;
    private JButton button1;
    private JPanel panelViews;
    private JCanvas3D c3d;

    private SimpleUniverse universe;


    public MainForm() {
//        c3d = new JCanvas3D();
//        c3d.setSize(500, 500);
//
//        panelViews.add(c3d);
//        panelViews.setOpaque(false);

        mainPanel.setLayout(null);
        mainPanel.setSize(500, 500);


        universe = new SimpleUniverse(c3d.getOffscreenCanvas3D());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("MainForm");
        frame.setContentPane(new MainForm().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void createUIComponents() {

        c3d = new JCanvas3D();
        c3d.setLocation(10, 10);
        c3d.setSize(500, 500);


//        panelViews.setLayout(null);
    }
}
