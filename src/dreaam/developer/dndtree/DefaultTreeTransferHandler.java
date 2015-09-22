/*
 * Modified from https://community.oracle.com/thread/1356196
 */
package dreaam.developer.dndtree;

import java.awt.*;
import javax.swing.tree.*;
import java.awt.dnd.*;

public class DefaultTreeTransferHandler extends AbstractTreeTransferHandler {

    public DefaultTreeTransferHandler(DndTree tree, int action) {
        super(tree, action, true);
    }

    public boolean canPerformAction(DndTree tree, DefaultMutableTreeNode draggedNode, int action, Point location) {
        TreePath pathTarget = tree.getPathForLocation(location.x, location.y);
        if (pathTarget == null) {
            tree.setSelectionPath(null);
            return (false);
        }
        tree.setSelectionPath(pathTarget);

        // Maybe add in clone & SM copy support via DND laterlater
//          if(action == DnDConstants.ACTION_COPY) {
//               return(true);
//          }
//          else
        if (action == DnDConstants.ACTION_MOVE) {
            DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) pathTarget.getLastPathComponent();
            if (draggedNode.getLevel() <= 1 || draggedNode == targetNode || draggedNode.getParent() != targetNode.getParent()) {
                // draggedNode.getLevel() <= 1: Is root or Plays/Checkers/Helpers/etc folder which we don't want to rearrange
                // draggedNode == targetNode: Is same node, don't need to do anything
                // draggedNode.getParent() != targetNode.getParent(): Not sibling nodes
                return (false);
            } else {
                return (true);
            }
        } else {
            return (false);
        }
    }

    public boolean executeDrop(DndTree target, DefaultMutableTreeNode draggedNode, DefaultMutableTreeNode targetNode, int action) {
        // Maybe add in clone & SM copy support via DND laterlater
//        if (action == DnDConstants.ACTION_COPY) {
//            DefaultMutableTreeNode newNode = target.makeDeepCopy(draggedNode);
//            ((DefaultTreeModel) target.getModel()).insertNodeInto(newNode, targetNode, targetNode.getChildCount());
//            TreePath treePath = new TreePath(newNode.getPath());
//            target.scrollPathToVisible(treePath);
//            target.setSelectionPath(treePath);
//            return (true);
//        }
        if (action == DnDConstants.ACTION_MOVE) {
            draggedNode.removeFromParent();
            int droppedIndex = targetNode.getParent().getIndex(targetNode);
            // Move the draggedNode immediately below the targetNode
            int newIndex = droppedIndex + 1;
            ((DefaultTreeModel) target.getModel()).insertNodeInto(draggedNode, (DefaultMutableTreeNode) targetNode.getParent(), newIndex);
            TreePath treePath = new TreePath(draggedNode.getPath());
            target.scrollPathToVisible(treePath);
            target.setSelectionPath(treePath);
            return (true);
        }
        return (false);
    }
}
