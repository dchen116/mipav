package gov.nih.mipav.view.renderer.surfaceview;


import gov.nih.mipav.view.*;
import gov.nih.mipav.view.renderer.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import javax.media.j3d.*;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Dialog to turn slices bounding box of surface renderer on and off, and to change the color of the frame. This dialog
 * also control the X, Y, Z slices movements.
 */
public class JPanelSlices extends JPanelRendererBase implements ChangeListener, MouseListener { // for slider changes
                                                                                                // (LOD change){

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = -8359831093707979536L;

    /** X, Y, Z label constants. */
    public static final int X = 0;

    /** DOCUMENT ME! */
    public static final int Y = 1;

    /** DOCUMENT ME! */
    public static final int Z = 2;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Check boxes that turn the image plane on and off. */
    public JCheckBox boxX, boxY, boxZ;

    /** Sliders for the image planes. */
    public JSlider sliderX, sliderY, sliderZ, sliderT;

    /** Which time slice is currently displayed. */
    public int tSlice;

    /** Which slice is currently displayed in the ZY plane. */
    public int xSlice;

    /** Flags indicating if the image slices are on or off. */
    public boolean xVisible = true, yVisible = true, zVisible = true;

    /** Which slice is currently displayed in the XZ plane. */
    public int ySlice;

    /** Which slice is currently displayed in the XY plane. */
    public int zSlice;

    /** Bounding box control panel. */
    private JPanel boundingBoxPanel;

    /** Check box for turning x on and off. */
    private JCheckBox boundingCheckX;

    /** Check box for turning y on and off. */
    private JCheckBox boundingCheckY;

    /** Check box for turning z on and off. */
    private JCheckBox boundingCheckZ;

    /** Color button for changing x color. */
    private JButton colorButtonX;

    /** Color button for changing y color. */
    private JButton colorButtonY;

    /** Color button for changing z color. */
    private JButton colorButtonZ;

    /** Color chooser dialog. */
    private ViewJColorChooser colorChooser;

    /** Main panel for sliders. */
    private JPanel controlPanel;

    /** Current event vector index. */
    private int current;

    /** Labels next to sliders. */
    private JLabel labelX, labelY, labelZ, labelT;

    /** Labels beneath sliders. */
    private JLabel labelX1, labelXMid, labelXEnd, labelY1, labelYMid, labelYEnd, labelZ1, labelZMid, labelZEnd;

    /** Opacity control panel. */
    private JPanel opacityControlPanel;

    /** The opacity slider label. */
    private JLabel opacityLabelX, opacityLabelY, opacityLabelZ;

    /** The labels below the opacity slider. */
    private JLabel[] opacitySliderLabelsX, opacitySliderLabelsY, opacitySliderLabelsZ;

    /** Opacity slider, not enabled yet. */
    private JSlider opacitySliderX, opacitySliderY, opacitySliderZ;

    /** The scroll pane holding the panel content. Useful when the screen is small. */
    private JScrollPane scroller;

    /** Scroll panel that holding the all the control components. */
    private DrawingPanel scrollPanel;

    /** Flag to indicate the first time slider name changes. */
    private boolean setSliderFlag;

    /** Slider events used by the mouse recorder. */
    private MouseEventVector sliderEvents;

    /* MipavCoordinateSystems upgrade TODO: */
    /** The positions of the sliders - and therefore how they map to x,y,z:. */
    private int sliderXPos, sliderYPos, sliderZPos;

    /** Text fields that display the slice number next to the sliders. */
    private JTextField textX, textY, textZ, textT;

    /** x, y, z and time dimension values. */
    private int xDim, yDim, zDim, tDim;

    /** x, y, z opacity slider values. */
    private int xOpacitySlice, yOpacitySlice, zOpacitySlice;

    /** Probe x, y, z position. */
    private int xProbe, yProbe, zProbe;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates new dialog for turning slices bounding box frame on and off.
     *
     * @param  parent  Should be of type ViewJFrameSurfaceRenderer
     */
    public JPanelSlices(SurfaceRender parent) {
        super(parent);
        xDim = renderBase.getImageA().getExtents()[0];
        yDim = renderBase.getImageA().getExtents()[1];
        zDim = renderBase.getImageA().getExtents()[2];

        xSlice = (xDim - 1) / 2;
        ySlice = (yDim - 1) / 2;
        zSlice = (zDim - 1) / 2;

        xProbe = xSlice;
        yProbe = ySlice;
        zProbe = zSlice;

        if (renderBase.getImageA().getNDims() == 4) {
            tDim = renderBase.getImageA().getExtents()[3];

            // tSlice  = 0;
            tSlice = (tDim - 1) / 2;
        }

        init();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Changes color of slices box frame and button if color button was pressed; turns bounding box on and off if
     * checkbox was pressed; and closes dialog if "Close" button was pressed.
     *
     * @param  event  Event that triggered function.
     */
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        String command = event.getActionCommand();

        if (source instanceof JButton) {
            colorChooser = new ViewJColorChooser(new Frame(), "Pick color", new OkColorListener((JButton) source),
                                                 new CancelListener());
        }

        if (source == boundingCheckX) {

            if (boundingCheckX.isSelected()) {
                ((SurfaceRender) renderBase).updateBoxSlicePos();
                ((SurfaceRender) renderBase).showBoxSliceX();
                colorButtonX.setEnabled(true);
            } else {
                ((SurfaceRender) renderBase).removeBoxSliceX();
                colorButtonX.setEnabled(false);
            }
        } else if (source == boundingCheckY) {

            if (boundingCheckY.isSelected()) {
                ((SurfaceRender) renderBase).updateBoxSlicePos();
                ((SurfaceRender) renderBase).showBoxSliceY();
                colorButtonY.setEnabled(true);
            } else {
                ((SurfaceRender) renderBase).removeBoxSliceY();
                colorButtonY.setEnabled(false);
            }
        } else if (source == boundingCheckZ) {

            if (boundingCheckZ.isSelected()) {
                ((SurfaceRender) renderBase).updateBoxSlicePos();
                ((SurfaceRender) renderBase).showBoxSliceZ();
                colorButtonZ.setEnabled(true);
            } else {
                ((SurfaceRender) renderBase).removeBoxSliceZ();
                colorButtonZ.setEnabled(false);
            }
        } else if (command.equals("X")) {

            if (!boxX.isSelected()) {
                ((SurfaceRender) renderBase).getObjZYPlaneBG().detach();
                setXSliderEnabled(false);
                setOpacitySliderXEnabled(false);
                xVisible = false;
            } else {
                renderBase.getTriPlanarViewBG().addChild(((SurfaceRender) renderBase).getObjZYPlaneBG());
                setXSliderEnabled(true);
                setOpacitySliderXEnabled(true);
                xVisible = true;
            }
        } else if (command.equals("Y")) {

            if (!boxY.isSelected()) {
                ((SurfaceRender) renderBase).getObjXZPlaneBG().detach();
                setYSliderEnabled(false);
                setOpacitySliderYEnabled(false);
                yVisible = false;
            } else {
                renderBase.getTriPlanarViewBG().addChild(((SurfaceRender) renderBase).getObjXZPlaneBG());
                setYSliderEnabled(true);
                setOpacitySliderYEnabled(true);
                yVisible = true;
            }
        } else if (command.equals("Z")) {

            if (!boxZ.isSelected()) {
                ((SurfaceRender) renderBase).getObjXYPlaneBG().detach();
                setZSliderEnabled(false);
                setOpacitySliderZEnabled(false);
                zVisible = false;
            } else {
                renderBase.getTriPlanarViewBG().addChild(((SurfaceRender) renderBase).getObjXYPlaneBG());
                setZSliderEnabled(true);
                setOpacitySliderZEnabled(true);
                zVisible = true;
            }
        }

    }

