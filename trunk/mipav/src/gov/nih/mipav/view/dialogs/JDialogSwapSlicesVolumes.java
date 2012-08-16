package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.algorithms.utilities.*;
import gov.nih.mipav.model.scripting.*;
import gov.nih.mipav.model.scripting.parameters.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;

import java.io.IOException;
import java.util.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;


/**
 * Creates the dialog for swapping slices/volumes. Allows 3D or 4D images.
 *
 * @author   Justin Senseney
 * @version  v1 2012
 */
public class JDialogSwapSlicesVolumes extends JDialogScriptableBase implements AlgorithmInterface {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    public enum SwapMode {
        ThreeD("Slice", 2),
        FourD("Volume", 3);
        
        private String title;
        private int dimLoc;

        SwapMode(String title, int dimLoc) {
            this.title = title;
            this.dimLoc = dimLoc;
        }
        
        public String getTitle() {
            return title;
        }
        
        public int getDim() {
            return dimLoc;
        }
    }
    
    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Running algorithm */
    private AlgorithmSwapSlicesVolume swapAlgo;

    /** Swap mode, either 3D or 4D */
    private SwapMode mode;
    
    /** Number of slices in mode */
    private int nSlices; // number of slices in image
    
    /** srcImage for keeping track of slices */
    private ModelImage srcImage;

    /** Result image */
    private ModelImage swapVolume;
    
    /** Reordering of slices/volumes */
    private int[][] sliceRenum;

    private TableTransferImporter importer;

    /** The visible JTable for slice/volume reordering */
    private JTable displayTable;
    
    /** Model to restore last model of JTable */
    private DefaultTableModel undoModel = null;

    /** Radio buttons for alg output options. */
    private JRadioButton newImage, replaceImage;
    
    /** Whether to perform alg in place */
    private boolean inPlace;

    /** Button for appending selected rows to end of JTable */
    private JButton appendButton;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Empty constructor needed for dynamic instantiation (used during scripting).
     */
    public JDialogSwapSlicesVolumes() { }

    /**
     * Creates new dialog for removing slices.
     *
     * @param  theParentFrame  Parent frame
     * @param  im              Source image
     */
    public JDialogSwapSlicesVolumes(Frame theParentFrame, ModelImage im, SwapMode mode) {
        super(theParentFrame, false);
        this.srcImage = im;
        this.mode = mode;
        init();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Closes dialog box when the OK button is pressed and calls the algorithm.
     *
     * @param  event  Event that triggers function
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        if (command.equals("Swap")) {

            if (setVariables()) {
                callAlgorithm();
            }
        } else if (command.equals("Reset")) {
            undoModel = buildUndoTableModel(false);
            displayTable.setModel(buildTableModel());
        } else if (command.equals("Help")) {
            MipavUtil.showHelp(null);
        } else if (command.equals("Append")) {
            displayTable.clearSelection();
            Action paste = TableTransferImporter.getPasteAction();
            paste.actionPerformed(new ActionEvent(displayTable, ActionEvent.ACTION_PERFORMED, "Paste"));
        } else if (command.equals("Undo")) {
            if(undoModel != null) {
                DefaultTableModel tempUndoModel = buildUndoTableModel(true);
                displayTable.setModel(undoModel);
                undoModel = tempUndoModel;
            }
            helpButton.setText("Redo");
            helpButton.setMnemonic(KeyEvent.VK_R);
        } else if(command.equals("Redo")) {
            if(undoModel != null) {
                DefaultTableModel tempUndoModel = buildUndoTableModel(false);
                displayTable.setModel(undoModel);
                undoModel = tempUndoModel;
            }
            helpButton.setText("Undo");
            helpButton.setMnemonic(KeyEvent.VK_U);
        }
    }

    // ************************************************************************
    // ************************** Algorithm Events ****************************
    // ************************************************************************

