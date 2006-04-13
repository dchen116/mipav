package gov.nih.mipav.view.dialogs;

import gov.nih.mipav.view.*;
import gov.nih.mipav.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.event.ActionEvent;
import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.model.file.*;

/**
 *   Dialog to transform an image in the tri planar view based on a bounding box.
 */
public class JDialogTriImageTransformation extends JDialogBase
{
    /** Image to transform.*/
    private ModelImage imageA;

    /** Image B to transform, can be null. */
    private ModelImage imageB;

    /** Pointer back to frame that called this. */
    private ViewJFrameTriImage frame;

    /** Progress bar. */
    private ViewJProgressBar progressBar;

    private JComboBox comboBoxInterp;

    private double thetaXY, thetaXZ, thetaZY; // rotation angles in degrees, the negative of these
    // angles are passed to setRotate
    private double centerX, centerY, centerZ; // centers of original images
    private int boxIndex = 0; // index from interpolation combo box
    private double xfrmD[][] = new double[4][4]; // (xfrm.inverse()).getArray()


    /**
     *   Creates confirmation dialog for cropping triplanar image.
     *   @param theParentFrame   Pointer to the frame that created this dialog.
     *   @param imA              Image A to be transformed.
     *   @param imB              Image B to be transformed (can be null).
     */
    public JDialogTriImageTransformation(ViewJFrameTriImage theParentFrame, ModelImage imA, ModelImage imB)
    {
        super(theParentFrame, true);
        frame = theParentFrame;
        imageA = imA;
        imageB = imB;
        centerX = frame.getCenter()[0];
        centerY = frame.getCenter()[1];
        centerZ = frame.getCenter()[2];
        init();
    }

    /**
     *   Creates confirmation dialog for transforming triplanar image.
     *   @param theParentFrame   Pointer to the frame that created this dialog.
     *   @param im               Image to be transformed.
     */
    public JDialogTriImageTransformation(ViewJFrameTriImage theParentFrame, ModelImage im)
    {
        this(theParentFrame, im, null);
    }


