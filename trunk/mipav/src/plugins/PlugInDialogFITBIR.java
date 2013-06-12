import gov.nih.mipav.plugins.JDialogStandalonePlugin;

import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.algorithms.utilities.*;
import gov.nih.mipav.model.file.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;
import gov.nih.mipav.view.components.WidgetFactory;

import gov.nih.tbi.dictionary.ws.*;
import gov.nih.tbi.dictionary.model.hibernate.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.MemoryImageSource;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.table.DefaultTableCellRenderer;

import org.bouncycastle.util.encoders.Hex;

import WildMagic.LibFoundation.Mathematics.*;

import com.sun.jimi.core.*;


public class PlugInDialogFITBIR extends JDialogStandalonePlugin implements ActionListener, ChangeListener,
        ItemListener, TreeSelectionListener, MouseListener, PreviewImageContainer {
	private static final long serialVersionUID = -5516621806537554154L;

	private WidgetFactory.ScrollTextArea logOutputArea;

    private JScrollPane listPane;

    private JTable sourceTable;

    private JLabel outputDirLabel;

    private JButton addSourceButton, loadCSVButton, finishButton, removeSourceButton, completeDataElementsButton,
            outputDirButton;

    private JPanel outputDirPanel;

    private JPanel previewPanel;

    private JPanel leftPanel;

    private JTextField outputDirTextField;

    private ViewTableModel sourceTableModel;

    private String outputDirBase;

    private String csvFileDir;

    /** List of info data that is to be written out in xml...it is linked to same order as the table * */
    private final ArrayList<LinkedHashMap<String, String>> infoList = new ArrayList<LinkedHashMap<String, String>>();

    /** this is an arraylist of selected DataStruct Objects * */
    private ArrayList<DataStruct> dataStructures = null;

    /** Buffered writer for writing to CSV file */
    protected BufferedWriter bw;

    protected FileWriter fw;

    private Hashtable<String, String> csvStructRowData;
    
    private Hashtable<String, Integer> csvStructRecordCounters;

    private static final String CSV_OUTPUT_DELIM = ",";

    private final ArrayList<ViewJComponentPreviewImage> previewImages = new ArrayList<ViewJComponentPreviewImage>();

    private final ArrayList<File> imageFiles = new ArrayList<File>();

    private final ArrayList<ArrayList<File>> allOtherFilesAL = new ArrayList<ArrayList<File>>();

    private final ArrayList<Boolean> multifiles = new ArrayList<Boolean>();

    private final ArrayList<TreeMap<JLabel, JComponent>> labelsAndCompsList = new ArrayList<TreeMap<JLabel, JComponent>>();

    /** DOCUMENT ME! */
    private final int origBrightness = 0;

    private JLabel current, current2;

    private JSlider brightnessSlider, contrastSlider;

    /** DOCUMENT ME! */
    private NumberFormat nfc;

    /** DOCUMENT ME! */
    private final float origContrast = 1;

    private JPanel brightnessContrastPanel;

    private ViewJComponentPreviewImage previewImg;

    /** DOCUMENT ME! */
    private float contrast = 1;

    /** DOCUMENT ME! */
    private int brightness = 0;

    /** Dev data dictionary server. */
    private static final String ddDevServer = "http://fitbir-dd-dev.cit.nih.gov/";
    
    /** Dev portal auth server. */
    private static final String authDevServer = "http://fitbir-portal-dev.cit.nih.gov/";
    
    /** Stage data dictionary server. */
    private static final String ddStageServer = "http://fitbir-dd-stage.cit.nih.gov/";
    
    /** Stage portal auth server. */
    private static final String authStageServer = "http://fitbir-portal-stage.cit.nih.gov/";
    
    /** Demo data dictionary server. */
    private static final String ddDemoServer = "http://fitbir-dd-demo.cit.nih.gov/";
    
    /** Demo portal auth server. */
    private static final String authDemoServer = "http://fitbir-portal-demo.cit.nih.gov/";
    
    /** Prod data dictionary server. */
    private static final String ddProdServer = "https://dictionary.fitbir.nih.gov/";
    
    /** Prod portal auth server. */
    private static final String authProdServer = "https://fitbir.nih.gov/";
    
    /** File name of server configuration. */
    private static final String configFileName = "brics_config.properties";
    
    /** Property for reading the dd server url from the fitbir config file. */
    private static final String ddServerURLProp = "ddServerURL";
    
    /** Property for reading the auth server url from the fitbir config file. */
    private static final String authServerURLProp = "authServerURL";
    
    /** Full data dictionary server url */
    private static String ddServerURL = ddProdServer;
    
    /** Full authentication server url */
    private static String authServerURL = authProdServer;

    private DictionaryProvider dictionaryProvider;

    private List<BasicDataStructure> dataStructureList;

    private File csvFile;

    private String[] csvFieldNames;

    private ArrayList<String> tempDirs = new ArrayList<String>();

    private boolean isFinished = false;

    /**
     * Indicates how to resolve conflicts between csv and image header values. 0 = no choice made/ask always, 1 = csv, 2 =
     * image
     */
    private int resolveConflictsUsing = 0;

    private static final String pluginVersion = "0.5";
    
    private static final String[] allowedGuidPrefixes = new String[] {"TBI", "PD"};
    
    private static final String MAIN_GROUP_NAME = "Main";
    
    private static final String FILE_ELEMENT_TYPE = "File";
    
    private static final String GUID_ELEMENT_NAME = "GUID";
    
    private static final String IMG_FILE_ELEMENT_NAME = "ImgFileName";
    
    private static final String IMG_PREVIEW_ELEMENT_NAME = "ImgPreviewFileName";
    
    private static final String IMG_HASH_CODE_ELEMENT_NAME = "ImgFileHashCode";
    
    private static final String recordIndicatorColumn = "record";

    /** Text of the privacy notice displayed to the user before the plugin can be used. */
     public static final String PRIVACY_NOTICE = 
    	   "BRICS is a collaborative environment with privacy rules that pertain to the collection\n"
         + "and display of imaging data. Before accessing and using BRICS, please ensure that you\n"
         + "familiarize yourself with our privacy rules, available through the BRICS Rules of Behavior\n"
         + "document and supporting documentation.\n"
         + "\n"
         + "Collection of this information is authorized under 42 U.S.C. 241, 242, 248, 281(a)(b)(1)(P)\n"
         + "and 44 U.S.C. 3101. The primary use of this information is to facilitate medical research.\n"
         + "This information may be disclosed to researchers for research purposes, and to system \n"
         + "administrators for evaluation and data normalization.\n"
         + "\n"
         + "Rules governing submission of this information are based on the data sharing rules defined in\n"
         + "the Notice of Grant Award (NOGA). If you do not have a grant defining data sharing requirements,\n"
         + "data submission is voluntary. Data entered into BRICS will be used solely for scientific and\n"
         + "research purposes. Significant system update information may be posted on\n"
         + "the BRICS site as required.";
    
    public PlugInDialogFITBIR() {
        super(false);

        final int response = JOptionPane.showConfirmDialog(this, PlugInDialogFITBIR.PRIVACY_NOTICE,
                "Image Submission Package Creation Tool", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            outputDirBase = Preferences.getProperty(Preferences.PREF_BRICS_PLUGIN_OUTPUT_DIR);
            if (outputDirBase == null) {
                outputDirBase = System.getProperty("user.home") + File.separator + "mipav" + File.separator
                        + "BRICS_Imaging_Submission" + File.separator;
                Preferences.setProperty(Preferences.PREF_BRICS_PLUGIN_OUTPUT_DIR, outputDirBase);
            }

            csvFileDir = Preferences.getProperty(Preferences.PREF_BRICS_PLUGIN_CSV_DIR);
            if (csvFileDir == null) {
                csvFileDir = ViewUserInterface.getReference().getDefaultDirectory();
            }

            init();
            setVisible(true);
            validate();
        } else {
            if (JDialogStandalonePlugin.isExitRequired()) {
                System.exit(0);
                // ViewUserInterface.getReference().windowClosing(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            } else {
                return;
            }
        }

        final Thread thread = new WebServiceThread(this);
        thread.start();

    }

    public void actionPerformed(final ActionEvent e) {

        /*
         * @todo Implement this java.awt.event.ActionListener abstract method
         */

        final String command = e.getActionCommand();

        // System.err.println("size : " + this.getSize());

        if (command.equalsIgnoreCase("AddSource")) {

            new ChooseDataStructDialog(this);

            removeSourceButton.setEnabled(sourceTableModel.getRowCount() > 0);
            completeDataElementsButton.setEnabled(sourceTableModel.getRowCount() > 0);
            listPane.setBorder(buildTitledBorder(sourceTableModel.getRowCount() + " Form Structure(s) "));

        } else if (command.equalsIgnoreCase("loadCSV")) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(csvFileDir));
            chooser.setDialogTitle("Choose CSV file");
            chooser.addChoosableFileFilter(new FileNameExtensionFilter("Comma separated value files (.csv)", "csv"));
            int returnValue = chooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                csvFile = chooser.getSelectedFile();
                readCSVFile();

                csvFileDir = chooser.getSelectedFile().getAbsolutePath() + File.separator;
                Preferences.setProperty(Preferences.PREF_BRICS_PLUGIN_CSV_DIR, csvFileDir);
            }
        } else if (command.equalsIgnoreCase("RemoveSource")) {
            final int selected = sourceTable.getSelectedRow();
            final String key = (String) sourceTable.getValueAt(selected, 0);
            sourceTableModel.removeRow(selected);
            previewImages.remove(selected);
            imageFiles.remove(selected);
            multifiles.remove(selected);
            infoList.remove(selected);

            previewPanel.removeAll();
            previewPanel.repaint();

            if (sourceTable.getRowCount() >= 1) {

                if (selected == 0) {
                    sourceTable.setRowSelectionInterval(0, 0);
                } else {
                    sourceTable.setRowSelectionInterval(selected - 1, selected - 1);
                }

                if (previewImages.get(sourceTable.getSelectedRow()) != null) {
                    previewPanel.add(previewImages.get(sourceTable.getSelectedRow()));
                    previewImages.get(sourceTable.getSelectedRow()).setSliceBrightness(brightness, contrast);
                    previewPanel.validate();
                    previewPanel.repaint();
                }
            }

            removeSourceButton.setEnabled(sourceTableModel.getRowCount() > 0);
            completeDataElementsButton.setEnabled(sourceTableModel.getRowCount() > 0);
            if (sourceTableModel.getRowCount() > 0) {
                enableDisableFinishButton();

                if (selected >= sourceTableModel.getRowCount()) {
                    sourceTable.setRowSelectionInterval(sourceTableModel.getRowCount() - 1, sourceTableModel
                            .getRowCount() - 1);
                } else {
                    sourceTable.setRowSelectionInterval(selected, selected);
                }

            } else {
                finishButton.setEnabled(false);

            }
            listPane.setBorder(buildTitledBorder(sourceTableModel.getRowCount() + " Form Structure(s) "));
        } else if (command.equalsIgnoreCase("Help")) {

            // MipavUtil.showHelp("ISPImages01");

        } else if (command.equalsIgnoreCase("Finish")) {
            final javax.swing.SwingWorker<Object, Object> worker = new javax.swing.SwingWorker<Object, Object>() {
                public Object doInBackground() {
                    createSubmissionFiles();

                    return null;
                }
            };
            final int response = JOptionPane.showConfirmDialog(this, "Done adding image datasets?",
                    "Done adding image datasets?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            // we're now letting the fields be enforced by the validation tool
            // final int numRows = sourceTableModel.getRowCount();
            // boolean areAllCompleted = true;
            // for (int i = 0; i < numRows; i++) {
            // if ( ((String) sourceTableModel.getValueAt(i, 1)).equalsIgnoreCase("No")) {
            // areAllCompleted = false;
            // break;
            // }
            // }
            //
            // if ( !areAllCompleted) {
            // MipavUtil.displayError("Please complete required fields for all Form Structures");
            // return;
            // }

            // instead, just require that the GUIDs are filled in
            final int numRows = sourceTableModel.getRowCount();
            boolean areGuidsCompleted = true;
            for (int i = 0; i < numRows; i++) {
                String struct = (String) sourceTableModel.getValueAt(i, 0);
                if (struct.endsWith("_UNKNOWNGUID")) {
                    areGuidsCompleted = false;
                    break;
                }
            }

            if ( !areGuidsCompleted) {
                MipavUtil.displayError("Please complete GUID field for all Form Structures");
                return;
            }

            if (response == JOptionPane.YES_OPTION) {
                worker.execute();
                removeSourceButton.setEnabled(false);
                finishButton.setEnabled(false);
                outputDirButton.setEnabled(false);
                addSourceButton.setEnabled(false);
                completeDataElementsButton.setEnabled(false);
                loadCSVButton.setEnabled(false);
            }
        } else if (command.equalsIgnoreCase("completeDataElements")) {

            final String dsName = (String) sourceTableModel.getValueAt(sourceTable.getSelectedRow(), 0);
            new InfoDialog(this, dsName, true, true, null);

        } else if (command.equalsIgnoreCase("outputDirBrowse")) {
            final JFileChooser chooser = new JFileChooser(outputDirBase);

            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Choose output directory for Validation Tool files");
            final int returnValue = chooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                outputDirTextField.setText(chooser.getSelectedFile().getAbsolutePath() + File.separator);

                outputDirBase = chooser.getSelectedFile().getAbsolutePath() + File.separator;
                Preferences.setProperty(Preferences.PREF_BRICS_PLUGIN_OUTPUT_DIR, outputDirBase);
            }
        }
    }

    private boolean readCSVFile() {
        try {
            String str;
            FileInputStream fis = new FileInputStream(csvFile);
            BufferedReader d = new BufferedReader(new InputStreamReader(fis));
            String dsName = "";
            String version = "";
            // first line is data structure name and version
            str = d.readLine().trim();
            String[] arr = str.split(",");
            dsName = arr[0];
            version = arr[1];
            if (version.length() == 1) {
                version = "0" + version;
            }
            
            // TODO: removed because new BIRCS ds names seem to not have version.  Do we need to check the version some other way?
            //dsName = dsName + version;
            
            // second line are the field names
            str = d.readLine().trim();
            csvFieldNames = str.split(",");
            for (int i = 0; i < csvFieldNames.length; i++) {
            	// TODO: when we add repeatable group support, this will need to be processed
            	if (csvFieldNames[i].equalsIgnoreCase(recordIndicatorColumn)) {
            		// skip the record field, since it doesn't map to a data element
            		continue;
            	}
            	
            	if (csvFieldNames[i].contains(".")) {
            		// found a period, so assume GROUPNAME.ELEMENTNAME. discard GROUPNAME
            		csvFieldNames[i] = csvFieldNames[i].split("\\.", 2)[1];
            	}
            }

            String[] csvParamsArr;
            while ( (str = d.readLine()) != null) {

                str = str.trim();
                arr = str.split(",");
                if (arr.length != csvFieldNames.length) {
                    csvParamsArr = new String[csvFieldNames.length];
                    for (int i = 0; i < arr.length; i++) {
                        csvParamsArr[i] = arr[i];
                    }
                    for (int i = arr.length; i < csvParamsArr.length; i++) {
                        csvParamsArr[i] = "";
                    }

                } else {
                    csvParamsArr = arr;
                }

                new InfoDialog(this, dsName, false, false, csvParamsArr);
            }
            fis.close();
        } catch (Exception e) {
            return false;
        }

        return true;

    }

    /**
     * Sets values based on knob along slider.
     * 
     * @param e Event that triggered this function
     */
    public void stateChanged(final ChangeEvent e) {
        final Object source = e.getSource();

        if (source == brightnessSlider) {
            brightness = brightnessSlider.getValue();
            current.setText(String.valueOf(brightness));

            // Change only the brightness and contrast of the current slice
            if (previewImg != null) {
                previewImages.get(sourceTable.getSelectedRow()).setSliceBrightness(brightness, contrast);
            }
        } else if (source == contrastSlider) {
            contrast = (float) Math.pow(10.0, contrastSlider.getValue() / 200.0);
            current2.setText(String.valueOf(nfc.format(contrast)));

            // Change only the brightness and contrast of the current slice
            if (previewImg != null) {
                previewImages.get(sourceTable.getSelectedRow()).setSliceBrightness(brightness, contrast);
            }
        }
    }

    public void itemStateChanged(final ItemEvent e) {

    }

    private void init() {
        setTitle("Image Submission Package Creation Tool v" + pluginVersion);

        dataStructures = new ArrayList<DataStruct>();

        final JPanel topPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc2 = new GridBagConstraints();
        final GridBagConstraints gbc3 = new GridBagConstraints();
        gbc2.anchor = GridBagConstraints.NORTHWEST;

        buildBrightnessContrastPanel();

        leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBorder(buildTitledBorder("Preview image"));
        leftPanel.setPreferredSize(new Dimension(200, 300));

        previewPanel = new JPanel();
        previewPanel.setBorder(buildTitledBorder("Preview image"));
        previewPanel.setPreferredSize(new Dimension(200, 250));

        gbc3.gridy = 0;
        gbc3.gridx = 0;
        gbc3.fill = GridBagConstraints.BOTH;
        leftPanel.add(previewPanel, gbc3);
        gbc3.gridy = 1;
        leftPanel.add(brightnessContrastPanel, gbc3);

        gbc2.gridy = 0;
        gbc2.gridx = 0;
        gbc2.fill = GridBagConstraints.BOTH;
        gbc2.weightx = 0;
        gbc2.weighty = 0;
        topPanel.add(previewPanel, gbc2);
        gbc2.gridy = 1;
        topPanel.add(brightnessContrastPanel, gbc2);

        gbc2.gridy = 0;
        gbc2.gridx = 1;
        gbc2.weightx = 1;
        gbc2.weighty = 1;
        gbc2.gridheight = 2;
        topPanel.add(buildSourcePanel(), gbc2);

        getContentPane().add(topPanel, BorderLayout.NORTH);

        getContentPane().add(buildLogPanel(), BorderLayout.CENTER);
        getContentPane().add(buildButtonPanel(), BorderLayout.SOUTH);
        pack();
        validate();
        this.setMinimumSize(this.getSize());
        this.setResizable(true);
        // this.setSize(new Dimension(610, 537));
    }

    /**
     * Build a panel for the zip and metadata file creation log.
     */
    private JPanel buildLogPanel() {
        final JPanel destPanel = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.anchor = GridBagConstraints.CENTER;

        gbc2.gridy = 0;
        gbc2.gridx = 0;

        logOutputArea = WidgetFactory.buildScrollTextArea(Color.white);
        logOutputArea.setBorder(buildTitledBorder("Output log"));
        logOutputArea.getTextArea().setEditable(false);
        logOutputArea.getTextArea().setRows(10);
        outputDirPanel = new JPanel();
        outputDirLabel = new JLabel("Output Directory for Validation Tool ");
        outputDirTextField = new JTextField(30);
        outputDirTextField.setEditable(false);
        outputDirTextField.setToolTipText(outputDirBase);
        outputDirTextField.setText(outputDirBase);
        outputDirButton = WidgetFactory.buildTextButton("Browse", "Choose Output Directory for Validation Tool files",
                "outputDirBrowse", this);
        outputDirButton.setPreferredSize(MipavUtil.defaultButtonSize);
        outputDirPanel.add(outputDirLabel);
        outputDirPanel.add(outputDirTextField);
        outputDirPanel.add(outputDirButton);

        destPanel.add(outputDirPanel, gbc2);

        gbc2.gridy = 1;
        gbc2.fill = GridBagConstraints.BOTH;
        gbc2.weightx = 1;
        gbc2.weighty = 1;
        gbc2.anchor = GridBagConstraints.NORTHWEST;
        destPanel.add(logOutputArea, gbc2);

        return destPanel;
    }

    public void valueChanged(final TreeSelectionEvent e) {

    }

    private JScrollPane buildSourcePanel() {
        sourceTableModel = new ViewTableModel();
        sourceTableModel.addColumn("Form Structure Name");
        sourceTableModel.addColumn("Completed?");

        sourceTable = new JTable(sourceTableModel);
        sourceTable.addMouseListener(this);
        sourceTable.setPreferredScrollableViewportSize(new Dimension(650, 300));
        sourceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        sourceTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        sourceTable.getColumn("Completed?").setMinWidth(100);
        sourceTable.getColumn("Completed?").setMaxWidth(100);

        sourceTable.getColumn("Completed?").setCellRenderer(new MyRightCellRenderer());

        listPane = WidgetFactory.buildScrollPane(sourceTable);
        listPane.setBorder(buildTitledBorder(0 + " Form Structure(s) "));

        return listPane;
    }

    private ModelImage createThumbnailImage(final ModelImage origImage) {
        ModelImage thumbnailImage = null;

        // create a thumbnail image...4 colums, 2 rows
        // grab the middle 8 slices from the image for the thumbnail
        // need to determine by what percentage...so...need to figure out by what percebtahe the xdim will go down
        // to 128
        // startSLice will be 3 less than middle slice
        // endSlice will be 4 more than middle slice
        final int xDim = origImage.getExtents()[0];
        int percentage = 100;
        if (xDim > 128) {
            final float perc = 128f / xDim * 100;
            percentage = (int) Math.floor(perc);
        }
        final int columns = 4;
        final int rows = 2;
        final int rBorderVal = 255;
        final int gBorderVal = 0;
        final int bBorderVal = 0;
        final int borderThick = 1;
        int startSlice = 0;
        int endSlice = 0;
        int numSlices = 0;
        int middleSlice = 0;
        LightboxGenerator lightGen;

        if (origImage.is2DImage()) {
            // Creating a blank TransMatrix for resampling
            final TransMatrix percentSizer = new TransMatrix(4);
            percentSizer.set(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);

            // Resample image size based on percent inputted
            final AlgorithmTransform transformer = new AlgorithmTransform(origImage, percentSizer, 1,
                    (float) (origImage.getResolutions(0)[0] / (percentage * .01)),
                    (float) (origImage.getResolutions(0)[1] / (percentage * .01)), (int) (origImage.getExtents()[0]
                            * percentage * .01), (int) (origImage.getExtents()[1] * percentage * .01), origImage
                            .getUnitsOfMeasure(), false, true, false, true, origImage.getImageCentermm(false));
            transformer.runAlgorithm();
            thumbnailImage = transformer.getTransformedImage();
            thumbnailImage.calcMinMax();
            // convert this image to color image if it is not
            if ( !thumbnailImage.isColorImage()) {
                final ModelImage newRGB = new ModelImage(ModelStorageBase.ARGB, thumbnailImage.getExtents(),
                        thumbnailImage.getImageName());
                final AlgorithmRGBConcat mathAlgo = new AlgorithmRGBConcat(thumbnailImage, thumbnailImage,
                        thumbnailImage, newRGB, true, true, 255.0f, true);
                mathAlgo.run();
                thumbnailImage.disposeLocal();
                thumbnailImage = null;
                thumbnailImage = newRGB;
            }
        } else if (origImage.is3DImage()) {
            numSlices = origImage.getExtents()[2];
            numSlices = numSlices - 1; // its 0 based
            middleSlice = numSlices / 2;
            startSlice = middleSlice - 3;
            if (startSlice < 0) {
                startSlice = 0;
            }
            endSlice = middleSlice + 4;
            if (endSlice > numSlices - 1) {
                endSlice = numSlices - 1;
            }

            try {
                // Make algorithm
                lightGen = new LightboxGenerator(origImage, startSlice, endSlice, percentage, rows, columns,
                        rBorderVal, gBorderVal, bBorderVal, false, borderThick);
                lightGen.run();
                thumbnailImage = lightGen.getImage();
                thumbnailImage.calcMinMax();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else if (origImage.is4DImage()) {
            // get middle time volume
            final int[] destExtents = new int[3];
            final int xSlices = origImage.getExtents()[0];
            final int ySlices = origImage.getExtents()[1];
            final int zSlices = origImage.getExtents()[2];
            destExtents[0] = xSlices;
            destExtents[1] = ySlices;
            destExtents[2] = zSlices;

            ModelImage timeImage = new ModelImage(origImage.getType(), destExtents, "");

            final int tSlices = origImage.getExtents()[3];
            int middleVol = (int) Math.floor(tSlices / 2);
            if (middleVol > 0) {
                middleVol = middleVol - 1; // 0 based
            }
            final AlgorithmSubset subsetAlgo = new AlgorithmSubset(origImage, timeImage, AlgorithmSubset.REMOVE_T,
                    middleVol);
            subsetAlgo.run();

            numSlices = timeImage.getExtents()[2];
            numSlices = numSlices - 1; // its 0 based
            middleSlice = numSlices / 2;
            startSlice = middleSlice - 3;
            if (startSlice < 0) {
                startSlice = 0;
            }
            endSlice = middleSlice + 4;
            if (endSlice > numSlices - 1) {
                endSlice = numSlices - 1;
            }
            try {
                // Make algorithm
                lightGen = new LightboxGenerator(timeImage, startSlice, endSlice, percentage, rows, columns,
                        rBorderVal, gBorderVal, bBorderVal, false, borderThick);
                lightGen.run();
                thumbnailImage = lightGen.getImage();
                thumbnailImage.calcMinMax();
                if (timeImage != null) {
                    timeImage.disposeLocal();
                    timeImage = null;
                }
            } catch (final Exception e) {

            }
        }

        return thumbnailImage;
    }

    /**
     * Create the ZIP(s) containing the original image files and the meta-data for each image dataset.
     */
    private void createSubmissionFiles() {

        final File outputDirFile = new File(outputDirBase);
        if ( !outputDirFile.exists()) {
            outputDirFile.mkdirs();
        }

        final int numDataStructs = sourceTableModel.getRowCount();

        // for each data structure chosen by the user, create a place to put rows of csv data
        csvStructRowData = new Hashtable<String, String>();
        csvStructRecordCounters = new Hashtable<String, Integer>();
        for (int i = 0; i < numDataStructs; i++) {
            String tableName = (String) sourceTableModel.getValueAt(i, 0);
            // format: "structname_PREFIXGUID"
            String lowerName = getStructFromString(tableName).toLowerCase();
            if ( !csvStructRowData.containsKey(lowerName)) {
                DataStruct structInfo = null;
                for (DataStruct struct : dataStructures) {
                    if (struct.getName().equalsIgnoreCase(lowerName)) {
                        structInfo = struct;
                    }
                }

                String n = lowerName;
                final String v = structInfo.getVersion().replaceFirst("^0", "");

                char c1 = n.charAt(n.length() - 1);
                if (Character.isDigit(c1)) {
                    n = n.substring(0, n.length() - 1);
                }
                char c2 = n.charAt(n.length() - 1);
                if (Character.isDigit(c2)) {
                    n = n.substring(0, n.length() - 1);
                }

                // # commas at end = # fields in struct - 2 (for name & version) + 1 (record column)
                String cStr = "";
                for (int j = 0; j < structInfo.size() - 1; j++) {
                    cStr += CSV_OUTPUT_DELIM;
                }

                String elementHeader = recordIndicatorColumn;
                for (int j = 0; j < structInfo.size(); j++) {
                	DataElement de = (DataElement) structInfo.get(j);
                    elementHeader += CSV_OUTPUT_DELIM + de.getGroup() + "." + de.getName();
                }

                String structHeader = n + CSV_OUTPUT_DELIM + v + cStr + "\n";
                structHeader += elementHeader + "\n";
                csvStructRowData.put(lowerName, structHeader);
                csvStructRecordCounters.put(lowerName, 1);
            }
        }

        for (String lowerName : csvStructRowData.keySet()) {
            System.out.println("**** " + lowerName);
            System.out.println(csvStructRowData.get(lowerName));
        }

        for (int i = 0; i < numDataStructs; i++) {
            int collisionCounter = 1;
            final String name = (String) sourceTableModel.getValueAt(i, 0);

            final String guid = getGuidFromString(name);

            final File imageFile = imageFiles.get(i);
            String outputFileNameBase;

            if (imageFile != null) {

                // this means we are working with the image datastructure
                printlnToLog("Creating submission file for " + name);
                printlnToLog("Opening: " + imageFile + ", multifile: " + multifiles.get(i));

                final FileIO fileIO = new FileIO();
                fileIO.setQuiet(true);
                final ModelImage origImage = fileIO.readImage(imageFile.getName(), imageFile.getParent()
                        + File.separator, multifiles.get(i), null);

                final List<String> origFiles = FileUtility.getFileNameList(origImage);

                final int modality = origImage.getFileInfo(0).getModality();
                final String modalityString = FileInfoBase.getModalityStr(modality).replaceAll("\\s+", "");

                final String dsName = getStructFromString(name);

                outputFileNameBase = guid + "_" + dsName + "_" + System.currentTimeMillis();

                // NEED TO ASK EVAN ABOUT THIS
                /*
                 * if (modality == FileInfoBase.UNKNOWN_MODALITY) { outputFileNameBase = guid + "_" + dsName + "_" +
                 * System.currentTimeMillis(); } else { outputFileNameBase = guid + "_" + modalityString + "_" +
                 * System.currentTimeMillis(); }
                 */

                final String zipFilePath = outputDirBase + outputFileNameBase + ".zip";

                ModelImage thumbnailImage = createThumbnailImage(origImage);

                final FileWriteOptions opts = new FileWriteOptions(outputFileNameBase + ".jpg", outputDirBase, true);
                writeThumbnailJIMI(thumbnailImage, opts);
                if (thumbnailImage != null) {
                    thumbnailImage.disposeLocal();
                    thumbnailImage = null;
                }
                printlnToLog("Creating thumbnail image:\t" + outputDirBase + outputFileNameBase + ".jpg");
                try {
                    printlnToLog("Creating ZIP file:\t" + zipFilePath);
                    for (final String file : origFiles) {
                        printlnToLog("Adding file to ZIP:\t" + file);
                    }

                    makeZipFile(zipFilePath, origFiles);
                } catch (final IOException ioe) {
                    ioe.printStackTrace();
                    MipavUtil.displayError("Unable to write original image dataset files to ZIP package:\n"
                            + ioe.getMessage());
                    continue;
                }
                
                // calculate hash of the zip file and then put it into the image file hash code CDE (if it exists in the struct)
                String hashCode = ",";
                try {
					hashCode = computeFileHash(zipFilePath);
				} catch (IOException e) {
					e.printStackTrace();
                    MipavUtil.displayError("Unable calculate hash code of ZIP file:\n" + e.getMessage());
                    continue;
				}

                DataStruct curStruct = null;
                for (DataStruct ds : dataStructures) {
                    if (ds.getName().equalsIgnoreCase(dsName)) {
                        curStruct = ds;
                        break;
                    }
                }
                
                // TODO: add support for not hardcoding the record column to enable repeating groups

                String newRow = getCSVDataRow(outputDirBase, outputFileNameBase, curStruct, imageFile, origImage, i, hashCode);
                if ( !newRow.equals("")) {
                    String lowerName = dsName.toLowerCase();
                    String data = csvStructRowData.get(lowerName);
                    data += csvStructRecordCounters.get(lowerName) + CSV_OUTPUT_DELIM + newRow + "\n";
                    csvStructRecordCounters.put(lowerName, csvStructRecordCounters.get(lowerName) + 1);
                    csvStructRowData.put(lowerName, data);
                }

                origImage.disposeLocal();

                printlnToLog("");

            } else {

                // this means that this is another data structure besides image

                printlnToLog("Creating submission file for " + name);

                String dsName = getStructFromString(name);

                outputFileNameBase = guid + "_" + dsName + "_" + System.currentTimeMillis();

                // if the data_structure contains image_file or image_thumbnail_file, just copy them over to submission
                // package

                LinkedHashMap<String, String> infoMap = infoList.get(i);
                Set<String> keySet = infoMap.keySet();
                Iterator<String> iter = keySet.iterator();
                String key;
                String value;
                File f;
                String csvDir = "";
                String copyFromImageFilePath = "";
                String copyFromImageThumbnailPath = "";
                String copyToImageFilePath = "";
                String copyToImageThumbnailPath = "";
                while (iter.hasNext()) {
                    key = (String) iter.next();
                    value = infoMap.get(key);
                    if (key.equalsIgnoreCase("image_file")) {

                        value = value.replace("\\", File.separator);
                        value = value.replace("/", File.separator);
                        f = new File(value);
                        if ( !f.exists()) {
                            // must be a relative path based on csv file
                            if (csvFile != null) {
                                csvDir = csvFile.getParentFile().getAbsolutePath() + File.separator;
                            }
                            f = new File(csvDir + value);
                            if (f.exists()) {
                                copyFromImageFilePath = csvDir + value;
                                copyToImageFilePath = value;
                            }
                        } else {
                            copyFromImageFilePath = value;
                            copyToImageFilePath = value
                                    .substring(value.lastIndexOf(File.separator) + 1, value.length());
                        }

                    } else if (key.equalsIgnoreCase("image_thumbnail_file")) {

                        value = value.replace("\\", File.separator);
                        value = value.replace("/", File.separator);
                        f = new File(value);
                        if ( !f.exists()) {
                            // must be a relative path based on csv file
                            if (csvFile != null) {
                                csvDir = csvFile.getParentFile().getAbsolutePath() + File.separator;
                            }
                            f = new File(csvDir + value);
                            if (f.exists()) {
                                copyFromImageThumbnailPath = csvDir + value;
                                copyToImageThumbnailPath = value;
                            }
                        } else {
                            copyFromImageThumbnailPath = value;
                            copyToImageThumbnailPath = value.substring(value.lastIndexOf(File.separator) + 1, value
                                    .length());
                        }
                    }
                }

                if (copyToImageFilePath.contains(File.separator)) {
                    // make directories
                    String dir = outputDirBase + File.separator
                            + copyToImageFilePath.substring(0, copyToImageFilePath.lastIndexOf(File.separator));
                    File f1 = new File(dir);
                    f1.mkdirs();

                    File f2 = new File(outputDirBase + File.separator + copyToImageFilePath);

                    try {
                        final InputStream in = new FileInputStream(copyFromImageFilePath);
                        final OutputStream out = new FileOutputStream(f2);

                        final byte[] buf = new byte[1024];
                        int len;
                        while ( (len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        in.close();
                        out.close();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    // just copy it over without making directories
                    File f2 = new File(outputDirBase + File.separator + copyToImageFilePath);

                    try {
                        final InputStream in = new FileInputStream(copyFromImageFilePath);
                        final OutputStream out = new FileOutputStream(f2);

                        final byte[] buf = new byte[1024];
                        int len;
                        while ( (len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        in.close();
                        out.close();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }

                if (copyToImageThumbnailPath.contains(File.separator)) {
                    // make directories
                    String dir = outputDirBase
                            + File.separator
                            + copyToImageThumbnailPath.substring(0, copyToImageThumbnailPath
                                    .lastIndexOf(File.separator));
                    File f1 = new File(dir);
                    f1.mkdirs();

                    File f2 = new File(outputDirBase + File.separator + copyToImageThumbnailPath);

                    try {
                        final InputStream in = new FileInputStream(copyFromImageFilePath);
                        final OutputStream out = new FileOutputStream(f2);

                        final byte[] buf = new byte[1024];
                        int len;
                        while ( (len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        in.close();
                        out.close();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    // just copy it over without making directories
                    File f2 = new File(outputDirBase + File.separator + copyToImageThumbnailPath);

                    try {
                        final InputStream in = new FileInputStream(copyFromImageFilePath);
                        final OutputStream out = new FileOutputStream(f2);

                        final byte[] buf = new byte[1024];
                        int len;
                        while ( (len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        in.close();
                        out.close();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }

                // copy all other file to this new dir
                final File allOtherFilesFile = new File(outputDirBase + outputFileNameBase);

                ArrayList<File> files = allOtherFilesAL.get(i);
                if (files != null && files.size() > 0) {

                    if (allOtherFilesFile.mkdir()) {

                        if (files != null && files.size() > 0) {
                            for (int k = 0; k < files.size(); k++) {
                                f = files.get(k);

                                File destFile = new File(outputDirBase + outputFileNameBase + File.separator
                                        + f.getName());
                                // check for collision
                                if (destFile.exists()) {
                                    // collision!
                                    String prefix = f.getName().substring(0, f.getName().lastIndexOf("."));
                                    String suffix = f.getName().substring(f.getName().lastIndexOf(".") + 1,
                                            f.getName().length());
                                    destFile = new File(outputDirBase + outputFileNameBase + File.separator + prefix
                                            + "_" + collisionCounter + "." + suffix);

                                    infoMap = infoList.get(i);

                                    keySet = infoMap.keySet();
                                    iter = keySet.iterator();

                                    while (iter.hasNext()) {
                                        key = (String) iter.next();
                                        value = infoMap.get(key);
                                        if (value.equals(f.getAbsolutePath())) {
                                            prefix = value.substring(0, value.lastIndexOf("."));
                                            suffix = value.substring(value.lastIndexOf(".") + 1, value.length());
                                            infoMap.put(key, prefix + "_" + collisionCounter + "." + suffix
                                                    + "_collision");
                                            break;

                                        }

                                    }

                                    collisionCounter++;
                                }

                                printlnToLog("Copying " + f.getName() + " to " + destFile.getAbsolutePath());

                                try {
                                    final InputStream in = new FileInputStream(f);
                                    final OutputStream out = new FileOutputStream(destFile);

                                    final byte[] buf = new byte[1024];
                                    int len;
                                    while ( (len = in.read(buf)) > 0) {
                                        out.write(buf, 0, len);
                                    }
                                    in.close();
                                    out.close();
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    }
                }

                DataStruct curStruct = null;
                for (DataStruct ds : dataStructures) {
                    if (ds.getName().equalsIgnoreCase(dsName)) {
                        curStruct = ds;
                        break;
                    }
                }

                //may need to have hashcode field fixed
                String newRow = getCSVDataRow(outputDirBase, outputFileNameBase, curStruct, imageFile, null, i, ",");
                if ( !newRow.equals("")) {
                    String lowerName = dsName.toLowerCase();
                    String data = csvStructRowData.get(lowerName);
                    data += csvStructRecordCounters.get(lowerName) + CSV_OUTPUT_DELIM + newRow + "\n";
                    csvStructRecordCounters.put(lowerName, csvStructRecordCounters.get(lowerName) + 1);
                    csvStructRowData.put(lowerName, data);
                }

                printlnToLog("");
            }
        }

        // write out the built up CSV data for each struct
        try {
            for (String lowerName : csvStructRowData.keySet()) {
                final String csvFileName = lowerName + "_output_" + System.currentTimeMillis() + ".csv";

                printlnToLog("Writing " + lowerName + " to CSV file: " + outputDirBase + csvFileName);

                final File csvFile = new File(outputDirBase + csvFileName);
                fw = new FileWriter(csvFile);
                bw = new BufferedWriter(fw);

                bw.write(csvStructRowData.get(lowerName));

//                System.out.println(csvStructRowData.get(lowerName) + "  ||||  ");
                
                bw.close();
            }

            printlnToLog("");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            printlnToLog("Unable to write CSV output file(s).");
        } finally {
            try {
                bw.close();
            } catch (Exception e) {
                // Do nothing
            }
        }

        printlnToLog("*** Submission pre-processing complete. ***");
        printlnToLog("*** Output files have been generated in directory " + outputDirBase + " ***");
        printlnToLog("*** To submit to BRICS, run the BRICS Validation Tool to package the files for submission. ***");

        // need to delete all tempDirs that were created
        if (tempDirs.size() > 0) {
            for (int i = 0; i < tempDirs.size(); i++) {
                String dir = tempDirs.get(i);
                File f = new File(dir);
                if (f.exists()) {
                    String[] list = f.list();
                    if (list != null) {
                        for (int k = 0; k < list.length; k++) {
                            File entry = new File(f, list[k]);
                            entry.delete();
                        }
                    }
                    f.delete();
                }
            }
        }

        isFinished = true;
    }

    /**
     * writes out csv file
     * 
     * @param outputDirBase
     * @param outputFileNameBase
     * @param imageFile
     * @param origImage
     */
    private final String getCSVDataRow(final String outputDirBase, final String outputFileNameBase, DataStruct ds,
            final File imageFile, final ModelImage origImage, int counter, String hashCode) {
        String csvRow = new String();

        if (ds == null) {
            return csvRow;
        }

        final LinkedHashMap<String, String> infoMap = infoList.get(counter);

        for (int k = 0; k < ds.size(); k++) {

            final Object o1 = ds.get(k);
            if (o1 instanceof DataElement) {
                // data element
                final DataElement de = (DataElement) o1;
                final String name = de.getName();
                String value = "";
                String v;
                if (imageFile != null) {
                    if (name.equalsIgnoreCase(IMG_FILE_ELEMENT_NAME)) {
                        value = outputFileNameBase + ".zip";
                    } else if (name.equalsIgnoreCase(IMG_PREVIEW_ELEMENT_NAME)) {
                        value = outputFileNameBase + ".jpg";
                    } else if (name.equalsIgnoreCase(IMG_HASH_CODE_ELEMENT_NAME)) {
                    	value = hashCode;
                    } else {
                        // need to get appropriate value
                        for (String key : infoMap.keySet()) {
                            if (key.equalsIgnoreCase(name)) {
                                v = infoMap.get(key);
                                value = v;
                                break;
                            }
                        }
                    }
                } else {
                    // need to get appropriate value
                    for (String key : infoMap.keySet()) {
                        if (key.equalsIgnoreCase(name)) {
                            v = infoMap.get(key);
                            value = v;
                            break;
                        }
                    }
                }

                // should we be outputting all fields? - if not, need to not include element name in header

                // if ( !value.trim().equalsIgnoreCase("")) {
                final File f = new File(value);
                if (f.isFile() || value.endsWith("_collision")) {
                    if (value.endsWith("_collision")) {
                        value = value.substring(0, value.indexOf("_collision"));
                        final String filename = value.substring(value.lastIndexOf(File.separator) + 1, value.length());
                        value = outputFileNameBase + File.separator + filename;
                    } else {
                        final String filename = f.getName();
                        value = outputFileNameBase + File.separator + filename;
                    }
                }

                // escape commas in values - if there's a comma, put quotes around the value and double up any existing
                // quotes
                if (value.contains(",")) {
                    value = "\"" + value.replaceAll("\"", "\"\"") + "\"";
                }

                if (k == 0) {
                    csvRow = value;
                } else {
                    csvRow += CSV_OUTPUT_DELIM + value;
                }
                // }
            }
        }

        return csvRow;
    }

    /**
     * Adds a set of files to a ZIP archive.
     * 
     * @param destZipFile The full path to the ZIP archive to create.
     * @param srcFiles A list of files (full paths) to include in the ZIP archive.
     * @throws IOException If there is a problem reading the srcFiles or writing to the ZIP file.
     */
    private void makeZipFile(final String destZipFile, final List<String> srcFiles) throws IOException {
        // Create a buffer for reading the files
        final byte[] buf = new byte[1024];

        // Create the ZIP file
        final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destZipFile));

        // Compress the files
        for (final String file : srcFiles) {
            final FileInputStream in = new FileInputStream(file);

            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(FileUtility.getFileName(file)));

            // Transfer bytes from the file to the ZIP file
            int len;
            while ( (len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            // Complete the entry
            out.closeEntry();
            in.close();
        }

        // Complete the ZIP file
        out.close();
    }
    
    /**
     * Calculates the SHA-256 hash of a file.
     * @param filePath The full path of the file to calculate the hash of.
     * @return The SHA-256 digest of the given file, or an empty string if the SHA-256 algorithm was not found.
     * @throws IOException If there is a problem reading the file specified.
     */
    private String computeFileHash(String filePath) throws IOException {
    	String hashCode;
		try {
			MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
			
			RandomAccessFile file = new RandomAccessFile(filePath, "r");
			
			// 64k buffer
	    	int buffSize = 65536;
	    	
	    	byte[] buffer = new byte[buffSize];

	        long read = 0;
	        long offset = file.length();
	        int size;
	        while (read < offset) {
	                size = (int) (((offset - read) >= buffSize) ? buffSize : (offset - read));
	                file.read(buffer, 0, size);
	                shaDigest.update(buffer, 0, size);
	                read += size;
	        }
	        file.close();
	        
	        hashCode = new String(Hex.encode(shaDigest.digest()));
	    	
	    	return hashCode;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			MipavUtil.displayError("Unable to generate file hash: SHA-256 algorithm not found.");
			return new String();
		}
    }

    /**
     * Append a line to the log output area in the Log tab.
     * 
     * @param line The line to append (do not include the trailing newline).
     */
    private void printlnToLog(final String line) {
        logOutputArea.getTextArea().append(line + "\n");
    }

    private JPanel buildButtonPanel() {

        final JPanel buttonPanel1 = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;

        addSourceButton = new JButton("Add Form Structure");
        addSourceButton.setToolTipText("Add Form Structure");
        addSourceButton.addActionListener(this);
        addSourceButton.setActionCommand("AddSource");

        loadCSVButton = new JButton("Load CSV File");
        loadCSVButton.setToolTipText("Load CSV File");
        loadCSVButton.addActionListener(this);
        loadCSVButton.setActionCommand("loadCSV");

        removeSourceButton = new JButton("Remove Form Structure");
        removeSourceButton.setToolTipText("Remove the selected Form Structure");
        removeSourceButton.addActionListener(this);
        removeSourceButton.setActionCommand("RemoveSource");

        finishButton = new JButton("Generate Files");
        finishButton.setToolTipText("Generate Files");
        finishButton.addActionListener(this);
        finishButton.setActionCommand("Finish");

        completeDataElementsButton = new JButton("Edit Data Elements");
        completeDataElementsButton.setToolTipText("Edit data elements for selected Form Structure");
        completeDataElementsButton.addActionListener(this);
        completeDataElementsButton.setActionCommand("completeDataElements");

        addSourceButton.setPreferredSize(MipavUtil.defaultButtonSize);
        removeSourceButton.setPreferredSize(MipavUtil.defaultButtonSize);
        finishButton.setPreferredSize(MipavUtil.defaultButtonSize);
        completeDataElementsButton.setPreferredSize(MipavUtil.defaultButtonSize);

        addSourceButton.setEnabled(false);
        loadCSVButton.setEnabled(false);
        removeSourceButton.setEnabled(false);
        completeDataElementsButton.setEnabled(false);
        finishButton.setEnabled(false);

        gbc.gridx = 0;
        buttonPanel1.add(loadCSVButton, gbc);
        gbc.gridx = 1;
        buttonPanel1.add(addSourceButton, gbc);
        gbc.gridx = 2;
        buttonPanel1.add(removeSourceButton, gbc);
        gbc.gridx = 3;
        buttonPanel1.add(completeDataElementsButton, gbc);
        gbc.gridx = 4;
        buttonPanel1.add(finishButton, gbc);

        return buttonPanel1;
    }

    public Dimension getPanelSize() {
        return new Dimension(previewPanel.getBounds().width, previewPanel.getBounds().height);
    }

    /**
     * Writes a JIMI file to store the image.
     * 
     * @param image The image to write.
     * @param options The options to use to write the image.
     * 
     * @return Flag indicating that this was a successful write.
     */
    private boolean writeThumbnailJIMI(final ModelImage image, final FileWriteOptions options) {
        final int imageSize = image.getExtents()[0] * image.getExtents()[1];
        final int[] paintBuffer = new int[imageSize];
        final ColorRGBA colorMappedA = new ColorRGBA();
        final float[] imageBufferA = new float[image.getExtents()[0] * image.getExtents()[1] * 4];
        final int length = imageBufferA.length;
        final ColorRGB[] m_akOffset = {new ColorRGB(0.0f, 0.0f, 0.0f), new ColorRGB(0.0f, 0.0f, 0.0f)};
        float fMaxColor = 255;
        final float[] m_afNormColor = {1, 1};

        if (image.getMinR() < 0.0) {
            fMaxColor = (float) (image.getMaxR() - image.getMinR());
            m_akOffset[0].R = (float) ( -image.getMinR());
        } else {
            fMaxColor = (float) image.getMaxR();
        }

        if (image.getMinG() < 0.0) {
            fMaxColor = Math.max((float) (image.getMaxG() - image.getMinG()), fMaxColor);
            m_akOffset[0].G = (float) ( -image.getMinG());
        } else {
            fMaxColor = Math.max((float) image.getMaxG(), fMaxColor);
        }

        if (image.getMinB() < 0.0) {
            fMaxColor = Math.max((float) (image.getMaxB() - image.getMinB()), fMaxColor);
            m_akOffset[0].B = (float) ( -image.getMinB());
        } else {
            fMaxColor = Math.max((float) image.getMaxB(), fMaxColor);
        }
        m_afNormColor[0] = 255 / fMaxColor;

        try {
            image.exportData(0, length, imageBufferA);
        } catch (final Exception e) {

        }
        for (int j = 0; j < image.getExtents()[1]; j++) {

            for (int i = 0; i < image.getExtents()[0]; i++) {
                final int ind4 = (j * image.getExtents()[0]) + i;
                final int index = 4 * ind4;
                int pixValue;

                colorMappedA.R = 0;
                colorMappedA.G = 0;
                colorMappedA.B = 0;
                colorMappedA.A = imageBufferA[index];

                colorMappedA.R = (imageBufferA[index + 1] + m_akOffset[0].R) * m_afNormColor[0];
                colorMappedA.G = (imageBufferA[index + 2] + m_akOffset[0].G) * m_afNormColor[0];
                colorMappedA.B = (imageBufferA[index + 3] + m_akOffset[0].B) * m_afNormColor[0];

                pixValue = 0xff000000 | ((int) (colorMappedA.R) << 16) | ((int) (colorMappedA.G) << 8)
                        | ((int) (colorMappedA.B));

                paintBuffer[ind4] = pixValue;
            }
        }

        final MemoryImageSource memImageA = new MemoryImageSource(image.getExtents()[0], image.getExtents()[1],
                paintBuffer, 0, image.getExtents()[0]);

        final int extIndex = options.getFileName().indexOf(".");
        final String prefix = options.getFileName().substring(0, extIndex); // Used for setting file name
        final String fileSuffix = options.getFileName().substring(extIndex);
        String name;

        final Image img = createImage(memImageA);

        name = options.getFileDirectory() + prefix + fileSuffix;

        try {
            Jimi.putImage(img, name);
        } catch (final JimiException jimiException) {
            Preferences.debug("JIMI write error: " + jimiException + "\n", Preferences.DEBUG_FILEIO);

            jimiException.printStackTrace();

            return false;
        }

        return true;
    }

    public void mouseClicked(final MouseEvent e) {
        final Component c = e.getComponent();
        if (c instanceof JTable) {
            if (sourceTable.getSelectedRow() == -1) {
                completeDataElementsButton.setEnabled(false);
                removeSourceButton.setEnabled(false);
                return;
            } else {
                if ( !isFinished) {
                    completeDataElementsButton.setEnabled(true);
                    removeSourceButton.setEnabled(true);
                }
            }

            previewPanel.removeAll();
            previewPanel.repaint();

            if (previewImages.get(sourceTable.getSelectedRow()) != null) {
                previewPanel.add(previewImages.get(sourceTable.getSelectedRow()));
                previewImages.get(sourceTable.getSelectedRow()).setSliceBrightness(brightness, contrast);
                previewPanel.validate();
                previewPanel.repaint();
            }

            if (e.getClickCount() == 2) {
                if ( !isFinished) {
                    final String dsName = (String) sourceTableModel.getValueAt(sourceTable.getSelectedRow(), 0);
                    new InfoDialog(this, dsName, true, true, null);
                }
            }
        }
    }

    public void mouseEntered(final MouseEvent e) {}

    public void mouseExited(final MouseEvent e) {}

    public void mousePressed(final MouseEvent e) {}

    public void mouseReleased(final MouseEvent e) {}

    public boolean contains(final File f) {
        boolean contains = false;

        for (int i = 0; i < sourceTableModel.getRowCount(); i++) {
            final File f1 = (File) sourceTableModel.getValueAt(i, 0);
            if (f1.getAbsolutePath().equalsIgnoreCase(f.getAbsolutePath())) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    public void enableDisableFinishButton() {
        // boolean allCompleted = true;

        // for (int i = 0; i < sourceTableModel.getRowCount(); i++) {
        // final String completed = (String) sourceTableModel.getValueAt(i, 1);
        // if (completed.equalsIgnoreCase("No")) {
        // allCompleted = false;
        // break;
        // }
        // }

        if (sourceTableModel.getRowCount() == 0) {
            finishButton.setEnabled(false);
        } else {
            // changed to always enabled if some data added
            finishButton.setEnabled(true);
            // if (allCompleted) {
            // finishButton.setEnabled(true);
            // } else {
            // finishButton.setEnabled(false);
            // }
        }

    }

    private void buildBrightnessContrastPanel() {
        brightnessSlider = new JSlider(SwingConstants.HORIZONTAL, -255, 255, origBrightness);

        brightnessSlider.setMajorTickSpacing(102);
        brightnessSlider.setPaintTicks(true);
        brightnessSlider.setEnabled(true);
        brightnessSlider.addChangeListener(this);

        final JLabel maximum = new JLabel(String.valueOf(255));

        maximum.setForeground(Color.black);
        maximum.setFont(serif12);

        current = new JLabel(String.valueOf(origBrightness));
        current.setForeground(Color.black);
        current.setFont(serif12B);

        final JLabel minimum = new JLabel(String.valueOf( -255));

        minimum.setForeground(Color.black);
        minimum.setFont(serif12);

        final JPanel sliderPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        sliderPanel.add(brightnessSlider, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        sliderPanel.add(minimum, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = .5;

        sliderPanel.add(current, gbc);

        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;

        sliderPanel.add(maximum, gbc);
        sliderPanel.setBorder(buildTitledBorder("Level"));

        contrastSlider = new JSlider(SwingConstants.HORIZONTAL, -200, 200, (int) (Math.round(86.85889638 * Math
                .log(origContrast))));

        contrastSlider.setMajorTickSpacing(80);
        contrastSlider.setPaintTicks(true);
        contrastSlider.setEnabled(true);
        contrastSlider.addChangeListener(this);

        final JLabel maximum2 = new JLabel(String.valueOf(10));

        maximum2.setForeground(Color.black);
        maximum2.setFont(serif12);

        nfc = NumberFormat.getNumberInstance();
        nfc.setMaximumFractionDigits(3);

        current2 = new JLabel(String.valueOf(nfc.format(origContrast)));
        current2.setForeground(Color.black);
        current2.setFont(serif12B);

        final JLabel minimum2 = new JLabel(String.valueOf(0.100));

        minimum2.setForeground(Color.black);
        minimum2.setFont(serif12);

        final JPanel sliderPanel2 = new JPanel(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        sliderPanel2.add(contrastSlider, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        sliderPanel2.add(minimum2, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = .5;

        sliderPanel2.add(current2, gbc);

        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;

        sliderPanel2.add(maximum2, gbc);
        sliderPanel2.setBorder(buildTitledBorder("Window"));

        final JPanel centerPanel = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc2 = new GridBagConstraints();

        gbc2.gridx = 0;
        gbc2.gridy = 0;
        gbc2.fill = GridBagConstraints.BOTH;
        gbc2.weightx = 1;
        gbc2.gridheight = 2;
        centerPanel.add(sliderPanel2, gbc2);

        gbc2.gridy = 2;
        centerPanel.add(sliderPanel, gbc2);

        gbc2.gridheight = 1;
        gbc2.gridy = 4;

        brightnessContrastPanel = new JPanel(new BorderLayout());
        brightnessContrastPanel.add(centerPanel);
        brightnessContrastPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    }
    
    /**
     * Tries to read server configuration from brics config file on local disk. 
     */
    private void readConfig() {
    	InputStream in = getClass().getResourceAsStream(configFileName);
    	if (in != null) {
    		Properties prop = new Properties();
    		try {
    			prop.load(in);
    		} catch (IOException e) {
    			Preferences.debug("Unable to load BRICS preferences file: " + configFileName + "\n", Preferences.DEBUG_MINOR);
    			e.printStackTrace();
    		}
    		// use pre-set, hardcoded values as defaults if properties are not found
    		authServerURL = prop.getProperty(authServerURLProp, authServerURL);
    		System.out.println("authServer:\t" + authServerURL);
    		ddServerURL = prop.getProperty(ddServerURLProp, ddServerURL);
    		System.out.println("ddServer:\t" + ddServerURL);
    	}
    }
    
    /**
     * Checks if the given string starts with an allowed GUID prefix.
     * @param str A string to check.
     * @return True if the string starts with one of the BIRCS prefixes (case sensitive).
     */
    private static final boolean isGuid(String str) {
    	for (String prefix : allowedGuidPrefixes) {
    		if (str.startsWith(prefix)) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    /**
     * Checks to see if the given string contains an allowed GUID prefix.
     * @param str A string to check.
     * @return True if one of the allowed BRICS prefixes is found anywhere in the string (case sensitive).
     */
    private static final boolean containsGuid(String str) {
    	for (String prefix : allowedGuidPrefixes) {
    		if (str.contains(prefix)) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    /**
     * Extracts the form structure name from a string in the format 'structname_BRICSGUID'.
     * @param str A string in the format 'structname_BRICSGUID'.
     * @return The form structure name from the given string or null if it could not be found (if no GUID prefix was found to initiate the parsing).
     */
    private static final String getStructFromString(String str) {
    	for (String prefix : allowedGuidPrefixes) {
    		if (str.contains(prefix)) {
    			return str.substring(0, str.indexOf(prefix) - 1);
    		}
    	}
    	
    	return null;
    }
    
    /**
     * Extracts the GUID from a string in the format 'structname_BRICSGUID'.
     * @param str A string in the format 'structname_BRICSGUID'.
     * @return The GUID from the given string or null if it could not be found (if no GUID prefix was found to initiate the parsing).
     */
    private static final String getGuidFromString(String str) {
    	for (String prefix : allowedGuidPrefixes) {
    		if (str.contains(prefix)) {
    			return str.substring(str.indexOf(prefix), str.length());
    		}
    	}
    	
    	return null;
    }
    
    /**
     * Converts from DICOM/US date format (MM/DD/YYYY) to ISO 8601 format (YYYY-MM-DDThh:mm:ss).
     * @param date A date string in the format MM/DD/YYYY.
     * @return An ISO 8601 formatted version of the given date.
     */
    private static final String convertDateTimeToISOFormat(String date) {
    	String pattern = "^(\\d{2})[/-]*(\\d{2})[/-]*(\\d{4})$";
    	Pattern p = Pattern.compile(pattern);
    	Matcher m = p.matcher(date);
    	if (m.find()) {
    		String month = m.group(1);
    		String day = m.group(2);
    		String year = m.group(3);
    		return year + "-" + month + "-" + day;
    	}
    	
    	return "";
    }
    
    /**
     * Multi-line tooltip creation helper method.
     *
     * Posted by user Paul Taylor at http://stackoverflow.com/questions/868651/multi-line-tooltips-in-java.
     * Changed the name/method names.  Also altered main method to no add html tags.
     */
    public static class WrapText {
        private static int DIALOG_TOOLTIP_MAX_SIZE = 80;
        private static final int SPACE_BUFFER = 25;

        /**
         * Returns the given string, with &lt;br/&gt; tags inserted to wrap the text.  Does NOT add html block tags to the beginning and end of the text.
         * @param tip The string to wrap.
         * @return The wrapped text.
         */
        public static String wrap(String tip) {
            return wrap(tip,DIALOG_TOOLTIP_MAX_SIZE);
        }
        
        /**
         * Returns the given string, with &lt;br/&gt; tags inserted to wrap the text.  Does NOT add html block tags to the beginning and end of the text.
         * @param tip The string to wrap.
         * @param length The length to wrap to.
         * @return The wrapped text.
         */
        public static String wrap(String tip,int length) {
            if(tip.length()<=length + SPACE_BUFFER ) {
                return tip;
            }

            List<String>  parts = new ArrayList<String>();

            int maxLength = 0;
            String overLong = tip.substring(0, length + SPACE_BUFFER);
            int lastSpace = overLong.lastIndexOf(' ');
            if(lastSpace >= length) {
                parts.add(tip.substring(0,lastSpace));
                maxLength = lastSpace;
            } else {
                parts.add(tip.substring(0,length));
                maxLength = length;
            }

            while(maxLength < tip.length()) {
                if(maxLength + length < tip.length()) {
                    parts.add(tip.substring(maxLength, maxLength + length));
                    maxLength+=maxLength+length;
                } else {
                    parts.add(tip.substring(maxLength));
                    break;
                }
            }

            //StringBuilder  sb = new StringBuilder("<html>");
            StringBuilder  sb = new StringBuilder("");
            for(int i=0;i<parts.size() - 1;i++) {
                sb.append(parts.get(i)+"<br/>");
            }
            sb.append(parts.get(parts.size() - 1));
            //sb.append(("</html>"));
            return sb.toString();
        }
    }

    /**
     * 
     * Inner class Right Renderer
     * 
     * @author pandyan
     * 
     */
    private class MyRightCellRenderer extends DefaultTableCellRenderer {

        /**
		 * 
		 */
		private static final long serialVersionUID = -7905716122046419275L;

		public Component getTableCellRendererComponent(final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            final Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);

            if (column == 1 && ((String) value).equalsIgnoreCase("No")) {
                setForeground(Color.red);
            } else {
                setForeground(Color.black);
            }

            return comp;
        }

    }

    private class ChooseDataStructDialog extends JDialog implements ActionListener {
        /**
		 * 
		 */
		private static final long serialVersionUID = 4199199899439094828L;

		private final PlugInDialogFITBIR owner;

        // private final File file;
        private ViewTableModel structsModel;

        private JTable structsTable;

        /*
         * private QName qName = null;
         * 
         * private QName qStatus = null;
         * 
         * private QName qDataType = null;
         * 
         * private QName qDescription = null;
         * 
         * private QName qParentDataStructure = null;
         * 
         * private QName qVersion = null;
         */

        private final ArrayList<String> descAL = new ArrayList<String>();

        private final ArrayList<String> shortNameAL = new ArrayList<String>();

        private final ArrayList<String> versionAL = new ArrayList<String>();

        private final ArrayList<String> statusAL = new ArrayList<String>();

        private JScrollPane structsScrollPane;

        public ChooseDataStructDialog(final PlugInDialogFITBIR owner) {
            super(owner, true);

            this.owner = owner;

            /*
             * qName = new QName(publishedDataStructs.getNamespace().getNamespaceURI(), "short_name");
             * System.out.println(qName.toString()); qStatus = new
             * QName(publishedDataStructs.getNamespace().getNamespaceURI(), "status"); qDataType = new
             * QName(publishedDataStructs.getNamespace().getNamespaceURI(), "type"); qDescription = new
             * QName(publishedDataStructs.getNamespace().getNamespaceURI(), "desc"); qParentDataStructure = new
             * QName(publishedDataStructs.getNamespace().getNamespaceURI(), "parent"); qVersion = new
             * QName(publishedDataStructs.getNamespace().getNamespaceURI(), "version");
             */

            init();

        }

        /**
         * init
         */
        private void init() {
            setTitle("Choose Form Structure");
            final int numColumns = 4;
            final String[] columnNames = {"Name", "Description", "Version", "Status"};
            structsModel = new ViewTableModel();
            structsTable = new JTable(structsModel) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 3053232611901005303L;

				public String getToolTipText(final MouseEvent e) {
                    String tip = "";

                    final java.awt.Point p = e.getPoint();
                    final int rowIndex = rowAtPoint(p);
                    final int colIndex = columnAtPoint(p);

                    tip = (String) structsModel.getValueAt(rowIndex, colIndex);

                    return tip;
                }
            };

            structsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            for (final String element : columnNames) {
                structsModel.addColumn(element);
            }

            structsTable.getColumn("Name").setMinWidth(150);
            structsTable.getColumn("Description").setMinWidth(300);

            // new way of doing web service
            for (BasicDataStructure ds : dataStructureList) {
                String desc = ds.getDescription();
                String shortname = ds.getShortName();
                String version = ds.getVersion().toString();
                String status = ds.getStatus().toString();
                String type = ds.getFileType().getType();

                if (type.equalsIgnoreCase("imaging")) {
                    descAL.add(desc);
                    shortNameAL.add(shortname);
                    versionAL.add(version);
                    statusAL.add(status);
                }
            }
            
            // make sure we found a structure for imaging
            if (shortNameAL.size() == 0) {
                MipavUtil.displayWarning("No Imaging structures were found in the data dictionary.");
                return;
            }

            // old way of using web service
            /*
             * final Iterator<OMElement> iter2 = publishedDataStructs.getChildElements(); while (iter2.hasNext()) {
             * final OMElement e = iter2.next();
             * 
             * if (e.getLocalName().equalsIgnoreCase("data_structure")) { final String shortname =
             * e.getAttributeValue(qName); final String version = e.getAttributeValue(qVersion); // This can also be
             * obtained using the last // two // characters of short name final String status =
             * e.getAttributeValue(qStatus); final String dataType = e.getAttributeValue(qDataType); final String desc =
             * e.getAttributeValue(qDescription); final String parent = e.getAttributeValue(qParentDataStructure); if
             * (dataType.equalsIgnoreCase("Imaging")) {
             * 
             * descAL.add(desc); shortNameAL.add(shortname); versionAL.add(version); statusAL.add(status); } } }
             */

            TreeSet<String> sortedNamesSet = new TreeSet<String>(new AlphabeticalComparator());
            for (int i = 0; i < shortNameAL.size(); i++) {
                sortedNamesSet.add(shortNameAL.get(i));
            }

            // we only want to list the most recent versions
            // so remove the less recent versions from this list
            String[] sortedNamesArray = (String[]) (sortedNamesSet.toArray(new String[0]));
            for (int i = sortedNamesArray.length - 1; i > 0; i--) {
                String name = sortedNamesArray[i];
                if ( !name.equals("")) {
                    // BRICS short name doesn't appear to have version number included
                    //String nameWithoutVersion = name.substring(0, name.length() - 2);
                    for (int k = i - 1; k >= 0; k--) {
                        String checkName = sortedNamesArray[k];
                        // BRICS short name doesn't appear to have version number included
                        //String checkNameWithoutVersion = checkName.substring(0, checkName.length() - 2);
                        if (name.equals(checkName)) {
                            sortedNamesArray[k] = "";
                        }
                    }
                }
            }
            sortedNamesSet = new TreeSet<String>(new AlphabeticalComparator());
            for (int i = 0; i < sortedNamesArray.length; i++) {
                if ( !sortedNamesArray[i].equals("")) {
                    sortedNamesSet.add(sortedNamesArray[i]);
                }
            }
            // now we only have the most recent versions

            final Object[] rowData = new Object[numColumns];

            Iterator<String> iter = sortedNamesSet.iterator();

            while (iter.hasNext()) {
                String name = (String) iter.next();

                for (int i = 0; i < shortNameAL.size(); i++) {
                    if (name.equals(shortNameAL.get(i))) {
                        rowData[0] = shortNameAL.get(i);
                        rowData[1] = descAL.get(i);
                        rowData[2] = versionAL.get(i);
                        rowData[3] = statusAL.get(i);
                        structsModel.addRow(rowData);

                        break;
                    }
                }
            }

            structsTable.setRowSelectionInterval(0, 0);

            // structsTable.setAutoCreateRowSorter(true);
            // structsTable.getRowSorter().toggleSortOrder(0);

            structsScrollPane = new JScrollPane(structsTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            structsScrollPane.setPreferredSize(new Dimension(600, 300));

            final JPanel OKPanel = new JPanel();
            buildOKButton();
            buildCancelButton();
            OKButton.setText("Add");
            OKButton.setActionCommand("ok4");
            OKButton.addActionListener(this);
            OKPanel.add(OKButton);

            getContentPane().add(structsScrollPane, BorderLayout.CENTER);

            getContentPane().add(OKPanel, BorderLayout.SOUTH);

            pack();

            MipavUtil.centerInWindow(owner, this);
            this.setMinimumSize(this.getSize());

            setVisible(true);

        }

        /**
         * action performed
         */
        public void actionPerformed(final ActionEvent e) {
            final String command = e.getActionCommand();
            if (command.equalsIgnoreCase("ok4")) {
                final int selectedRow = structsTable.getSelectedRow();
                if (selectedRow != -1) {
                    this.dispose();
                    final String dsName = (String) structsModel.getValueAt(selectedRow, 0);
                    new InfoDialog(owner, dsName, false, true, null);
                }
            }

        }

        /**
         * This inner class is used to sort the list by instance number
         */
        private class AlphabeticalComparator implements Comparator<String> {
            public int compare(String a, String b) {
                return (a.toLowerCase().compareTo(b.toLowerCase()));

            }
        }

    }

    /**
     * launches the dialog to add info
     * 
     * @author pandyan
     * 
     */
    private class InfoDialog extends JDialog implements ActionListener, WindowListener, ItemListener, FocusListener {
		private static final long serialVersionUID = 859201819000159789L;

		private final PlugInDialogFITBIR owner;

        private final JTabbedPane tabbedPane = new JTabbedPane();

        private JPanel mainPanel;

        private GridBagConstraints gbc;

        private final JScrollPane tabScrollPane;

        private String guid = "";

        private boolean launchedFromInProcessState = false;

        private JLabel requiredLabel, conditionalLabel;

        private String dataStructureName;

        private TreeMap<JLabel, JComponent> labelsAndComps;
        
        private Set<RepeatableGroup> groups;

        private final ArrayList<File> allOtherFiles = new ArrayList<File>();

        private boolean addedPreviewImage = false;

        private BasicDataStructure dataStructure;

        private boolean setInitialVisible;

        private String[] csvParams;
        
        private final String[] unchangableElements = new String[] {
        		IMG_HASH_CODE_ELEMENT_NAME, "ImgDimensNum", "ImgDim1Extent", "ImgDim2Extent", "ImgDim3Extent", "ImgDim4Extent", "ImgDim5Extent", IMG_FILE_ELEMENT_NAME, IMG_PREVIEW_ELEMENT_NAME
        };

		private String currFile;

		private boolean validFile;

        /**
         * constructor
         * 
         * @param owner
         * @param file
         * @param launchedFromCompletedState
         */
        public InfoDialog(final PlugInDialogFITBIR owner, final String name, final boolean launchedFromInProcessState,
                boolean setInitialVisible, String[] csvParams) {

            super(owner, true);

            this.owner = owner;
            this.launchedFromInProcessState = launchedFromInProcessState;
            this.setInitialVisible = setInitialVisible;
            this.csvParams = csvParams;

            if (launchedFromInProcessState) {
                // System.out.println("*** launched from in process: " + name);
                if (containsGuid(name)) {
                    this.dataStructureName = getStructFromString(name);
                } else {
                    this.dataStructureName = name.substring(0, name.lastIndexOf("_"));
                }

            } else {
                previewImages.add(null);
                imageFiles.add(null);
                multifiles.add(new Boolean(false));
                infoList.add(null);
                allOtherFilesAL.add(null);
                this.dataStructureName = name;
            }

            final JPanel panel = new JPanel(new GridBagLayout());
            tabScrollPane = new JScrollPane(panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            tabScrollPane.setPreferredSize(new Dimension(600, 500));
            tabbedPane.addTab(dataStructureName, tabScrollPane);

            for (BasicDataStructure ds : dataStructureList) {
                if (ds.getShortName().equals(dataStructureName)) {
                    dataStructure = ds;
                }
            }

            init();
        }

        /**
         * init
         */
        private void init() {

            setTitle("Edit Data Elements");
            addWindowListener(this);
            mainPanel = new JPanel(new GridBagLayout());

            gbc = new GridBagConstraints();

            try {
                setIconImage(MipavUtil.getIconImage(Preferences.getIconName()));
            } catch (final Exception e) {
                // setIconImage() is not part of the Java 1.5 API - catch any runtime error on those systems
            }

            try {
                String n = dataStructure.getShortName();
                String s = dataStructure.getShortName();
                String d = dataStructure.getDescription();
                String v = dataStructure.getVersion().toString();
                String t = dataStructure.getFileType().getType();

                final DataStruct dataStruct = new DataStruct(n, s, d, v, t);
                boolean found = false;
                for (int i = 0; i < dataStructures.size(); i++) {
                    final String sn = dataStructures.get(i).getShortname();
                    if (s.equalsIgnoreCase(sn)) {
                        found = true;
                    }
                }
                if ( !found) {
                    dataStructures.add(dataStruct);
                }

                if (launchedFromInProcessState) {
                    final int selectedRow = sourceTable.getSelectedRow();
                    final LinkedHashMap<String, String> infoMap = infoList.get(selectedRow);

                    labelsAndComps = labelsAndCompsList.get(selectedRow);

                    // parse(dataStructElement, dataStruct, dataStructureName, labelsAndComps);

                    parse_new(dataStructure, dataStruct, dataStructureName, labelsAndComps);

                    parseForInitLabelsAndComponents(dataStruct, labelsAndComps);

                    populateFieldsFromInProcessState(labelsAndComps, infoMap);

                } else {

                    labelsAndComps = new TreeMap<JLabel, JComponent>(new JLabelComparator());

                    // parse(dataStructElement, dataStruct, dataStructureName, labelsAndComps);

                    parse_new(dataStructure, dataStruct, dataStructureName, labelsAndComps);

                    parseForInitLabelsAndComponents(dataStruct, labelsAndComps);

                    labelsAndCompsList.add(labelsAndComps);

                    if ( !setInitialVisible) {
                        // this means it was launched via the csv file
                        populateFieldsFromCSV(labelsAndComps, csvParams);

                    }

                }
            } catch (final Exception e) {
                e.printStackTrace();
            }

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(10, 5, 10, 25);
            gbc.gridwidth = 1;

            final JPanel OKPanel = new JPanel();
            buildOKButton();
            buildCancelButton();
            OKButton.setText("Save");
            OKButton.setActionCommand("ok3");
            OKButton.addActionListener(this);
            cancelButton.setActionCommand("cancel3");
            cancelButton.addActionListener(this);
            OKPanel.add(OKButton);
            OKPanel.add(cancelButton);

            requiredLabel = new JLabel("<html>Mouse over data element name for a description.<br/>Mouse over the data element fields for more information on filling them in.<br/>* Required data elements are in <font color=\"red\">red</font></html>");
            // conditionalLabel = new JLabel("<html>* Conditional data elements are in <font
            // color=\"blue\">blue</font></html>");

            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 0;
            gbc.gridx = 0;
            gbc.gridy = 0;
            mainPanel.add(requiredLabel, gbc);
            // gbc.gridy = 1;
            // mainPanel.add(conditionalLabel, gbc);

            gbc.gridy = 2;
            gbc.weightx = 1;
            gbc.weighty = 1;
            mainPanel.add(tabbedPane, gbc);
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.gridy = 3;
            mainPanel.add(OKPanel, gbc);

            getContentPane().add(mainPanel);

            pack();
            MipavUtil.centerInWindow(owner, this);
            this.setMinimumSize(this.getSize());
            if (setInitialVisible) {
                setVisible(true);
            }
        }

        private void populateFieldsFromCSV(TreeMap<JLabel, JComponent> labelsAndComps, String[] csvparams) {
            // TODO: hardcoded structure handling
        	if (dataStructureName.startsWith("Imag")) {
        		// TODO: handle the record column when we add support for repeating groups in a form record
        		
                // first check to see if image_file was supplied in the csv
                int imageFileIndex = -1;
                for (int i = 0; i < csvFieldNames.length; i++) {
                    if (csvFieldNames[i].trim().equalsIgnoreCase(IMG_FILE_ELEMENT_NAME)) {
                        imageFileIndex = i;
                        break;
                    }
                }

                if (imageFileIndex != -1 && !csvParams[imageFileIndex].equals("")) {

                    // if image_file is in zip format....first unzip it temporarily

                    String imageFile = csvparams[imageFileIndex];
                    ModelImage srcImage = null;
                    
                    srcImage = readImgFromCSV(imageFile);
                    
                    if (srcImage != null) {

                        // final String labelName = command.substring(command.indexOf("_") + 1, command.length());

                        final Set<JLabel> keySet = labelsAndComps.keySet();
                        final Iterator<JLabel> iter = keySet.iterator();
                        while (iter.hasNext()) {
                            final JLabel l = (JLabel) iter.next();
                            if (l.getName().equalsIgnoreCase(IMG_FILE_ELEMENT_NAME)) {
                                final JTextField tf = (JTextField) labelsAndComps.get(l);
                                tf.setText(imageFile);
                                tf.setEnabled(false);
                            }
                        }

                        // need to determine if there are any entries in cvs that has things like image extents or
                        // resolutions
                        // that are different than the ones determined by header....if there are, then prompt a warning

                        int response = determineImageHeaderDescrepencies(srcImage);

                        if (response == 1) {
                            populateFields(labelsAndComps, srcImage);

                            String key;
                            String value;
                            for (int i = 0; i < csvFieldNames.length; i++) {

                                key = csvFieldNames[i];
                                value = csvParams[i];
                                if ( !key.equals(IMG_FILE_ELEMENT_NAME)) {
                                    final Set<JLabel> keySet2 = labelsAndComps.keySet();
                                    final Iterator<JLabel> iter2 = keySet2.iterator();
                                    // System.out.println();
                                    while (iter2.hasNext()) {
                                        final JLabel l = (JLabel) iter2.next();
                                        final Component comp = labelsAndComps.get(l);
                                        final String name = comp.getName();
                                        // System.out.println("--- " + name);
                                        if (name.equalsIgnoreCase(key)) {
                                            if (comp instanceof JTextField) {
                                                final JTextField t = (JTextField) comp;
                                                t.setText(value);

                                            } else if (comp instanceof JComboBox) {
												final JComboBox c = (JComboBox) comp;

                                                for (int k = 0; k < c.getItemCount(); k++) {
                                                    final String item = (String) c.getItemAt(k);
                                                    if (value.equalsIgnoreCase(item)) {
                                                        c.setSelectedIndex(k);
                                                    }
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        } else {
                            String key;
                            String value;
                            for (int i = 0; i < csvFieldNames.length; i++) {
                                key = csvFieldNames[i];
                                value = csvParams[i];
                                if ( !key.equals(IMG_FILE_ELEMENT_NAME)) {
                                    final Set<JLabel> keySet2 = labelsAndComps.keySet();
                                    final Iterator<JLabel> iter2 = keySet2.iterator();
                                    // System.out.println();
                                    while (iter2.hasNext()) {
                                        final JLabel l = (JLabel) iter2.next();
                                        final Component comp = labelsAndComps.get(l);
                                        final String name = comp.getName();
                                        // System.out.println("--- " + name);
                                        if (name.equalsIgnoreCase(key)) {
                                            if (comp instanceof JTextField) {
                                                final JTextField t = (JTextField) comp;
                                                t.setText(value);

                                            } else if (comp instanceof JComboBox) {
                                                final JComboBox c = (JComboBox) comp;

                                                for (int k = 0; k < c.getItemCount(); k++) {
                                                    final String item = (String) c.getItemAt(k);
                                                    if (value.equalsIgnoreCase(item)) {
                                                        c.setSelectedIndex(k);
                                                    }
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            populateFields(labelsAndComps, srcImage);

                        }

                        srcImage.disposeLocal();
                        srcImage = null;
                    }
                }
            } else {
                // this means its not an imaging data structure

                String key;
                String value;
                for (int i = 0; i < csvFieldNames.length; i++) {

                    key = csvFieldNames[i];
                    value = csvParams[i];

                    final Set<JLabel> keySet2 = labelsAndComps.keySet();
                    final Iterator<JLabel> iter2 = keySet2.iterator();
                    // System.out.println();
                    while (iter2.hasNext()) {
                        final JLabel l = (JLabel) iter2.next();
                        final Component comp = labelsAndComps.get(l);
                        final String name = comp.getName();
                        // System.out.println("--- " + name);
                        if (name.equalsIgnoreCase(key)) {
                            if (comp instanceof JTextField) {
                                final JTextField t = (JTextField) comp;
                                t.setText(value);

                            } else if (comp instanceof JComboBox) {
								final JComboBox c = (JComboBox) comp;

                                for (int k = 0; k < c.getItemCount(); k++) {
                                    final String item = (String) c.getItemAt(k);
                                    if (value.equalsIgnoreCase(item)) {
                                        c.setSelectedIndex(k);
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }

            // need to validate and then close window
            ArrayList<String> errs;
            final StringBuffer errors = new StringBuffer();
            errs = validateFields();

            boolean isComplete = true;
            if (errs.size() != 0) {
                for (int i = 0; i < errs.size(); i++) {
                    errors.append(" - " + errs.get(i) + "\n");
                }
                // MipavUtil.displayWarning(errors.toString());
                isComplete = false;
            }
            
            if (validFile)
            	complete(labelsAndComps, dataStructureName, isComplete);
            else {
            	if ( !launchedFromInProcessState) {
                    previewImages.remove(previewImages.size() - 1);
                    imageFiles.remove(imageFiles.size() - 1);
                    multifiles.remove(multifiles.size() - 1);
                    infoList.remove(infoList.size() - 1);
                    allOtherFilesAL.remove(allOtherFilesAL.size() - 1);
                    if (addedPreviewImage) {
                        previewPanel.removeAll();
                        previewPanel.repaint();
                    }
                }
            }

            enableDisableFinishButton();
            dispose();
            
        }
        
        private ModelImage readImgFromCSV(String imageFile) {
        	String filePath;
        	boolean isMultifile;
        	FileIO fileIO = new FileIO();
            fileIO.setQuiet(true);
            ModelImage srcImage = null;
            validFile = true;
            
            try {
        	
        	if (imageFile.endsWith(".zip")) {

                String destName = imageFile.replace("/", File.separator);
                destName = destName.replace("\\", File.separator);
                destName = destName.substring(destName.lastIndexOf(File.separator) + 1, destName
                        .lastIndexOf("."));
                // String destDirName =
                String tempDir = csvFile.getParentFile().getAbsolutePath() + File.separator + destName
                        + "_temp_" + System.currentTimeMillis();
                tempDirs.add(tempDir);
                File imageZipFile = new File(csvFile.getParentFile().getAbsolutePath() + File.separator
                        + imageFile);
                String fileName = "";
//                try {
                    FileInputStream fis = new FileInputStream(imageZipFile);
                    ZipInputStream zin = new ZipInputStream(new BufferedInputStream(fis));
                    FileOutputStream fout;
                    ZipEntry entry;
                    BufferedOutputStream dest = null;
                    int BUFFER = 2048;
                    int count;
                    byte[] data = new byte[BUFFER];
                    File f;
                    // while we are at it, find the first file that does not have a .raw extension, so we can
                    // open it

                    while ( (entry = zin.getNextEntry()) != null) {
                        f = new File(tempDir);
                        if ( !f.exists()) {
                            f.mkdir();
                        }
                        if (fileName.equals("")) {
                            if ( !entry.getName().endsWith(".raw")) {
                                fileName = entry.getName();
                            }
                        }
                        fout = new FileOutputStream(tempDir + File.separator + entry.getName());
                        dest = new BufferedOutputStream(fout, BUFFER);
                        while ( (count = zin.read(data, 0, BUFFER)) != -1) {
                            dest.write(data, 0, count);
                        }
                    }
                    dest.flush();
                    dest.close();
                    zin.close();
//                } catch (FileNotFoundException f) {
//                	MipavUtil.displayError("The system cannot find the file specified");
//                } catch (final Exception e) {
//                    e.printStackTrace();
//                }

                // now that everything has been unzipped, open the image from the tempDir
                filePath = tempDir + File.separator + fileName;
                isMultifile = true;
            } else {
            	// try to only open as a single file, since it wasn't zipped
            	filePath = csvFile.getParentFile().getAbsolutePath() + File.separator + imageFile;
            	isMultifile = false;
            }

            File file = new File(filePath);
            srcImage = fileIO.readImage(file.getName(), file.getParent() + File.separator, isMultifile, null);
            
            final int[] extents = new int[] {srcImage.getExtents()[0], srcImage.getExtents()[1]};

            previewImg = new ViewJComponentPreviewImage(srcImage, extents, owner);
            int slice = 0;
            if ( !srcImage.is2DImage()) {
                slice = (srcImage.getExtents()[2] / 2);
            }
            previewImg.createImg(slice);

            previewPanel.removeAll();
            previewPanel.repaint();

            previewPanel.add(previewImg);

            addedPreviewImage = true;

            if (launchedFromInProcessState) {
                final int selectedRow = sourceTable.getSelectedRow();
                previewImages.set(selectedRow, previewImg);
                previewImages.get(selectedRow).setSliceBrightness(brightness, contrast);
                imageFiles.set(selectedRow, file);
                multifiles.set(selectedRow, new Boolean(isMultifile));
            } else {
                final int size = previewImages.size();
                previewImages.set(size - 1, previewImg);
                previewImages.get(size - 1).setSliceBrightness(brightness, contrast);
                imageFiles.set(size - 1, file);
                multifiles.set(size - 1, new Boolean(isMultifile));
            }

            previewPanel.validate();
            previewPanel.repaint();
            
            } catch (FileNotFoundException e) {
            	MipavUtil.displayError("The system cannot find the file specified");
            	validFile = false;
            } catch (NullPointerException e) {
            	MipavUtil.displayError("The system cannot find the file specified");
            	validFile = false;
            } catch (final Exception e) {
            	e.printStackTrace();
            }
            
            
            return srcImage;
        }

        private void parseForInitLabelsAndComponents(final DataStruct ds2,
                final TreeMap<JLabel, JComponent> labelsAndComps) {
            JPanel mainPanel = null ;
            JScrollPane sp;
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                final String title = tabbedPane.getTitleAt(i);
                if (title.toLowerCase().startsWith(ds2.getShortname().toLowerCase())) {
                	sp = (JScrollPane) (tabbedPane.getComponentAt(i));
                	mainPanel = (JPanel) (sp.getViewport().getComponent(0));
                }
            }

            TreeMap<String, JPanel> groupPanels = new TreeMap<String, JPanel>();
            for (RepeatableGroup g : groups) {
            	JPanel p = new JPanel(new GridBagLayout());
            	p.setBorder(buildTitledBorder(g.getName()));
            	groupPanels.put(g.getName(), p);
            }
            
            final Set<JLabel> keySet = labelsAndComps.keySet();
            final Iterator<JLabel> iter = keySet.iterator();
            while (iter.hasNext()) {
                final JLabel l = (JLabel) iter.next();
                final JComponent t = labelsAndComps.get(l);
                final String labelName = l.getName();

                for (int k = 0; k < ds2.size(); k++) {
                    final Object o1 = ds2.get(k);
                    if (o1 instanceof DataElement) {
                    	DataElement de = (DataElement)o1;
                        JPanel curPanel = groupPanels.get(de.getGroup());
                            
                        if (l.getName().equalsIgnoreCase(de.getName())) {
                        	gbc.gridy = de.getPosition() - 1;
                        	
                        	gbc.insets = new Insets(2, 5, 2, 5);
                            gbc.fill = GridBagConstraints.HORIZONTAL;
                            gbc.anchor = GridBagConstraints.EAST;
                            gbc.weightx = 0;
                            curPanel.add(l, gbc);
                            gbc.weightx = 1;
                            gbc.gridx = 1;
                            gbc.anchor = GridBagConstraints.WEST;
                            if (de.getType().equalsIgnoreCase(FILE_ELEMENT_TYPE)) {
                            	curPanel.add(t, gbc);
                                gbc.gridx = 2;
                                final JButton browseButton = new JButton("Browse");
                                browseButton.addActionListener(this);
                                browseButton.setActionCommand("browse_" + labelName);
                                curPanel.add(browseButton, gbc);

                            } else {
                                gbc.gridwidth = 2;
                                curPanel.add(t, gbc);
                            }

                            //gridYCounter = gridYCounter + 1;
                            //gbc.gridy = gridYCounter;
                            gbc.gridx = 0;
                            gbc.gridwidth = 1;
                            break;
                        }
                    }
                }
            } 
            
            GridBagConstraints gbc2 = new GridBagConstraints();
            gbc2.fill = GridBagConstraints.HORIZONTAL;
            gbc2.gridx = 0;
            gbc2.gridy = 0;
            mainPanel.add(groupPanels.get(MAIN_GROUP_NAME), gbc2);
            gbc2.gridy++;
            for (String g : groupPanels.navigableKeySet()) {
            	if (!g.equals(MAIN_GROUP_NAME)) {
            		mainPanel.add(groupPanels.get(g), gbc2);
            		gbc2.gridy++;
            	}
            }
        }

        /**
         * parses the OMElement
         * 
         * @param ds
         */
        private void parse_new(final BasicDataStructure dataStructure, final DataStruct ds2, final String shortname,
                final TreeMap<JLabel, JComponent> labelsAndComps) {
            List<BasicDataStructure> bdsToGet = new Vector<BasicDataStructure>();
            bdsToGet.add(dataStructure);

            List<DataStructure> dsList = dictionaryProvider.getDataDictionary(bdsToGet);
            Set<MapElement> dataElements = null;
            for (DataStructure ds : dsList) {
                dataElements = ds.getDataElements();
                groups = ds.getRepeatableGroups();
            }
            Iterator<MapElement> iter = dataElements.iterator();

            while (iter.hasNext()) {
                MapElement dataElement = iter.next();

                final String n = dataElement.getName();
                
                final String title = dataElement.getTitle();

                final String d = dataElement.getDescription();

                final String sh = dataElement.getShortDescription();
                
                final String notes = dataElement.getNotes();

                final String t = dataElement.getType().getValue();
                
                String s = "";
                if (dataElement.getSize() != null) {
                    s = dataElement.getSize().toString();
                }

                // final String r = dataElement.getRequired();
                final String r = dataElement.getRequiredType().toString();

                // final String v = dataElement.getValueRange();
                Set<ValueRange> valueRangeSet = dataElement.getValueRangeList();
                String v = "";
                for (ValueRange valueRange : valueRangeSet) {
                    if (valueRange != null) {
                        if (v.equals("")) {
                            v += valueRange.getValueRange();
                        } else {
                            v += "; " + valueRange.getValueRange();
                        }
                    }
                }

                // final String c = dataElement.getRequiredCondition();
                final String c = "";
                
                final String guide = dataElement.getGuidelines();
                final String g = dataElement.getRepeatableGroup().getName();
                final int p = dataElement.getPosition().intValue();

                final String parentDataStruct = ds2.getName();
                final String parentDataStructShortName = ds2.getShortname();
                final DataElement de = new DataElement(n, title, d, sh, notes, t, s, r, v, c, guide, g, p, parentDataStruct, parentDataStructShortName);
                ds2.add(de);

                JLabel l;
                /*
                 * if (sh == null || sh.equalsIgnoreCase("")) { l = new JLabel(n); } else { l = new JLabel(sh); }
                 */
                // switched to always use the name for now with description as tooltip (sh was badly truncated for many demo elements)
                //if (sh != null && !sh.equals("")) {
                //    l = new JLabel(sh);
                //} else {
                //    l = new JLabel(n);
                //}

                l = new JLabel(title);
                l.setFont(MipavUtil.font12);
                l.setName(n);
                
                String tooltip = "<html><p><b>Name:</b> " + dataElement.getName() + "<br/>";
                tooltip += "<b>Description:</b><br/>" + WrapText.wrap(d);
                tooltip += "</p></html>";
                l.setToolTipText(tooltip);
                
                /*
                 * System.out.println("^^^ " + n); System.out.println("^^^-- " + sh); System.out.println("^^^--" + t);
                 * System.out.println("^^^--" + s); System.out.println("^^^--" + v); System.out.println();
                 */
                // System.out.println("^^^ " + n);
                // System.out.println("^^^ " + r);
                // System.out.println("^^^ " + c);
                // System.out.println();
                // if valuerange is enumeration, create a combo box...otherwise create a textfield
                if (v != null && v.contains(";") && t != null && !t.equalsIgnoreCase("DATE")) {
                    final JComboBox cb = new JComboBox();
                    cb.setName(n);
                    cb.setFont(MipavUtil.font12);
                    final String[] items = v.split(";");
                    cb.addItem("");
                    for (final String element : items) {
                        final String item = element.trim();
                        cb.addItem(item);
                    }
                    cb.addItemListener(this);
                    if (r.equalsIgnoreCase("Required")) {
                        l.setForeground(Color.red);
                    }/*
                         * else if(r.equalsIgnoreCase("Conditional")) { l.setForeground(Color.blue); }
                         */
                    //if (cb.getPreferredSize().width > tabScrollPane.getPreferredSize().width - 100) {
                    //	cb.setPreferredSize(new Dimension(tabScrollPane.getPreferredSize().width - 100, cb.getPreferredSize().height));
                    //}
                    
                    tooltip = "<html>";
                    if (guide != null) {
                    	tooltip += "<p><b>Guidelines & Instructions:</b> " + WrapText.wrap(guide) + "</p>";
                    }
                    if (notes != null) {
                    	tooltip += "<p><b>Notes:</b><br/>" + WrapText.wrap(notes) + "</p>";
                    }
                    tooltip += "</html>";
                    l.setToolTipText(tooltip);

                    labelsAndComps.put(l, cb);
                } else {
                    final JTextField tf = new JTextField(20);
                    tf.setName(n);
                    tf.setFont(MipavUtil.font12);

                    tooltip = "<html><b>Type:</b> " + t;
                    if (t.equalsIgnoreCase("String")) {
                        tooltip += " (" + s + ")";
                    }
                    if (v != null && !v.trim().equalsIgnoreCase("")) {
                        tooltip += ".  Value range: " + v;
                    }
                    if (guide != null) {
                    	tooltip += "<p><b>Guidelines & Instructions:</b><br/>" + WrapText.wrap(guide) + "</p>";
                    }
                    if (notes != null) {
                    	tooltip += "<p><b>Notes:</b><br/>" + WrapText.wrap(notes) + "</p>";
                    }
                    tooltip += "</html>";
                    tf.setToolTipText(tooltip);
                    tf.addFocusListener(this);
                    
                    disableUnchangableFields(n, tf);
                    
                    if (n.equalsIgnoreCase(IMG_HASH_CODE_ELEMENT_NAME)) {
                    	tf.setText("Automatically generated from selected image files.");
                    } else if (n.equalsIgnoreCase(IMG_PREVIEW_ELEMENT_NAME)) {
                    	tf.setText("Automatically generated from selected image files.");
                    }
                    
                    if (r.equalsIgnoreCase("Required")) {
                        l.setForeground(Color.red);
                    }
                    
                    /*
                     * else if(r.equalsIgnoreCase("Conditional")) { l.setForeground(Color.blue); }
                     */
                    
                    labelsAndComps.put(l, tf);
                }
                // }
            }
        }
        
        private void disableUnchangableFields(String elementName, Component c) {
        	for (String e : unchangableElements) {
        		if (elementName.equalsIgnoreCase(e)) {
        			c.setEnabled(false);
        		}
        	}
        }

        /**
         * populates dialog from completed state
         */
        public void populateFieldsFromInProcessState(final TreeMap<JLabel, JComponent> labelsAndComps,
                final LinkedHashMap<String, String> infoMap2) {
            if (infoMap2 != null) {
                final Set<String> keySet = infoMap2.keySet();
                final Iterator<String> iter = keySet.iterator();
                String key;
                String value;
                while (iter.hasNext()) {
                    key = (String) iter.next();
                    value = infoMap2.get(key);
                    /*
                     * if(!value.equals("")) { System.out.println(" * " + key + " * " + value); }
                     */
                    final Set<JLabel> keySet2 = labelsAndComps.keySet();
                    final Iterator<JLabel> iter2 = keySet2.iterator();
                    // System.out.println();
                    while (iter2.hasNext()) {
                        final JLabel l = (JLabel) iter2.next();
                        final Component comp = labelsAndComps.get(l);
                        final String name = comp.getName();
                        // System.out.println("--- " + name);
                        if (name.equalsIgnoreCase(key)) {
                            if (comp instanceof JTextField) {
                                final JTextField t = (JTextField) comp;
                                t.setText(value);

                            } else if (comp instanceof JComboBox) {
								final JComboBox c = (JComboBox) comp;

                                for (int k = 0; k < c.getItemCount(); k++) {
                                    final String item = (String) c.getItemAt(k);
                                    if (value.equalsIgnoreCase(item)) {
                                        c.setSelectedIndex(k);
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        // TODO: hardcoded element handling
        public int determineImageHeaderDescrepencies(ModelImage img) {
            final float[] res = img.getResolutions(0);
            final int[] units = img.getUnitsOfMeasure();
            final int exts[] = img.getExtents();
            final int nDims = img.getNDims();
            final int modality = img.getFileInfo(0).getModality();
            final String modalityString = FileInfoBase.getModalityStr(modality);
            final float sliceThickness = img.getFileInfo(0).getSliceThickness();
            final int orient = img.getFileInfo(0).getImageOrientation();
            final String orientation = FileInfoBase.getImageOrientationStr(orient);

            final int fileFormatInt = img.getFileInfo(0).getFileFormat();
            String fileFormatString = FileUtility.getFileTypeStr(fileFormatInt);
            
            ArrayList<String> csvFList = new ArrayList<String>();
            ArrayList<String> csvPList = new ArrayList<String>();
            ArrayList<String> headerList = new ArrayList<String>();

            for (int i = 0; i < csvFieldNames.length; i++) {

                if ( !csvParams[i].trim().equals("")) {

                    if (csvFieldNames[i].equalsIgnoreCase("ImgDimensNum")) {
                        if ( !csvParams[i].trim().equals(String.valueOf(nDims) + "D")) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(nDims));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim1Extent")) {

                        if ( !csvParams[i].trim().equals(String.valueOf(exts[0]))) {

                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(exts[0]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim2Extent")) {
                        if ( !csvParams[i].trim().equals(String.valueOf(exts[1]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(exts[1]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim3Extent")) {
                        if ( !csvParams[i].trim().equals(String.valueOf(exts[2]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(exts[2]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim4Extent")) {
                        if ( !csvParams[i].trim().equals(String.valueOf(exts[3]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(exts[3]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim5Extent")) {
                        if ( !csvParams[i].trim().equals(String.valueOf(exts[4]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(exts[4]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim1UoM")) {
                        if ( !csvParams[i].trim().equals(FileInfoBase.getUnitsOfMeasureStr(units[0]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(FileInfoBase.getUnitsOfMeasureStr(units[0]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim2UoM")) {
                        if ( !csvParams[i].trim().equals(FileInfoBase.getUnitsOfMeasureStr(units[1]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(FileInfoBase.getUnitsOfMeasureStr(units[1]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim3UoM")) {
                        if ( !csvParams[i].trim().equals(FileInfoBase.getUnitsOfMeasureStr(units[2]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(FileInfoBase.getUnitsOfMeasureStr(units[2]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim4UoM")) {
                        if ( !csvParams[i].trim().equals(FileInfoBase.getUnitsOfMeasureStr(units[3]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(FileInfoBase.getUnitsOfMeasureStr(units[3]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim5UoM")) {
                        if ( !csvParams[i].trim().equals(FileInfoBase.getUnitsOfMeasureStr(units[4]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(FileInfoBase.getUnitsOfMeasureStr(units[4]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim1Resol")) {
                    	// TODO: does not handle differences like 1.0 vs 1 well
                        if ( !csvParams[i].trim().equals(String.valueOf(res[0]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(res[0]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim2Resol")) {
                    	// TODO: does not handle differences like 1.0 vs 1 well
                        if ( !csvParams[i].trim().equals(String.valueOf(res[1]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(res[1]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim3Resol")) {
                    	// TODO: does not handle differences like 1.0 vs 1 well
                        if ( !csvParams[i].trim().equals(String.valueOf(res[2]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(res[2]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim4Resol")) {
                    	// TODO: does not handle differences like 1.0 vs 1 well
                        if ( !csvParams[i].trim().equals(String.valueOf(res[3]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(res[3]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgDim5Resol")) {
                    	// TODO: does not handle differences like 1.0 vs 1 well
                        if ( !csvParams[i].trim().equals(String.valueOf(res[4]))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(res[4]));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgModltyTyp")) {
                    	// TODO: need to do mapping between MIPAV modalities and BRICS values
                        if ( !csvParams[i].trim().equals(modalityString)) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(modalityString);
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgSliceThicknessVal")) {
                    	// TODO: does not handle differences like 1.0 vs 1 well
                        if ( !csvParams[i].trim().equals(String.valueOf(sliceThickness))) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(String.valueOf(sliceThickness));
                        }
                    } else if (csvFieldNames[i].equalsIgnoreCase("ImgSliceOrientTyp")) {
                        if ( !csvParams[i].trim().equals(orientation)) {
                            csvFList.add(csvFieldNames[i]);
                            csvPList.add(csvParams[i]);
                            headerList.add(orientation);
                        }
                    }
                }
            }
            
            if (fileFormatString.equalsIgnoreCase("dicom")) {
            	FileInfoDicom fileInfoDicom = (FileInfoDicom) img.getFileInfo(0);
            	
            	String ageVal = (String) (fileInfoDicom.getTagTable().getValue("0010,1010"));
                String siteName = (String) (fileInfoDicom.getTagTable().getValue("0008,0080"));
                String visitDate = convertDateTimeToISOFormat((String) (fileInfoDicom.getTagTable().getValue("0008,0020")));
                String visitTime = (String) (fileInfoDicom.getTagTable().getValue("0008,0030"));
                String sliceOversample = (String) (fileInfoDicom.getTagTable().getValue("0018,0093"));
                String gap = (String) (fileInfoDicom.getTagTable().getValue("0018,0088"));
                String bodyPart = (String) (fileInfoDicom.getTagTable().getValue("0018,0015"));
                               
                String fieldOfView = (String) (fileInfoDicom.getTagTable().getValue("0018,1100"));
                String manufacturer = (String) (fileInfoDicom.getTagTable().getValue("0008,0070"));
                String softwareVersion = (String) (fileInfoDicom.getTagTable().getValue("0018,1020"));
                String patientPosition = (String) (fileInfoDicom.getTagTable().getValue("0018,5100"));
                
                String scannerModel = (String) (fileInfoDicom.getTagTable().getValue("0008,1090"));
                String bandwidth = (String) (fileInfoDicom.getTagTable().getValue("0018,0095"));
                
                for (int i = 0; i < csvFieldNames.length; i++) {

                    if ( !csvParams[i].trim().equals("")) {

                        if (csvFieldNames[i].equalsIgnoreCase("AgeVal")) {
                        	if (ageVal.contains("Y")){
                        		String temp = ageVal.substring(0,ageVal.length()-6);
                        		ageVal = Integer.toString(Integer.parseInt(temp)*12);
                        	}
                            if ( !csvParams[i].trim().equals(ageVal)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(ageVal);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("visitDate")) {
                            if ( !csvParams[i].trim().equals(visitDate)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(visitDate);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("SiteName")) {
                            if ( !csvParams[i].trim().equals(siteName)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(siteName);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("ImgStdyDateTimet")) {
                            if ( !csvParams[i].trim().equals(visitDate + " " + visitTime)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(visitDate + " " + visitTime);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("ImgSliceOversampleVal")) {
                            if ( !csvParams[i].trim().equals(sliceOversample)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(sliceOversample);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("ImgGapBtwnSlicesMeasr")) {
                            if ( !csvParams[i].trim().equals(gap)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(gap);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("ImgAntmicSite")) {
                            if ( !csvParams[i].trim().equals(bodyPart)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(bodyPart);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("ImgFoVMeasr")) {
                            if ( !csvParams[i].trim().equals(fieldOfView)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(fieldOfView);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("ImgScanManufName")) {
                            if ( !csvParams[i].trim().equals(manufacturer)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(manufacturer);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("ImgScanSftwrVerNum")) {
                            if ( !csvParams[i].trim().equals(softwareVersion)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(softwareVersion);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("ImgHeadPositionTxt")) {
                            if ( !csvParams[i].trim().equals(patientPosition)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(patientPosition);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("ImgScanModelName")) {
                            if ( !csvParams[i].trim().equals(scannerModel)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(scannerModel);
                            }
                        } else if (csvFieldNames[i].equalsIgnoreCase("ImgBandwidthVal")) {
                            if ( !csvParams[i].trim().equals(bandwidth)) {
                                csvFList.add(csvFieldNames[i]);
                                csvPList.add(csvParams[i]);
                                headerList.add(bandwidth);
                            }
                        } 
                    }
                }
                
                if (modalityString.equalsIgnoreCase("magnetic resonance")) {
                	
                    String echoTime = (String) (fileInfoDicom.getTagTable().getValue("0018,0081"));
                    String repetitionTime = (String) (fileInfoDicom.getTagTable().getValue("0018,0080"));
                    String magnaticFieldStrength = (String) (fileInfoDicom.getTagTable().getValue("0018,0087"));
                    String flipAngle = (String) (fileInfoDicom.getTagTable().getValue("0018,1314"));
                    
                    String mriT1T2Name = (String) (fileInfoDicom.getTagTable().getValue("0018,0024"));
                    String inversionTime = (String) (fileInfoDicom.getTagTable().getValue("0018,0082"));
                    String echoTrainMeas = (String) (fileInfoDicom.getTagTable().getValue("0018,0091"));
                    String phaseEncode = (String) (fileInfoDicom.getTagTable().getValue("0018,1312"));
                    String numAverages = (String) (fileInfoDicom.getTagTable().getValue("0018,0083"));
                    String receiveCoilName = (String) (fileInfoDicom.getTagTable().getValue("0018,1250"));
                    
                    for (int i = 0; i < csvFieldNames.length; i++) {

                        if ( !csvParams[i].trim().equals("")) {

                            if (csvFieldNames[i].equalsIgnoreCase("ImgEchoTime")) {
                                if ( !csvParams[i].trim().equals(echoTime)) {
                                    csvFList.add(csvFieldNames[i]);
                                    csvPList.add(csvParams[i]);
                                    headerList.add(echoTime);
                                }
                            } else if (csvFieldNames[i].equalsIgnoreCase("ImgReptitnTime")) {

                                if ( !csvParams[i].trim().equals(repetitionTime)) {

                                    csvFList.add(csvFieldNames[i]);
                                    csvPList.add(csvParams[i]);
                                    headerList.add(repetitionTime);
                                }
                            } else if (csvFieldNames[i].equalsIgnoreCase("ImgScanStrgthVal")) {
                                if ( !csvParams[i].trim().equals(magnaticFieldStrength)) {
                                    csvFList.add(csvFieldNames[i]);
                                    csvPList.add(csvParams[i]);
                                    headerList.add(magnaticFieldStrength);
                                }
                            } else if (csvFieldNames[i].equalsIgnoreCase("ImgFlipAngleMeasr")) {
                                if ( !csvParams[i].trim().equals(flipAngle)) {
                                    csvFList.add(csvFieldNames[i]);
                                    csvPList.add(csvParams[i]);
                                    headerList.add(flipAngle);
                                }
                            } else if (csvFieldNames[i].equalsIgnoreCase("ImgMRIT1T2Name")) {
                                if ( !csvParams[i].trim().equals(mriT1T2Name)) {
                                    csvFList.add(csvFieldNames[i]);
                                    csvPList.add(csvParams[i]);
                                    headerList.add(mriT1T2Name);
                                }
                            } else if (csvFieldNames[i].equalsIgnoreCase("ImgInversionTime")) {
                                if ( !csvParams[i].trim().equals(inversionTime)) {
                                    csvFList.add(csvFieldNames[i]);
                                    csvPList.add(csvParams[i]);
                                    headerList.add(inversionTime);
                                }
                            } else if (csvFieldNames[i].equalsIgnoreCase("ImgEchoTrainLengthMeasr")) {
                                if ( !csvParams[i].trim().equals(echoTrainMeas)) {
                                    csvFList.add(csvFieldNames[i]);
                                    csvPList.add(csvParams[i]);
                                    headerList.add(echoTrainMeas);
                                }
                            } else if (csvFieldNames[i].equalsIgnoreCase("ImgPhaseEncodeDirTxt")) {
                                if ( !csvParams[i].trim().equals(phaseEncode)) {
                                    csvFList.add(csvFieldNames[i]);
                                    csvPList.add(csvParams[i]);
                                    headerList.add(phaseEncode);
                                }
                            } else if (csvFieldNames[i].equalsIgnoreCase("ImgSignalAvgNum")) {
                                if ( !csvParams[i].trim().equals(numAverages)) {
                                    csvFList.add(csvFieldNames[i]);
                                    csvPList.add(csvParams[i]);
                                    headerList.add(numAverages);
                                }
                            } else if (csvFieldNames[i].equalsIgnoreCase("ImgRFReceivCoilName")) {
                                if ( !csvParams[i].trim().equals(receiveCoilName)) {
                                    csvFList.add(csvFieldNames[i]);
                                    csvPList.add(csvParams[i]);
                                    headerList.add(receiveCoilName);
                                }
                            } 
                        }
                    }
                }
            }

            if (csvFList.size() > 0) {
                if (resolveConflictsUsing == 0) {
                    String message = "Certain image info in the csv do not match with info obtained from header : \n";
                    for (int i = 0; i < csvFList.size(); i++) {
                        String fieldName = csvFList.get(i);
                        String param = csvPList.get(i);
                        String headerInfo = headerList.get(i);

                        message = message + fieldName + " : " + "      csv:" + param + "     header:" + headerInfo
                                + "\n";

                    }
                    // message = message + "Press Yes to use CSV info. Press No to use Header info";
                    UIManager.put("OptionPane.yesButtonText", "Use CSV");
                    UIManager.put("OptionPane.noButtonText", "Use Image Header");

                    JCheckBox checkbox = new JCheckBox("Do not show this message again", false);
                    Object[] content = {message, checkbox};
                    
                    // I'd like to create a merge option using the selected button as the dominating input - Sara

                    int response = JOptionPane.showConfirmDialog(null, content, "", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);

                    // Object[] options = {"ok button text"};
                    // int response = JOptionPane.showOptionDialog(null, message, "", JOptionPane.YES_NO_OPTION,
                    // JOptionPane.WARNING_MESSAGE,null,options,options[0]);

                    UIManager.put("OptionPane.yesButtonText", "Yes");
                    UIManager.put("OptionPane.noButtonText", "No");

                    if (response == JOptionPane.YES_OPTION) {
                        if (checkbox.isSelected()) {
                            resolveConflictsUsing = 1;
                        }
                        return 1;
                    } else {
                        if (checkbox.isSelected()) {
                            resolveConflictsUsing = 2;
                        }
                        return 2;
                    }
                } else {
                    return resolveConflictsUsing;
                }
            } else {
                return 0;
            }
        }

        /**
         * prepopulates some of the fields with info from image header
         */
        public void populateFields(final TreeMap<JLabel, JComponent> labelsAndComps, final ModelImage img) {
            final float[] res = img.getResolutions(0);
            final int[] units = img.getUnitsOfMeasure();
            final int exts[] = img.getExtents();
            final int modality = img.getFileInfo(0).getModality();
            final String modalityString = FileInfoBase.getModalityStr(modality);
            // System.out.println(modalityString);
            final int fileFormatInt = img.getFileInfo(0).getFileFormat();
            String fileFormatString = FileUtility.getFileTypeStr(fileFormatInt);
            if (fileFormatString.equalsIgnoreCase("xml")) {
                fileFormatString = "mipav xml";
            } else if (fileFormatString.equalsIgnoreCase("mat")) {
                fileFormatString.equalsIgnoreCase("matlab");
            }
            // System.out.println(fileFormatString);

            final float sliceThickness = img.getFileInfo(0).getSliceThickness();
            final int orient = img.getFileInfo(0).getImageOrientation();
            final String orientation = FileInfoBase.getImageOrientationStr(orient);
            // get index for extents
            Set<JLabel> keySet = labelsAndComps.keySet();
            Iterator<JLabel> iter = keySet.iterator();
            while (iter.hasNext()) {
                final JLabel label = (JLabel) iter.next();
                final String l = label.getName();
                final JComponent comp = labelsAndComps.get(label);
                
                if (l.equalsIgnoreCase("ImgDimensNum")) {
                	final JComboBox jc = (JComboBox) comp;
                	final String item = exts.length + "D";
                	for (int k = 0; k < jc.getItemCount(); k++) {
                        if (((String) jc.getItemAt(k)).equalsIgnoreCase(item)) {
                            jc.setSelectedIndex(k);
                        }
                    }
                	if (jc.getSelectedIndex() == 0)
                		jc.setSelectedIndex(jc.getItemCount()-1);
                    label.setForeground(Color.red);
                } else if (l.equalsIgnoreCase("ImgDim1Extent")) {
                    ((JTextField) comp).setText(String.valueOf(exts[0]));
                    label.setForeground(Color.red);
                } else if (l.equalsIgnoreCase("ImgDim2Extent")) {
                    ((JTextField) comp).setText(String.valueOf(exts[1]));
                    label.setForeground(Color.red);
                } else if (l.equalsIgnoreCase("ImgDim3Extent")) {
                    if (img.getNDims() > 2) {
                        ((JTextField) comp).setText(String.valueOf(exts[2]));
                        label.setForeground(Color.red);
                    }
                } else if (l.equalsIgnoreCase("ImgDim4Extent")) {
                    if (img.getNDims() > 3) {
                        ((JTextField) comp).setText(String.valueOf(exts[3]));
                        label.setForeground(Color.red);
                    }
                } else if (l.equalsIgnoreCase("ImgDim5Extent")) {
                    // for now...nothing
                } else if (l.equalsIgnoreCase("ImgDim1UoM")) {
					final JComboBox jc = (JComboBox) comp;
                    for (int k = 0; k < jc.getItemCount(); k++) {
                        final String item = (String) jc.getItemAt(k);
                        if (FileInfoBase.getUnitsOfMeasureStr(units[0]).equalsIgnoreCase(item)) {
                            jc.setSelectedIndex(k);
                        }
                    }
                    label.setForeground(Color.red);
                } else if (l.equalsIgnoreCase("ImgDim2UoM")) {
					final JComboBox jc = (JComboBox) comp;
                    for (int k = 0; k < jc.getItemCount(); k++) {
                        final String item = (String) jc.getItemAt(k);
                        if (FileInfoBase.getUnitsOfMeasureStr(units[1]).equalsIgnoreCase(item)) {
                            jc.setSelectedIndex(k);
                        }
                    }
                    label.setForeground(Color.red);
                } else if (l.equalsIgnoreCase("ImgDim3UoM")) {
                    if (img.getNDims() > 2) {
    					final JComboBox jc = (JComboBox) comp;
                        for (int k = 0; k < jc.getItemCount(); k++) {
                            final String item = (String) jc.getItemAt(k);
                            if (FileInfoBase.getUnitsOfMeasureStr(units[2]).equalsIgnoreCase(item)) {
                                jc.setSelectedIndex(k);
                            }
                        }
                        label.setForeground(Color.red);
                    }
                } else if (l.equalsIgnoreCase("ImgDim4UoM")) {
                    if (img.getNDims() > 3) {
    					final JComboBox jc = (JComboBox) comp;
                        for (int k = 0; k < jc.getItemCount(); k++) {
                            final String item = (String) jc.getItemAt(k);
                            if (FileInfoBase.getUnitsOfMeasureStr(units[3]).equalsIgnoreCase(item)) {
                                jc.setSelectedIndex(k);
                            }
                        }
                        label.setForeground(Color.red);
                    }
                } else if (l.equalsIgnoreCase("ImgDim5UoM")) {
                    // for now...nothing
                } else if (l.equalsIgnoreCase("ImgDim1Resol")) {
                    ((JTextField) comp).setText(String.valueOf(res[0]));
                    label.setForeground(Color.red);
                } else if (l.equalsIgnoreCase("ImgDim2Resol")) {
                    ((JTextField) comp).setText(String.valueOf(res[1]));
                    label.setForeground(Color.red);
                } else if (l.equalsIgnoreCase("ImgDim3Resol")) {
                    if (img.getNDims() > 2) {
                        ((JTextField) comp).setText(String.valueOf(res[2]));
                        label.setForeground(Color.red);
                    }
                } else if (l.equalsIgnoreCase("ImgDim4Resol")) {
                    if (img.getNDims() > 3) {
                        ((JTextField) comp).setText(String.valueOf(res[3]));
                        label.setForeground(Color.red);
                    }
                } else if (l.equalsIgnoreCase("ImgDim5Resol")) {
                    // for now...nothing
                } else if (l.equalsIgnoreCase("ImgModltyTyp")) {
					final JComboBox jc = (JComboBox) comp;
                    if (modalityString.contains("Magnetic"))
                    	jc.setSelectedItem("MRI");
                    label.setForeground(Color.red);
                } else if (l.equalsIgnoreCase("ImgFileFormat")) {
					final JComboBox jc = (JComboBox) comp;
                    for (int k = 0; k < jc.getItemCount(); k++) {
                        final String item = (String) jc.getItemAt(k);
                        if (fileFormatString.equalsIgnoreCase(item)) {
                            jc.setSelectedIndex(k);
                        }
                    }
                    label.setForeground(Color.red);
                } else if (l.equalsIgnoreCase("ImgSliceThicknessVal")) {
                    if (sliceThickness == 0) {
                        ((JTextField) comp).setText("");
                    } else {
                        ((JTextField) comp).setText(String.valueOf(sliceThickness));
                    }
                } else if (l.equalsIgnoreCase("ImgSliceOrientTyp")) {
					final JComboBox jc = (JComboBox) comp;
                    for (int k = 0; k < jc.getItemCount(); k++) {
                        final String item = (String) jc.getItemAt(k);
                        if (item.contains(orientation)) {
                            jc.setSelectedIndex(k);
                        }
                    }
//                } else if (l.equalsIgnoreCase(IMG_HASH_CODE_ELEMENT_NAME)) {
//                	((JTextField) comp).setText("This will be filled for you upon generating files");
                }
            }
            
            if (fileFormatString.equalsIgnoreCase("dicom")) {
            	
            	FileInfoDicom fileInfoDicom = (FileInfoDicom) img.getFileInfo(0);
            	
            	String ageVal = (String) (fileInfoDicom.getTagTable().getValue("0010,1010"));
                String siteName = (String) (fileInfoDicom.getTagTable().getValue("0008,0080"));
                String visitDate = convertDateTimeToISOFormat((String) (fileInfoDicom.getTagTable().getValue("0008,0020")));
                String visitTime = (String) (fileInfoDicom.getTagTable().getValue("0008,0030"));
                String sliceOversample = (String) (fileInfoDicom.getTagTable().getValue("0018,0093"));
                String gap = (String) (fileInfoDicom.getTagTable().getValue("0018,0088"));
                String bodyPart = (String) (fileInfoDicom.getTagTable().getValue("0018,0015"));
                               
                String fieldOfView = (String) (fileInfoDicom.getTagTable().getValue("0018,1100"));
                String manufacturer = (String) (fileInfoDicom.getTagTable().getValue("0008,0070"));
                String softwareVersion = (String) (fileInfoDicom.getTagTable().getValue("0018,1020"));
                String patientPosition = (String) (fileInfoDicom.getTagTable().getValue("0018,5100"));
                
                String scannerModel = (String) (fileInfoDicom.getTagTable().getValue("0008,1090"));
                String bandwidth = (String) (fileInfoDicom.getTagTable().getValue("0018,0095"));
                
                keySet = labelsAndComps.keySet();
                iter = keySet.iterator();
                while (iter.hasNext()) {
                    final JLabel label = (JLabel) iter.next();
                    final String l = label.getName();
                    final JComponent comp = labelsAndComps.get(label);
                    if (l.equalsIgnoreCase("AgeVal")) {
                    	if (ageVal.contains("Y")){
                    		String temp = ageVal.substring(0,ageVal.length()-6);
                    		ageVal = Integer.toString(Integer.parseInt(temp)*12);
                    	}
                        ((JTextField) comp).setText(ageVal);
                        label.setForeground(Color.red);
                    } else if (l.equalsIgnoreCase("SiteName")) {
                        ((JTextField) comp).setText(siteName);
                    } else if (l.equalsIgnoreCase("VisitDate")) {
                        ((JTextField) comp).setText(visitDate);
                    } else if (l.equalsIgnoreCase("ImgAntmicSite")) {
                    	final JComboBox jc = (JComboBox) comp;
                        for (int k = 0; k < jc.getItemCount(); k++) {
                            final String item = (String) jc.getItemAt(k);
                            if (bodyPart.equalsIgnoreCase(item)) {
                                jc.setSelectedIndex(k);
                            }
                        }

                    } else if (l.equalsIgnoreCase("ImgStdyDateTime")) {
                        ((JTextField) comp).setText(visitDate + " " + visitTime);
                        label.setForeground(Color.red);
                    } else if (l.equalsIgnoreCase("ImgSliceOversampleVal")) {
                        ((JTextField) comp).setText(sliceOversample);
                    } else if (l.equalsIgnoreCase("ImgGapBtwnSlicesMeasr")) {
                        ((JTextField) comp).setText(gap);
                    } else if (l.equalsIgnoreCase("ImgFoVMeasr")) {
                        ((JTextField) comp).setText(fieldOfView);
                    } else if (l.equalsIgnoreCase("ImgScanManufName")) {
                    	final JComboBox jc = (JComboBox) comp;
                        for (int k = 0; k < jc.getItemCount(); k++) {
                            final String item = (String) jc.getItemAt(k);
                            if (manufacturer.contains(item)) {
                                jc.setSelectedIndex(k);
                            }
                        }
                        label.setForeground(Color.red);
                    } else if (l.equalsIgnoreCase("ImgScanSftwrVerNum")) {
                        ((JTextField) comp).setText(softwareVersion);
                    } else if (l.equalsIgnoreCase("ImgHeadPositionTxt")) {
                        ((JTextField) comp).setText(patientPosition);
                    } else if (l.equalsIgnoreCase("ImgScanModelName")) {
                        ((JTextField) comp).setText(scannerModel);
                    } else if (l.equalsIgnoreCase("ImgBandwidthVal")) {
                        ((JTextField) comp).setText(bandwidth);
                    }
                }
                
                if (modalityString.equalsIgnoreCase("magnetic resonance")) {
                	
                    String echoTime = (String) (fileInfoDicom.getTagTable().getValue("0018,0081"));
                    String repetitionTime = (String) (fileInfoDicom.getTagTable().getValue("0018,0080"));
                    String magnaticFieldStrength = (String) (fileInfoDicom.getTagTable().getValue("0018,0087"));
                    String flipAngle = (String) (fileInfoDicom.getTagTable().getValue("0018,1314"));
                    
                    String mriT1T2Name = (String) (fileInfoDicom.getTagTable().getValue("0018,0024"));
                    String inversionTime = (String) (fileInfoDicom.getTagTable().getValue("0018,0082"));
                    String echoTrainMeas = (String) (fileInfoDicom.getTagTable().getValue("0018,0091"));
                    String phaseEncode = (String) (fileInfoDicom.getTagTable().getValue("0018,1312"));
                    String numAverages = (String) (fileInfoDicom.getTagTable().getValue("0018,0083"));
                    String receiveCoilName = (String) (fileInfoDicom.getTagTable().getValue("0018,1250"));
                    
                    keySet = labelsAndComps.keySet();
                    iter = keySet.iterator();
                    while (iter.hasNext()) {
                        final JLabel label = (JLabel) iter.next();
                        final String l = label.getName();
                        final JComponent comp = labelsAndComps.get(label);
                        if (l.equalsIgnoreCase("ImgEchoTime")) {
                            ((JTextField) comp).setText(echoTime);
                            label.setForeground(Color.red);
                        } else if (l.equalsIgnoreCase("ImgRepetitnTime")) {
                            ((JTextField) comp).setText(repetitionTime);
                        } else if (l.equalsIgnoreCase("ImgScanStrgthVal")) {
                            final JComboBox jc = (JComboBox) comp;
                            for (int k = 0; k < jc.getItemCount(); k++) {
                                final String item = (String) jc.getItemAt(k);
                                if (item.contains(magnaticFieldStrength)) {
                                    jc.setSelectedIndex(k);
                                }
                            }
                            label.setForeground(Color.red);
                        } else if (l.equalsIgnoreCase("ImgMRIT1T2Name")) {
                            ((JTextField) comp).setText(mriT1T2Name);
                        } else if (l.equalsIgnoreCase("ImgSignalAvgNum")) {
                            ((JTextField) comp).setText(numAverages);
                        } else if (l.equalsIgnoreCase("ImgFlipAngleMeasr")) {
                            ((JTextField) comp).setText(flipAngle);
                        } else if (l.equalsIgnoreCase("ImgEchoTrainLengthMeasr")) {
                            ((JTextField) comp).setText(echoTrainMeas);
                        } else if (l.equalsIgnoreCase("ImgInversionTime")) {
                            ((JTextField) comp).setText(inversionTime);
                        } else if (l.equalsIgnoreCase("ImgRFReceivCoilName")) {
                            ((JTextField) comp).setText(receiveCoilName);
                        } else if (l.equalsIgnoreCase("ImgPhaseEncodeDirTxt")) {
                            ((JTextField) comp).setText(phaseEncode);
                        }
                    }
                }
            }
        }
        
        /**
         * clear populated fields
         * @param labelsAndComps
         */
        public void clearFields(final TreeMap<JLabel, JComponent> labelsAndComps){
            Set<JLabel> keySet = labelsAndComps.keySet();
            Iterator<JLabel> iter = keySet.iterator();
            while (iter.hasNext()) {
            	final JLabel label = (JLabel) iter.next();
                final String l = label.getName();
                final JComponent comp = labelsAndComps.get(label);
                if (!l.equalsIgnoreCase("ImgFileName")){
	                try {
	                	((JTextField) comp).setText("");
	                } catch (ClassCastException e) {
	                	try {
	                		((JComboBox) comp).setSelectedItem("");
	                	} catch (ClassCastException f) {
	                		
	                	}
	                }
                }
            }
        }

        /**
         * action performed
         */
        public void actionPerformed(final ActionEvent e) {
            final String command = e.getActionCommand();
            ArrayList<String> errs;
            StringBuffer errors = new StringBuffer();
            ;
            if (command.equalsIgnoreCase("ok3")) {
                errs = validateFields();

                boolean isComplete = true;
                if (errs.size() != 0) {
                    for (int i = 0; i < errs.size(); i++) {
                        errors.append(" - " + errs.get(i) + "\n");
                    }
                    MipavUtil.displayWarning(errors.toString());
                    isComplete = false;
                }

                complete(labelsAndComps, dataStructureName, isComplete);

                enableDisableFinishButton();
                dispose();

            } else if (command.equalsIgnoreCase("cancel3")) {
                if ( !launchedFromInProcessState) {
                    previewImages.remove(previewImages.size() - 1);
                    imageFiles.remove(imageFiles.size() - 1);
                    multifiles.remove(multifiles.size() - 1);
                    infoList.remove(infoList.size() - 1);
                    allOtherFilesAL.remove(allOtherFilesAL.size() - 1);
                    if (addedPreviewImage) {
                        previewPanel.removeAll();
                        previewPanel.repaint();
                    }
                }
                enableDisableFinishButton();
                dispose();
            } else if (command.startsWith("browse_")) {
            	if (currFile == null)
            		currFile = "";
                boolean isMultiFile = false;
                //System.out.println(dataStructureName);
                if (dataStructureName.startsWith("Imag")) {
                    final ViewFileChooserBase fileChooser = new ViewFileChooserBase(true, false);
                    fileChooser.setMulti(ViewUserInterface.getReference().getLastStackFlag());

                    final JFileChooser chooser = fileChooser.getFileChooser();
                    chooser.setCurrentDirectory(new File(ViewUserInterface.getReference().getDefaultDirectory()));

                    // default to TECH filter
                    int filter = ViewImageFileFilter.TECH;

                    try {
                        filter = Integer.parseInt(Preferences.getProperty(Preferences.PREF_FILENAME_FILTER));
                    } catch (final NumberFormatException nfe) {

                        // an invalid value was set in preferences -- so don't use it!
                        filter = -1;
                    }

                    chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.GEN));
                    chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.TECH));
                    chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.MICROSCOPY));
                    chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.MISC));

                    if (filter != -1) {
                        // it seems that the set command adds the filter again...
                        // chooser.addChoosableFileFilter(new ViewImageFileFilter(filter));

                        // if filter is something we already added, then remove it before
                        // setting it..... (kludgy, kludgy....)
                        final javax.swing.filechooser.FileFilter found = ViewOpenFileUI.findFilter(chooser, filter);

                        if (found != null) {
                            chooser.removeChoosableFileFilter(found);
                        }

                        // initially set to the preferences
                        chooser.setFileFilter(new ViewImageFileFilter(filter));
                    }

                    final int returnVal = chooser.showOpenDialog(this);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        isMultiFile = fileChooser.isMulti();

                        final File file = chooser.getSelectedFile();
                        ViewUserInterface.getReference().setDefaultDirectory(file.getParent());

                        final FileIO fileIO = new FileIO();
                        fileIO.setQuiet(true);
                        ModelImage srcImage = fileIO.readImage(file.getName(), file.getParent() + File.separator,
                                isMultiFile, null);

                        if (srcImage != null) {

                            final String labelName = command.substring(command.indexOf("_") + 1, command.length());

                            String tempName = currFile;
                            final Set<JLabel> keySet = labelsAndComps.keySet();
                            final Iterator<JLabel> iter = keySet.iterator();
                            while (iter.hasNext()) {
                                final JLabel l = (JLabel) iter.next();
                                // TODO: hardcoded element handling
                                if (l.getName().equalsIgnoreCase(IMG_PREVIEW_ELEMENT_NAME)) {
                                    final JTextField tf = (JTextField) labelsAndComps.get(l);
                                    final String n = file.getName();
                                    tf.setText("Automatically generated from selected image files.");
                                } else if (l.getName().equalsIgnoreCase(labelName)) {
                                    final JTextField tf = (JTextField) labelsAndComps.get(l);
                                    tf.setText(file.getName());
                                    tempName = file.getName();
                                    tf.setEnabled(false);
                                }

                            }

                            final int[] extents = new int[] {srcImage.getExtents()[0], srcImage.getExtents()[1]};

                            previewImg = new ViewJComponentPreviewImage(srcImage, extents, owner);
                            int slice = 0;
                            if ( !srcImage.is2DImage()) {
                                slice = (srcImage.getExtents()[2] / 2);
                            }
                            previewImg.createImg(slice);

                            previewPanel.removeAll();
                            previewPanel.repaint();

                            previewPanel.add(previewImg);

                            addedPreviewImage = true;

                            if (launchedFromInProcessState) {
                                final int selectedRow = sourceTable.getSelectedRow();
                                previewImages.set(selectedRow, previewImg);
                                previewImages.get(selectedRow).setSliceBrightness(brightness, contrast);
                                imageFiles.set(selectedRow, file);
                                multifiles.set(selectedRow, new Boolean(isMultiFile));

                            } else {
                                final int size = previewImages.size();
                                previewImages.set(size - 1, previewImg);
                                previewImages.get(size - 1).setSliceBrightness(brightness, contrast);
                                imageFiles.set(size - 1, file);
                                multifiles.set(size - 1, new Boolean(isMultiFile));
                            }

                            previewPanel.validate();
                            previewPanel.repaint();

                            if (!currFile.equals(tempName))
                            	clearFields(labelsAndComps);
                            populateFields(labelsAndComps, srcImage);

                            srcImage.disposeLocal();
                            srcImage = null;

                        }

                    }
                } else {
                    final JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Choose file");
                    final int returnValue = chooser.showOpenDialog(this);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {

                        final String labelName = command.substring(command.indexOf("_") + 1, command.length());

                        final File file = chooser.getSelectedFile();

                        final Set<JLabel> keySet = labelsAndComps.keySet();
                        final Iterator<JLabel> iter = keySet.iterator();
                        while (iter.hasNext()) {
                            final JLabel l = (JLabel) iter.next();
                            if (l.getName().equalsIgnoreCase(labelName)) {
                                final JTextField tf = (JTextField) labelsAndComps.get(l);
                                tf.setText(file.getAbsolutePath());
                                tf.setEnabled(false);
                                break;
                            }
                        }

                    }
                }

            }

        }

        /**
         * validates fields
         * 
         * @return
         */
        public ArrayList<String> validateFields() {
            final ArrayList<String> errs = new ArrayList<String>();

            for (int k = 0; k < dataStructures.size(); k++) {
                final String sName = dataStructures.get(k).getShortname();

                if (dataStructureName.equalsIgnoreCase(sName)) {
                    parseDataStructForValidation(dataStructures.get(k), errs, labelsAndComps);
                }
            }

            return errs;

        }

        public boolean isNumber(String exp) {
            try {
                Double.parseDouble(exp);
            } catch (NumberFormatException e) {
                return false;
            }

            return true;
        }

        public double getNumber(String exp) {
            return Double.parseDouble(exp);
        }

        public boolean evaluateStringExpression(String op1, String oper, String op2) {
            if (op1.startsWith("'")) {
                op1 = op1.substring(1, op1.length() - 1);
            }

            if (op2.startsWith("'")) {
                op2 = op2.substring(1, op2.length() - 1);
            }

            // need to special case for MRI
            /*
             * if(op1.equalsIgnoreCase("Magnetic Resonance")) { op1 = "MRI"; }
             */

            if (oper.equals("=")) {
                if (op1.equals(op2)) {
                    return true;
                } else {
                    return false;
                }
            } else if (oper.equals("!=")) {
                if ( !op1.equals(op2)) {
                    return true;
                } else {
                    return false;
                }

            }

            return false;
        }

        public boolean evaluateNumberExpression(String op1, String oper, String op2) {
            double d1 = getNumber(op1);
            double d2 = getNumber(op2);
            if (oper.equals("=")) {
                if (d1 == d2) {
                    return true;
                } else {
                    return false;
                }
            } else if (oper.equals("<")) {
                if (d1 < d2) {
                    return true;
                } else {
                    return false;
                }
            } else if (oper.equals(">")) {
                if (d1 > d2) {
                    return true;
                } else {
                    return false;
                }
            } else if (oper.equals("!=")) {
                if (d1 != d2) {
                    return true;
                } else {
                    return false;
                }
            } else if (oper.equals("<=")) {
                if (d1 <= d2) {
                    return true;
                } else {
                    return false;
                }
            } else if (oper.equals(">=")) {
                if (d1 >= d2) {
                    return true;
                } else {
                    return false;
                }
            }

            return false;
        }

		public boolean testCondition(String[] tokens) {
            String value = "";
            String key;
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (token.startsWith("#")) {
                    // get the corresponding value from this component
                    // and replace
                    String name = token.substring(1, token.length());
                    Set<JLabel> keySet = labelsAndComps.keySet();
                    Iterator<JLabel> iter = keySet.iterator();
                    while (iter.hasNext()) {
                        final JLabel label = (JLabel) iter.next();
                        final JComponent comp = labelsAndComps.get(label);
                        key = label.getName();

                        if (comp instanceof JTextField) {
                            value = ((JTextField) comp).getText().trim();
                        } else if (comp instanceof JComboBox) {
                            value = (String) ( ((JComboBox) comp).getSelectedItem());
                        }
                        if (key.equalsIgnoreCase(name)) {
                            tokens[i] = value;
                            break;
                        }
                    }
                }
            }

            boolean testCondition = false;
            String intersect = "";
            for (int i = 0; i < tokens.length;) {
                boolean t = false;
                String op1 = tokens[i];
                String oper = tokens[i + 1];
                String op2 = tokens[i + 2];

                if (isNumber(op1)) {
                    t = evaluateNumberExpression(op1, oper, op2);

                } else {
                    t = evaluateStringExpression(op1, oper, op2);

                }

                if (intersect.equals("")) {
                    testCondition = t;
                } else {
                    if (intersect.equalsIgnoreCase("AND")) {
                        if (testCondition && t) {
                            testCondition = true;
                        } else {
                            testCondition = false;
                        }
                    } else if (intersect.equalsIgnoreCase("OR")) {
                        if (testCondition || t) {
                            testCondition = true;
                        } else {
                            testCondition = false;
                        }
                    }
                }

                if (i + 3 < tokens.length) {
                    intersect = tokens[i + 3];
                }

                i = i + 4;

            }

            return testCondition;
        }

        /**
         * validates fields
         * 
         * @param ds2
         * @param imageFile
         * @param errs
         */
		public void parseDataStructForValidation(final DataStruct ds2, final ArrayList<String> errs,
                final TreeMap<JLabel, JComponent> labelsAndComps) {

            String value = "";
            String key = "";
            String labelText = "";
            String required = "";
            String valuerange = "";
            String type = "";
            String size = "";
            boolean found = false;
            String condition = "";
            JLabel label = null;
            for (int k = 0; k < ds2.size(); k++) {
                final Object o1 = ds2.get(k);
                if (o1 instanceof DataElement) {
                    // data element
                    final DataElement de = (DataElement) o1;
                    final String name = de.getName();

                    // need to get appropriat value
                    Set<JLabel> keySet = labelsAndComps.keySet();
                    Iterator<JLabel> iter = keySet.iterator();
                    while (iter.hasNext()) {
                        label = (JLabel) iter.next();
                        JComponent comp = labelsAndComps.get(label);
                        key = label.getName();
                        labelText = label.getText();
                        if (comp instanceof JTextField) {
                            value = ((JTextField) comp).getText().trim();
                        } else if (comp instanceof JComboBox) {
                            value = (String) ( ((JComboBox) comp).getSelectedItem());
                        }
                        if (key.equalsIgnoreCase(name)) {
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        // now we need to validate
                        required = de.getRequired();
                        type = de.getType();
                        size = de.getSize();

                        valuerange = de.getValuerange();

                        if (required.equalsIgnoreCase("Required")) {
                            if (value.trim().equalsIgnoreCase("")) {
                                errs.add(labelText + " is a required field");
                            } else {
                                if (key.equalsIgnoreCase(GUID_ELEMENT_NAME)) {
                                    if ( !isGuid(value.trim())) {
                                        errs.add(labelText + " must begin with a valid GUID prefix");
                                    }
                                }
                            }
                        }
                        if (required.equalsIgnoreCase("Conditional")) {

                            condition = de.getCondition();

                            condition = condition.replaceAll("<=", " <= ");
                            condition = condition.replaceAll(">=", " >= ");
                            condition = condition.replaceAll("!=", " != ");
                            condition = condition.replaceAll("AND", " AND ");
                            condition = condition.replaceAll("OR", " OR ");

                            String[] tokens = condition.split("\\s+");
                            String s2 = "";
                            for (int i = 0; i < tokens.length; i++) {
                                String token = tokens[i];
                                if ( !token.equals("<=") && !token.equals(">=") && !token.equals("!=")) {
                                    token = token.replaceAll("=", " = ");

                                }
                                s2 = s2 + token + " ";

                            }

                            String[] tokens2 = s2.split("\\s+");

                            boolean test = testCondition(tokens2);

                            if (test == true) {
                                label.setForeground(Color.red);
                            } else {
                                label.setForeground(Color.black);
                            }
                            if (test == true && value.trim().equals("")) {
                                errs.add(labelText + " is a required field");
                            }

                        }

                        if (type.equalsIgnoreCase("Integer")) {
                            if ( !value.trim().equalsIgnoreCase("")) {
                                try {
                                    final int intValue = Integer.valueOf(value.trim()).intValue();
                                    if (valuerange != null && valuerange.contains("+")) {
                                        // test int if its in valuerange
                                        final int min = Integer.valueOf(
                                                valuerange.substring(0, valuerange.indexOf("+")).trim()).intValue();
                                        // I think that 0 should be included in allowed range
                                        // if (min == 0) {
                                        // if (intValue <= min) {
                                        // errs.add(labelText + " must be greater than 0");
                                        // }
                                        // } else {
                                        if (intValue < min) {
                                            errs.add(labelText + " must be greater than or equal to " + min);
                                        }
                                        // }

                                    } else if (valuerange != null && valuerange.contains(" to ")) {
                                        final int min = Integer.valueOf(
                                                valuerange.substring(0, valuerange.indexOf(" to ")).trim()).intValue();
                                        final int max = Integer.valueOf(
                                                valuerange.substring(valuerange.indexOf(" to ") + 4,
                                                        valuerange.length()).trim()).intValue();
                                        if (intValue < min || intValue > max) {
                                            errs.add(labelText + " must be in the range of " + min + " to " + max);
                                        }
                                    }
                                } catch (final NumberFormatException e) {
                                    errs.add(labelText + " must be an Integer");
                                }
                            }
                        }
                        if (type.equalsIgnoreCase("Float")) {
                            if ( !value.trim().equalsIgnoreCase("")) {
                                try {
                                    final float floatValue = Float.valueOf(value.trim()).floatValue();
                                    if (valuerange != null && valuerange.contains("+")) {
                                        // test int if its in valuerange
                                        final float min = Float.valueOf(
                                                valuerange.substring(0, valuerange.indexOf("+")).trim()).floatValue();
                                        // I think that 0 should be included in allowed range
                                        // if (min == 0) {
                                        // if (floatValue <= min) {
                                        // errs.add(labelText + " must be greater than 0");
                                        // }
                                        // } else {
                                        if (floatValue < min) {
                                            errs.add(labelText + " must be greater than or equal to " + min);
                                        }
                                        // }
                                    } else if (valuerange != null && valuerange.contains(" to ")) {
                                        final float min = Float.valueOf(
                                                valuerange.substring(0, valuerange.indexOf(" to ")).trim())
                                                .floatValue();
                                        final float max = Float.valueOf(
                                                valuerange.substring(valuerange.indexOf(" to ") + 4,
                                                        valuerange.length()).trim()).floatValue();
                                        if (floatValue < min || floatValue > max) {
                                            errs.add(labelText + " must be in the range of " + min + " to " + max);
                                        }
                                    }

                                } catch (final NumberFormatException e) {
                                    errs.add(labelText + " must be an Float");
                                }
                            }
                        }
                        if ( !size.equalsIgnoreCase("") && !size.equalsIgnoreCase("0")) {
                            final int intValue = Integer.valueOf(size.trim()).intValue();
                            if ( !value.trim().equalsIgnoreCase("")) {
                                if (value.length() > intValue) {
                                    errs.add(labelText + " must not exceed " + intValue + " in length");
                                }
                            }

                        }
                        found = false;
                    }
                }
            }
        }

        /**
         * called after validation is done
         */
		public void complete(final TreeMap<JLabel, JComponent> labelsAndComps, final String dataStructShortname,
                final boolean isComplete) {
            // System.out.println("in complete");
            final LinkedHashMap<String, String> infoMap = new LinkedHashMap<String, String>();
            String value = "";
            final Set<JLabel> keySet = labelsAndComps.keySet();
            final Iterator<JLabel> iter = keySet.iterator();
            while (iter.hasNext()) {
                final JLabel label = (JLabel) iter.next();
                final JComponent comp = labelsAndComps.get(label);
                // TODO: hardcoded element handling
                if (label.getName().equalsIgnoreCase(GUID_ELEMENT_NAME)) {
                    guid = ((JTextField) comp).getText().trim();
                }
                final String key = label.getName();
                if (comp instanceof JTextField) {
                    value = ((JTextField) comp).getText().trim();
                    // ok...all files will go into the allOtherFiles AL

                    final File f = new File(value);
                    if (f.isFile()) {
                        allOtherFiles.add(f);
                    }

                } else if (comp instanceof JComboBox) {
                    value = (String) ( ((JComboBox) comp).getSelectedItem());
                }
                /*
                 * if(!value.equals("")) { System.out.println("the key is " + key); System.out.println("the value is " +
                 * value); }
                 */
                infoMap.put(key, value);
            }

//            boolean guidKnown = true;
//            if (guid != null && !guid.trim().equalsIgnoreCase("")) {
//                guidKnown = false;
//            }

            String name = "";

            if (guid != null && !guid.trim().equalsIgnoreCase("")) {
                name = dataStructShortname + "_" + guid;
            } else {
                name = dataStructShortname + "_UNKNOWNGUID";
            }

            if (launchedFromInProcessState) {
                final int selectedRow = sourceTable.getSelectedRow();

                sourceTableModel.setValueAt(name, selectedRow, 0);
                if (isComplete) {
                    sourceTableModel.setValueAt("Yes", selectedRow, 1);
                } else {
                    sourceTableModel.setValueAt("No", selectedRow, 1);
                }

                infoList.set(selectedRow, infoMap);

                allOtherFilesAL.set(selectedRow, allOtherFiles);

            } else {
                if ( !addedPreviewImage) {
                    previewPanel.removeAll();
                    previewPanel.repaint();
                }

                infoList.set(infoList.size() - 1, infoMap);
                // infoTable.put(k, infoMap);
                final Vector<String> rowData = new Vector<String>();
                rowData.add(name);
                if (isComplete) {
                    rowData.add("Yes");
                } else {
                    rowData.add("No");
                }
                sourceTableModel.addRow(rowData);
                sourceTable.setRowSelectionInterval(sourceTableModel.getRowCount() - 1,
                        sourceTableModel.getRowCount() - 1);

                allOtherFilesAL.set(allOtherFilesAL.size() - 1, allOtherFiles);
            }
        }

        public void windowActivated(final WindowEvent e) {}

        public void windowClosed(final WindowEvent e) {}

        public void windowClosing(final WindowEvent e) {
            enableDisableFinishButton();
        }

        public void windowDeactivated(final WindowEvent e) {}

        public void windowDeiconified(final WindowEvent e) {}

        public void windowIconified(final WindowEvent e) {}

        public void windowOpened(final WindowEvent e) {}

        /**
         * This inner class is used to sort the list by instance number
         */
        private class JLabelComparator implements Comparator<JLabel> {
            public int compare(final JLabel lA, final JLabel lB) {
                final String aText = lA.getName();
                final String bText = lB.getName();
                return aText.compareTo(bText);
            }
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            validateFields();

        }

        @Override
        public void focusGained(FocusEvent e) {}

        @Override
        public void focusLost(FocusEvent e) {
            validateFields();
        }

    }

    /**
     * represents the DataStructure of the xml
     */
    public class DataStruct extends Vector<DataElement> {
        /**
		 * 
		 */
		private static final long serialVersionUID = -2562266763497985432L;

		private final String name;

        private String shortname;

        private String desc;

        private final String version;

        private String type;

        public DataStruct(final String name, final String version) {
            super();
            this.name = name;
            this.version = version;
        }

        public DataStruct(final String name, final String shortname, final String desc, final String version,
                final String type) {
            super();
            this.name = name;
            this.shortname = shortname;
            this.desc = desc;
            this.version = version;
            this.shortname = shortname;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getShortname() {
            return shortname;
        }

        public String getType() {
            return type;
        }

        public String getDesc() {
            return desc;
        }

    }

    /**
     * represents the DataElement of the XML
     * 
     * @author pandyan
     * 
     */
    public class DataElement {
        private final String name;
        
        private final String title;

        private final String desc;

        private final String shortDesc;
        
        private final String notes;

        private final String type;

        private final String size;

        private final String required;

        private final String valuerange;

        private final String condition;
        
        private final String guidelines;
        
        private final String group;
        
        private final int position;

        private final String parentDataStruct;

        private final String parentDataStructShortname;

        public DataElement(final String name, final String title, final String desc, final String shortDesc,
        		final String notes, final String type, final String size, final String required, final String valuerange,
        		final String condition, final String guidelines, final String group, final int position,
                final String parentDataStruct, final String parentDataStructShortname) {
            this.name = name;
            this.title = title;
            this.desc = desc;
            this.shortDesc = shortDesc;
            this.notes = notes;
            this.type = type;
            this.size = size;
            this.required = required;
            this.valuerange = valuerange;
            this.condition = condition;
            this.guidelines = guidelines;
            this.group = group;
            this.position = position;
            this.parentDataStruct = parentDataStruct;
            this.parentDataStructShortname = parentDataStructShortname;
        }

        public String getName() {
            return name;
        }
        
        public String getTitle() {
        	return title;
        }

        public String getType() {
            return type;
        }

        public String getSize() {
            return size;
        }

        public String getCondition() {
            return condition;
        }

        public String getRequired() {
            return required;
        }

        public String getValuerange() {
            return valuerange;
        }

        public String getDesc() {
            return desc;
        }

        public String getShortDesc() {
            return shortDesc;
        }
        
        public String getGuidelines() {
            return guidelines;
        }
        
        public String getNotes() {
            return notes;
        }
        
        public String getGroup() {
        	return group;
        }
        
        public int getPosition() {
        	return position;
        }

        public String getParentDataStruct() {
            return parentDataStruct;
        }

        public String getParentDataStructShortname() {
            return parentDataStructShortname;
        }
    }

    /**
     * Class that connects to FITBIR data dictionary web service
     * 
     * @author pandyan
     * 
     */
    public class WebServiceThread extends Thread {

        PlugInDialogFITBIR dial;

        WebServiceThread(final PlugInDialogFITBIR dial) {
            super();
            this.dial = dial;
        }

        public void run() {

            try {
                progressBar = new ViewJProgressBar("BRICS", "Connecting to BRICS data dictionary web service...", 0,
                        100, true);
                progressBar.setVisible(true);
                progressBar.updateValue(20);
                
                // try to read the server config from disk, if it is there.  otherwise the value set above at initialization is used.
                readConfig();

                dictionaryProvider = new DictionaryProvider(ddServerURL, authServerURL);

                dataStructureList = dictionaryProvider.getDataStructures();

                progressBar.updateValue(80);

                progressBar.updateValue(100);
                progressBar.setVisible(false);
                progressBar.dispose();
                printlnToLog("Successful connection to BRICS data dictionary web service");

                addSourceButton.setEnabled(true);
                loadCSVButton.setEnabled(true);
            } catch (final Exception e) {
                e.printStackTrace();
                if (progressBar != null) {
                    progressBar.setVisible(false);
                    progressBar.dispose();
                    MipavUtil.displayError("Error in connecting to web service");
                    dial.dispose();
                }
            }
        }
    }
}