    /**
     * This method is required if the AlgorithmPerformed interface is implemented. It is called by the algorithms when
     * it has completed or failed to to complete, so that the dialog can be display the result image and/or clean up.
     *
     * @param  algorithm  Algorithm that caused the event.
     */
    public void algorithmPerformed(AlgorithmBase algorithm) {

        if (algorithm instanceof AlgorithmSwapSlicesVolume) {
            if(!inPlace) {
                swapVolume = swapAlgo.getSwappedVolume();
    
                new ViewJFrameImage(swapVolume);
            }

            if (algorithm.isCompleted()) {
                insertScriptLine();
            }
        } // if ( algorithm instanceof AlgorithmExtractSlicesVolumes )

        algorithm.finalize();
        algorithm = null;
        dispose();
    }

    /**
     * Once all the necessary variables are set, call the Remove Slices algorithm based on what type of image this is
     * and whether or not there is a separate destination image.
     */
    protected void callAlgorithm() {
        System.gc();

        ModelImage destImage;
        
        if(inPlace) {
            destImage = srcImage;
        } else {
            int extent = 0;
            for(int i=0; i<sliceRenum.length; i++) {
                extent += sliceRenum[i].length;
            }
            
            int[] extents = Arrays.copyOf(srcImage.getExtents(), srcImage.getExtents().length);
            
            extents[mode.getDim()] = extent;
            
            destImage = new ModelImage(srcImage.getDataType(), extents, srcImage.getImageName()+"_swap");
        }
        
        swapAlgo = new AlgorithmSwapSlicesVolume(destImage, mode, sliceRenum, srcImage);
        swapAlgo.addListener(this);

        createProgressBar(srcImage.getImageName(), swapAlgo);

        setVisible(false); // Hide dialog

        if (isRunInSeparateThread()) {

            // Start the thread as a low priority because we wish to still have user interface work fast.
            if (swapAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                MipavUtil.displayError("A thread is already running on this object");
            }
        } else {
            swapAlgo.run();
        }
    }

    /**
     * Store the result image in the script runner's image table now that the action execution is finished.
     */
    protected void doPostAlgorithmActions() {
        AlgorithmParameters.storeImageInRunner(swapVolume);
    }

    /**
     * {@inheritDoc}
     */
    protected void setGUIFromParams() {
        srcImage = scriptParameters.retrieveInputImage();
        parentFrame = srcImage.getParentFrame();

        if (srcImage.getNDims() < 3) {
            throw new ParameterException(AlgorithmParameters.getInputImageLabel(1), "3D or 4D image required.");
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void storeParamsFromGUI() throws ParserException {
        scriptParameters.storeInputImage(srcImage);
        scriptParameters.storeImageInRecorder(swapVolume);

        scriptParameters.getParams().put(ParameterFactory.newParameter("slices",
                                                                       nSlices));
    }

    /**
     * Sets up the GUI (panels, buttons, etc) and displays it on the screen.
     */
    private void init() {
        
        setFocusable(true);
        
        // make sure that this is a 3D image first
        // make sure this image, im, is not 2D, for removing an image's only slice makes no sense...
        if ((srcImage.getNDims() == 2) || (srcImage.getExtents()[2] == 1)) {
            MipavUtil.displayError("Extract Individual Slices does not make sense for single-slice (2-D)\n" +
                                   "images.  No operation may be performed.");

            return; // the wrong kind of image gets sent back before wasting anymore time.
        }
        
        nSlices = srcImage.getExtents()[mode.getDim()];

        JPanel mainPanel = new JPanel(new BorderLayout()); // everything gets placed on this panel
        
        setTitle("Swap "+mode.getTitle());
        setForeground(Color.black);
        
        GuiBuilder gui = new GuiBuilder(this);
        
        JLabel dir = new JLabel("<html>Use the keyboard and mouse to select the rows you would like to swap.  You can drag, drop, cut, copy, and paste.</html>");
        
        dir.setFont(MipavUtil.font12);

        mainPanel.add(dir, BorderLayout.NORTH);
        
        DefaultTableModel d = buildTableModel();
        
        displayTable = new JTable(d);
        
        displayTable.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent key) {
              
                switch(key.getKeyCode()) {
               
                case KeyEvent.VK_DELETE:
                case KeyEvent.VK_BACK_SPACE:
                    doDeleteRows(displayTable);
                }
            }

            public void keyReleased(KeyEvent key) {}

            public void keyTyped(KeyEvent key) {}
            
        });
        