    /**
     * Builds panel that has 3 sliders for the 3 planes shown, 3 checkboxes for showing the planes, 3 text boxes for the
     * current values of the sliders, and a fourth slider and text box for the time dimension, if necessary.
     */
    public void buildControlPanel() {
        int levelX = 0, levelY = 1, levelZ = 2;

        GridBagLayout cpGBL = new GridBagLayout();
        GridBagConstraints cpGBC = new GridBagConstraints();

        cpGBC.fill = GridBagConstraints.NONE;
        cpGBC.weightx = 100;
        cpGBC.weighty = 100;
        controlPanel = new JPanel();
        controlPanel.setBounds(10, 100, 500, 120);
        controlPanel.setBorder(buildTitledBorder("Slices control box"));
        controlPanel.setLayout(cpGBL);

        cpGBC.fill = GridBagConstraints.BOTH;

        switch (((SurfaceRender) renderBase).getLabel(X, true)) {

            case X:
                labelX = new JLabel(" X (1 - " + String.valueOf(xDim) + ")");
                levelX = 0;
                break;

            case Y:
                labelX = new JLabel(" Y (1 - " + String.valueOf(xDim) + ")");
                levelX = 1;
                break;

            case Z:
                labelX = new JLabel(" Z (1 - " + String.valueOf(xDim) + ")");
                levelX = 2;
                break;
        }

        boxX = new JCheckBox();
        boxX.setSelected(true);
        boxX.addActionListener(((SurfaceRender) renderBase).getMouseDialog());
        boxX.addActionListener(this);
        boxX.setActionCommand("X");
        addControlPanel(boxX, cpGBC, 0, levelX, 1, 1);

        labelX.setForeground(Color.black);
        labelX.setFont(MipavUtil.font12);
        labelX.setEnabled(true);
        addControlPanel(labelX, cpGBC, 1, levelX, 2, 1);

        sliderX = new JSlider(0, xDim - 1, xSlice);
        sliderX.setFont(MipavUtil.font12);
        sliderX.setEnabled(true);
        sliderX.setMinorTickSpacing(xDim / 10);
        sliderX.setPaintTicks(true);
        sliderX.addChangeListener(((SurfaceRender) renderBase).getMouseDialog());
        sliderX.addChangeListener(this);
        sliderX.addMouseListener(this);
        sliderX.setVisible(true);

        labelX1 = new JLabel("1");
        labelX1.setForeground(Color.black);
        labelX1.setFont(MipavUtil.font12);
        labelX1.setEnabled(true);
        labelXMid = new JLabel(String.valueOf(xSlice + 1));
        labelXMid.setForeground(Color.black);
        labelXMid.setFont(MipavUtil.font12);
        labelXMid.setEnabled(true);
        labelXEnd = new JLabel(String.valueOf(xDim));
        labelXEnd.setForeground(Color.black);
        labelXEnd.setFont(MipavUtil.font12);
        labelXEnd.setEnabled(true);

        Hashtable labelTableX = new Hashtable();

        labelTableX.put(new Integer(0), labelX1);
        labelTableX.put(new Integer(xSlice), labelXMid);
        labelTableX.put(new Integer(xDim - 1), labelXEnd);
        sliderX.setLabelTable(labelTableX);
        sliderX.setPaintLabels(true);
        addControlPanel(sliderX, cpGBC, 4, levelX, 8, 1);

        textX = new JTextField(String.valueOf(xSlice + 1), 4);
        textX.setFont(MipavUtil.font12);
        textX.setEnabled(false);
        cpGBC.fill = GridBagConstraints.NONE;
        addControlPanel(textX, cpGBC, 14, levelX, 1, 1);

        cpGBC.fill = GridBagConstraints.BOTH;

        switch (((SurfaceRender) renderBase).getLabel(Y, true)) {

            case X:
                labelY = new JLabel(" X (1 - " + String.valueOf(yDim) + ")");
                levelY = 0;
                break;

            case Y:
                labelY = new JLabel(" Y (1 - " + String.valueOf(yDim) + ")");
                levelY = 1;
                break;

            case Z:
                labelY = new JLabel(" Z (1 - " + String.valueOf(yDim) + ")");
                levelY = 2;
                break;
        }

        boxY = new JCheckBox();
        boxY.setSelected(true);
        boxY.addActionListener(((SurfaceRender) renderBase).getMouseDialog());
        boxY.addActionListener(this);
        boxY.setActionCommand("Y");
        addControlPanel(boxY, cpGBC, 0, levelY, 1, 1);

        labelY.setForeground(Color.black);
        labelY.setFont(MipavUtil.font12);
        labelY.setEnabled(true);
        addControlPanel(labelY, cpGBC, 1, levelY, 2, 1);

        sliderY = new JSlider(0, yDim - 1, ySlice);
        sliderY.setFont(MipavUtil.font12);
        sliderY.setEnabled(true);
        sliderY.setMinorTickSpacing(yDim / 10);
        sliderY.setPaintTicks(true);
        sliderY.addChangeListener(((SurfaceRender) renderBase).getMouseDialog());
        sliderY.addChangeListener(this);
        sliderY.addMouseListener(this);
        sliderY.setVisible(true);

        labelY1 = new JLabel("1");
        labelY1.setForeground(Color.black);
        labelY1.setFont(MipavUtil.font12);
        labelY1.setEnabled(true);
        labelYMid = new JLabel(String.valueOf(ySlice + 1));
        labelYMid.setForeground(Color.black);
        labelYMid.setFont(MipavUtil.font12);
        labelYMid.setEnabled(true);
        labelYEnd = new JLabel(String.valueOf(yDim));
        labelYEnd.setForeground(Color.black);
        labelYEnd.setFont(MipavUtil.font12);
        labelYEnd.setEnabled(true);

        Hashtable labelTableY = new Hashtable();

        labelTableY.put(new Integer(0), labelY1);
        labelTableY.put(new Integer(ySlice), labelYMid);
        labelTableY.put(new Integer(yDim - 1), labelYEnd);
        sliderY.setLabelTable(labelTableY);
        sliderY.setPaintLabels(true);
        addControlPanel(sliderY, cpGBC, 4, levelY, 8, 1);

        textY = new JTextField(String.valueOf(ySlice + 1), 4);
        textY.setFont(MipavUtil.font12);
        textY.setEnabled(false);
        cpGBC.fill = GridBagConstraints.NONE;
        addControlPanel(textY, cpGBC, 14, levelY, 1, 1);

        cpGBC.fill = GridBagConstraints.BOTH;

        switch (((SurfaceRender) renderBase).getLabel(Z, true)) {

            case X:
                labelZ = new JLabel(" X (1 - " + String.valueOf(zDim) + ")");
                levelZ = 0;
                break;

            case Y:
                labelZ = new JLabel(" Y (1 - " + String.valueOf(zDim) + ")");
                levelZ = 1;
                break;

            case Z:
                labelZ = new JLabel(" Z (1 - " + String.valueOf(zDim) + ")");
                levelZ = 2;
                break;
        }

        boxZ = new JCheckBox();
        boxZ.setSelected(true);
        boxZ.addActionListener(((SurfaceRender) renderBase).getMouseDialog());
        boxZ.addActionListener(this);
        boxZ.setActionCommand("Z");
        addControlPanel(boxZ, cpGBC, 0, levelZ, 1, 1);

        labelZ.setForeground(Color.black);
        labelZ.setFont(MipavUtil.font12);
        labelZ.setEnabled(true);
        addControlPanel(labelZ, cpGBC, 1, levelZ, 2, 1);

        sliderZ = new JSlider(0, zDim - 1, zSlice);
        sliderZ.setFont(MipavUtil.font12);
        sliderZ.setEnabled(true);
        sliderZ.setMinorTickSpacing(zDim / 10);
        sliderZ.setPaintTicks(true);
        sliderZ.addChangeListener(((SurfaceRender) renderBase).getMouseDialog());
        sliderZ.addChangeListener(this);
        sliderZ.addMouseListener(this);
        sliderZ.setVisible(true);

        labelZ1 = new JLabel("1");
        labelZ1.setForeground(Color.black);
        labelZ1.setFont(MipavUtil.font12);
        labelZ1.setEnabled(true);
        labelZMid = new JLabel(String.valueOf(zSlice + 1));
        labelZMid.setForeground(Color.black);
        labelZMid.setFont(MipavUtil.font12);
        labelZMid.setEnabled(true);
        labelZEnd = new JLabel(String.valueOf(zDim));
        labelZEnd.setForeground(Color.black);
        labelZEnd.setFont(MipavUtil.font12);
        labelZEnd.setEnabled(true);

        Hashtable labelTableZ = new Hashtable();

        labelTableZ.put(new Integer(0), labelZ1);
        labelTableZ.put(new Integer(zSlice), labelZMid);
        labelTableZ.put(new Integer(zDim - 1), labelZEnd);
        sliderZ.setLabelTable(labelTableZ);
        sliderZ.setPaintLabels(true);
        addControlPanel(sliderZ, cpGBC, 4, levelZ, 8, 1);

        textZ = new JTextField(String.valueOf(zSlice + 1), 4);
        textZ.setFont(MipavUtil.font12);
        textZ.setEnabled(false);
        cpGBC.fill = GridBagConstraints.NONE;

        addControlPanel(textZ, cpGBC, 14, levelZ, 1, 1);

        if (renderBase.getImageA().getNDims() == 4) {
            buildTimeSlider();
        }

        /* Keep track of the positions of the sliders so we can keep track of
         * how they map to x,y,z: */
        sliderXPos = levelX;
        sliderYPos = levelY;
        sliderZPos = levelZ;
    }

