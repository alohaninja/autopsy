/*
 * Autopsy Forensic Browser
 *
 * Copyright 2014 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.modules.interestingitems;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import org.sleuthkit.autopsy.ingest.IngestModuleIngestJobSettings;
import org.sleuthkit.autopsy.ingest.IngestModuleIngestJobSettingsPanel;

/**
 * Ingest job settings panel for interesting files identifier ingest modules.
 */
final class FilesIdentifierIngestJobSettingsPanel extends IngestModuleIngestJobSettingsPanel implements Observer {

    private final FilesSetsTableModel tableModel;

    // This map of interesting interesting files set names to files sets is used 
    // both to sort interesting files sets by name and to detect when a new 
    // files set is defined, so that the new set can be enabled by default.
    private TreeMap<String, FilesSet> filesSetSnapshot;

    /**
     * A factory method that avoids publishing the "this" reference from the
     * constructor.
     *
     * @return An instance of the ingest job settings panel interesting files
     * identifier ingest modules.
     */
    static FilesIdentifierIngestJobSettingsPanel makePanel(FilesIdentifierIngestJobSettings settings) {
        FilesIdentifierIngestJobSettingsPanel panel = new FilesIdentifierIngestJobSettingsPanel(settings);

        // Observe the interesting item definitions manager for changes to the 
        // interesting file set definitions. This is used to keep this panel in
        // synch with changes made using the global settings/option panel for 
        // this module.
        InterestingItemDefsManager.getInstance().addObserver(panel);

        return panel;
    }

    /**
     * Constructs an ingest job settings panel for interesting files identifier
     * ingest modules.
     */
    private FilesIdentifierIngestJobSettingsPanel(FilesIdentifierIngestJobSettings settings) {
        initComponents();

        /* Make a table row object for each interesting files set, bundling the
         * set with an enabled flag. The files sets are loaded into a tree map 
         * so they are sorted by name. The keys set also serves as a cache of 
         * set names so that new sets can be easily detected in the override of 
         * Observer.update().
         */
        List<FilesSetRow> filesSetRows = new ArrayList<>();
        this.filesSetSnapshot = new TreeMap<>(InterestingItemDefsManager.getInstance().getInterestingFilesSets());
        for (FilesSet set : this.filesSetSnapshot.values()) {
            filesSetRows.add(new FilesSetRow(set, settings.isInterestingFilesSetEnabled(set.getName())));
        }

        // Make a table model to manage the row objects.
        this.tableModel = new FilesSetsTableModel(filesSetRows);

        // Set up the table component that presents the table model that allows 
        // users to enable and disable interesting files set definitions for an 
        // ingest job.
        this.filesSetTable.setModel(tableModel);
        this.filesSetTable.setTableHeader(null);
        this.filesSetTable.setRowSelectionAllowed(false);
        final int width = this.filesSetScrollPane.getPreferredSize().width;
        this.filesSetTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        TableColumn column;
        for (int i = 0; i < this.filesSetTable.getColumnCount(); i++) {
            column = this.filesSetTable.getColumnModel().getColumn(i);
            if (i == 0) {
                column.setPreferredWidth(((int) (width * 0.07)));
            } else {
                column.setPreferredWidth(((int) (width * 0.92)));
            }
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public IngestModuleIngestJobSettings getSettings() {
        List<String> enabledInterestingFilesSets = new ArrayList<>();
        this.tableModel.filesSetRows.stream().filter((rowModel) -> (rowModel.isEnabled())).forEach((rowModel) -> {
            enabledInterestingFilesSets.add(rowModel.getFilesSet().getName());
        });
        return new FilesIdentifierIngestJobSettings(enabledInterestingFilesSets);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void update(Observable o, Object arg
    ) {
        // Get the user's current enabled/disabled settings.
        FilesIdentifierIngestJobSettings settings = (FilesIdentifierIngestJobSettings) this.getSettings();

        // Refresh the view of the interesting files set definitions.
        List<FilesSetRow> rowModels = new ArrayList<>();
        TreeMap<String, FilesSet> newFilesSetSnapshot = new TreeMap<>(InterestingItemDefsManager.getInstance().getInterestingFilesSets());
        for (FilesSet set : newFilesSetSnapshot.values()) {
            if (this.filesSetSnapshot.keySet().contains(set.getName())) {
                // Preserve the current enabled/diabled state of the set.
                rowModels.add(new FilesSetRow(set, settings.isInterestingFilesSetEnabled(set.getName())));
            } else {
                // New sets are enabled by default.
                rowModels.add(new FilesSetRow(set, true));
            }
        }
        this.tableModel.resetTableData(rowModels);

        // Cache the snapshot so it will be avaialble for the next update.
        this.filesSetSnapshot = newFilesSetSnapshot;
    }

    /**
     * Table model for a JTable component that allows users to enable and
     * disable interesting files set definitions for an ingest job.
     */
    private final static class FilesSetsTableModel extends AbstractTableModel {

        private List<FilesSetRow> filesSetRows;

        /**
         * Constructs a table model for a JTable component that allows users to
         * enable and disable interesting files set definitions for an ingest
         * job.
         *
         * @param filesSetRows A collection of row objects that bundles an
         * interesting files set with an enabled flag
         */
        FilesSetsTableModel(List<FilesSetRow> filesSetRows) {
            this.filesSetRows = filesSetRows;
        }

        /**
         * Refreshes the table with a new set of rows.
         *
         * @param filesSetRows A collection of row objects that bundles an
         * interesting files set with an enabled flag
         */
        void resetTableData(List<FilesSetRow> filesSetRows) {
            this.filesSetRows = filesSetRows;
            this.fireTableDataChanged();
        }

        /**
         * @inheritDoc
         */
        @Override
        public int getRowCount() {
            return this.filesSetRows.size();
        }

        /**
         * @inheritDoc
         */
        @Override
        public int getColumnCount() {
            return 2;
        }

        /**
         * @inheritDoc
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return this.filesSetRows.get(rowIndex).isEnabled();
            } else {
                return this.filesSetRows.get(rowIndex).getFilesSet().getName();
            }
        }

        /**
         * @inheritDoc
         */
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return (columnIndex == 0);
        }

        /**
         * @inheritDoc
         */
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                this.filesSetRows.get(rowIndex).setEnabled((Boolean) aValue);
            }
        }

        /**
         * @inheritDoc
         */
        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
    }

    /**
     * Bundles an interesting files set with an enabled flag.
     */
    private final static class FilesSetRow {

        private final FilesSet set;
        private boolean enabled;

        FilesSetRow(FilesSet set, boolean enabled) {
            this.set = set;
            this.enabled = enabled;
        }

        FilesSet getFilesSet() {
            return this.set;
        }

        boolean isEnabled() {
            return this.enabled;
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        filesSetScrollPane = new javax.swing.JScrollPane();
        filesSetTable = new javax.swing.JTable();

        filesSetTable.setBackground(new java.awt.Color(240, 240, 240));
        filesSetTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        filesSetTable.setShowHorizontalLines(false);
        filesSetTable.setShowVerticalLines(false);
        filesSetScrollPane.setViewportView(filesSetTable);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filesSetScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filesSetScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane filesSetScrollPane;
    private javax.swing.JTable filesSetTable;
    // End of variables declaration//GEN-END:variables
}