        displayTable.setRequestFocusEnabled(true);
        displayTable.setFocusable(true);
        displayTable.setDragEnabled(true);
        displayTable.setDropMode(DropMode.INSERT_ROWS);
        importer = new TableTransferImporter(displayTable);
        displayTable.setTransferHandler(importer);
        
        JPanel algOptionPanel = new JPanel();
        algOptionPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = .9;
        gbc.fill = GridBagConstraints.BOTH;
        
        JScrollPane scroll = new JScrollPane(displayTable);
        scroll.setPreferredSize(new Dimension(340, 450));
        algOptionPanel.add(scroll, gbc);
        
        // destination goes in the left of the lower box
        JPanel destinationPanel = new JPanel();
        destinationPanel.setLayout(new BoxLayout(destinationPanel, BoxLayout.Y_AXIS));

        destinationPanel.setForeground(Color.black);
        destinationPanel.setBorder(buildTitledBorder("Destination"));

        ButtonGroup destinationGroup = new ButtonGroup();
        newImage = new JRadioButton("New image", false);
        newImage.setFont(serif12);
        destinationGroup.add(newImage); // add the button to the grouping
        destinationPanel.add(newImage); // add the button to the component

        replaceImage = new JRadioButton("Replace image", true);
        replaceImage.setFont(serif12);
        destinationGroup.add(replaceImage); // add the button to the grouping
        destinationPanel.add(replaceImage); // add the button to the component

        gbc.gridy++;
        gbc.weighty = .1;
        algOptionPanel.add(destinationPanel, gbc);

        // Only if the image is unlocked can it be replaced.
        if (srcImage.getLockStatus() == ModelStorageBase.UNLOCKED) {
            replaceImage.setEnabled(true);
        } else {
            replaceImage.setEnabled(false);
        }
        
        mainPanel.add(algOptionPanel, BorderLayout.CENTER);
        JPanel buttonPanel = buildButtons();
        OKButton.setText("Swap");
        OKButton.setMnemonic(KeyEvent.VK_S);
        cancelButton.setText("Reset");
        cancelButton.setMnemonic(KeyEvent.VK_R);
        helpButton.setText("Undo");
        helpButton.setMnemonic(KeyEvent.VK_U);
        appendButton = gui.buildButton("Append");
        appendButton.setMnemonic(KeyEvent.VK_A);
        appendButton.setToolTipText("Appends copied "+mode.getTitle().toLowerCase()+"s to end of image");
        appendButton.addActionListener(this);
        buttonPanel.add(appendButton);
        