    /**
     * Builds panel for opacity control change on the triplanar X, Y, Z.
     */
    public void buildOpacityPanel() {

        // Slider for changing opacity; not currently enabled.
        int levelX = 0, levelY = 2, levelZ = 4;
        GridBagLayout cpGBL = new GridBagLayout();
        GridBagConstraints cpGBC = new GridBagConstraints();

        cpGBC.fill = GridBagConstraints.NONE;
        cpGBC.weightx = 100;
        cpGBC.weighty = 100;
        opacityControlPanel = new JPanel();
        opacityControlPanel.setBounds(10, 100, 500, 120);
        opacityControlPanel.setBorder(buildTitledBorder("Opacity control box"));
        opacityControlPanel.setLayout(cpGBL);

        cpGBC.fill = GridBagConstraints.BOTH;

        switch (((SurfaceRender) renderBase).getLabel(X, true)) {

            case X:
                opacityLabelX = new JLabel("X Opacity");
                levelX = 0;
                break;

            case Y:
                opacityLabelX = new JLabel("Y Opacity");
                levelX = 2;
                break;

            case Z:
                opacityLabelX = new JLabel("Z Opacity");
                levelX = 4;
                break;
        }

        opacityLabelX.setFont(serif12B);
        opacityLabelX.setForeground(Color.black);
        addOpacityControlPanel(opacityLabelX, cpGBC, 0, levelX, 2, 1);

        opacitySliderLabelsX = new JLabel[3];
        opacitySliderLabelsX[0] = createLabel("0");
        opacitySliderLabelsX[1] = createLabel("50");
        opacitySliderLabelsX[2] = createLabel("100");

        Hashtable labelsX = new Hashtable();

        labelsX.put(new Integer(0), opacitySliderLabelsX[0]);
        labelsX.put(new Integer(50), opacitySliderLabelsX[1]);
        labelsX.put(new Integer(100), opacitySliderLabelsX[2]);

        opacitySliderX = new JSlider(0, 100, 100);
        opacitySliderX.setFont(serif12);
        opacitySliderX.setMinorTickSpacing(10);
        opacitySliderX.setPaintTicks(true);
        opacitySliderX.addChangeListener(((SurfaceRender) renderBase).getMouseDialog());
        opacitySliderX.addChangeListener(this);
        opacitySliderX.addMouseListener(this);
        opacitySliderX.setLabelTable(labelsX);
        opacitySliderX.setPaintLabels(true);
        opacitySliderX.setAlignmentX(Component.LEFT_ALIGNMENT);
        opacityLabelX.setAlignmentX(Component.LEFT_ALIGNMENT);
        opacitySliderX.setEnabled(true);
        opacityLabelX.setEnabled(true);
        xOpacitySlice = opacitySliderX.getValue();
        opacitySliderLabelsX[0].setEnabled(true);
        opacitySliderLabelsX[1].setEnabled(true);
        opacitySliderLabelsX[2].setEnabled(true);
        addOpacityControlPanel(opacitySliderX, cpGBC, 0, levelX + 1, 8, 1);

        switch (((SurfaceRender) renderBase).getLabel(Y, true)) {

            case X:
                opacityLabelY = new JLabel("X Opacity");
                levelY = 0;
                break;

            case Y:
                opacityLabelY = new JLabel("Y Opacity");
                levelY = 2;
                break;

            case Z:
                opacityLabelY = new JLabel("Z Opacity");
                levelY = 4;
                break;
        }

        opacityLabelY.setFont(serif12B);
        opacityLabelY.setForeground(Color.black);
        addOpacityControlPanel(opacityLabelY, cpGBC, 0, levelY, 2, 1);

        opacitySliderLabelsY = new JLabel[3];
        opacitySliderLabelsY[0] = createLabel("0");
        opacitySliderLabelsY[1] = createLabel("50");
        opacitySliderLabelsY[2] = createLabel("100");

        Hashtable labelsY = new Hashtable();

        labelsY.put(new Integer(0), opacitySliderLabelsY[0]);
        labelsY.put(new Integer(50), opacitySliderLabelsY[1]);
        labelsY.put(new Integer(100), opacitySliderLabelsY[2]);

        opacitySliderY = new JSlider(0, 100, 100);
        opacitySliderY.setFont(serif12);
        opacitySliderY.setMinorTickSpacing(10);
        opacitySliderY.setPaintTicks(true);
        opacitySliderY.addChangeListener(((SurfaceRender) renderBase).getMouseDialog());
        opacitySliderY.addChangeListener(this);
        opacitySliderY.addMouseListener(this);
        opacitySliderY.setLabelTable(labelsY);
        opacitySliderY.setPaintLabels(true);
        opacitySliderY.setAlignmentX(Component.LEFT_ALIGNMENT);
        opacityLabelY.setAlignmentX(Component.LEFT_ALIGNMENT);
        opacitySliderY.setEnabled(true);
        opacityLabelY.setEnabled(true);
        yOpacitySlice = opacitySliderY.getValue();
        opacitySliderLabelsY[0].setEnabled(true);
        opacitySliderLabelsY[1].setEnabled(true);
        opacitySliderLabelsY[2].setEnabled(true);
        addOpacityControlPanel(opacitySliderY, cpGBC, 0, levelY + 1, 8, 1);

        switch (((SurfaceRender) renderBase).getLabel(Z, true)) {

            case X:
                opacityLabelZ = new JLabel("X Opacity");
                levelZ = 0;
                break;

            case Y:
                opacityLabelZ = new JLabel("Y Opacity");
                levelZ = 2;
                break;

            case Z:
                opacityLabelZ = new JLabel("Z Opacity");
                levelZ = 4;
                break;
        }

        opacityLabelZ.setFont(serif12B);
        opacityLabelZ.setForeground(Color.black);
        addOpacityControlPanel(opacityLabelZ, cpGBC, 0, levelZ, 2, 1);

        opacitySliderLabelsZ = new JLabel[3];
        opacitySliderLabelsZ[0] = createLabel("0");
        opacitySliderLabelsZ[1] = createLabel("50");
        opacitySliderLabelsZ[2] = createLabel("100");

        Hashtable labelsZ = new Hashtable();

        labelsZ.put(new Integer(0), opacitySliderLabelsZ[0]);
        labelsZ.put(new Integer(50), opacitySliderLabelsZ[1]);
        labelsZ.put(new Integer(100), opacitySliderLabelsZ[2]);

        opacitySliderZ = new JSlider(0, 100, 100);
        opacitySliderZ.setFont(serif12);
        opacitySliderZ.setMinorTickSpacing(10);
        opacitySliderZ.setPaintTicks(true);
        opacitySliderZ.addChangeListener(((SurfaceRender) renderBase).getMouseDialog());
        opacitySliderZ.addChangeListener(this);
        opacitySliderZ.addMouseListener(this);
        opacitySliderZ.setLabelTable(labelsZ);
        opacitySliderZ.setPaintLabels(true);
        opacitySliderZ.setAlignmentX(Component.LEFT_ALIGNMENT);
        opacityLabelZ.setAlignmentX(Component.LEFT_ALIGNMENT);
        opacitySliderZ.setEnabled(true);
        opacityLabelZ.setEnabled(true);
        zOpacitySlice = opacitySliderZ.getValue();
        opacitySliderLabelsZ[0].setEnabled(true);
        opacitySliderLabelsZ[1].setEnabled(true);
        opacitySliderLabelsZ[2].setEnabled(true);

        addOpacityControlPanel(opacitySliderZ, cpGBC, 0, levelZ + 1, 8, 1);
    }

