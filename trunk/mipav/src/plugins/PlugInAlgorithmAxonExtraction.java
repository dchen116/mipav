import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.awt.*;

import java.io.*;

import java.util.*;
import javax.swing.JOptionPane;
import WildMagic.LibFoundation.Mathematics.Vector3f;


/**
 *
 * @version  March 26, 2009
 * @author   William Gandler
 * @see      AlgorithmBase
 *
 
 */
public class PlugInAlgorithmAxonExtraction extends AlgorithmBase {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private int redRadius = 3;
    
    private int greenRadius = 3;
    
    
    private int xDim = srcImage.getExtents()[0];
    private int yDim = srcImage.getExtents()[1];
    private int zDim = srcImage.getExtents()[2];
    private int length = xDim * yDim * zDim;
    private int xySlice = xDim * yDim;
    
    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Constructor.
     *
     * @param  srcImg         Source image model.
     * @param  redRadius      Radius of red disc in preprocessing morphology
     */
    public PlugInAlgorithmAxonExtraction(ModelImage srcImg, int redRadius, int greenRadius) {
        super(null, srcImg);
        this.redRadius = redRadius;
        this.greenRadius = greenRadius;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        srcImage = null;
        super.finalize();
    }

    /**
     * Starts the algorithm.
     */
    public void runAlgorithm() {

        if (srcImage == null) {
            displayError("Source Image is null");

            return;
        }

        

       calc3D();
    } // end runAlgorithm()

