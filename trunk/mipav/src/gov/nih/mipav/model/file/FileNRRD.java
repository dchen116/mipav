package gov.nih.mipav.model.file;


import gov.nih.mipav.model.algorithms.utilities.*;
import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.model.structures.jama.*;

import gov.nih.mipav.view.*;
import gov.nih.mipav.view.dialogs.*;

import java.awt.Toolkit;
import java.io.*;
import java.util.zip.GZIPInputStream;


/**
 
 * @see  FileIO
 * @see  FileInfoNRRD
 * @see  FileRaw
 */

public class FileNRRD extends FileBase {

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    private int[] axisOrientation;

    

    /** DOCUMENT ME! */
    private String fileDir;

    /** DOCUMENT ME! */
    private File file;

    /** DOCUMENT ME! */
    private FileInfoNRRD fileInfo = null;

    /** DOCUMENT ME! */
    private String fileName;

    /** DOCUMENT ME! */
    private ModelImage image;

    
    /** DOCUMENT ME! */
    private TransMatrix matrix = new TransMatrix(4);


    /** DOCUMENT ME! */
    private boolean oneFileStorage;

    /** DOCUMENT ME! */
    private float[] origin;

   
    /** DOCUMENT ME! */
    private boolean showProgress = true;

    /** DOCUMENT ME! */
    private boolean foundEOF = false;
    
    private boolean foundEOHeader = false;

    /** DOCUMENT ME! */
    private ViewUserInterface UI;
    
    private long fileLength;
    
    private int versionNumber;
    
    /** Does not tell if color or black and white */
    private int nrrdDataType;

    /** May not be the same as MIPAV dimensions because in nrrd color is a dimension */
    private int nrrdDimensions;
    
    private int nrrdSizes[];
    
    private int startBlank[];
    
    private int finishBlank[];
    
    private int startQuote[];
    
    private int finishQuote[];
    
    private int mipavDataType;
    
    private int mipavDimensions;
    
    private int imgExtents[];
    
    /** DOCUMENT ME! */
    private boolean endianess;
    
    private int skippedLines = 0;
    
    private int skippedBytes = 0;
    
    private long offset1;
    
    /** If true, data has gzip compression */
    private boolean gunzip = false;
    
    private double spacings[] = null;
    
    private float resols[];
    
    private double axisMins[];
    
    private double axisMaxs[];
    
    private String nrrdUnits[] = null;
    
    private int mipavUnits[];
    
    private String nrrdLabels[] = null;
    
    private String mipavLabels[] = null;
    
    private String kindsString[] = null;
    
    private int numSpaceKinds = 0;
    
    private String spaceUnitsString = null;
    
    private String nrrdSpaceUnits[] = null;
    
    private double thicknesses[] = null;
    
    private int numThicknesses = 0;
    
    private float sliceThickness;
    
    private String dwmriGradient[][] = null;
    
    private String dwmriNex[][] = null;
    
    private boolean autoSequence = false;
    
    private int paddingNumber = 0;
    
    private String baseBeforeNumber = null;
    
    private String baseAfterNumber = null;
    
    private boolean baseNumber = false;
    
    private String ext = null;
    
    private int sequenceStart;
    
    private int sequenceFinish;
    
    private int sequenceStep;
    
