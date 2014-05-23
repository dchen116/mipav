package gov.nih.mipav.model.file;


import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.view.dialogs.*;


/**
 * File info storage container.
 */
public class FileInfoCZI extends FileInfoBase {
	
	//~ Static fields/initializers -------------------------------------------------------------------------------------
	private String focusPosition = null;
	private String acquisitionTime = null;
	private String stageXPosition = null;
	private String stageYPosition = null;
	private String validBitsPerPixel = null;
	private double timeStamps[] = null;
	private String imageName = null;
	private String author = null;
	private String userName = null;
	private String subType = null;
	private String title = null;
	
	//~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * File info storage constructor.
     *
     * @param  name       file name
     * @param  directory  directory
     * @param  format     file format
     */
    public FileInfoCZI(String name, String directory, int format) {
        super(name, directory, format);
    }
    
  //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Displays the file information.
     *
     * @param  dlog    dialog box that is written to
     * @param  matrix  transformation matrix
     */
    public void displayAboutInfo(JDialogBase dlog, TransMatrix matrix) {
    	int i;
        JDialogText dialog = (JDialogText) dlog;
        displayPrimaryInfo(dialog, matrix);
        dialog.append("\n\n                Other information\n\n");
        
        if (imageName != null) {
        	dialog.append("Image name = " + imageName + "\n");
        }
        
        if (author != null) {
        	dialog.append("Author = " + author + "\n");
        }
        
        if (userName != null) {
        	dialog.append("User name = " + userName + "\n");
        }
        
        if (subType != null) {
        	dialog.append("SubType = " + subType + "\n");
        }
        
        if (title != null) {
        	dialog.append("Title = " + title + "\n");
        }
        
        if (focusPosition != null) {
        	dialog.append("Focus position in micrometers = " + focusPosition + "\n");
        }
        
        if (acquisitionTime != null) {
        	dialog.append("Acquisition time = " + acquisitionTime + "\n");
        }
        
        if (stageXPosition != null) {
        	dialog.append("Stage axis X position in micrometers = " + stageXPosition + "\n");
        }
        
        if (stageYPosition != null) {
        	dialog.append("Stage axis Y position in micrometers = " + stageYPosition + "\n");
        }
        
        if (validBitsPerPixel != null) {
        	dialog.append("Valid bits per pixel = " + validBitsPerPixel + "\n");
        }
        
        if (timeStamps != null) {
        	dialog.append("Time stamps in seconds relative to the start time of acquisition:\n");
        	for (i = 0; i < timeStamps.length; i++) {
        	    dialog.append("Time stamp  " + i + ":     " + timeStamps[i] + "\n");	
        	}
        }
    }
    
    /**
     * 
     * @param focusPosition
     */
    public void setFocusPosition(String focusPosition) {
    	this.focusPosition = focusPosition;
    }
    
    /**
     * 
     * @param acquisitionTime
     */
    public void setAcquisitionTime(String acquisitionTime) {
    	this.acquisitionTime = acquisitionTime;
    }
    
    /**
     * 
     * @param stageXPosition
     */
    public void setStageXPosition(String stageXPosition) {
    	this.stageXPosition = stageXPosition;
    }
    
    /**
     * 
     * @param stageYPosition
     */
    public void setStageYPosition(String stageYPosition) {
    	this.stageYPosition = stageYPosition;
    }
    
    /**
     * 
     * @param validBitsPerPixel
     */
    public void setValidBitsPerPixel(String validBitsPerPixel) {
    	this.validBitsPerPixel = validBitsPerPixel;
    }
    
    /**
     * 
     * @param timeStamps
     */
    public void setTimeStamps(double timeStamps[]) {
    	this.timeStamps = timeStamps;
    }
    
    /**
     * 
     * @param imageName
     */
    public void setImageName(String imageName) {
    	this.imageName = imageName;
    }
    
    /**
     * 
     * @param author
     */
    public void setAuthor(String author) {
    	this.author = author;
    }
    
    /**
     * 
     * @param userName
     */
    public void setUserName(String userName) {
    	this.userName = userName;
    }
    
    /**
     * 
     * @param subType
     */
    public void setSubType(String subType) {
    	this.subType = subType;
    }
    
    /**
     * 
     * @param title
     */
    public void setTitle(String title) {
    	this.title = title;
    }
}