        ActionKeyListener key = new ActionKeyListener();
        displayTable.addKeyListener(key);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        getContentPane().add(mainPanel);
        pack();
        setSize(450, 500);
        setVisible(true); 

    }
    
    private class ActionKeyListener implements KeyListener {

        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode() == OKButton.getMnemonic()) {
                OKButton.doClick();
            } else if(e.getKeyCode() == cancelButton.getMnemonic()) {
                cancelButton.doClick();
            } else if(e.getKeyCode() == helpButton.getMnemonic()) {
                helpButton.doClick();
            } else if(e.getKeyCode() == appendButton.getMnemonic()) {
                appendButton.doClick();
            }
            
            System.out.println("Here1");
        }

        @Override
        public void keyReleased(KeyEvent e) {
            System.out.println("Here");
            
        }

        @Override
        public void keyTyped(KeyEvent e) {
            System.out.println("Here2");
        }
        
    }
    
    private DefaultTableModel buildBasicTableModel() {
        String[] columnName = new String[]{"Index", mode.getTitle()};
        
        DefaultTableModel d = new DefaultTableModel() {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        d.setColumnCount(2);
        d.setColumnIdentifiers(columnName);
        
        return d;
    }
    
    private DefaultTableModel buildTableModel() {
        DefaultTableModel d = buildBasicTableModel();
        
        for(int i=0; i<nSlices; i++) {
            Vector<String> v = new Vector<String>();
            v.add(String.valueOf("Position "+i));
            v.add(mode.getTitle()+" "+i);
            
            d.addRow(v);
        }
        
        return d;
    }

    /**
     * Removes the currently selected rows from table.
     * 
     * @param table the currently used JTable
     */
    private void doDeleteRows(JTable table) {
        undoModel = buildUndoTableModel(false);
        
        DefaultTableModel model = ((DefaultTableModel)table.getModel());
        int[] rows = table.getSelectedRows();
        
        if(rows.length > 0) {
            
            for(int i=0; i<rows.length; i++) {
                if(rows[i] < table.getRowCount()-1) {
                    table.setValueAt(table.getValueAt(rows[i]-i+1, 1), rows[i]-i, 1);

                    for(int j=rows[i]-i+1; j<table.getRowCount()-1; j++) {
                        table.setValueAt(table.getValueAt(j+1, 1), j, 1);
                    }
                }
            }
            
            for(int i=0; i<rows.length; i++) {
                model.removeRow(table.getRowCount()-1);
            }
        }
        
        table.clearSelection();
    }
    
    private DefaultTableModel buildUndoTableModel(boolean undoSource) {
        if(!undoSource) {
            helpButton.setText("Undo");
            helpButton.setMnemonic(KeyEvent.VK_U);
        }
        
        DefaultTableModel undoModel = buildBasicTableModel();
        
        undoModel.setColumnCount(displayTable.getModel().getColumnCount());
        undoModel.setRowCount(displayTable.getModel().getRowCount());
        
        for(int i=0; i<displayTable.getModel().getColumnCount(); i++) {
            for(int j=0; j<displayTable.getModel().getRowCount(); j++) {
                undoModel.setValueAt(displayTable.getValueAt(j, i), j, i);
            }
        }
        
        return undoModel;
    }
    
    /**
     * Imports by cut/copy/paste/drag/drop slice elements from part of parent JTable 
     * to another part of the JTable.
     * 
     * @author senseneyj
     */
    private class TableTransferImporter extends TransferHandler implements ClipboardOwner {

        private JTable parent;
        
        public TableTransferImporter(JTable parent) {
            this.parent = parent;
        }
        
        /* (non-Javadoc)
         * @see javax.swing.TransferHandler#canImport(javax.swing.JComponent, java.awt.datatransfer.DataFlavor[])
         */
        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            if(comp.equals(parent)) {
                for(int i=0; i<transferFlavors.length; i++) {
                    if(!transferFlavors[i].equals(DataFlavor.stringFlavor)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        /* (non-Javadoc)
         * @see javax.swing.TransferHandler#createTransferable(javax.swing.JComponent)
         */
        @Override
        protected Transferable createTransferable(JComponent c) {
            if(c.equals(parent)) {
               return new SliceTransferable(TransferHandler.MOVE); 
            }
            
            return null;
        }

        /* (non-Javadoc)
         * @see javax.swing.TransferHandler#exportAsDrag(javax.swing.JComponent, java.awt.event.InputEvent, int)
         */
        @Override
        public void exportAsDrag(JComponent comp, InputEvent e, int action) {
            // TODO Auto-generated method stub
            super.exportAsDrag(comp, e, action);
        }

        /* (non-Javadoc)
         * @see javax.swing.TransferHandler#exportDone(javax.swing.JComponent, java.awt.datatransfer.Transferable, int)
         */
        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            // TODO Auto-generated method stub
            super.exportDone(source, data, action);
        }

        /* (non-Javadoc)
         * @see javax.swing.TransferHandler#exportToClipboard(javax.swing.JComponent, java.awt.datatransfer.Clipboard, int)
         */
        @Override
        public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
            try {
                if(comp.equals(parent)) {
                    SliceTransferable slice = new SliceTransferable(action);
                    clip.setContents(slice, this);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        /* (non-Javadoc)
         * @see javax.swing.TransferHandler#getSourceActions(javax.swing.JComponent)
         */
        @Override
        public int getSourceActions(JComponent c) {
            // TODO Auto-generated method stub
            return super.getSourceActions(c);
        }

        /* (non-Javadoc)
         * @see javax.swing.TransferHandler#importData(javax.swing.JComponent, java.awt.datatransfer.Transferable)
         */
        @Override
        public boolean importData(JComponent comp, Transferable t) {
            try {
                if(comp.equals(parent)) {
                    String insertSlicesStr = t.getTransferData(DataFlavor.stringFlavor).toString();
                    
                    String[] insertSlices = insertSlicesStr.split(",");
                    
                    if(insertSlices.length != -1) {
                        undoModel = buildUndoTableModel(false);
                    }
                    
                    DefaultTableModel table = ((DefaultTableModel)parent.getModel());

                    int insertLoc = parent.getSelectedRow();
                    
                    if(insertLoc == -1) {
                        insertLoc = parent.getRowCount(); // append action
                    }
                    
                    for(int i=0; i<insertSlices.length; i++) {
                        table.addRow(new Object[]{"Position "+table.getRowCount(), ""});
                    }
                    
                    for(int i=table.getRowCount()-1; i>=insertSlices.length && i>insertLoc; i--) {
                        parent.setValueAt(parent.getValueAt(i-insertSlices.length, 1), i, 1);
                    }  
                    
                    for(int i=0; i<insertSlices.length; i++) {
                        parent.setValueAt(insertSlices[i], i+insertLoc, 1);
                    }
                    
//                    parent.clearSelection();
//                    ListSelectionModel model = parent.getSelectionModel();
//                    model.setSelectionInterval(parent.getSelectedRow(), parent.getSelectedRow()+insertSlices.length);
                    
                    return true;
                }
                
                return false;
            } catch(Exception e) {
                e.printStackTrace();
                
                return false;
            }
        }

        /* (non-Javadoc)
         * @see javax.swing.TransferHandler#importData(javax.swing.TransferHandler.TransferSupport)
         */
        @Override
        public boolean importData(TransferSupport support) {
            // TODO Auto-generated method stub
            return super.importData(support);
        }
        
        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {}
        
        /**
         * Describes how the TransferHandler should handle slice transfers.
         * 
         * @author senseneyj
         */
        private class SliceTransferable implements Transferable {

            private String bufferData = new String();
            private int action = TransferHandler.MOVE;
            
            
            public SliceTransferable(int action) {
                this.action = action;
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                
                if(bufferData.length() == 0) {
                    int[] rows =  parent.getSelectedRows();
                    
                    StringBuffer buffer = new StringBuffer();
                    for(int i=0; i<rows.length; i++) {
                        buffer.append(parent.getValueAt(rows[i], 1)).append(',');
                    }
                    
                    if(buffer.length() > 0) {
                        buffer.replace(buffer.length()-1, buffer.length(), "");
                    }
                        
                    if(action == TransferHandler.MOVE) {
                        doDeleteRows(parent);
                    }

                    bufferData = buffer.toString();
                }
                
                return bufferData;
            }

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.stringFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return flavor.equals(DataFlavor.stringFlavor); 
            }
            
        }
        
    }

    /**
     * Use the GUI results to set up the variables needed to run the algorithm.
     *
     * @return  <code>true</code> if parameters set successfully, <code>false</code> otherwise.
     */
    private boolean setVariables() {

        int maxSlice = 0;
        
        for(int i=0; i<displayTable.getRowCount(); i++) {
            int slice = Integer.valueOf(displayTable.getValueAt(i, 1).toString().split(" ")[1]); 
            if(slice > maxSlice) {
                maxSlice = slice;
            }
        }
        
        sliceRenum = new int[maxSlice+1][];
        
        for(int i=0; i<sliceRenum.length; i++) {
            sliceRenum[i] = new int[0];
        }
        
        for(int i=0; i<displayTable.getRowCount(); i++) {
            int slice = Integer.valueOf(displayTable.getValueAt(i, 1).toString().split(" ")[1]);
            
            int[] oldAr = sliceRenum[slice];
            int[] newAr = new int[oldAr.length+1];
            
            for(int j=0; j<oldAr.length; j++) {
                newAr[j] = oldAr[j];
            }
            
            newAr[oldAr.length] = i;
            
            sliceRenum[slice] = newAr;
        }
        
        inPlace = replaceImage.isSelected();
        
        return true;
    }
}
