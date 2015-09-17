/*
 * Modified from https://community.oracle.com/thread/1356196
 */
package dreaam.developer.dndtree;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.dnd.*;

public class DndTree extends JTree {

    Insets autoscrollInsets = new Insets(20, 20, 20, 20);

    public DndTree(DefaultMutableTreeNode root) {
        setAutoscrolls(true);
        DefaultTreeModel treemodel = new DefaultTreeModel(root);
        setModel(treemodel);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setEditable(false);
        // Maybe add in clone & SM copy support via DND laterlater
//          new DefaultTreeTransferHandler(this, DnDConstants.ACTION_COPY_OR_MOVE);
        new DefaultTreeTransferHandler(this, DnDConstants.ACTION_MOVE);
    }

    public void autoscroll(Point cursorLocation) {
        Insets insets = getAutoscrollInsets();
        Rectangle outer = getVisibleRect();
        Rectangle inner = new Rectangle(outer.x + insets.left, outer.y + insets.top, outer.width - (insets.left + insets.right), outer.height - (insets.top + insets.bottom));
        if (!inner.contains(cursorLocation)) {
            Rectangle scrollRect = new Rectangle(cursorLocation.x - insets.left, cursorLocation.y - insets.top, insets.left + insets.right, insets.top + insets.bottom);
            scrollRectToVisible(scrollRect);
        }
    }

    public Insets getAutoscrollInsets() {
        return (autoscrollInsets);
    }

    public static DefaultMutableTreeNode makeDeepCopy(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(node.getUserObject());
        for (Enumeration e = node.children(); e.hasMoreElements();) {
            copy.add(makeDeepCopy((DefaultMutableTreeNode) e.nextElement()));
        }
        return (copy);
    }
}