    /**
     * When 3D texture volume render is invoked, disable all the slices and bounding frame boxes.
     */
    public void disableSlices() {

        if (((SurfaceRender) renderBase).getObjZYPlaneBG().isLive()) {
            ((SurfaceRender) renderBase).getObjZYPlaneBG().detach();
        }

        if (((SurfaceRender) renderBase).getObjXZPlaneBG().isLive()) {
            ((SurfaceRender) renderBase).getObjXZPlaneBG().detach();
        }

        if (((SurfaceRender) renderBase).getObjXYPlaneBG().isLive()) {
            ((SurfaceRender) renderBase).getObjXYPlaneBG().detach();
        }

        if (((SurfaceRender) renderBase).getObjBoxSliceX_BG().isLive()) {
            ((SurfaceRender) renderBase).getObjBoxSliceX_BG().detach();
        }

        if (((SurfaceRender) renderBase).getObjBoxSliceY_BG().isLive()) {
            ((SurfaceRender) renderBase).getObjBoxSliceY_BG().detach();
        }

        if (((SurfaceRender) renderBase).getObjBoxSliceZ_BG().isLive()) {
            ((SurfaceRender) renderBase).getObjBoxSliceZ_BG().detach();
        }

    }

    /**
     * Dispose memory.
     */
    public void dispose() {
        boundingCheckX = null;
        boundingCheckY = null;
        boundingCheckZ = null;
        colorButtonX = null;
        colorButtonY = null;
        colorButtonZ = null;
        boundingBoxPanel = null;
        opacityLabelX = null;
        opacityLabelY = null;
        opacityLabelZ = null;
        opacitySliderLabelsX = null;
        opacitySliderLabelsY = null;
        opacitySliderLabelsZ = null;
        sliderX = null;
        sliderY = null;
        sliderZ = null;
        sliderT = null;
        opacitySliderX = null;
        opacitySliderY = null;
        opacitySliderZ = null;
        boxX = null;
        boxY = null;
        boxZ = null;
        labelX = null;
        labelY = null;
        labelZ = null;
        labelT = null;
        labelX1 = null;
        labelXMid = null;
        labelXEnd = null;
        labelY1 = null;
        labelYMid = null;
        labelYEnd = null;
        labelZ1 = null;
        labelZMid = null;
        labelZEnd = null;
        textX = null;
        textY = null;
        textZ = null;
        textT = null;
        opacityControlPanel = null;
        controlPanel = null;
        colorChooser = null;
        sliderEvents = null;
    }

    /**
     * Enable slider X, Y, Z and triplanar.
     */
    public void enableSlices() {

        if (yVisible && !((SurfaceRender) renderBase).getObjXZPlaneBG().isLive()) {
            renderBase.getTriPlanarViewBG().addChild(((SurfaceRender) renderBase).getObjXZPlaneBG());
            setYSliderEnabled(true);
            setOpacitySliderYEnabled(true);
            boxY.setSelected(true);
            yVisible = true;
        }

        if (zVisible && !((SurfaceRender) renderBase).getObjXYPlaneBG().isLive()) {
            renderBase.getTriPlanarViewBG().addChild(((SurfaceRender) renderBase).getObjXYPlaneBG());
            setZSliderEnabled(true);
            setOpacitySliderZEnabled(true);
            boxZ.setSelected(true);
            zVisible = true;
        }

        if (xVisible && !((SurfaceRender) renderBase).getObjZYPlaneBG().isLive()) {
            renderBase.getTriPlanarViewBG().addChild(((SurfaceRender) renderBase).getObjZYPlaneBG());
            setXSliderEnabled(true);
            setOpacitySliderXEnabled(true);
            boxX.setSelected(true);
            xVisible = true;
        }

        if (boundingCheckX.isSelected() && !((SurfaceRender) renderBase).getObjBoxSliceX_BG().isLive()) {
            renderBase.getTriPlanarViewBG().addChild(((SurfaceRender) renderBase).getObjBoxSliceX_BG());
        }

        if (boundingCheckY.isSelected() && !((SurfaceRender) renderBase).getObjBoxSliceY_BG().isLive()) {
            renderBase.getTriPlanarViewBG().addChild(((SurfaceRender) renderBase).getObjBoxSliceY_BG());
        }

        if (boundingCheckZ.isSelected() && !((SurfaceRender) renderBase).getObjBoxSliceZ_BG().isLive()) {
            renderBase.getTriPlanarViewBG().addChild(((SurfaceRender) renderBase).getObjBoxSliceZ_BG());
        }

    }

    /**
     * Return the checkbox of X slider.
     *
     * @return  boxX Get X slider check box.
     */
    public JCheckBox getBoxX() {
        return boxX;
    }

    /**
     * Return the checkbox of Y slider.
     *
     * @return  boxY Get Y slider check box.
     */
    public JCheckBox getBoxY() {
        return boxY;
    }

    /**
     * Return the checkbox of Z slider.
     *
     * @return  boxZ Get Z slider check box.
     */
    public JCheckBox getBoxZ() {
        return boxZ;
    }