    /**
     *   Initializes GUI components and displays dialog.
     */
    private void init()
    {
        setTitle("Apply transformation matrix");

        GridBagConstraints gbc = new GridBagConstraints();
        
        JPanel interpolationPanel = new JPanel(new GridBagLayout());
        interpolationPanel.setBorder(buildTitledBorder("Interpolation"));

        JLabel labelInterp = new JLabel("Interpolation:");
        labelInterp.setForeground(Color.black);
        labelInterp.setFont(serif12);
        labelInterp.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboBoxInterp = new JComboBox();
        comboBoxInterp.setFont(serif12);
        comboBoxInterp.setBackground(Color.white);
        comboBoxInterp.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboBoxInterp.addItem("Trilinear");
        comboBoxInterp.addItem("Bspline 3rd order");
        comboBoxInterp.addItem("Bspline 4th order");
        // comboBoxInterp.addItem("Nearest Neighbor");
        comboBoxInterp.setBounds(100, 20, 120, 30);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        interpolationPanel.add(labelInterp, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        interpolationPanel.add(comboBoxInterp, gbc);

        buildOKButton();
        OKButton.setText("Apply");
        buildCancelButton();

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(OKButton);
        buttonPanel.add(cancelButton);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(interpolationPanel, gbc);
        gbc.gridy = 1;
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        getContentPane().add(mainPanel);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setVisible(true);
    }

    /**
     *  Calls transform methods to transform image if "Apply" is pressed; if "Cancel" is pressed, disposes.
     *  @param event Event that triggered function.
     */
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("Apply"))
        {
            boxIndex = comboBoxInterp.getSelectedIndex();

            dispose();

            final SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    transform(imageA);
                    if (imageB != null)
                    {
                        transform(imageB);
                    }

                    return null;
                }
            };

            worker.start();

        }
        else if (command.equals("Cancel"))
        {
            dispose();
        }
    }

    /**
     *   Sets the transformation matrix and sends the image data to the appropriate
     *   interpolation routine.
     *   @param image    Image on which to perform the transformation.
     */
    private void transform(ModelImage image)
    {
    	try
    	{
	        ModelImage clonedImage = null;
	        float[] imgBuffer;
	        int bufferSize;
	        
	        progressBar = new ViewJProgressBar("Preparing image ...", "Transforming image ...", 0, 100, false, null, null);
	        progressBar.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width / 2, 50);
	        progressBar.setSeparateThread(!SwingUtilities.isEventDispatchThread());
	        MipavUtil.centerOnScreen(progressBar);
	        progressBar.setVisible(true);
	
	        if (image.isColorImage()) {
	            bufferSize = 4 * image.getSliceSize() * image.getExtents()[2];
	        }
	        else {
	            bufferSize = image.getSliceSize() * image.getExtents()[2];
	        }
	
            clonedImage = (ModelImage) image.clone();
            clonedImage.resetVOIs();
            
            int selectedImage;
            boolean oldLayout = Preferences.is(Preferences.PREF_TRIPLANAR_2X2_LAYOUT);
            
            if (frame.getTriImage(ViewJFrameTriImage.AXIAL_B) != null)
            {
	            if (oldLayout == true)
	            {
	            	// if old 2x2 layout is in use (which means the image is the composite AB) 
	            	// we must force selectedImage to be BOTH
	            	selectedImage = ViewJComponentBase.BOTH;
	            }
	            else
	            {
	            	selectedImage = frame.getSelectedImage();
	            }
            }
            else
            {
            	selectedImage = ViewJComponentBase.IMAGE_A;
            }
            
            if (selectedImage == ViewJComponentBase.IMAGE_A)
            {
		        thetaXY = ( (ViewJComponentTriImage) frame.getTriImage(ViewJFrameTriImage.AXIAL_A)).getTheta();
		        thetaXZ = ( (ViewJComponentTriImage) frame.getTriImage(ViewJFrameTriImage.CORONAL_A)).getTheta();
		        thetaZY = ( (ViewJComponentTriImage) frame.getTriImage(ViewJFrameTriImage.SAGITTAL_A)).getTheta();
            }
            else if (selectedImage == ViewJComponentBase.IMAGE_B)
            {
		        thetaXY = ( (ViewJComponentTriImage) frame.getTriImage(ViewJFrameTriImage.AXIAL_B)).getTheta();
		        thetaXZ = ( (ViewJComponentTriImage) frame.getTriImage(ViewJFrameTriImage.CORONAL_B)).getTheta();
		        thetaZY = ( (ViewJComponentTriImage) frame.getTriImage(ViewJFrameTriImage.SAGITTAL_B)).getTheta();
            } 
            else
            {
		        thetaXY = ( (ViewJComponentTriImage) frame.getTriImage(ViewJFrameTriImage.AXIAL_AB)).getTheta();
		        thetaXZ = ( (ViewJComponentTriImage) frame.getTriImage(ViewJFrameTriImage.CORONAL_AB)).getTheta();
		        thetaZY = ( (ViewJComponentTriImage) frame.getTriImage(ViewJFrameTriImage.SAGITTAL_AB)).getTheta();
            }
	        
	        TransMatrix xfrm = new TransMatrix(4);
	        xfrm.identity();
	
	        xfrm.setTranslate(centerX * image.getFileInfo()[0].getResolutions()[0],
	                          centerY * image.getFileInfo()[0].getResolutions()[1],
	                          centerZ * image.getFileInfo()[0].getResolutions()[2]);
	
	        int imageOrient = imageA.getFileInfo(0).getImageOrientation();
	        if (imageOrient == FileInfoBase.AXIAL){
	            xfrm.setRotate( -thetaZY, -thetaXZ, -thetaXY, TransMatrix.DEGREES);
	        }
	        else if (imageOrient == FileInfoBase.CORONAL){
	            xfrm.setRotate( -thetaXY, -thetaZY, -thetaXZ, TransMatrix.DEGREES);
	        }
	        else if (imageOrient == FileInfoBase.SAGITTAL){
	            xfrm.setRotate( -thetaXZ, -thetaXY, -thetaZY, TransMatrix.DEGREES);
	        }
	        else
	        {
	            xfrm.setRotate( -thetaZY, -thetaXZ, -thetaXY, TransMatrix.DEGREES);
	        }
	
	        xfrm.setTranslate( - (double) centerX * image.getFileInfo()[0].getResolutions()[0],
	                           - (double) centerY * image.getFileInfo()[0].getResolutions()[1],
	                           - (double) centerZ * image.getFileInfo()[0].getResolutions()[2]);
	
	        System.gc();
	        imgBuffer = new float[bufferSize];
	
	        clonedImage.exportData(0, bufferSize, imgBuffer);
	        
	        progressBar.setTitle("Transforming image ...");
	        
	        if (image.getNDims() == 3)
	        {
	            if (boxIndex == 0)
	            {
	                if (!image.isColorImage())
	                {
	                    image = doTrilinear(clonedImage, null, imgBuffer, xfrm, progressBar); // black and white
	                }
	                else
	                {
	                    image = AlgorithmTransform.transformTrilinearC(image, clonedImage, imgBuffer, xfrm, xfrmD, progressBar); // color
	                }
	            }
	            else if (boxIndex == 1)
	            {
	                if (!image.isColorImage())
	                {
	                    image = AlgorithmTransform.bspline(image, clonedImage, 3, xfrm, progressBar); // black and white
	                }
	                else
	                {
	                    image = AlgorithmTransform.bsplineC(image, clonedImage, 3, xfrm, progressBar); // color
	                }
	            }
	            else if (boxIndex == 2)
	            {
	                if (!image.isColorImage())
	                {
	                    image = AlgorithmTransform.bspline(image, clonedImage, 4, xfrm, progressBar); // black and white
	                }
	                else
	                {
	                    image = AlgorithmTransform.bsplineC(image, clonedImage, 4, xfrm, progressBar); // color
	                }
	            }
	        } // if (image.getNDims() == 3)
	        else if (image.getNDims() == 4)
	        {
	            if (boxIndex == 0)
	            {
	                if (!image.isColorImage())
	                {
	                    image = AlgorithmTransform.transformTrilinear4D(image, clonedImage, xfrm, progressBar); // black and white
	                }
	                else
	                {
	                    image = AlgorithmTransform.transformTrilinearC4D(image, clonedImage, xfrm, progressBar); // color
	                }
	            }
	            else if (boxIndex == 1)
	            {
	                if (!image.isColorImage())
	                {
	                    image = AlgorithmTransform.bspline4D(image, clonedImage, 3, xfrm, progressBar); // black and white
	                }
	                else
	                {
	                    image = AlgorithmTransform.bsplineC4D(image, clonedImage, 3, xfrm, progressBar); // color
	                }
	            }
	            else if (boxIndex == 2)
	            {
	                if (!image.isColorImage())
	                {
	                    image = AlgorithmTransform.bspline4D(image, clonedImage, 4, xfrm, progressBar); // black and white
	                }
	                else
	                {
	                    image = AlgorithmTransform.bsplineC4D(image, clonedImage, 4, xfrm, progressBar); // color
	                }
	            }
	        } // else if (image.getNDims == 4)
	        
	        new ViewJFrameImage(image);
    	}
    	catch (Throwable t)
    	{
    		MipavUtil.displayError("Error: unable to complete transform algorithm");
    		t.printStackTrace();
    	}
    }

    /**
     *   Performs trilinear interpolation on black and white image data
     *   @param image        Image from which the data is derived
     *   @param resultImage  Image to put result in; can be null.
     *   @param imgBuffer    Buffer containing image data.
     *   @param xfrm         Transformation to apply.
     */
    public static ModelImage doTrilinear(ModelImage image,
                                                ModelImage resultImage,
                                                float[] imgBuffer,
                                                TransMatrix xfrm,
                                                ViewJProgressBar progressBar)
    {
        int xDim = image.getExtents()[0];
        int yDim = image.getExtents()[1];
        int zDim = image.getExtents()[2];

        float xRes = image.getFileInfo(0).getResolutions()[0];
        float yRes = image.getFileInfo(0).getResolutions()[1];
        float zRes = image.getFileInfo(0).getResolutions()[2];

        if (resultImage != null)
        {
            AlgorithmTransform.transformTrilinear(imgBuffer, resultImage, xfrm,
                                                  xDim, yDim, zDim, xRes, yRes, zRes,
                                                  progressBar, true);
        }
        else
        {
            AlgorithmTransform.transformTrilinear(imgBuffer, image, xfrm,
                                                  xDim, yDim, zDim, xRes, yRes, zRes,
                                                  progressBar, true);
        }

        if (resultImage != null)
        {
            resultImage.calcMinMax();
        }
        else
        {
            image.calcMinMax();
        }

        //new ViewJFrameImage(resultImage, null, new Dimension(610, 200), image.getLogMagDisplay());
        return image;
    }



    public JPanel getMainPanel()
    {
        setTitle("Apply transformation matrix");

        GridBagConstraints gbc = new GridBagConstraints();

        JPanel interpolationPanel = new JPanel(new GridBagLayout());
        interpolationPanel.setBorder(buildTitledBorder("Interpolation"));

        JLabel labelInterp = new JLabel("Interpolation:");
        labelInterp.setForeground(Color.black);
        labelInterp.setFont(serif12);
        labelInterp.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboBoxInterp = new JComboBox();
        comboBoxInterp.setFont(serif12);
        comboBoxInterp.setBackground(Color.white);
        comboBoxInterp.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboBoxInterp.addItem("Trilinear");
        comboBoxInterp.addItem("Bspline 3rd order");
        comboBoxInterp.addItem("Bspline 4th order");
        // comboBoxInterp.addItem("Nearest Neighbor");
        comboBoxInterp.setBounds(100, 20, 120, 30);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        interpolationPanel.add(labelInterp, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        interpolationPanel.add(comboBoxInterp, gbc);

        buildOKButton();
        OKButton.setText("Apply");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(OKButton);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(interpolationPanel, gbc);
        gbc.gridy = 1;

        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel panel = new JPanel();
        panel.add(mainPanel);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

}
