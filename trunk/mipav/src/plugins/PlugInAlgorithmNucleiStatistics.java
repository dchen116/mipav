import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.file.*;
import gov.nih.mipav.model.file.FileInfoBase.Unit;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;
import gov.nih.mipav.view.dialogs.JPanelPixelExclusionSelector.RangeType;

import java.io.*;
import java.util.Vector;


/**
 * PlugInAlgorithmNucleiStatistics is used to identify nuclei and output statistics for each nucleus
 * @version  May 9, 2013
 * @author   William Gandler
 * @see      AlgorithmBase 
 */
public class PlugInAlgorithmNucleiStatistics extends AlgorithmBase {
  //~ Instance fields ------------------------------------------------------------------------------------------------
    
    /** The list of files to try to process with the algorithm. */
    private File[] inputFiles;
    
    /** A count of the number of images processed. */
    private int numProcessedImages = 0;
    
    private static final String[] statsToCalculate = new String[] {VOIStatisticalProperties.quantityDescription, VOIStatisticalProperties.areaDescription, 
		VOIStatisticalProperties.perimeterDescription, VOIStatisticalProperties.circularityDescription,
		VOIStatisticalProperties.solidityDescription, VOIStatisticalProperties.minIntensity, VOIStatisticalProperties.maxIntensity,
		VOIStatisticalProperties.avgIntensity, VOIStatisticalProperties.deviationDescription,
        VOIStatisticalProperties.eccentricityDescription, VOIStatisticalProperties.meanCurvatureDescription,
        VOIStatisticalProperties.stdDevCurvatureDescription, VOIStatisticalProperties.meanNegativeCurvatureDescription,
        VOIStatisticalProperties.numberOfIndentationsCurvatureDescription, VOIStatisticalProperties.numberOfIndentationsHullDescription};

    private static final boolean[] checkList = new boolean[VOIStatisticalProperties.numberOfStatistics];

    static {
    	String[] statDescr = VOIStatisticalProperties.statisticDescription;
		for (int i = 0; i < statDescr.length; i++) {
			checkList[i] = false;
			for (int j = 0; j < statsToCalculate.length; j++) {
				if (statDescr[i].equals(statsToCalculate[j])) {
					checkList[i] = true;
					break;
				}
			}
		} 
    }
    
    /**
     * 
     * @param srcImg
     * @param minSize
     * @param maxSize
     */
    public PlugInAlgorithmNucleiStatistics(File[] inputFiles) {
        super(null, null);
        this.inputFiles = inputFiles;
    }
    