    /**
     * Return the control panel.
     *
     * @return  JPanel slices move control panel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Return the X opacity slider.
     *
     * @return  opcitySliderX Opacity slider X.
     */
    public JSlider getOpacitySliderX() {
        return opacitySliderX;
    }

    /**
     * Return the Y opacity slider.
     *
     * @return  opcitySliderY Opacity slider Y.
     */
    public JSlider getOpacitySliderY() {
        return opacitySliderY;
    }

    /**
     * Return the Z opacity slider.
     *
     * @return  opcitySliderZ Opacity slider Z.
     */
    public JSlider getOpacitySliderZ() {
        return opacitySliderZ;
    }

    /**
     * Return the X slider.
     *
     * @return  sliderX X slider.
     */
    public JSlider getSliderX() {
        return sliderX;
    }

    /**
     * Return the Y slider.
     *
     * @return  sliderY Y slider.
     */
    public JSlider getSliderY() {
        return sliderY;
    }

    /**
     * Return the Z slider.
     *
     * @return  sliderZ Z slider.
     */
    public JSlider getSliderZ() {
        return sliderZ;
    }

    /**
     * Get the x opacity slider value.
     *
     * @return  xOpacityslice X opacity slider value.
     */
    public int getXOpacitySlice() {
        return xOpacitySlice;
    }

    /**
     * Get the probe x coordinate.
     *
     * @return  xProbe probe x position
     */
    public int getXProbePos() {
        return xProbe;
    }

    /**
     * Get the X slider value.
     *
     * @return  xSlice X slider value.
     */
    public int getXSlice() {
        return xSlice;
    }

    /**
     * X slider visible or not.
     *
     * @return  xVisible if <code>true</code> visible, otherwise invisible.
     */
    public boolean getXVisible() {
        return xVisible;
    }

    /**
     * Get the y opacity slider value.
     *
     * @return  yOpacityslice Y opacity slider value.
     */
    public int getYOpacitySlice() {
        return yOpacitySlice;
    }

    /**
     * Get the probe y coordinate.
     *
     * @return  yProbe probe y position
     */
    public int getYProbePos() {
        return yProbe;
    }

    /**
     * Get the y slider value.
     *
     * @return  ySlice Y slider value.
     */
    public int getYSlice() {
        return ySlice;
    }

    /**
     * Y slider visible or not.
     *
     * @return  yVisible if <code>true</code> visible, otherwise invisible.
     */
    public boolean getYVisible() {
        return yVisible;
    }

    /**
     * Get the z opacity slider value.
     *
     * @return  zOpacityslice Z opacity slider value.
     */
    public int getZOpacitySlice() {
        return zOpacitySlice;
    }

    /**
     * Get the probe z coordinate.
     *
     * @return  zProbe probe z position
     */
    public int getZProbePos() {
        return zProbe;
    }

    /**
     * Get the z slider value.
     *
     * @return  zSlice Z slider value.
     */
    public int getZSlice() {
        return zSlice;
    }

    /**
     * Z slider visible or not.
     *
     * @return  zVisible if <code>true</code> visible, otherwise invisible.
     */
    public boolean getZVisible() {
        return zVisible;
    }

