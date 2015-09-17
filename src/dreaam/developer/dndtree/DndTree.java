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

     Insets autoscrollInsets = new Insets(20, 20, 20, 20); // insets

     public DndTree(DefaultMutableTreeNode root) {
          setAutoscrolls(true);
          DefaultTreeModel treemodel = new  DefaultTreeModel(root);
          setModel(treemodel);
//          setRootVisible(true); 
          setShowsRootHandles(false);//to show the root icon
          getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); //set single selection for the Tree
          setEditable(false);
          new DefaultTreeTransferHandler(this, DnDConstants.ACTION_MOVE);
//          new DefaultTreeTransferHandler(this, DnDConstants.ACTION_COPY_OR_MOVE);
     }

     public void autoscroll(Point cursorLocation)  {
          Insets insets = getAutoscrollInsets();
          Rectangle outer = getVisibleRect();
          Rectangle inner = new Rectangle(outer.x+insets.left, outer.y+insets.top, outer.width-(insets.left+insets.right), outer.height-(insets.top+insets.bottom));
          if (!inner.contains(cursorLocation))  {
               Rectangle scrollRect = new Rectangle(cursorLocation.x-insets.left, cursorLocation.y-insets.top,     insets.left+insets.right, insets.top+insets.bottom);
               scrollRectToVisible(scrollRect);
          }
     }

     public Insets getAutoscrollInsets()  {
          return (autoscrollInsets);
     }

     public static DefaultMutableTreeNode makeDeepCopy(DefaultMutableTreeNode node) {
          DefaultMutableTreeNode copy = new DefaultMutableTreeNode(node.getUserObject());
          for (Enumeration e = node.children(); e.hasMoreElements();) {     
               copy.add(makeDeepCopy((DefaultMutableTreeNode)e.nextElement()));
          }
          return(copy);
     }

     public static DefaultMutableTreeNode createTree() {
          DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
          DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("node1");
          DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("node2");     
          root.add(node1);
          root.add(node2);
          node1.add(new DefaultMutableTreeNode("sub1_1"));               
          node1.add(new DefaultMutableTreeNode("sub1_2"));          
          node1.add(new DefaultMutableTreeNode("sub1_3"));
          node2.add(new DefaultMutableTreeNode("sub2_1"));               
          node2.add(new DefaultMutableTreeNode("sub2_2"));          
          node2.add(new DefaultMutableTreeNode("sub2_3"));
          return(root);
     }
     
     public static void main(String[] args) { 
          try {
               UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
               JFrame frame = new JFrame();
               Container contentPane  = frame.getContentPane();
               contentPane.setLayout(new GridLayout(1,2));
               DefaultMutableTreeNode root1 = DndTree.createTree();
               DndTree tree1 = new DndTree(root1);
               DefaultMutableTreeNode root2 = DndTree.createTree();
               DndTree tree2 = new DndTree(root2);
               contentPane.add(new JScrollPane(tree1));
               contentPane.add(new JScrollPane(tree2));
               frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
               frame.setSize(400,400);
               frame.setVisible(true);
          } 
          catch (Exception e) {
               System.out.println(e);
          }
     }
}