    private int subdim = 0;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Constructs new file object.
     *
     * @param  _UI    User interface.
     * @param  fName  File name.
     * @param  fDir   File directory.
     * @param  show   Flag for showing the progress bar.
     */
    public FileNRRD(ViewUserInterface _UI, String fName, String fDir, boolean show) {
        UI = _UI;
        fileName = fName;
        fileDir = fDir;
        showProgress = show;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    
    

    /**
     * Returns the FileInfoNRRD read from the file.
     *
     * @return  File info read from file, or null if it has not been read.
     */
    public FileInfoNRRD getFileInfo() {
        return fileInfo;
    }

    /**
     * Reads the NRRD header and stores the information in fileInfo.
     *
     * @param      imageFileName  File name of image.
     * @param      fileDir        Directory.
     *
     * @return     Flag to confirm a successful read.
     *
     * @exception  IOException  if there is an error reading the header
     *
     * @see        FileInfoNRRD
     */
    public boolean readHeader(String imageFileName, String fileDir) throws IOException {
        int i, j;
        int index;
        String fileHeaderName;
        int[] nrrdExtents = new int[5];
        int numDims = 0;
        String lineString = null;
        int colonIndex;
        int equalIndex;
        String fieldIDString;
        String fieldDescriptorString;
        int numBlanks;
        int lastSlashIndex;
        String keyString;
        String valueString;
        int gradientIndex;
        int startIndex;
        int finishIndex;
        String keyNumberString;
        String formatString;
        int percentIndex;
        int dIndex;
        int periodIndex;

        // index         = fileName.toLowerCase().indexOf(".img");
        index = fileName.lastIndexOf(".");

        if (fileName.substring(index + 1).equalsIgnoreCase("nrrd")) {
            oneFileStorage = true;
            fileHeaderName = fileName;
        } else {
            oneFileStorage = false;
            fileHeaderName = fileName.substring(0, index) + ".nhdr";
        }

        file = new File(fileDir + fileHeaderName);

        if (file.exists() == false) {
            fileHeaderName = fileName.substring(0, index) + ".NHDR";
            file = new File(fileDir + fileHeaderName);

            if (file.exists() == false) {
                return false;
            }
        }

        raFile = new RandomAccessFile(file, "r");
        fileLength = raFile.length();
        
        // Check that this is a NRRD file
        lineString = readLine();

        if (lineString == null) {
            raFile.close();
            throw new IOException("The file had no uncommented lines");
        }

        if (!lineString.substring(0,4).equalsIgnoreCase("NRRD")) {
            raFile.close();
            throw new IOException("Required NRRD magic not found at start of file");
        }
        lineString = lineString.substring(4);
        versionNumber = Integer.valueOf(lineString).intValue();
        fileInfo.setVersionNumber(versionNumber);
        Preferences.debug("versionNumber = " + versionNumber + "\n");
        
        while (lineString != null) {
            lineString = readLine();

            if (lineString != null) {
                 equalIndex = lineString.lastIndexOf("=");
                 colonIndex = lineString.lastIndexOf(":");
                 if ((equalIndex >= 2) && (colonIndex == (equalIndex - 1))) {
                     // Key-value pair present
                     keyString = lineString.substring(0,colonIndex);
                     valueString = lineString.substring(equalIndex+1).trim();
                     if (keyString.equalsIgnoreCase("MODALITY")) {
                         if (valueString.equalsIgnoreCase("DWMRI")) {
                             Preferences.debug("Modality = Diffusion weighted MRI\n");
                             fileInfo.setModality("Diffusion weighted MRI");
                         }
                         else {
                             Preferences.debug("Modality = " + valueString + "\n");
                             fileInfo.setModality(valueString);
                         }
                     } // if (keyString.equalsIgnoreCase("MODALITY"))
                     else if (keyString.equalsIgnoreCase("DWMRI_B-VALUE")) {
                         Preferences.debug("Scalar diffusion weighting b-value = " + valueString +
                                           " sec/mm^2\n");
                         fileInfo.setDWMRI_B_VALUE(valueString);
                     } // else if (keyString.equalsIgnoreCase("DWMRI_B-VALUE"))
                     else if ((keyString.length() >= 19) &&
                              (keyString.substring(0,14).equalsIgnoreCase("DWMRI_GRADIENT"))) {
                         Preferences.debug(keyString + " = " +  valueString + "\n");
                         if (dwmriGradient == null) {
                             dwmriGradient = new String[100][2];
                         }
                         gradientIndex = Integer.valueOf(keyString.substring(15)).intValue();
                         dwmriGradient[gradientIndex][0] = keyString;
                         dwmriGradient[gradientIndex][1] = valueString;
                     }
                     else if ((keyString.length() >= 14) &&
                              (keyString.substring(0,9).equalsIgnoreCase("DWMRI_NEX"))) {
                         Preferences.debug(keyString + " = " +  valueString + "\n");
                         if (dwmriNex == null) {
                             dwmriNex = new String[100][2];
                         }
                         gradientIndex = Integer.valueOf(keyString.substring(10)).intValue();
                         dwmriNex[gradientIndex][0] = keyString;
                         dwmriNex[gradientIndex][1] = valueString;
                     }
                 } // if ((equalIndex >= 2) && (colonIndex == (equalIndex - 1)))
                 else if (colonIndex >= 1) {
                     // field identifier: field descriptor present
                     fieldIDString = lineString.substring(0,colonIndex);
                     fieldDescriptorString = lineString.substring(colonIndex+2).trim();
                     if (fieldIDString.equalsIgnoreCase("TYPE")) {
                         if ((fieldDescriptorString.equalsIgnoreCase("SIGNED CHAR")) ||
                             (fieldDescriptorString.equalsIgnoreCase("INT8"))||
                             (fieldDescriptorString.equalsIgnoreCase("INT8_T"))) {
                             nrrdDataType = ModelStorageBase.BYTE;
                             Preferences.debug("NRRD data type = BYTE\n");
                         }
                         else if ((fieldDescriptorString.equalsIgnoreCase("UCHAR")) ||
                             (fieldDescriptorString.equalsIgnoreCase("UNSIGNED CHAR")) ||
                             (fieldDescriptorString.equalsIgnoreCase("UINT8"))||
                             (fieldDescriptorString.equalsIgnoreCase("UINT8_T"))) {
                             nrrdDataType = ModelStorageBase.UBYTE;
                             Preferences.debug("NRRD data type = UBYTE\n");
                         }
                         else if ((fieldDescriptorString.equalsIgnoreCase("SHORT")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("SHORT INT")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("SIGNED SHORT")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("SIGNED SHORT INT")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("INT16")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("INT16_T"))) {
                             nrrdDataType = ModelStorageBase.SHORT;
                             Preferences.debug("NRRD data type = SHORT\n");
                         }
                         else if ((fieldDescriptorString.equalsIgnoreCase("USHORT")) ||
                                 (fieldDescriptorString.equalsIgnoreCase("UNSIGNED SHORT")) ||
                                 (fieldDescriptorString.equalsIgnoreCase("UNSIGNED SHORT INT")) ||
                                 (fieldDescriptorString.equalsIgnoreCase("UINT16")) ||
                                 (fieldDescriptorString.equalsIgnoreCase("UINT16_T"))) {
                             nrrdDataType = ModelStorageBase.USHORT;
                             Preferences.debug("NRRD data type = USHORT\n");
                         }
                         else if ((fieldDescriptorString.equalsIgnoreCase("INT")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("SIGNED INT")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("INT32")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("INT32_T"))) {
                             nrrdDataType = ModelStorageBase.INTEGER;
                             Preferences.debug("NRRD data type = INTEGER\n");
                         }
                         else if ((fieldDescriptorString.equalsIgnoreCase("UINT")) ||
                                 (fieldDescriptorString.equalsIgnoreCase("UNSIGNED INT")) ||
                                 (fieldDescriptorString.equalsIgnoreCase("UINT32")) ||
                                 (fieldDescriptorString.equalsIgnoreCase("UINT32_T"))) {
                             nrrdDataType = ModelStorageBase.UINTEGER;
                             Preferences.debug("NRRD data type = UINTEGER\n");
                         }
                         else if ((fieldDescriptorString.equalsIgnoreCase("LONGLONG")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("LONG LONG")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("LONG LONG INT")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("SIGNED LONG LONG")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("SIGNED LONG LONG INT")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("INT64")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("INT64_T"))) {
                             nrrdDataType = ModelStorageBase.LONG;
                             Preferences.debug("NRRD data type = LONG\n");
                         }
                         else if ((fieldDescriptorString.equalsIgnoreCase("ULONGLONG")) ||
                                 (fieldDescriptorString.equalsIgnoreCase("UNSIGNED LONG LONG")) ||
                                 (fieldDescriptorString.equalsIgnoreCase("UNSIGNED LONG LONG INT")) ||
                                 (fieldDescriptorString.equalsIgnoreCase("UINT64")) ||
                                 (fieldDescriptorString.equalsIgnoreCase("UINT64_T"))) {
                             nrrdDataType = ModelStorageBase.LONG;
                             MipavUtil.displayWarning("Warning: Reading unsigned long as signed long");
                             Preferences.debug("Warning: Reading unsigned long as signed long\n");
                         }
                         else if (fieldDescriptorString.equalsIgnoreCase("FLOAT")) {
                             nrrdDataType = ModelStorageBase.FLOAT;
                             Preferences.debug("NRRD data type = FLOAT\n");
                         }
                         else if (fieldDescriptorString.equalsIgnoreCase("DOUBLE")) {
                             nrrdDataType = ModelStorageBase.DOUBLE;
                             Preferences.debug("NRRD data type = DOUBLE\n");
                         }
                         else if (fieldDescriptorString.equalsIgnoreCase("BLOCK")) {
                             MipavUtil.displayError("Cannot handle nrrd block data type");
                             Preferences.debug("NRRD data type = BLOCK\n");
                             throw new IOException();  
                         }
                         else {
                             MipavUtil.displayError("Unknown NRRD data type = " + fieldDescriptorString);
                             Preferences.debug("Unknown NRRD data type = " + fieldDescriptorString + "\n");
                             throw new IOException();
                         }
                     } // if (fieldIDString.equalsIgnoreCase("TYPE"))
                     else if (fieldIDString.equalsIgnoreCase("DIMENSION")) {
                         nrrdDimensions = Integer.valueOf(fieldDescriptorString).intValue();
                         Preferences.debug("NRRD dimensions = " + nrrdDimensions + "\n");
                         startBlank = new int[Math.max(nrrdDimensions-1,4)];
                         finishBlank = new int[Math.max(nrrdDimensions-1,4)];
                         startQuote = new int[nrrdDimensions];
                         finishQuote = new int[nrrdDimensions];
                     } // else if (fieldIDString.equalsIgnoreCase("DIMENSION"))
                     else if (fieldIDString.equalsIgnoreCase("SIZES")) {
                         nrrdSizes = new int[nrrdDimensions];
                         for (i = 0, j = 0; i < nrrdDimensions-1;) {
                          if (!fieldDescriptorString.substring(j,j+1).equals(" ")) {
                              j++;
                          }
                          else {
                              startBlank[i] = j;
                              finishBlank[i] = j++;
                              while (fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                  finishBlank[i] = j++;
                              }
                              if (i == 0) {
                                  nrrdSizes[i] = Integer.valueOf
                                  (fieldDescriptorString.substring(0,startBlank[0])).intValue();
                                  Preferences.debug("NRRD sizes[" + i + "] = " + nrrdSizes[i] + "\n");
                              }
                              else {
                                  nrrdSizes[i] = Integer.valueOf
                                  (fieldDescriptorString.substring(finishBlank[i-1]+1, startBlank[i])).intValue();
                                  Preferences.debug("NRRD sizes[" + i + "] = " + nrrdSizes[i] + "\n");
                              }
                              i++;
                          }
                         } // for (i = 0; j = 0; i < nrrdDimensions-1;)
                         nrrdSizes[nrrdDimensions-1] = Integer.valueOf
                         (fieldDescriptorString.substring(finishBlank[nrrdDimensions-2]+1)).intValue();
                         Preferences.debug("NRRD sizes[" + i + "] = " + nrrdSizes[i] + "\n");
                     } // else if (fieldIDString.equalsIgnoreCase("SIZES"))
                     else if (fieldIDString.equalsIgnoreCase("ENDIAN")) {
                         if (fieldDescriptorString.equalsIgnoreCase("BIG")) {
                             endianess = FileBase.BIG_ENDIAN;
                             fileInfo.setEndianess(endianess);
                             Preferences.debug("BIG ENDIAN\n");
                         }
                         else if (fieldDescriptorString.equalsIgnoreCase("LITTLE")) {
                             endianess = FileBase.LITTLE_ENDIAN;
                             fileInfo.setEndianess(endianess);
                             Preferences.debug("LITTLE ENDIAN\n");
                         }
                         else {
                             MipavUtil.displayError("Illegal endian value of " + fieldDescriptorString);
                             Preferences.debug("Illegal endian value of " + fieldDescriptorString + "\n");
                             throw new IOException();
                         }
                     } // else if (fieldIDString.equalsIgnoreCase("ENDIAN"))
                     else if ((fieldIDString.equalsIgnoreCase("LINE SKIP")) ||
                              (fieldIDString.equalsIgnoreCase("LINESKIP"))) {
                          skippedLines = Integer.valueOf(fieldDescriptorString).intValue();
                          Preferences.debug("Skipped lines = " + skippedLines + "\n");
                     } // else if ((fieldIDString.equalsIgnoreCase("LINE SKIP")) ||
                     else if ((fieldIDString.equalsIgnoreCase("BYTE SKIP")) ||
                              (fieldIDString.equalsIgnoreCase("BYTESKIP"))) {
                          skippedBytes = Integer.valueOf(fieldDescriptorString).intValue();
                          Preferences.debug("Skipped bytes = " + skippedBytes + "\n");
                     } // else if ((fieldIDString.equalsIgnoreCase("BYTE SKIP")) ||
                     else if ((fieldIDString.equalsIgnoreCase("DATA FILE")) ||
                              (fieldIDString.equalsIgnoreCase("DATAFILE"))) {
                         numBlanks = 0;
                         for (i = 0, j = 0; j < fieldDescriptorString.length();) {
                             if (!fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                 j++;
                             }
                             else {
                                 numBlanks++;
                                 startBlank[i] = j;
                                 finishBlank[i] = j++;
                                 while (fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                     finishBlank[i] = j++;
                                 }
                                 i++;
                             }
                         }
                         if ((numBlanks == 0) && (!fieldDescriptorString.equalsIgnoreCase("LIST"))) {
                             // There is a single detached data file and its file directory
                             // and fileName are given by fieldDescriptorString
                             if ((fieldDescriptorString.substring(0,1).equals("."))  &&
                                 (fieldDescriptorString.substring(1,2).equals("/"))) {
                                 lastSlashIndex = fieldDescriptorString.lastIndexOf("/");
                                 if (lastSlashIndex > 1) {
                                     fileDir = fileDir.concat
                                     (fieldDescriptorString.substring(1,lastSlashIndex+1));
                                     Preferences.debug("Data file directory = " + fileDir + "\n");
                                     fileInfo.setFileDirectory(fileDir);
                                     fileName = fieldDescriptorString.substring(lastSlashIndex+1);
                                     Preferences.debug("Data file name = " + fileName + "\n");
                                     fileInfo.setFileName(fileName);
                                 }
                                 else {
                                     fileName = fieldDescriptorString.substring(2);
                                     Preferences.debug("Data file name = " + fileName + "\n");
                                     fileInfo.setFileName(fileName);
                                 }
                             }
                             else if (fieldDescriptorString.substring(0,1).equals(".")) {
                                lastSlashIndex = fieldDescriptorString.lastIndexOf("/");
                                fileDir = fieldDescriptorString.substring(0,lastSlashIndex+1);
                                Preferences.debug("Data file directory = " + fileDir + "\n");
                                fileInfo.setFileDirectory(fileName);
                                fileName = fieldDescriptorString.substring(lastSlashIndex+1);
                                Preferences.debug("Data file name = " + fileName + "\n");
                                fileInfo.setFileName(fileName);
                             }
                             else {
                                 fileName = fieldDescriptorString;
                                 Preferences.debug("Data file name = " + fileName + "\n");
                                 fileInfo.setFileName(fileName);
                             }
                         } // if ((numBlanks == 0) && (!fieldDescriptorString.equalsIgnoreCase("LIST")))
                         else if (numBlanks >= 3) {
                             autoSequence = true;
                             formatString = fieldDescriptorString.substring(0,startBlank[0]); 
                             percentIndex = formatString.lastIndexOf("%");
                             dIndex = 0;
                             for (i = percentIndex + 1; dIndex == 0; i++) {
                                 if (formatString.substring(i,i+1).equalsIgnoreCase("D")) {
                                     dIndex = i;
                                 }
                             }
                             if (dIndex == (percentIndex+1)) {
                                 paddingNumber = 0;
                             }
                             else {
                                 paddingNumber = Integer.valueOf(formatString.substring
                                                     (percentIndex+1,dIndex)).intValue();
                             }
                             periodIndex = formatString.lastIndexOf(".");
                             if (periodIndex > dIndex) {
                                 // Increment in file base
                                 baseNumber = true;
                                 ext = formatString.substring(periodIndex+1);
                             }
                             else {
                                 // Increment in file extension
                                 baseNumber = false;
                                 ext = null;
                             }
                             if (percentIndex == 0) {
                                 baseBeforeNumber = null;
                             }
                             else if (percentIndex < periodIndex) {
                                 baseBeforeNumber = formatString.substring(0,percentIndex);
                             }
                             else { // percentIndex > periodIndex
                                 baseBeforeNumber = formatString.substring(0,periodIndex);
                             }
                             if (periodIndex > (dIndex+1)) {
                                 baseAfterNumber = formatString.substring(dIndex+1,periodIndex);
                             }
                             else {
                                 baseAfterNumber = null;
                             }
                             sequenceStart = Integer.valueOf(fieldDescriptorString.
                                     substring(finishBlank[0]+1,startBlank[1])).intValue();
                             sequenceFinish = Integer.valueOf(fieldDescriptorString.
                                     substring(finishBlank[1]+1,startBlank[2])).intValue();
                             if (numBlanks == 3) {
                                 sequenceStep = Integer.valueOf(fieldDescriptorString.
                                     substring(finishBlank[2]+1)).intValue();
                             } // if (numBlanks == 3)
                             else {
                                 sequenceStep = Integer.valueOf(fieldDescriptorString.
                                         substring(finishBlank[2]+1,startBlank[3])).intValue();
                                 subdim = Integer.valueOf(fieldDescriptorString.
                                         substring(finishBlank[3]+1)).intValue();
                             }
                         } // else if (numBlanks >= 3)
                     } // else if ((fieldIDString.equalsIgnoreCase("DATA FILE"))
                     else if (fieldIDString.equalsIgnoreCase("ENCODING")) {
                         if (fieldDescriptorString.equalsIgnoreCase("RAW")) {
                             Preferences.debug("Encoding = raw\n");
                             fileInfo.setEncoding("Raw");
                         }
                         else if ((fieldDescriptorString.equalsIgnoreCase("GZ")) ||
                                  (fieldDescriptorString.equalsIgnoreCase("GZIP"))) {
                             Preferences.debug("Encoding = gzip\n");
                             fileInfo.setEncoding("Gzip");
                             gunzip = true;
                         }
                     } // else if (fieldIDString.equalsIgnoreCase("ENCODING"))
                     else if (fieldIDString.equalsIgnoreCase("CONTENT")) {
                         Preferences.debug("Content = " + fieldDescriptorString + "\n");
                         fileInfo.setContent(fieldDescriptorString);
                     } // else if (fieldIDString.equalsIgnoreCase("CONTENT"))
                     else if (fieldIDString.equalsIgnoreCase("SPACINGS")) {
                         spacings = new double[nrrdDimensions];
                         for (i = 0, j = 0; i < nrrdDimensions-1;) {
                          if (!fieldDescriptorString.substring(j,j+1).equals(" ")) {
                              j++;
                          }
                          else {
                              startBlank[i] = j;
                              finishBlank[i] = j++;
                              while (fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                  finishBlank[i] = j++;
                              }
                              if (i == 0) {
                                  if (fieldDescriptorString.substring(0,startBlank[0]).equalsIgnoreCase("NAN")) {
                                      spacings[i] = Double.NaN;
                                  }
                                  else {
                                      spacings[i] = Double.valueOf
                                      (fieldDescriptorString.substring(0,startBlank[0])).doubleValue();
                                  }
                                  Preferences.debug("NRRD spacings[" + i + "] = " + spacings[i] + "\n");
                              }
                              else {
                                  if (fieldDescriptorString.substring(finishBlank[i-1]+1, startBlank[i]).equalsIgnoreCase("NAN")) {
                                      spacings[i] = Double.NaN;
                                  }
                                  else {
                                      spacings[i] = Double.valueOf(fieldDescriptorString.substring
                                                     (finishBlank[i-1]+1, startBlank[i])).doubleValue();
                                  }
                                  Preferences.debug("NRRD spacings[" + i + "] = " + spacings[i] + "\n");
                              }
                              i++;
                          }
                         } // for (i = 0; j = 0; i < nrrdDimensions-1;)
                         if (fieldDescriptorString.substring(finishBlank[nrrdDimensions-2]+1).equalsIgnoreCase("NAN")) {
                             spacings[nrrdDimensions-1] = Double.NaN;
                         }
                         else {
                             spacings[nrrdDimensions-1] = Double.valueOf
                             (fieldDescriptorString.substring(finishBlank[nrrdDimensions-2]+1)).doubleValue();
                         }
                         Preferences.debug("NRRD spacings[" + i + "] = " + spacings[i] + "\n");    
                     } // else if (fieldIDString.equalsIgnoreCase("SPACINGS"))
                     else if ((fieldIDString.equalsIgnoreCase("AXIS MINS")) ||
                             (fieldIDString.equalsIgnoreCase("AXISMINS"))) {
                        axisMins = new double[nrrdDimensions];
                        for (i = 0, j = 0; i < nrrdDimensions-1;) {
                            if (!fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                j++;
                            }
                            else {
                                startBlank[i] = j;
                                finishBlank[i] = j++;
                                while (fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                    finishBlank[i] = j++;
                                }
                                if (i == 0) {
                                    if (fieldDescriptorString.substring(0,startBlank[0]).equalsIgnoreCase("NAN")) {
                                        axisMins[i] = Double.NaN;
                                    }
                                    else {
                                        axisMins[i] = Double.valueOf
                                        (fieldDescriptorString.substring(0,startBlank[0])).doubleValue();
                                    }
                                    Preferences.debug("NRRD axis minimum[" + i + "] = " + axisMins[i] + "\n");
                                }
                                else {
                                    if (fieldDescriptorString.substring(finishBlank[i-1]+1,startBlank[i])
                                            .equalsIgnoreCase("NAN")) {
                                        axisMins[i] = Double.NaN;
                                    }
                                    else {
                                        axisMins[i] = Double.valueOf(fieldDescriptorString.substring
                                                       (finishBlank[i-1]+1, startBlank[i])).doubleValue();
                                    }
                                    Preferences.debug("NRRD axis minimum[" + i + "] = " + axisMins[i] + "\n");
                                }
                                i++;
                            }
                        } // for (i = 0; j = 0; i < nrrdDimensions-1;)
                        if (fieldDescriptorString.substring(finishBlank[nrrdDimensions-2]+1).equalsIgnoreCase("NAN")) {
                            axisMins[nrrdDimensions-1] = Double.NaN;
                        }
                        else {
                            axisMins[nrrdDimensions-1] = Double.valueOf
                            (fieldDescriptorString.substring(finishBlank[nrrdDimensions-2]+1)).doubleValue();
                        }
                        Preferences.debug("NRRD axis minimum[" + i + "] = " + axisMins[i] + "\n");    
                     } // else if (fieldIDString.equalsIgnoreCase("AXIS MINS")) ||
                     else if ((fieldIDString.equalsIgnoreCase("AXIS MAXS")) ||
                             (fieldIDString.equalsIgnoreCase("AXISMAXS"))) {
                        axisMaxs = new double[nrrdDimensions];
                        for (i = 0, j = 0; i < nrrdDimensions-1;) {
                            if (!fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                j++;
                            }
                            else {
                                startBlank[i] = j;
                                finishBlank[i] = j++;
                                while (fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                    finishBlank[i] = j++;
                                }
                                if (i == 0) {
                                    if (fieldDescriptorString.substring(0,startBlank[0]).equalsIgnoreCase("NAN")) {
                                        axisMaxs[i] = Double.NaN;
                                    }
                                    else {
                                        axisMaxs[i] = Double.valueOf
                                        (fieldDescriptorString.substring(0,startBlank[0])).doubleValue();
                                    }
                                    Preferences.debug("NRRD axis maximum[" + i + "] = " + axisMaxs[i] + "\n");
                                }
                                else {
                                    if (fieldDescriptorString.substring(finishBlank[i-1]+1, startBlank[i])
                                            .equalsIgnoreCase("NAN")) {
                                        axisMaxs[i] = Double.NaN;
                                    }
                                    else {
                                        axisMaxs[i] = Double.valueOf(fieldDescriptorString.substring
                                                       (finishBlank[i-1]+1, startBlank[i])).doubleValue();
                                    }
                                    Preferences.debug("NRRD axis maximum[" + i + "] = " + axisMaxs[i] + "\n");
                                }
                                i++;
                            }
                        } // for (i = 0; j = 0; i < nrrdDimensions-1;)
                        if (fieldDescriptorString.substring(finishBlank[nrrdDimensions-2]+1).equalsIgnoreCase("NAN")) {
                            axisMaxs[nrrdDimensions-1] = Double.NaN;
                        }
                        else {
                            axisMaxs[nrrdDimensions-1] = Double.valueOf
                            (fieldDescriptorString.substring(finishBlank[nrrdDimensions-2]+1)).doubleValue();
                        }
                        Preferences.debug("NRRD axis maximum[" + i + "] = " + axisMaxs[i] + "\n");    
                     } // else if (fieldIDString.equalsIgnoreCase("AXIS MAXS")) ||
                     else if (fieldIDString.equalsIgnoreCase("UNITS")) {
                         nrrdUnits = new String[nrrdDimensions];  
                         for (i = 0, j = 0; i < nrrdDimensions;) {
                             if (!fieldDescriptorString.substring(j,j+1).equals("\"")) {
                                 j++;
                             } 
                             else {
                                 startQuote[i] = j++;
                                 while (!fieldDescriptorString.substring(j,j+1).equals("\"")) {
                                     j++;
                                 } 
                                 finishQuote[i++] = j++;
                             }
                         } // for (i = 0, j = 0; i < nrrdDimensions;)
                         for (i = 0; i < nrrdDimensions; i++) {
                             nrrdUnits[i] = fieldDescriptorString.substring(startQuote[i]+1,finishQuote[i]);
                             Preferences.debug("NRRD units[ " + i + "] = " + nrrdUnits[i] + "\n");
                         }
                     } // else if (fieldIDString.equalsIgnoreCase("UNITS))
                     else if (fieldIDString.equalsIgnoreCase("LABELS")) {
                         nrrdLabels = new String[nrrdDimensions];  
                         for (i = 0, j = 0; i < nrrdDimensions;) {
                             if (!fieldDescriptorString.substring(j,j+1).equals("\"")) {
                                 j++;
                             } 
                             else {
                                 startQuote[i] = j++;
                                 while (!fieldDescriptorString.substring(j,j+1).equals("\"")) {
                                     j++;
                                 } 
                                 finishQuote[i++] = j++;
                             }
                         } // for (i = 0, j = 0; i < nrrdDimensions;)
                         for (i = 0; i < nrrdDimensions; i++) {
                             nrrdLabels[i] = fieldDescriptorString.substring(startQuote[i]+1,finishQuote[i]);
                             Preferences.debug("NRRD labels[ " + i + "] = " + nrrdLabels[i] + "\n");
                         }
                     } // else if (fieldIDString.equalsIgnoreCase("LABELS))
                     else if (fieldIDString.equalsIgnoreCase("KINDS")) {
                         kindsString = new String[nrrdDimensions]; 
                         for (i = 0, j = 0; i < nrrdDimensions-1;) {
                             if (!fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                 j++;
                             }
                             else {
                                 startBlank[i] = j;
                                 finishBlank[i] = j++;
                                 while (fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                     finishBlank[i] = j++;
                                 }
                                 if (i == 0) {
                                     kindsString[i] = fieldDescriptorString.substring(0,startBlank[0]);
                                     Preferences.debug("NRRD kind[" + i + "] = " + kindsString[i] + "\n");
                                 }
                                 else {
                                     kindsString[i] = fieldDescriptorString.substring(finishBlank[i-1]+1, startBlank[i]);
                                     Preferences.debug("NRRD kind[" + i + "] = " + kindsString[i] + "\n");
                                 }
                                 i++;
                             }
                         } // for (i = 0; j = 0; i < nrrdDimensions-1;)
                         kindsString[nrrdDimensions-1] = fieldDescriptorString.substring(finishBlank[nrrdDimensions-2]+1);
                         Preferences.debug("NRRD kind[" + i + "] = " + kindsString[i] + "\n");
                         for (i = 0; i < nrrdDimensions; i++) {
                             if (kindsString[i] != null) {
                                 if (kindsString[i].equalsIgnoreCase("SPACE")) {
                                     numSpaceKinds++;
                                 }
                             }
                         }
                     } // else if (fieldIDString.equalsIgnoreCase("KINDS"))
                     else if (fieldIDString.equalsIgnoreCase("SPACE UNITS")) {
                         spaceUnitsString = fieldDescriptorString;
                     } // else if (fieldIDString.equalsIgnoreCase("SPACE UNITS"))
                     else if (fieldIDString.equalsIgnoreCase("THICKNESSES")) {
                         thicknesses = new double[nrrdDimensions];
                         for (i = 0, j = 0; i < nrrdDimensions-1;) {
                             if (!fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                 j++;
                             }
                             else {
                                 startBlank[i] = j;
                                 finishBlank[i] = j++;
                                 while (fieldDescriptorString.substring(j,j+1).equals(" ")) {
                                     finishBlank[i] = j++;
                                 }
                                 if (i == 0) {
                                     if (fieldDescriptorString.substring(0,startBlank[0]).equalsIgnoreCase("NAN")) {
                                         thicknesses[i] = Double.NaN;
                                     }
                                     else {
                                         thicknesses[i] = Double.valueOf
                                         (fieldDescriptorString.substring(0,startBlank[0])).doubleValue();
                                         numThicknesses++;
                                         sliceThickness = (float)thicknesses[i];
                                     }
                                     Preferences.debug("NRRD axis thickness[" + i + "] = " + thicknesses[i] + "\n");
                                 }
                                 else {
                                     if (fieldDescriptorString.substring(finishBlank[i-1]+1, startBlank[i])
                                             .equalsIgnoreCase("NAN")) {
                                         thicknesses[i] = Double.NaN;
                                     }
                                     else {
                                         thicknesses[i] = Double.valueOf(fieldDescriptorString.substring
                                                        (finishBlank[i-1]+1, startBlank[i])).doubleValue();
                                         numThicknesses++;
                                         sliceThickness = (float)thicknesses[i];
                                     }
                                     Preferences.debug("NRRD axis thickness[" + i + "] = " + thicknesses[i] + "\n");
                                 }
                                 i++;
                             }
                         } // for (i = 0; j = 0; i < nrrdDimensions-1;)  
                         if (fieldDescriptorString.substring(finishBlank[nrrdDimensions-2]+1).equalsIgnoreCase("NAN")) {
                             thicknesses[nrrdDimensions-1] = Double.NaN;
                         }
                         else {
                             thicknesses[nrrdDimensions-1] = Double.valueOf
                             (fieldDescriptorString.substring(finishBlank[nrrdDimensions-2]+1)).doubleValue();
                             numThicknesses++;
                             sliceThickness = (float)thicknesses[i];
                         }
                         Preferences.debug("NRRD axis thickness[" + i + "] = " + thicknesses[i] + "\n");    
                     } // else if (fieldIDString.equalsIgnoreCase("THICKNESSES"))
                 } // else if (colonIndex >= 1)
            } // if (lineString != null)
        } // while (lineString != null)
        
        if (!oneFileStorage) {
            raFile.close();
        }
        
        if (spaceUnitsString != null) {
            nrrdSpaceUnits = new String[numSpaceKinds];  
            for (i = 0, j = 0; i < numSpaceKinds;) {
                if (!spaceUnitsString.substring(j,j+1).equals("\"")) {
                    j++;
                } 
                else {
                    startQuote[i] = j++;
                    while (!spaceUnitsString.substring(j,j+1).equals("\"")) {
                        j++;
                    } 
                    finishQuote[i++] = j++;
                }
            } // for (i = 0, j = 0; i < numSpaceKinds;)
            for (i = 0; i < numSpaceKinds; i++) {
                nrrdSpaceUnits[i] = spaceUnitsString.substring(startQuote[i]+1,finishQuote[i]);
                Preferences.debug("NRRD space units[ " + i + "] = " + nrrdSpaceUnits[i] + "\n");
            }
            if (nrrdUnits == null) {
                nrrdUnits = new String[nrrdDimensions];
            }
            for (i = 0, j = 0; i < nrrdDimensions; i++) {
                if ((kindsString[i] != null) && (kindsString[i].equalsIgnoreCase("SPACE"))) {
                    nrrdUnits[i] = nrrdSpaceUnits[j++];   
                }
            }
        } // if (spaceUnitsString != null)
        
        if (((nrrdSizes[0] == 2) || (nrrdSizes[0] == 3)) && (nrrdDimensions >= 3) &&
             ((nrrdDataType == ModelStorageBase.UBYTE) || (nrrdDataType == ModelStorageBase.USHORT) ||
              (nrrdDataType == ModelStorageBase.FLOAT))) { 
            if (nrrdDataType == ModelStorageBase.UBYTE) {
                mipavDataType = ModelStorageBase.ARGB;
            }
            else if (nrrdDataType == ModelStorageBase.USHORT) {
                mipavDataType = ModelStorageBase.ARGB_USHORT;
            }
            else {
                mipavDataType = ModelStorageBase.ARGB_FLOAT;
            }
            mipavDimensions = nrrdDimensions - 1;
            imgExtents = new int[mipavDimensions];
            resols = new float[mipavDimensions];
            mipavUnits = new int[mipavDimensions];
            if (nrrdLabels != null) {
                mipavLabels = new String[mipavDimensions];
            }
            for (i = 0; i < mipavDimensions; i++) {
                imgExtents[i] = nrrdSizes[i+1];
                if (spacings != null) {
                    if (Double.isNaN(spacings[i+1])) {
                        resols[i] = 1.0f;
                    }
                    else {
                        resols[i] = (float)Math.abs(spacings[i+1]);
                    }
                } // if (spacings != null)
                else { // spacings == null
                    resols[i] = 1.0f;
                } // else spacings == null
                if (nrrdUnits != null) {
                   if ((nrrdUnits[i+1] == null) || (nrrdUnits[i+1].length() == 0) ||
                       (nrrdUnits[i+1].trim().length() == 0)) {
                       mipavUnits[i] = FileInfoBase.UNKNOWN_MEASURE;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("MM")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("MILLIMETERS"))) {
                       mipavUnits[i] = FileInfoBase.MILLIMETERS;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("IN")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("INCHES"))) {
                       mipavUnits[i] = FileInfoBase.INCHES;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("CM")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("CENTIMETERS"))) {
                       mipavUnits[i] = FileInfoBase.CENTIMETERS;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("A")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("ANGSTROMS"))) {
                       mipavUnits[i] = FileInfoBase.ANGSTROMS;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("NM")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("NANOMETERS"))) {
                       mipavUnits[i] = FileInfoBase.NANOMETERS;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("UM")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("MICROMETERS"))) {
                       mipavUnits[i] = FileInfoBase.MICROMETERS;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("M")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("METERS"))) {
                       mipavUnits[i] = FileInfoBase.METERS;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("KM")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("KILOMETERS"))) {
                       mipavUnits[i] = FileInfoBase.KILOMETERS;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("MI")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("MILES"))) {
                       mipavUnits[i] = FileInfoBase.MILES;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("NSEC")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("NANOSECONDS"))) {
                       mipavUnits[i] = FileInfoBase.NANOSEC;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("USEC")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("MICROSECONDS"))) {
                       mipavUnits[i] = FileInfoBase.MICROSEC;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("MSEC")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("MILLISECONDS"))) {
                       mipavUnits[i] = FileInfoBase.MILLISEC;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("SEC")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("SECONDS"))) {
                       mipavUnits[i] = FileInfoBase.SECONDS;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("MIN")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("MINUTES"))) {
                       mipavUnits[i] = FileInfoBase.MINUTES;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("HR")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("HOURS"))) {
                       mipavUnits[i] = FileInfoBase.HOURS;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("HZ")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("HERTZ"))) {
                       mipavUnits[i] = FileInfoBase.HZ;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("PPM")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("PARTS PER MILLION"))) {
                       mipavUnits[i] = FileInfoBase.PPM;
                   }
                   else if ((nrrdUnits[i+1].equalsIgnoreCase("RADS")) ||
                            (nrrdUnits[i+1].equalsIgnoreCase("RADIANS PER SECOND"))) {
                       mipavUnits[i] = FileInfoBase.RADS;
                   }
                   else {
                       mipavUnits[i] = FileInfoBase.UNKNOWN_MEASURE;
                   }
                } // if (nrrdUnits != null)
                else { // nrrdUnits == null
                    mipavUnits[i] = FileInfoBase.UNKNOWN_MEASURE;    
                } // nrrdUnits == null
                if (nrrdLabels != null) {
                    mipavLabels[i] = nrrdLabels[i+1];
                }
            } // for (i = 0; i < mipavDimensions; i++)
        } // if (((nrrdSizes[0] == 2) || (nrrdSizes[0] == 3)) && (nrrdDimensions >= 3))
        else {
            mipavDataType = nrrdDataType;
            mipavDimensions = nrrdDimensions;
            imgExtents = new int[mipavDimensions];
            resols = new float[mipavDimensions];
            mipavUnits = new int[mipavDimensions];
            if (nrrdLabels != null) {
                mipavLabels = new String[mipavDimensions];
            }
            for (i = 0; i < mipavDimensions; i++) {
                imgExtents[i] = nrrdSizes[i];
                if (spacings != null) {
                    if (Double.isNaN(spacings[i])) {
                        resols[i] = 1.0f;
                    }
                    else {
                        resols[i] = (float)Math.abs(spacings[i]);
                    }
                } // if (spacings != null)
                else { // spacings == null
                    resols[i] = 1.0f;
                } // else spacings == null
                if (nrrdUnits != null) {
                    if ((nrrdUnits[i] == null) || (nrrdUnits[i].length() == 0) ||
                        (nrrdUnits[i].trim().length() == 0)) {
                        mipavUnits[i] = FileInfoBase.UNKNOWN_MEASURE;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("MM")) ||
                             (nrrdUnits[i].equalsIgnoreCase("MILLIMETERS"))) {
                        mipavUnits[i] = FileInfoBase.MILLIMETERS;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("IN")) ||
                             (nrrdUnits[i].equalsIgnoreCase("INCHES"))) {
                        mipavUnits[i] = FileInfoBase.INCHES;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("CM")) ||
                             (nrrdUnits[i].equalsIgnoreCase("CENTIMETERS"))) {
                        mipavUnits[i] = FileInfoBase.CENTIMETERS;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("A")) ||
                             (nrrdUnits[i].equalsIgnoreCase("ANGSTROMS"))) {
                        mipavUnits[i] = FileInfoBase.ANGSTROMS;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("NM")) ||
                             (nrrdUnits[i].equalsIgnoreCase("NANOMETERS"))) {
                        mipavUnits[i] = FileInfoBase.NANOMETERS;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("UM")) ||
                             (nrrdUnits[i].equalsIgnoreCase("MICROMETERS"))) {
                        mipavUnits[i] = FileInfoBase.MICROMETERS;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("M")) ||
                             (nrrdUnits[i].equalsIgnoreCase("METERS"))) {
                        mipavUnits[i] = FileInfoBase.METERS;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("KM")) ||
                             (nrrdUnits[i].equalsIgnoreCase("KILOMETERS"))) {
                        mipavUnits[i] = FileInfoBase.KILOMETERS;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("MI")) ||
                             (nrrdUnits[i].equalsIgnoreCase("MILES"))) {
                        mipavUnits[i] = FileInfoBase.MILES;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("NSEC")) ||
                             (nrrdUnits[i].equalsIgnoreCase("NANOSECONDS"))) {
                        mipavUnits[i] = FileInfoBase.NANOSEC;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("USEC")) ||
                             (nrrdUnits[i].equalsIgnoreCase("MICROSECONDS"))) {
                        mipavUnits[i] = FileInfoBase.MICROSEC;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("MSEC")) ||
                             (nrrdUnits[i].equalsIgnoreCase("MILLISECONDS"))) {
                        mipavUnits[i] = FileInfoBase.MILLISEC;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("SEC")) ||
                             (nrrdUnits[i].equalsIgnoreCase("SECONDS"))) {
                        mipavUnits[i] = FileInfoBase.SECONDS;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("MIN")) ||
                             (nrrdUnits[i].equalsIgnoreCase("MINUTES"))) {
                        mipavUnits[i] = FileInfoBase.MINUTES;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("HR")) ||
                             (nrrdUnits[i].equalsIgnoreCase("HOURS"))) {
                        mipavUnits[i] = FileInfoBase.HOURS;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("HZ")) ||
                             (nrrdUnits[i].equalsIgnoreCase("HERTZ"))) {
                        mipavUnits[i] = FileInfoBase.HZ;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("PPM")) ||
                             (nrrdUnits[i].equalsIgnoreCase("PARTS PER MILLION"))) {
                        mipavUnits[i] = FileInfoBase.PPM;
                    }
                    else if ((nrrdUnits[i].equalsIgnoreCase("RADS")) ||
                             (nrrdUnits[i].equalsIgnoreCase("RADIANS PER SECOND"))) {
                        mipavUnits[i] = FileInfoBase.RADS;
                    }
                    else {
                        mipavUnits[i] = FileInfoBase.UNKNOWN_MEASURE;
                    }
                 } // if (nrrdUnits != null)
                 else { // nrrdUnits == null
                     mipavUnits[i] = FileInfoBase.UNKNOWN_MEASURE;    
                 } // else nrrdUnits == null
                if (nrrdLabels != null) {
                    mipavLabels[i] = nrrdLabels[i];
                }
            } // for (i = 0; i < mipavDimensions; i++)
        }
        
        if (subdim == 0) {
            subdim = mipavDimensions - 1;
        }
        else if (nrrdDimensions > mipavDimensions) {
            subdim--;
        }
        
        fileInfo.setDataType(mipavDataType);
        fileInfo.setExtents(imgExtents);
        fileInfo.setResolutions(resols);
        fileInfo.setUnitsOfMeasure(mipavUnits);
        fileInfo.setLabels(mipavLabels);
        if (numThicknesses == 1) {
            fileInfo.setSliceThickness(sliceThickness);
        }
        if (dwmriNex != null) {
            for (i = 0; i < dwmriNex.length; i++) {
                if (dwmriNex[i] != null) {
                    startIndex = Integer.valueOf(dwmriNex[i][0].substring(10)).intValue();
                    finishIndex = startIndex + Integer.valueOf(dwmriNex[i][1]).intValue() - 1;
                    for (j = startIndex+1; j <= finishIndex; j++) {
                        keyNumberString = String.valueOf(j);
                        while (keyNumberString.length() < 4) {
                            keyNumberString = "0".concat(keyNumberString);
                        }
                        dwmriGradient[j][0] = "DWMRI_gradient_".concat(keyNumberString);
                        dwmriGradient[j][1] = dwmriGradient[startIndex][1];
                    }
                }
            }
        }
        if (dwmriGradient != null) {
            fileInfo.setDwmriGradient(dwmriGradient);
        }
        
        return true; // If it got this far, it has successfully read in the header
    }
    
    /**
     * Reads lines of the file and strips comments indicated by the # symbol until a nonnull String results or the end
     * of the file is reached or a blank line containing end of header with zero characters is reached.
     *
     * @return     the line read in
     *
     * @exception  IOException  if there is an error reading the file
     */
    private String readLine() throws IOException {
        String tempString = null;
        int index;

        while ((tempString == null) && (raFile.getFilePointer() < (fileLength - 1)) && (!foundEOF) 
               && (!foundEOHeader)) {

            try {
                tempString = raFile.readLine();
            } catch (EOFException error) {
                tempString = null;
                foundEOF = true;
            } catch (IOException error) {
                throw (error);
            }
            
            if (tempString == null) {
                foundEOHeader = true;
            }
            else if (tempString.length() == 0) {
                foundEOHeader = true;
            }

            if (tempString != null) {
                index = tempString.indexOf("#");

                if (index != -1) {
                    tempString = tempString.substring(0, index);
                }

                tempString = tempString.trim();
            }

            if (tempString != null) {

                if (tempString.length() == 0) {
                    tempString = null;
                }
            }
        } // while

        return tempString;
    }

    /**
     * Reads a NRRD image file by reading the header then making a FileRaw to read the image for all filenames in the
     * file list. Only the one file directory (currently) supported.
     *
     * @param      one  flag indicating one image of a 3D dataset should be read in.
     *
     * @exception  IOException  if there is an error reading the file
     *
     * @return     The image.
     *
     * @see        FileRaw
     */
    public ModelImage readImage(boolean one) throws IOException, OutOfMemoryError {
        int offset;
        int linesFound;
        int i;
        byte buf[] = new byte[1];
        fileInfo = new FileInfoNRRD(fileName, fileDir, FileBase.NRRD);
        long dataSize;
        FileInputStream fis;
        int s;
        String sequenceNumber;
        int numSlabs;
        int subExtents[];
        ModelImage subImage;
        byte dataBuffer[];
        int pointer = 0;
        
        progressBar = new ViewJProgressBar(ViewUserInterface.getReference().getProgressBarPrefix() + fileName,
                ViewUserInterface.getReference().getProgressBarPrefix() + "NRRD image(s) ...",
                0, 100, false, null, null);
        progressBar.setLocation((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2, 50);
        setProgressBarVisible(ViewUserInterface.getReference().isAppFrameVisible());

        if (!readHeader(fileInfo.getFileName(), fileInfo.getFileDirectory())) {
            throw (new IOException(" NRRD header file error"));
        }
        
        if (autoSequence) {
            try {
                image = new ModelImage(fileInfo.getDataType(), fileInfo.getExtents(), fileInfo.getFileName(), UI);
            } catch (OutOfMemoryError error) {
                throw (error);
            }

            setFileInfo(fileInfo, image);
            updateorigins(image.getFileInfo());
            image.setMatrix(matrix);
            
            if (subdim == mipavDimensions) {
                numSlabs = 1 + (sequenceFinish - sequenceStart)/sequenceStep;
                dataSize = 1;
                for (i = 0; i < mipavDimensions; i++) {
                    dataSize *= imgExtents[i];
                }
                dataSize = dataSize/numSlabs;
                subExtents = new int[mipavDimensions];
                for (i = 0; i < mipavDimensions-1; i++) {
                    subExtents[i] = imgExtents[i];
                }
                subExtents[mipavDimensions-1] = imgExtents[mipavDimensions-1]/numSlabs;
            }
            else { // subdim < mipavDimensions
                dataSize = 1;
                for (i = 0; i < subdim; i++) {
                    dataSize *= imgExtents[i];
                }
                subExtents = new int[subdim];
                for (i = 0; i < mipavDimensions; i++) {
                    subExtents[i] = imgExtents[i];
                }
            } // else subdim < mipavDimensions
            switch (nrrdDataType) {

                case ModelStorageBase.BYTE:
                case ModelStorageBase.UBYTE:
                    break;
    
                case ModelStorageBase.SHORT:
                case ModelStorageBase.USHORT:
                    dataSize *= 2;
                    break;
    
                case ModelStorageBase.INTEGER:
                case ModelStorageBase.UINTEGER:
                case ModelStorageBase.FLOAT:
                    dataSize *= 4;
                    break;
    
                case ModelStorageBase.LONG:
                case ModelStorageBase.DOUBLE:
                    dataSize *= 8;
                    break;

            } // switch (nrrdDataType)
            
            dataBuffer = new byte[(int)dataSize];
            
            try {
                subImage = new ModelImage(fileInfo.getDataType(), subExtents, "subImage", UI);
            } catch (OutOfMemoryError error) {
                throw (error);
            }
            
            for (i = sequenceStart; i <= sequenceFinish; i += sequenceStep) {
                progressBar.updateValue((i - sequenceStart)*100/sequenceStep,true);
                // Create fileName with initial sequence number
                fileName = null;
                sequenceNumber = String.valueOf(i);
                while (sequenceNumber.length() < paddingNumber) {
                    sequenceNumber = "0".concat(sequenceNumber);
                }
                if (baseBeforeNumber != null) {
                    fileName = baseBeforeNumber;
                }
                if (baseNumber) {
                    if (fileName == null) {
                        fileName = sequenceNumber;
                    }
                    else {
                        fileName = fileName.concat(sequenceNumber);
                    }
                } // if (baseNumber)
                if (baseAfterNumber != null) {
                    fileName = fileName.concat(baseAfterNumber);
                }
                fileName = fileName.concat(".");
                if (baseNumber) {
                    fileName = fileName.concat(ext);
                }
                else {
                    fileName = fileName.concat(sequenceNumber);
                }
                Preferences.debug("Autosequencing on " + fileDir + fileName + "\n"); 
                
                // Do line skipping before byte skipping
                // Do line skipping before decompression
                file = new File(fileDir + fileName);
                raFile = new RandomAccessFile(file, "r");
                fileLength = raFile.length();
                
                if (skippedLines > 0) {
                    linesFound = 0;
                    for (i = 0; linesFound < skippedLines; i++) {
                        raFile.read(buf);
                        if (buf[0] == 10 /* new line */) {
                            linesFound++;
                        }
                    }
                } // if(skippedLines > 0)
                offset1 = raFile.getFilePointer();
                raFile.close();
                
                if (gunzip) {
                    int totalBytesRead = 0;
                    progressBar.setVisible(isProgressBarVisible());
                    progressBar.setMessage("Uncompressing GZIP file ...");
                    fis = new FileInputStream(file);
                    fis.skip(offset1);

                    GZIPInputStream gzin = new GZIPInputStream(new BufferedInputStream(fis));
                    s = fileName.lastIndexOf(".");
                    String uncompressedName = fileDir + fileName.substring(0, s);
                    FileOutputStream out = new FileOutputStream(uncompressedName);
                    byte[] buffer = new byte[256];

                    while (true) {
                        int bytesRead = gzin.read(buffer);

                        if (bytesRead == -1) {
                            break;
                        }

                        totalBytesRead += bytesRead;
                        out.write(buffer, 0, bytesRead);
                    }

                    out.close();
                    file = new File(uncompressedName);
                    fileInfo.setFileName(fileName.substring(0,s));
                    offset1 = 0;
                    raFile = new RandomAccessFile(file, "r");
                    fileLength = raFile.length();
                    raFile.close();
                } // if (gunzip)
                
                // Do byte skipping after decompression
                if (skippedBytes >= 0) {
                    offset = (int)(offset1 + skippedBytes);
                }
                else { // skippedBytes < 0
                    offset = (int)(fileLength - dataSize);
                } // else skippedBytes < 0
                
                try { // Construct a FileRaw to actually read the image.

                    FileRaw rawFile;
                    rawFile = new FileRaw(fileInfo.getFileName(), fileInfo.getFileDirectory(), fileInfo, false,
                                          FileBase.READ);
                   
                    rawFile.readImage(subImage, offset);
                    
                    subImage.exportData(0, (int)dataSize, dataBuffer);
                    
                    image.importData(pointer, dataBuffer, false);
                    pointer = pointer + (int)dataSize;
                } catch (IOException error) {
                    throw new IOException("FileNRRD: " + error);
                } catch (OutOfMemoryError e) {
                    throw (e);
                }
            } // for (i = sequenceStart; i <= sequenceFinish; i += sequenceStep)
            image.calcMinMax();
            if (progressBar != null) {
                progressBar.dispose();
            }
            return image;
        } // if (autoSequence)
        
        if (!oneFileStorage) {
            file = new File(fileDir + fileName);
            raFile = new RandomAccessFile(file, "r");
            fileLength = raFile.length();
        }
        
        // Do line skipping before byte skipping
        // Do line skipping before decompression
        if (skippedLines > 0) {
            linesFound = 0;
            for (i = 0; linesFound < skippedLines; i++) {
                raFile.read(buf);
                if (buf[0] == 10 /* new line */) {
                    linesFound++;
                }
            }
        } // if(skippedLines > 0)
        offset1 = raFile.getFilePointer();
        raFile.close();
        
        if (gunzip) {
            int totalBytesRead = 0;
            progressBar.setVisible(isProgressBarVisible());
            progressBar.setMessage("Uncompressing GZIP file ...");
            fis = new FileInputStream(file);
            fis.skip(offset1);

            GZIPInputStream gzin = new GZIPInputStream(new BufferedInputStream(fis));
            s = fileName.lastIndexOf(".");
            String uncompressedName = fileDir + fileName.substring(0, s);
            FileOutputStream out = new FileOutputStream(uncompressedName);
            byte[] buffer = new byte[256];

            while (true) {
                int bytesRead = gzin.read(buffer);

                if (bytesRead == -1) {
                    break;
                }

                totalBytesRead += bytesRead;
                out.write(buffer, 0, bytesRead);
            }

            out.close();
            file = new File(uncompressedName);
            fileInfo.setFileName(fileName.substring(0,s));
            offset1 = 0;
            raFile = new RandomAccessFile(file, "r");
            fileLength = raFile.length();
            raFile.close();
        } // if (gunzip)
        
        // Do byte skipping after decompression
        if (skippedBytes >= 0) {
            offset = (int)(offset1 + skippedBytes);
        }
        else { // skippedBytes < 0
            dataSize = imgExtents[0] * imgExtents[1];
            if (mipavDimensions >= 3) {
                dataSize *= imgExtents[2];
            }
            if (mipavDimensions >= 4) {
                dataSize *= imgExtents[3];
            }
            switch (nrrdDataType) {

                case ModelStorageBase.BYTE:
                case ModelStorageBase.UBYTE:
                    break;
    
                case ModelStorageBase.SHORT:
                case ModelStorageBase.USHORT:
                    dataSize *= 2;
                    break;
    
                case ModelStorageBase.INTEGER:
                case ModelStorageBase.UINTEGER:
                case ModelStorageBase.FLOAT:
                    dataSize *= 4;
                    break;
    
                case ModelStorageBase.LONG:
                case ModelStorageBase.DOUBLE:
                    dataSize *= 8;
                    break;

            } // switch (nrrdDataType)
            offset = (int)(fileLength - dataSize);
        } // else skippedBytes < 0

        int[] extents = null;

        try {

            if (one) {
                extents = new int[fileInfo.getExtents().length];

                for (i = 0; i < extents.length; i++) {
                    extents[i] = fileInfo.getExtents()[i];
                }

                image = new ModelImage(fileInfo.getDataType(), new int[] { extents[0], extents[1] },
                                       fileInfo.getFileName(), UI);
            } else {
                image = new ModelImage(fileInfo.getDataType(), fileInfo.getExtents(), fileInfo.getFileName(), UI);
            }
        } catch (OutOfMemoryError error) {
            throw (error);
        }

        setFileInfo(fileInfo, image);
        updateorigins(image.getFileInfo());
        image.setMatrix(matrix);
        
        try { // Construct a FileRaw to actually read the image.

            FileRaw rawFile;
            rawFile = new FileRaw(fileInfo.getFileName(), fileInfo.getFileDirectory(), fileInfo, showProgress,
                                  FileBase.READ);

            
            if (one) {

                if (fileInfo.getExtents().length > 2) {
                    offset = offset + getOffset(fileInfo);
                }
            }
            progressBar.setVisible(isProgressBarVisible());
            progressBar.setMessage("Reading in data ...");
            rawFile.readImage(image, offset);

            
            if (one) {
                fileInfo.setExtents(extents);
            }
        } catch (IOException error) {
            throw new IOException("FileNRRD: " + error);
        } catch (OutOfMemoryError e) {
            throw (e);
        }


        
        if (progressBar != null) {
            progressBar.dispose();
        }
        return image;
    }
    
    /**
     * DOCUMENT ME!
     *
     * @param  fileInfo  -- a NRRD file Info that has already been read
     * @param  image     -- a ModelImage that the fileInfo needs to be attached to
     */
    private void setFileInfo(FileInfoNRRD fileInfo, ModelImage image) {

        int[] extents = fileInfo.getExtents();

        if (image.getNDims() == 2) {
            
            image.setFileInfo(fileInfo, 0); // Otherwise just set the first fileInfo
        } else if (image.getNDims() == 3) { // If there is more than one image

            for (int i = 0; i < extents[2]; i++) {
                FileInfoNRRD newFileInfo = (FileInfoNRRD) fileInfo.clone();
                newFileInfo.setOrigin(fileInfo.getOriginAtSlice(i));
                image.setFileInfo(newFileInfo, i); // Set the array of fileInfos in ModelImage
            }
        } else if (image.getNDims() == 4) { // If there is more than one image

            for (int i = 0; i < (extents[2] * extents[3]); i++) {
                FileInfoNRRD newFileInfo = (FileInfoNRRD) fileInfo.clone();
                newFileInfo.setOrigin(fileInfo.getOriginAtSlice(i));
                image.setFileInfo(newFileInfo, i); // Set the array of fileInfos in ModelImage
            }
        }

    } // setFileInfo
    
    /**
     * Updates the start locations. Each image has a fileinfo where the start locations are stored. Note that the start
     * location for the Z (3rd) dimension change with the change is the slice. The origin is in the upper left corner
     * and we are using the right hand rule. + x -> left to right; + y -> top to bottom and + z -> into screen.
     *
     * @param  fileInfo  DOCUMENT ME!
     */
    private void updateorigins(FileInfoBase[] fileInfo) {
        int axisOrient;

        float[] origin = (float[]) (fileInfo[0].getOrigin().clone());
        float[] resolutions = fileInfo[0].getResolutions();

        if (image.getNDims() == 3) {

            for (int i = 0; i < image.getExtents()[2]; i++) {
                fileInfo[i].setOrigin(origin);
                axisOrient = fileInfo[i].getAxisOrientation(2);

                if ((axisOrient == FileInfoBase.ORI_R2L_TYPE) || (axisOrient == FileInfoBase.ORI_P2A_TYPE) ||
                        (axisOrient == FileInfoBase.ORI_I2S_TYPE)) {
                    origin[2] += resolutions[2];
                } else { // ORI_L2R_TYPE, ORI_A2P_TYPE, ORI_S2I_TYPE
                    origin[2] -= resolutions[2];
                }
            }
        } else if (image.getNDims() == 4) {
            float tmp = origin[2];

            for (int i = 0; i < image.getExtents()[3]; i++) {

                for (int j = 0; j < image.getExtents()[2]; j++) {
                    fileInfo[(i * image.getExtents()[2]) + j].setOrigin(origin);
                    axisOrient = fileInfo[i].getAxisOrientation(2);

                    if ((axisOrient == FileInfoBase.ORI_R2L_TYPE) || (axisOrient == FileInfoBase.ORI_P2A_TYPE) ||
                            (axisOrient == FileInfoBase.ORI_I2S_TYPE)) {
                        origin[2] += resolutions[2];
                    } else { // ORI_L2R_TYPE, ORI_A2P_TYPE, ORI_S2I_TYPE
                        origin[2] -= resolutions[2];
                    }
                }

                origin[3] += resolutions[3];
                origin[2] = tmp;
            }
        }
        /*else if (image.getNDims() == 5) {
         * fileInfo = image.getFileInfo(); for (int i = 0;    i <    image.getExtents()[2] * image.getExtents()[3] *
         * image.getExtents()[4];    i++) { fileInfo[i].setorigins(startLocs); startLocs[4] += resolutions[4]; }  }*/
    }
    
    /**
     * Helper method to calculate the offset for getting only the middle NIFTI image slice from the 3D file.
     *
     * @param   fileInfo  File info.
     *
     * @return  offset
     */
    private int getOffset(FileInfoNRRD fileInfo) {
        int offset = fileInfo.getExtents()[0] * fileInfo.getExtents()[1] * (fileInfo.getExtents()[2] / 2);

        switch (nrrdDataType) {

            case ModelStorageBase.BYTE:
            case ModelStorageBase.UBYTE:
                break;

            case ModelStorageBase.SHORT:
            case ModelStorageBase.USHORT:
                offset *= 2;
                break;

            case ModelStorageBase.INTEGER:
            case ModelStorageBase.UINTEGER:
            case ModelStorageBase.FLOAT:
                offset *= 4;
                break;

            case ModelStorageBase.LONG:
            case ModelStorageBase.DOUBLE:
                offset *= 8;
                break;

        }

        return offset;
    }

    /**
     * Reads a NRRD image file by reading the header then making a FileRaw to read the file. Image data is left in
     * buffer. If the fileInfo cannot be found, the header will be located and read first. Image is not 'flipped', and
     * neither units of measure nor orientation are set.
     *
     * @param      buffer  Image buffer to store image data into.
     *
     * @exception  IOException  if there is an error reading the file
     *
     * @see        FileRaw
     */
    public void readImage(float[] buffer) throws IOException, OutOfMemoryError {
        int i;
        int offset;

        

        return;
    }

    
   
}