    /**
     * Initializes GUI components.
     */
    public void init() {
        buildBoundingBox();
        buildControlPanel();
        buildOpacityPanel();

        Box contentBox = new Box(BoxLayout.Y_AXIS);

        contentBox.add(opacityControlPanel);
        contentBox.add(boundingBoxPanel);
        contentBox.add(controlPanel);

        // Scroll panel that hold the control panel layout in order to use JScrollPane
        scrollPanel = new DrawingPanel();
        scrollPanel.setLayout(new BorderLayout());
        scrollPanel.add(contentBox, BorderLayout.NORTH);

        scroller = new JScrollPane(scrollPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        mainPanel = new JPanel();
        mainPanel.add(scroller);
    }

    /**
     * Unchanged.
     *
     * @param  event  Original mouse event.
     */
    public void mouseClicked(MouseEvent event) { }

    /**
     * Unchanged.
     *
     * @param  event  Original mouse event.
     */
    public void mouseEntered(MouseEvent event) { }

    /**
     * Unchanged.
     *
     * @param  event  Original mouse event.
     */
    public void mouseExited(MouseEvent event) { }

    /**
     * Unchanged.
     *
     * @param  event  Original mouse event.
     */
    public void mousePressed(MouseEvent event) {
        JPanelMouse myMouseDialog = ((SurfaceRender) renderBase).getMouseDialog();

        if (myMouseDialog.isRecording()) {
            Transform3D t3D = new Transform3D();

            // get the view
            renderBase.getSceneRootTG().getTransform(t3D);

            // store name and view together
            sliderEvents = new MouseEventVector("Slider" + myMouseDialog.sliderCount, t3D, myMouseDialog.first,
                                                renderBase.getSceneState(),
                                                ((SurfaceRender) renderBase).getMouseMode());
            setSliderFlag = true;
            myMouseDialog.events.add(sliderEvents);
            current = myMouseDialog.events.indexOf(sliderEvents);
        }

    }

    /**
     * Used in MouseRecorder to stop one series of slide moves.
     *
     * @param  event  Original mouse event.
     */
    public void mouseReleased(MouseEvent event) {
        JPanelMouse myMouseDialog = ((SurfaceRender) renderBase).getMouseDialog();

        if (myMouseDialog.isRecording()) {
            myMouseDialog.sliderCount++;
        }
    }

    /**
     * Resizig the control panel with ViewJFrameVolumeView's frame width and height.
     *
     * @param  panelWidth   DOCUMENT ME!
     * @param  frameHeight  DOCUMENT ME!
     */
    public void resizePanel(int panelWidth, int frameHeight) {

        // /
        scroller.setPreferredSize(new Dimension(panelWidth, frameHeight - 40));
        scroller.setSize(new Dimension(panelWidth, frameHeight - 40));
        scroller.revalidate();
    }

    /**
     * Set the opacity slider X with given boolean value.
     *
     * @param  flag  indicate opacity slider is set or not
     */
    public void setOpacitySliderXEnabled(boolean flag) {
        opacityLabelX.setEnabled(flag);
        opacitySliderLabelsX[0].setEnabled(flag);
        opacitySliderLabelsX[1].setEnabled(flag);
        opacitySliderLabelsX[2].setEnabled(flag);
        opacitySliderX.setEnabled(flag);
    }

    /**
     * Set the opacity slider Y with given boolean value.
     *
     * @param  flag  indicate opacity slider is set or not
     */
    public void setOpacitySliderYEnabled(boolean flag) {
        opacityLabelY.setEnabled(flag);
        opacitySliderLabelsY[0].setEnabled(flag);
        opacitySliderLabelsY[1].setEnabled(flag);
        opacitySliderLabelsY[2].setEnabled(flag);
        opacitySliderY.setEnabled(flag);
    }

    /**
     * Set the opacity slider Z with given boolean value.
     *
     * @param  flag  indicate opacity slider is set or not
     */
    public void setOpacitySliderZEnabled(boolean flag) {
        opacityLabelZ.setEnabled(flag);
        opacitySliderLabelsZ[0].setEnabled(flag);
        opacitySliderLabelsZ[1].setEnabled(flag);
        opacitySliderLabelsZ[2].setEnabled(flag);
        opacitySliderZ.setEnabled(flag);
    }

    /**
     * Sets the scene state appropriately, so that the slices that are supposed to be visible are showing, the ones that
     * aren't are hidden, and the sliders are starting at the appropriate value.
     *
     * @param  scene  The state of the scene.
     */
    public void setSceneState(Object scene) {
        xSlice = ((SceneState) scene).x;
        ySlice = ((SceneState) scene).y;
        zSlice = ((SceneState) scene).z;

        if (!((SceneState) scene).xVisible) {
            ((SurfaceRender) renderBase).getObjZYPlaneBG().detach();
            setXSliderEnabled(false);
            xVisible = false;
            boxX.setSelected(false);
        } else if (((SceneState) scene).xVisible) {

            if (!((SurfaceRender) renderBase).getObjZYPlaneBG().isLive()) {
                (renderBase.getTriPlanarViewBG()).addChild(((SurfaceRender) renderBase).getObjZYPlaneBG());
            }

            setXSliderEnabled(true);
            xVisible = true;
            boxX.setSelected(true);
        }

        if (!((SceneState) scene).yVisible) {
            ((SurfaceRender) renderBase).getObjXZPlaneBG().detach();
            setYSliderEnabled(false);
            yVisible = false;
            boxY.setSelected(false);
        } else if (((SceneState) scene).yVisible) {

            if (!((SurfaceRender) renderBase).getObjXZPlaneBG().isLive()) {
                (renderBase.getTriPlanarViewBG()).addChild(((SurfaceRender) renderBase).getObjXZPlaneBG());
            }

            setYSliderEnabled(true);
            yVisible = true;
            boxY.setSelected(true);
        }

        if (!((SceneState) scene).zVisible) {
            ((SurfaceRender) renderBase).getObjXYPlaneBG().detach();
            setZSliderEnabled(false);
            zVisible = false;
            boxZ.setSelected(false);
        } else if (((SceneState) scene).zVisible) {

            if (!((SurfaceRender) renderBase).getObjXYPlaneBG().isLive()) {
                (renderBase.getTriPlanarViewBG()).addChild(((SurfaceRender) renderBase).getObjXYPlaneBG());
            }

            setZSliderEnabled(true);
            zVisible = true;
            boxZ.setSelected(true);
        }

        xVisible = ((SceneState) scene).xVisible;
        yVisible = ((SceneState) scene).yVisible;
        zVisible = ((SceneState) scene).zVisible;
    }

    /**
     * Sets the slice position based on a relative percentage of the X dimension.
     *
     * @param  iView   the Slice to set the position for
     * @param  fSlice  the relative slize position
     */
    public void setSlicePos( int iView, float fSlice )
    {
        if ( iView == ViewJComponentBase.AXIAL )
        {
            if ( sliderXPos == Z )
            {
                setXSlicePos(Math.round((xDim - 1) * fSlice));
            }
            else if ( sliderYPos == Z )
            {
                setYSlicePos(Math.round((yDim - 1) * fSlice));
            }
            else if ( sliderZPos == Z )
            {
                setZSlicePos(Math.round((zDim - 1) * fSlice));
            }
        }
        else if ( iView == ViewJComponentBase.SAGITTAL )
        {
            if ( sliderXPos == X )
            {
                setXSlicePos(Math.round((xDim - 1) * fSlice));
            }
            else if ( sliderYPos == X )
            {
                setYSlicePos(Math.round((yDim - 1) * fSlice));
            }
            else if ( sliderZPos == X )
            {
                setZSlicePos(Math.round((zDim - 1) * fSlice));
            }
        }
        else if  ( iView == ViewJComponentBase.CORONAL )
        {
            if ( sliderXPos == Y )
            {
                setXSlicePos(Math.round((xDim - 1) * fSlice));
            }
            else if ( sliderYPos == Y )
            {
                setYSlicePos(Math.round((yDim - 1) * fSlice));
            }
            else if ( sliderZPos == Y )
            {
                setZSlicePos(Math.round((zDim - 1) * fSlice));
            }
        }
    }


    /**
     * Set probe x coordinate.
     *
     * @param  _xProbe  probe x position
     */
    public void setXProbePos(int _xProbe) {
        xProbe = _xProbe;
    }

    /**
     * Set the current x slider move position.
     *
     * @param  _xSlice  x slider position
     */
    public void setXSlicePos(int _xSlice) {
        xSlice = _xSlice;
        sliderX.setValue(xSlice);
    }

    /**
     * Sets the x slider and the labels beside and beneath it to the state given by <code>flag</code>.
     *
     * @param  flag  if <code>true</code> enable, otherwise disable.
     */
    public void setXSliderEnabled(boolean flag) {
        sliderX.setEnabled(flag);
        labelX.setEnabled(flag);
        labelX1.setEnabled(flag);
        labelXMid.setEnabled(flag);
        labelXEnd.setEnabled(flag);
    }

    /**
     * Set probe y coordinate.
     *
     * @param  _yProbe  probe y position
     */
    public void setYProbePos(int _yProbe) {
        yProbe = _yProbe;
    }

    /**
     * Set the current y slider move position.
     *
     * @param  _ySlice  y slider position
     */
    public void setYSlicePos(int _ySlice) {
        ySlice = _ySlice;
        sliderY.setValue(ySlice);
    }

    /**
     * Sets the y slider and the labels beside and beneath it to the state given by <code>flag</code>.
     *
     * @param  flag  if <code>true</code> enable, otherwise disable.
     */
    public void setYSliderEnabled(boolean flag) {
        sliderY.setEnabled(flag);
        labelY.setEnabled(flag);
        labelY1.setEnabled(flag);
        labelYMid.setEnabled(flag);
        labelYEnd.setEnabled(flag);
    }

    /**
     * Set probe z coordinate.
     *
     * @param  _zProbe  probe z position
     */
    public void setZProbePos(int _zProbe) {
        zProbe = _zProbe;
    }

    /**
     * Set the current z slider move position.
     *
     * @param  _zSlice  z slider position
     */
    public void setZSlicePos(int _zSlice) {
        zSlice = _zSlice;
        sliderZ.setValue(zSlice);
    }

    /**
     * Sets the z slider and the labels beside and beneath it to the state given by <code>flag</code>.
     *
     * @param  flag  if <code>true</code> enable, otherwise disable.
     */
    public void setZSliderEnabled(boolean flag) {
        sliderZ.setEnabled(flag);
        labelZ.setEnabled(flag);
        labelZ1.setEnabled(flag);
        labelZMid.setEnabled(flag);
        labelZEnd.setEnabled(flag);

    }

    /**
     * Sets how the image plane should be displayed depending on value of slider.
     *
     * @param  e  Event that triggered this function.
     */
    public void stateChanged(ChangeEvent e) {
        Object source = e.getSource();
        JPanelMouse myMouseDialog = ((SurfaceRender) renderBase).getMouseDialog();

        ((SurfaceRender) renderBase).updateBoxSlicePos();

        if (source == sliderX) {

            // Change the currently displayed x slice
            xSlice = sliderX.getValue();
            textX.setText(String.valueOf(xSlice + 1));
            ((SurfaceRender) renderBase).update3DTriplanar(null, null, false);

            if (myMouseDialog.isRecording() && setSliderFlag) {

                switch (sliderXPos) {

                    case X:
                        sliderEvents.setName("xSlider" + current);
                        myMouseDialog.listModel.addElement("xSlider" + current);
                        break;

                    case Y:
                        sliderEvents.setName("ySlider" + current);
                        myMouseDialog.listModel.addElement("ySlider" + current);
                        break;

                    case Z:
                        sliderEvents.setName("zSlider" + current);
                        myMouseDialog.listModel.addElement("zSlider" + current);
                        break;
                }

                setSliderFlag = false;
            }

            ((SurfaceRender) renderBase).updateBoxSlicePos();

            /* Update the SurfaceRender, and frame ViewJFrameVolumeView based
             * on the relative position of the slider */
            switch (sliderXPos)
            {
            case X:
                ((SurfaceRender) renderBase).updateTriPlanar( ViewJComponentBase.SAGITTAL, (float) (xSlice) / (float) (xDim - 1) );
                break;
            case Y:
                ((SurfaceRender) renderBase).updateTriPlanar( ViewJComponentBase.CORONAL, (float) (xSlice) / (float) (xDim - 1) );
                break;
            case Z:
                ((SurfaceRender) renderBase).updateTriPlanar( ViewJComponentBase.AXIAL, (float) (xSlice) / (float) (xDim - 1) );
                break;
            }
        } else if (source == sliderY) {

            // Change the currently displayed y slice
            ySlice = sliderY.getValue();
            textY.setText(String.valueOf(ySlice + 1));
            ((SurfaceRender) renderBase).update3DTriplanar(null, null, false);

            if (myMouseDialog.isRecording() && setSliderFlag) {

                switch (sliderYPos) {

                    case X:
                        sliderEvents.setName("xSlider" + current);
                        myMouseDialog.listModel.addElement("xSlider" + current);
                        break;

                    case Y:
                        sliderEvents.setName("ySlider" + current);
                        myMouseDialog.listModel.addElement("ySlider" + current);
                        break;

                    case Z:
                        sliderEvents.setName("zSlider" + current);
                        myMouseDialog.listModel.addElement("zSlider" + current);
                        break;
                }

                setSliderFlag = false;
            }

            ((SurfaceRender) renderBase).updateBoxSlicePos();

            /* Update the SurfaceRender, and frame ViewJFrameVolumeView based
             * on the relative position of the slider */
            switch (sliderYPos)
            {
            case X:
                ((SurfaceRender) renderBase).updateTriPlanar( ViewJComponentBase.SAGITTAL, (float) (ySlice) / (float) (yDim - 1) );
                break;
            case Y:
                ((SurfaceRender) renderBase).updateTriPlanar( ViewJComponentBase.CORONAL, (float) (ySlice) / (float) (yDim - 1) );
                break;
            case Z:
                ((SurfaceRender) renderBase).updateTriPlanar( ViewJComponentBase.AXIAL, (float) (ySlice) / (float) (yDim - 1) );
                break;
            }
        } else if (source == sliderZ) {

            // Change the currently displayed z slice
            zSlice = sliderZ.getValue();
            textZ.setText(String.valueOf(zSlice + 1));
            ((SurfaceRender) renderBase).update3DTriplanar(null, null, false);

            if (myMouseDialog.isRecording() && setSliderFlag) {

                switch (sliderZPos) {

                    case X:
                        sliderEvents.setName("xSlider" + current);
                        myMouseDialog.listModel.addElement("xSlider" + current);
                        break;

                    case Y:
                        sliderEvents.setName("ySlider" + current);
                        myMouseDialog.listModel.addElement("ySlider" + current);
                        break;

                    case Z:
                        sliderEvents.setName("zSlider" + current);
                        myMouseDialog.listModel.addElement("zSlider" + current);
                        break;
                }

                setSliderFlag = false;
            }

            ((SurfaceRender) renderBase).updateBoxSlicePos();

            /* Update the SurfaceRender, and frame ViewJFrameVolumeView based
             * on the relative position of the slider */
            switch (sliderZPos)
            {
            case X:
                ((SurfaceRender) renderBase).updateTriPlanar( ViewJComponentBase.SAGITTAL, (float) (zSlice) / (float) (zDim - 1) );
                break;
            case Y:
                ((SurfaceRender) renderBase).updateTriPlanar( ViewJComponentBase.CORONAL, (float) (zSlice) / (float) (zDim - 1) );
                break;
            case Z:
                ((SurfaceRender) renderBase).updateTriPlanar( ViewJComponentBase.AXIAL, (float) (zSlice) / (float) (zDim - 1) );
                break;
            }
        } else if (source == sliderT) {

            // Change the currently displayed t slice
            tSlice = sliderT.getValue() - 1;
            textT.setText(String.valueOf(tSlice + 1));
            renderBase.updateImages(true);
        } else if (source == opacitySliderX) {
            xOpacitySlice = opacitySliderX.getValue();
            yOpacitySlice = opacitySliderY.getValue();
            zOpacitySlice = opacitySliderZ.getValue();
            ((SurfaceRender) renderBase).updateOpacityOfOthrogPlanes(xOpacitySlice, -1, -1);

            if (myMouseDialog.isRecording() && setSliderFlag) {

                switch (sliderXPos) {

                    case X:
                        sliderEvents.setName("xSliderOpacity" + current);
                        myMouseDialog.listModel.addElement("xSliderOpacity" + current);
                        break;

                    case Y:
                        sliderEvents.setName("ySliderOpacity" + current);
                        myMouseDialog.listModel.addElement("ySliderOpacity" + current);
                        break;

                    case Z:
                        sliderEvents.setName("zSliderOpacity" + current);
                        myMouseDialog.listModel.addElement("zSliderOpacity" + current);
                        break;
                }

                setSliderFlag = false;
            }
        } else if (source == opacitySliderY) {
            xOpacitySlice = opacitySliderX.getValue();
            yOpacitySlice = opacitySliderY.getValue();
            zOpacitySlice = opacitySliderZ.getValue();
            ((SurfaceRender) renderBase).updateOpacityOfOthrogPlanes(-1, yOpacitySlice, -1);

            if (myMouseDialog.isRecording() && setSliderFlag) {

                switch (sliderYPos) {

                    case X:
                        sliderEvents.setName("xSliderOpacity" + current);
                        myMouseDialog.listModel.addElement("xSliderOpacity" + current);
                        break;

                    case Y:
                        sliderEvents.setName("ySliderOpacity" + current);
                        myMouseDialog.listModel.addElement("ySliderOpacity" + current);
                        break;

                    case Z:
                        sliderEvents.setName("zSliderOpacity" + current);
                        myMouseDialog.listModel.addElement("zSliderOpacity" + current);
                        break;
                }

                setSliderFlag = false;
            }
        } else if (source == opacitySliderZ) {
            xOpacitySlice = opacitySliderX.getValue();
            yOpacitySlice = opacitySliderY.getValue();
            zOpacitySlice = opacitySliderZ.getValue();
            ((SurfaceRender) renderBase).updateOpacityOfOthrogPlanes(-1, -1, zOpacitySlice);

            if (myMouseDialog.isRecording() && setSliderFlag) {

                switch (sliderZPos) {

                    case X:
                        sliderEvents.setName("xSliderOpacity" + current);
                        myMouseDialog.listModel.addElement("xSliderOpacity" + current);
                        break;

                    case Y:
                        sliderEvents.setName("ySliderOpacity" + current);
                        myMouseDialog.listModel.addElement("ySliderOpacity" + current);
                        break;

                    case Z:
                        sliderEvents.setName("zSliderOpacity" + current);
                        myMouseDialog.listModel.addElement("zSliderOpacity" + current);
                        break;
                }

                setSliderFlag = false;
            }
        }

        if (myMouseDialog.isRecording()) {
            sliderEvents.add(e, renderBase.getSceneState());
        }
    }

    /**
     * Calls the appropriate method in the parent frame.
     *
     * @param  button  DOCUMENT ME!
     * @param  color   Color to set box frame to.
     */
    protected void setBoxColor(JButton button, Color color) {

        if (button == colorButtonX) {
            ((SurfaceRender) renderBase).setSliceXColor(color);
        } else if (button == colorButtonY) {
            ((SurfaceRender) renderBase).setSliceYColor(color);
        } else if (button == colorButtonZ) {
            ((SurfaceRender) renderBase).setSliceZColor(color);
        }
    }

    /**
     * Helper method that adds components to the control panel for the grid bag layout.
     *
     * @param  c    Component added to the control panel.
     * @param  gbc  GridBagConstraints of added component.
     * @param  x    Gridx location
     * @param  y    Gridy location
     * @param  w    Gridwidth
     * @param  h    Gridheight
     */
    private void addControlPanel(Component c, GridBagConstraints gbc, int x, int y, int w, int h) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        controlPanel.add(c, gbc);
    }

