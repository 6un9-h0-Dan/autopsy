/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011 Basis Technology Corp.
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
package org.sleuthkit.autopsy.directorytree;

import java.awt.event.ActionEvent;
// ATTENTION MR. PROGRAMMER:
// DO NOT COMMIT THIS CODE. IT IS NOT FOR PUBLIC CONSUMPTION
// THANK YOU
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.ingest.IngestDialog;
import org.sleuthkit.autopsy.ingest.IngestManager;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.Directory;
import org.sleuthkit.datamodel.Image;
import org.sleuthkit.datamodel.SleuthkitCase;

/**
 * This class sets the actions for the nodes in the directory tree and creates
 * the children filter so that files and such are hidden from the tree. 
 *
 */
class DirectoryTreeFilterNode extends FilterNode {

    private static final Action collapseAll = new CollapseAction("Collapse All");

    /** the constructor */
    DirectoryTreeFilterNode(Node arg, boolean createChildren) {
        super(arg, DirectoryTreeFilterChildren.createInstance(arg, createChildren),
                new ProxyLookup(Lookups.singleton(new OriginalNode(arg)),
                arg.getLookup()));
    }

    /**
     * Right click action for the nodes in the directory tree.
     *
     * @param popup
     * @return
     */
    @Override
    public Action[] getActions(boolean popup) {
        List<Action> actions = new ArrayList<Action>();

        Content c = this.getLookup().lookup(Content.class);
        if (c != null) {
            actions.addAll(DirectoryTreeFilterNode.getDetailActions(c));
            actions.add(collapseAll);

            Directory dir = this.getLookup().lookup(Directory.class);
            if (dir != null) {
                actions.add(new ExtractAction("Extract Directory",
                        getOriginal()));
            }
            final Image img = this.getLookup().lookup(Image.class);
            if (img != null) {
                actions.add(new AbstractAction("Restart Ingest Modules") {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final IngestDialog ingestDialog = new IngestDialog();
                        ingestDialog.setImage(img);
                        ingestDialog.display();
                    }
                });
            }
        }
        actions.add(new DeleteArtifactsAndAttributes());
        return actions.toArray(new Action[actions.size()]);
    }

    private static List<Action> getDetailActions(Content c) {
        List<Action> actions = new ArrayList<Action>();

        actions.addAll(ShowDetailActionVisitor.getActions(c));

        return actions;
    }

    // ATTENTION MR. PROGRAMMER:
    // DO NOT COMMIT THIS CODE. IT IS NOT FOR PUBLIC CONSUMPTION
    // THANK YOU
    private class DeleteArtifactsAndAttributes extends AbstractAction {

        DeleteArtifactsAndAttributes() {
            super("Delete Artifacts and Attributes");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SleuthkitCase skCase = Case.getCurrentCase().getSleuthkitCase();
            try {
                String query = "delete from blackboard_artifacts";
                skCase.runQuery(query);
            } catch (SQLException ex) {
                Logger.getLogger(DirectoryTreeFilterNode.class.getName()).log(Level.INFO, "No resultset returned from query 1");
            }
            try {
                String query = "delete from blackboard_attributes";
                skCase.runQuery(query);
            } catch (SQLException ex) {
                Logger.getLogger(DirectoryTreeFilterNode.class.getName()).log(Level.INFO, "No resultset returned from query 2");
            }

            IngestManager.fireServiceEvent(IngestManager.SERVICE_HAS_DATA_EVT, NAME);

        }
    }
}

class OriginalNode {

    private Node original;

    OriginalNode(Node original) {
        this.original = original;
    }

    Node getNode() {
        return original;
    }
}