  //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        inputFiles = null;
        super.finalize();
    }

    /**
     * Starts the algorithm.
     */
    public void runAlgorithm() {
        for (File inFile : inputFiles) {
        	srcImage = openFile(inFile);

	        if (srcImage == null) {
	            System.err.println("Source Image is null - skipping: " + inFile.getName());
	            continue;
	        }
	        
	        loadAllVOIs(srcImage, false);

	        // run statistics generator on VOIs in image and output to file on disk
	        outputStatistics(srcImage);
	        
	        // TODO: calc overall stats
        	
        	// TODO: gen heatmap image for boundary curvature
	        
	        numProcessedImages++;
	        
	        // cleanup current image
        	srcImage.disposeLocal();
        	srcImage = null;
        }
        
        if (threadStopped) {
            finalize();

            return;
        }

        setCompleted(true);
    }
    
    /**
     * Try to open an image file.
     * @param file The file to try to open.
     * @return The ModelImage of the specified file, or null if there was an error.
     */
    private static final ModelImage openFile(File file) {
    	ModelImage img = null;
    	
    	if (file.isDirectory()) {
    		System.err.println("Skipping directory:\t" + file.getName());
    		return null;
    	} else if (file.getName().startsWith(".")) {
    		System.err.println("Skipping file that starts with .:\t" + file.getName());
    		return null;
    	} else {
    		System.err.println("Trying to open file:\t" + file.getName());
    	}
    	
    	FileIO io = new FileIO();
    	try {
    		img = io.readImage(file.getAbsolutePath());
    	} catch (Exception e) {
    		System.err.println("Failed to open file:\t" + file.getName());
    		e.printStackTrace();
    		img = null;
    	}
    	
    	return img;
    }
    
    /**
     * This method loads all VOIs to the active image from the default VOI directory for that image.
     * @param quietMode if true indicates that warnings should not be displayed.
     */
    private static void loadAllVOIs(ModelImage img, boolean quietMode) {
        String fileDir = img.getFileInfo(0).getFileDirectory();
        String imageName;

        // if the image is a dicom image, then base the new directory name
        // on the actual filename, not the image name
        if (img.isDicomImage()) {
            imageName = img.getFileInfo(0).getFileName();

            final int index = imageName.lastIndexOf(".");

            if (index > 0) {
                imageName = imageName.substring(0, index);
            }

            // now, get rid of any numbers at the end of the name (these
            // are part of the dicom file name, but we only want the 'base'
            // part of the name
            int newIndex = imageName.length();

            for (int i = imageName.length() - 1; i >= 0; i--) {
                final char myChar = imageName.charAt(i);

                if (Character.isDigit(myChar)) {
                    newIndex = i;
                } else {
                    break;
                } // as soon as something is NOT a digit, leave loop
            }

            if (newIndex == 0) {

                // give the base name a generic name
                imageName = new String("DICOM");
            } else {
                imageName = imageName.substring(0, newIndex);
            }
        } else {
            imageName = img.getImageName();
        }

        // get rid of any '^' and ',' which may exist in dicom images
        imageName = imageName.replace('^', '_');
        imageName = imageName.replace(',', '_');

        String voiDir = new String(fileDir + File.separator + "defaultVOIs_" + imageName + File.separator);
        
        int i, j;
        VOI[] VOIs;
        FileVOI fileVOI;

        try {

            // if voiDir does not exist, then return
            // if voiDir exists, then get list of voi's from directory (*.voi)
            final File voiFileDir = new File(voiDir);
            final Vector<String> filenames = new Vector<String>();
            final Vector<Boolean> isLabel = new Vector<Boolean>();

            if (voiFileDir.exists() && voiFileDir.isDirectory()) {

                // get list of files
                final File[] files = voiFileDir.listFiles();

                for (final File element : files) {

                    if (element.getName().endsWith(".voi") || element.getName().endsWith(".xml")) {
                        filenames.add(element.getName());
                        isLabel.add(false);
                    } else if (element.getName().endsWith(".lbl")) {
                        filenames.add(element.getName());
                        isLabel.add(true);
                    }
                }
            } else { // voiFileDir either doesn't exist, or isn't a directory

                if ( !quietMode) {
                    MipavUtil.displayError("No VOIs are found in directory: " + voiDir);
                }

                return;
            }

            // open each voi array, then register voi array to this image
            for (i = 0; i < filenames.size(); i++) {

                fileVOI = new FileVOI( (filenames.elementAt(i)), voiDir, img);

                VOIs = fileVOI.readVOI(isLabel.get(i));

                //if(m_kCurrentVOIGroup != null) {
                //    advanceVOIUID();
                //}
                for (j = 0; j < VOIs.length; j++) {
                    if(VOIs[j].getColor() == null) {
                        //VOIs[j].setColor(toolbarBuilder.getVOIColorButton().getBackground());
                    }
                    img.registerVOI(VOIs[j]);
                    //VOIs[j].addVOIListener(this);
                    //advanceVOIUID();
                }
            }

            // when everything's done, notify the image listeners
            img.notifyImageDisplayListeners();

        } catch (final Exception error) {

            if ( !quietMode) {
                MipavUtil.displayError("Error loading all VOIs from " + voiDir + ": " + error);
            }
        }
    }
    
    private void outputStatistics(ModelImage img) {
    	// TODO: run statistics generator on VOIs in image and output to file on disk
    	AlgorithmVOIProps calculator = new AlgorithmVOIProps(img, AlgorithmVOIProps.PROCESS_PER_CONTOUR, RangeType.NO_RANGE, img.getVOIs());
    	calculator.setSelectedStatistics(checkList);
    	calculator.setShowTotals(false);
    	calculator.run();
    	
    	Vector<VOIStatisticalProperties> statsList = new Vector<VOIStatisticalProperties>(img.getVOIs().size());
    	for (VOI voi : img.getVOIs()) {
    		statsList.add(calculator.getVOIProperties(voi));
    	}
    	
    	File statsDir = new File(img.getFileInfo(0).getFileDirectory() + File.separator + "statistics" + File.separator);
    	if (!statsDir.exists()) { 
    		statsDir.mkdirs();
    	}
    	
    	String statsOutputFile = statsDir.getAbsolutePath() + File.separator + img.getImageName() + ".table";
    	Vector<String> columnHeaders = getColumnHeaders(img);
    	writeStatisticFile(statsOutputFile, columnHeaders, statsList, img.getVOIs());
    }
    
    /**
     * Writes out the statistics file based on the current logModel
     */
    private void writeStatisticFile(String tableDestination, Vector<String> columnHeaders, Vector<VOIStatisticalProperties> statsList, VOIVector VOIs) {
    	final boolean noisyProcess = true;
    	final boolean overwrite = true;
    	final boolean append = false;
    	StringBuffer logFileText = null;
        FileWriter statFW;
        final File statFile = new File(tableDestination);

        try {

            if (statFile.exists()) {

                if (overwrite) {
                    statFile.delete();
                }
            }
        } catch (final SecurityException se) {

            if (noisyProcess) {
                MipavUtil.displayError("security violation incurred while creating \"" + statFile.getName()
                        + "\"; \n" + "is destination directory still writable?  " + "Table file not written.");
            }

            Preferences.debug("security violation incurred while creating \"" + statFile.getName() + "\";\n");

            return;
        }

        try {

            if ( !statFile.createNewFile()) { /* there was an error here! */
            }
        } catch (final IOException io) {
            Preferences.debug("IOexception error in creating statFile!  threw "
                    + "exception rather than createNewFile == false;\n" + io);
            io.printStackTrace();
            Preferences.debug("IO exception while writing VOIStatistic's \"" + statFile.getAbsolutePath()
                    + "\"\n");

            return;
        } catch (final SecurityException se) {

            if (noisyProcess) {
                MipavUtil.displayError("security violation incurred while creating \""
                        + statFile.getAbsolutePath() + "\"; \n" + "is destination directory still writable?  "
                        + "Table file not written.");
            }

            Preferences.debug("security violation incurred while creating \"" + statFile.getAbsolutePath()
                    + "\";\n");

            return;
        }

        try {

            if (overwrite) {
                statFW = new FileWriter(statFile.getAbsolutePath(), false);
            } else if (append) {
                statFW = new FileWriter(statFile.getAbsolutePath(), true);           
            } else { // WRITE
                statFW = new FileWriter(statFile.getAbsolutePath());
            }
        
            logFileText = writeStatsToString(columnHeaders, statsList, VOIs);
            statFW.write(logFileText.toString());
            statFW.flush();
            statFW.close();
        } catch (final IOException ioe) {

            if (noisyProcess) {
                MipavUtil.displayError("error writing the logging to \"" + statFile.getAbsolutePath() + "\""); // figure
                // out
                // where
                // to
                // store
                // somewhere
                // else?
            }
        } finally {
        	if (logFileText != null) {
        		logFileText.delete(0, logFileText.length() - 1); // empty out the buffer
        	}
        }
    }
    
    /**
     * Converts the current logModel into either a tab-delimited text file or an XML file.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    protected StringBuffer writeStatsToString(Vector<String> columnHeaders, Vector<VOIStatisticalProperties> statsList, VOIVector VOIs) {
        StringBuffer total = new StringBuffer();
        String newLine = System.getProperty("line.separator");
        //get column names
        for(int i=0; i<columnHeaders.size(); i++) {
            total.append(columnHeaders.get(i)).append("\t");
        }
        total.append(newLine);
        
        //get total data
        Vector<Vector<String>> column = getStatsData(statsList, VOIs);
        Vector<String> row;
        String cellEntry;
        for(int i=0; i<column.size(); i++) {
            row = column.get(i);
            for(int j=0; j<row.size(); j++) {
                if(row.get(j) == null || row.get(j).toString().length() == 0) {
                    cellEntry = " ";
                } else {
                    cellEntry = row.get(j).toString();
                }
                total.append(cellEntry).append("\t");
            }
            total.append(newLine);
        }
        
        return total;
    }
    
    /**
     * Writes the column titles of selected statistics calculations to the logModel.
     */
    private static final Vector<String> getColumnHeaders(ModelImage img) {
        Vector<String> logModelCol = new Vector<String>();
        logModelCol.add("Name");
        
        // assume that always PER_CONTOUR
        logModelCol.add("Contour");
        
        int totalCount = 0;
        String str;
        
        int xUnits = img.getFileInfo(0).getUnitsOfMeasure()[0];
        int yUnits = img.getFileInfo(0).getUnitsOfMeasure()[1];
        int zUnits = Unit.UNKNOWN_MEASURE.getLegacyNum();
        
        for (int i = 0; i < VOIStatisticList.numberOfStatistics; i++) {
    
            //add statistic to column list if selected by user
            if (checkList[i]) {
                if ( (VOIStatisticList.statisticDescription[i].indexOf("Volume") != -1) && (xUnits == yUnits)
                        && (xUnits == zUnits) && (xUnits != Unit.UNKNOWN_MEASURE.getLegacyNum())) {
                    str = img.getFileInfo(0).getVolumeUnitsOfMeasureStr().trim();
                    logModelCol.add(VOIStatisticList.statisticDescription[i] + " (" + str + ")");
                } else if ( (VOIStatisticList.statisticDescription[i].indexOf("Area") != -1)
                        && (xUnits == yUnits) && (xUnits != Unit.UNKNOWN_MEASURE.getLegacyNum())) {
                    str = img.getFileInfo(0).getAreaUnitsOfMeasureStr().trim();
                    logModelCol.add(VOIStatisticList.statisticDescription[i] + " (" + str + ")");
                } else if ( (VOIStatisticList.statisticDescription[i].indexOf("Perimeter") != -1)
                        && (xUnits == yUnits) && (xUnits != Unit.UNKNOWN_MEASURE.getLegacyNum())) {
                    str = (Unit.getUnitFromLegacyNum(xUnits)).getAbbrev();
                    logModelCol.add(VOIStatisticList.statisticDescription[i] + " (" + str + ")");
                } else if (VOIStatisticList.statisticDescription[i].indexOf("Principal Axis") != -1) {
                    logModelCol.add(VOIStatisticList.statisticDescription[i] + " (degrees)");
                } else if ( (VOIStatisticList.statisticDescription[i].indexOf("Major axis length") != -1)
                        && (xUnits == yUnits) && (xUnits != Unit.UNKNOWN_MEASURE.getLegacyNum())) {
                    str = (Unit.getUnitFromLegacyNum(xUnits)).getAbbrev();
                    logModelCol.add(VOIStatisticList.statisticDescription[i] + " (" + str + ")");
                } else if ( (VOIStatisticList.statisticDescription[i].indexOf("Minor axis length") != -1)
                        && (xUnits == yUnits) && (xUnits != Unit.UNKNOWN_MEASURE.getLegacyNum())) {
                    str = (Unit.getUnitFromLegacyNum(xUnits)).getAbbrev();
                    logModelCol.add(VOIStatisticList.statisticDescription[i] + " (" + str + ")");
                } else {
                    logModelCol.add(VOIStatisticList.statisticDescription[i]);
                }
    
                // total count used for total # of data elements, need to add 3 if color
                // image and intensity related (R,G,B)
                totalCount++;
    
                if (img.isColorImage() && (VOIStatisticList.statisticDescription[i].indexOf("Intensity") != -1)) {
                    totalCount += 2;
                }
            }
        }

        return logModelCol;
    }
    
    private Vector<Vector<String>> getStatsData(Vector<VOIStatisticalProperties> statsList, VOIVector VOIs) {
    	Vector<Vector<String>> data = new Vector<Vector<String>>();
    	int voiIndex = 0;
    	for (VOIStatisticalProperties prop : statsList) {
    		VOIBaseVector contours = VOIs.get(voiIndex).getCurves();
    		for (VOIBase voi : contours) {
	    		Vector<String> row = new Vector<String>();
	    		String contourLabel = voi.getLabel();
	    		row.add(VOIs.get(voiIndex).getName().replaceAll("[\\t+]", ", ").replaceAll("[\\n\\r+]", ":"));
	    		row.add(contourLabel.replaceAll("[\\t+]", ", ").replaceAll("[\\n\\r+]", ":"));
		    	for (int i = 0; i < checkList.length; i++) {
		        	if (checkList[i]) {
		        		row.add(prop.getProperty(VOIStatisticList.statisticDescription[i] + contourLabel).replaceAll("[\\t+]", ", ").replaceAll("[\\n\\r+]", ":"));
		        	}
		        }
		    	data.add(row);
    		}
	    	voiIndex++;
    	}
    	
    	return data;
    	
    	// end = contours[slice].elementAt(num).getLabel()
    	//VOIBaseVector contours = ((VOI) list.getElementAt(i)).getCurves();
        //updateDialogRow((VOI) list.getElementAt(i), new Vector[]{contours}, properties, list, i, rowData, totalData);
    }
    
    public int getNumProcessedImages() {
    	return numProcessedImages;
    }
}