    /**
     * Helper method that adds components to the control panel for the grid bag layout.
     *
     * @param  c    Component added to the control panel.
     * @param  gbc  GridBagConstraints of added component.
     * @param  x    Gridx location
     * @param  y    Gridy location
     * @param  w    Gridwidth
     * @param  h    Gridheight
     */
    private void addOpacityControlPanel(Component c, GridBagConstraints gbc, int x, int y, int w, int h) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        opacityControlPanel.add(c, gbc);
    }

    /**
     * Build the boudning box for X, Y, Z slices.
     */
    private void buildBoundingBox() {

        boundingCheckX = new JCheckBox("Show x slice frame");
        boundingCheckX.setFont(serif12);
        boundingCheckX.addActionListener(this);
        boundingCheckX.setSelected(true);

        colorButtonX = new JButton();
        colorButtonX.setPreferredSize(new Dimension(25, 25));
        colorButtonX.setToolTipText("Change x frame color");
        colorButtonX.addActionListener(this);
        colorButtonX.setBackground(Color.yellow);
        colorButtonX.setEnabled(true);

        boundingCheckY = new JCheckBox("Show y slice frame");
        boundingCheckY.setFont(serif12);
        boundingCheckY.addActionListener(this);
        boundingCheckY.setSelected(true);

        colorButtonY = new JButton();
        colorButtonY.setPreferredSize(new Dimension(25, 25));
        colorButtonY.setToolTipText("Change y frame color");
        colorButtonY.addActionListener(this);
        colorButtonY.setBackground(Color.green);
        colorButtonY.setEnabled(true);

        boundingCheckZ = new JCheckBox("Show z slice frame");
        boundingCheckZ.setFont(serif12);
        boundingCheckZ.addActionListener(this);
        boundingCheckZ.setSelected(true);

        colorButtonZ = new JButton();
        colorButtonZ.setPreferredSize(new Dimension(25, 25));
        colorButtonZ.setToolTipText("Change z frame color");
        colorButtonZ.addActionListener(this);
        colorButtonZ.setBackground(Color.red);
        colorButtonZ.setEnabled(true);

        boundingBoxPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.weightx = 1;
        gbc.anchor = gbc.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 1;
        boundingBoxPanel.add(colorButtonX, gbc);
        gbc.gridx = 1;
        boundingBoxPanel.add(boundingCheckX, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        boundingBoxPanel.add(colorButtonY, gbc);
        gbc.gridx = 1;
        boundingBoxPanel.add(boundingCheckY, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        boundingBoxPanel.add(colorButtonZ, gbc);
        gbc.gridx = 1;
        boundingBoxPanel.add(boundingCheckZ, gbc);
        boundingBoxPanel.setBorder(buildTitledBorder("Slices bounding box"));

    }

    /**
     * Builds the time (4D) slider. No check box, because there is no plane to turn off.
     */
    private void buildTimeSlider() {
        GridBagConstraints cpGBC = new GridBagConstraints();

        cpGBC.fill = GridBagConstraints.BOTH;
        labelT = new JLabel(" T (1 - " + String.valueOf(tDim) + ")");
        labelT.setForeground(Color.black);
        labelT.setFont(MipavUtil.font12);
        labelT.setEnabled(true);
        addControlPanel(labelT, cpGBC, 1, 3, 2, 1);

        sliderT = new JSlider(1, tDim, tSlice + 1);
        sliderT.setFont(MipavUtil.font12);
        sliderT.setEnabled(true);
        sliderT.setMinorTickSpacing(tDim / 10);
        sliderT.setPaintTicks(true);
        sliderT.addChangeListener(this);
        sliderT.setVisible(true);

        JLabel labelT1 = new JLabel("1");

        labelT1.setForeground(Color.black);
        labelT1.setFont(MipavUtil.font12);
        labelT1.setEnabled(true);

        JLabel labelTMid = new JLabel(String.valueOf(tSlice + 1));

        labelTMid.setForeground(Color.black);
        labelTMid.setFont(MipavUtil.font12);
        labelTMid.setEnabled(true);

        JLabel labelTEnd = new JLabel(String.valueOf(tDim));

        labelTEnd.setForeground(Color.black);
        labelTEnd.setFont(MipavUtil.font12);
        labelTEnd.setEnabled(true);

        Hashtable labelTableT = new Hashtable();

        labelTableT.put(new Integer(1), labelT1);
        labelTableT.put(new Integer(tSlice + 1), labelTMid);
        labelTableT.put(new Integer(tDim), labelTEnd);
        sliderT.setLabelTable(labelTableT);
        sliderT.setPaintLabels(true);
        addControlPanel(sliderT, cpGBC, 4, 3, 8, 1);

        textT = new JTextField(String.valueOf(tSlice + 1), 4);
        textT.setFont(MipavUtil.font12);
        textT.setEnabled(false);
        cpGBC.fill = GridBagConstraints.NONE;
        addControlPanel(textT, cpGBC, 14, 3, 1, 1);

    }

    /**
     * Creates a label in the proper font and color.
     *
     * @param   title  The title of the label.
     *
     * @return  The new label.
     */
    private JLabel createLabel(String title) {
        JLabel label = new JLabel(title);

        label.setFont(serif12);
        label.setForeground(Color.black);

        return label;
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    /**
     * Does nothing.
     */
    class CancelListener implements ActionListener {

        /**
         * Does nothing.
         *
         * @param  e  DOCUMENT ME!
         */
        public void actionPerformed(ActionEvent e) { }
    }

    /**
     * Wrapper in order to hold the control panel layout in the JScrollPane.
     */
    class DrawingPanel extends JPanel {

        /** Use serialVersionUID for interoperability. */
        private static final long serialVersionUID = -6456589720445279985L;

        /**
         * DOCUMENT ME!
         *
         * @param  g  DOCUMENT ME!
         */
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
        }
    }

    /**
     * Pick up the selected color and call method to change the VOI color.
     */
    class OkColorListener implements ActionListener {

        /** DOCUMENT ME! */
        JButton button;

        /**
         * Creates a new OkColorListener object.
         *
         * @param  _button  DOCUMENT ME!
         */
        OkColorListener(JButton _button) {
            super();
            button = _button;
        }

        /**
         * Get color from chooser and set button and VOI color.
         *
         * @param  e  Event that triggered function.
         */
        public void actionPerformed(ActionEvent e) {
            Color color = colorChooser.getColor();

            button.setBackground(color);
            setBoxColor(button, color);
        }
    }
}