    /**
     * DOCUMENT ME!
     */
    private void calc3D() {
        long time;
        int z;
        short shortBuffer[];
        short shortOpen[];
        short shortClose[];
        byte byteBuffer[];
        short redBuffer[];
        short greenBuffer[];
        short blueBuffer[];
        int extents2D[];
        ModelImage shortImage;
        int color;
        AlgorithmGrayScaleMorphology2D openAlgo2D;
        AlgorithmGrayScaleMorphology2D closeAlgo2D;
        int kernelSize;
        int itersD = 1;
        int itersE = 1;
        int i;
        boolean pointsEntered;
        Vector[] curves;
        int nPts;
        int s;
        Vector3f[] pt = null;
        Vector3f[] tmpPt = null;
        int ptNum = 0;

        time = System.currentTimeMillis();

        if (threadStopped) {
            finalize();

            return;
        }
        
        // Do 2.5D morphological preprocessing on red and green structures
        // Use a disc
        // I = I + Itop - Ibottom
        // Itop = I - Iopen
        // Ibottom = Iclose - I
        // I = I + I - Iopen - Iclose + I = 3*I - Iopen - IClose
        shortBuffer = new short[xySlice];
        shortOpen = new short[xySlice];
        shortClose = new short[xySlice];
        byteBuffer = new byte[xySlice];
        extents2D = new int[2];
        extents2D[0] = xDim;
        extents2D[1] = yDim;
        shortImage = new ModelImage(ModelImage.SHORT, extents2D, "short_image");
        for (color = 1; color <= 2; color++) {
            if (((color == 1) && (redRadius >= 1)) || ((color == 2) && (greenRadius >= 1))) {
                if (color == 1) {
                    kernelSize = redRadius;
                }
                else {
                    kernelSize = greenRadius;
                }
                for (z = 0; z < zDim; z++) {
                    fireProgressStateChanged(100 *((color - 1)* zDim +  z)/(2 * zDim));
                    // Do morphological preprocessing on red and green
                    
                        try {
                            srcImage.exportRGBData(color, 4*z*xySlice, xySlice, shortBuffer); // export color data
                        } catch (IOException error) {
                            shortBuffer = null;
                            shortOpen = null;
                            shortClose = null;
                            errorCleanUp("Algorithm Axon extraction reports: source image locked", true);
                
                            return;
                        }
                        
                        System.arraycopy(shortBuffer, 0, shortOpen, 0, shortBuffer.length);
                        System.arraycopy(shortBuffer, 0, shortClose, 0, shortBuffer.length);
                        
                        try {
                            shortImage.importData(0, shortOpen, true);
                        }
                        catch(IOException error) {
                            shortBuffer = null;
                            shortOpen = null;
                            shortClose = null;
                            errorCleanUp("Algorithm Axon extraction reports: shortImage locked", true);
                            
                            return;
                        }
                        
                        openAlgo2D = new AlgorithmGrayScaleMorphology2D(shortImage, AlgorithmMorphology2D.SIZED_CIRCLE, kernelSize, AlgorithmMorphology2D.OPEN,
                                itersD, itersE, 0, 0, true);
                        openAlgo2D.run();
                        openAlgo2D.finalize();
                        openAlgo2D = null;
                        
                        try {
                            shortImage.exportData(0, xySlice, shortOpen);
                        }
                        catch(IOException error) {
                            shortBuffer = null;
                            shortOpen = null;
                            shortClose = null;
                            errorCleanUp("Algorithm Axon extraction reports: shortImage locked", true);
                            
                            return;
                        }
                        
                        try {
                            shortImage.importData(0, shortClose, true);
                        }
                        catch(IOException error) {
                            shortBuffer = null;
                            shortOpen = null;
                            shortClose = null;
                            errorCleanUp("Algorithm Axon extraction reports: shortImage locked", true);
                            
                            return;
                        }
                        
                        closeAlgo2D = new AlgorithmGrayScaleMorphology2D(shortImage, AlgorithmMorphology2D.SIZED_CIRCLE, kernelSize, AlgorithmMorphology2D.CLOSE,
                                itersD, itersE, 0, 0, true);
                        closeAlgo2D.run();
                        closeAlgo2D.finalize();
                        closeAlgo2D = null;
                        
                        try {
                            shortImage.exportData(0, xySlice, shortClose);
                        }
                        catch(IOException error) {
                            shortBuffer = null;
                            shortOpen = null;
                            shortClose = null;
                            errorCleanUp("Algorithm Axon extraction reports: shortImage locked", true);
                            
                            return;
                        }
                        
                        for (i = 0; i < xySlice; i++) {
                            shortBuffer[i] = (short)(3 * shortBuffer[i] - shortOpen[i] - shortClose[i]); 
                            if (shortBuffer[i] > 255) {
                                shortBuffer[i] = 255;
                            }
                            else if (shortBuffer[i] < 0) {
                                shortBuffer[i] = 0;
                            }
                            byteBuffer[i] = (byte)(shortBuffer[i]);
                        }
                        
                        try {
                            srcImage.importRGBData(color, 4*z*xySlice, byteBuffer, false); // import color data
                        } catch (IOException error) {
                            shortBuffer = null;
                            shortOpen = null;
                            shortClose = null;
                            errorCleanUp("Algorithm Axon extraction reports: source image locked", true);
                
                            return;
                        }
                    } // for (z = 0; z < zDim; z++)
            } // if (((color == 1) && (redRadius >= 1)) || ((color == 2) && (greenRadius >= 1)))
        } // for (color = 1; color <= 2; color++)
        shortBuffer = null;
        shortOpen = null;
        shortClose = null;
        srcImage.calcMinMax();
        

        fireProgressStateChanged(100);
        time = System.currentTimeMillis() - time;
        Preferences.debug("PlugInAlgorithmAxonExtraction preprocessing elapsed time in seconds = " + (time/1000.0));
        
        pointsEntered = false;
        while (!pointsEntered) {
        JOptionPane.showMessageDialog(null, "Click okay after entering extraction points", "Point Entry",
                                      JOptionPane.PLAIN_MESSAGE);
            if (srcImage.getVOIs().size() == 0) {
                JOptionPane.showMessageDialog(null, "No extraction points were entered", "Points Not Entered",
                                      JOptionPane.ERROR_MESSAGE);   
            }
            else {
                pointsEntered = true;
            }
        } // while (!pointsEntered)
        
        curves = srcImage.getVOIs().VOIAt(0).getCurves(); // curves[s] holds all VOIs in slice s

        nPts = 0;
        for (i = 0; i < srcImage.getExtents()[2]; i++) {
            nPts += curves[i].size();
        }
        Preferences.debug("nPts = " + nPts + "\n");
        pt = new Vector3f[nPts];
        
        ptNum = 0;
        for (s = 0; s < srcImage.getExtents()[2]; s++) {
            tmpPt = srcImage.getVOIs().VOIAt(0).exportPoints(s);

            for (i = 0; i < tmpPt.length; i++) {
                pt[ptNum++] = tmpPt[i];
            }
        }
        
        redBuffer = new short[length];
        try {
            srcImage.exportRGBData(1, 0, length, redBuffer); // export red data
        } catch (IOException error) {
            redBuffer = null;
            errorCleanUp("Algorithm Axon extraction reports: source image locked", true);

            return;
        }
        
        greenBuffer = new short[length];
        try {
            srcImage.exportRGBData(2, 0, length, greenBuffer); // export green data
        } catch (IOException error) {
            redBuffer = null;
            greenBuffer = null;
            errorCleanUp("Algorithm Axon extraction reports: source image locked", true);

            return;
        }
        
        blueBuffer = new short[length];
        try {
            srcImage.exportRGBData(3, 0, length, blueBuffer); // export blue data
        } catch (IOException error) {
            redBuffer = null;
            greenBuffer = null;
            blueBuffer = null;
            errorCleanUp("Algorithm Axon extraction reports: source image locked", true);

            return;
        }
        
        time = System.currentTimeMillis();
        fireProgressStateChanged(0);
        
        fireProgressStateChanged(100);
        time = System.currentTimeMillis() - time;
        Preferences.debug("PlugInAlgorithmAxonExtraction processing elapsed time in seconds = " + (time/1000.0));
        setCompleted(true);
    }
    
    /**
     * DOCUMENT ME!
     */
    private void calc2D() {
        long time;
        
        
        fireProgressStateChanged("Synapse detection on image");

        time = System.currentTimeMillis();
        
         
        fireProgressStateChanged(100);
        time = System.currentTimeMillis() - time;
        Preferences.debug("PlugInAlgorithmAxonExtraction elapsed time in seconds = " + (time/1000.0));
        setCompleted(true);
        
    }
    
}
