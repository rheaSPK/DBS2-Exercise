package exercise2;

import de.hpi.dbs2.ChosenImplementation;
import de.hpi.dbs2.exercise2.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.stream.IntStream;

/**
 * This is the B+-Tree implementation you will work on.
 * Your task is to implement the insert-operation.
 *
 */
@ChosenImplementation(true)
public class BPlusTreeJava extends AbstractBPlusTree {
    public BPlusTreeJava(int order) {
        super(order);
    }

    public BPlusTreeJava(BPlusTreeNode<?> rootNode) {
        super(rootNode);
    }

    @Nullable
    @Override
    public ValueReference insert(@NotNull Integer key, @NotNull ValueReference value) {
        //throw new UnsupportedOperationException("~~~ your implementation here ~~~");

        // Find LeafNode in which the key has to be inserted
        //   It is a good idea to track the "path" to the LeafNode in a Stack or something alike.
        // Does the key already exist? Overwrite!
        //   leafNode.references[pos] = value;
        //   But remember return the old value!
        // New key - Is there still space?
        //   leafNode.keys[pos] = key;
        //   leafNode.references[pos] = value;
        //   Don't forget to update the parent keys and so on...
        // Otherwise
        //   Split the LeafNode in two!
        //   Is parent node root?
        //     update rootNode = ... // will have only one key
        //   Was node instanceof LeafNode?
        //     update parentNode.keys[?] = ...
        //   Don't forget to update the parent keys and so on...

        // Check out the exercise slides for a flow chart of this logic.
        // If you feel stuck, try to draw what you want to do and
        // check out Ex2Main for playing around with the tree by e.g. printing or debugging it.
        // Also check out all the methods on BPlusTreeNode and how they are implemented or
        // the tests in BPlusTreeNodeTests and BPlusTreeTests!


        Stack<BPlusTreeNode> path = getPathToKey(key);
        LeafNode leaf = (LeafNode) path.pop();

        //key exists?
        if (leaf.getOrNull(key) != null){
            int pos = Arrays.asList(leaf.keys).indexOf(key);
            ValueReference old_value = leaf.references[pos];
            leaf.references[pos] = value;
            return old_value; //what value do they want?
        }

        /*
        When leaf isn't full, you can add the key/value to the leaf, but need to update the parent
        if !leaf.isFull
            const originalLeafKey = leaf.smallestValue
            addToNode(leaf, key, value)
            pop parent
            updateParent(originalLeafKey,  parent)
         */

        if(!leaf.isFull()){
            addToNode(leaf, key, value);
            return value;
        }


        //leaf is full and you have to split it
        /*

        erstellen neuen knoten mit order 1 höher
        füge alle key und value vom leaf in neuen Knoten
        addToNode(newNode, key, value)
        */
        BPlusTreeNode originalNode = leaf;
        BPlusTreeNode currentNode = BPlusTreeNode.buildTree(rootNode.order + 1, leaf.getEntries().toArray());
        ((LeafNode) currentNode).nextSibling = leaf.nextSibling;
        addToNode(currentNode, key, value);
        while (true){
            //split
            BPlusTreeNode[] split = splitNode(currentNode, rootNode.order);
            BPlusTreeNode n1 = split[0];
            BPlusTreeNode n2 = split[1];
            /*
            N Wurzel? (path leer)
            - neuer Knoten (Innernode)
            - N2.getsmallestKey als key setzen
            - wurzel value auf N1 und N2 setzen
             */
            if (currentNode instanceof LeafNode) {
                ((LeafNode) n1).nextSibling = (LeafNode) n2;
            }

            if (path.isEmpty()){
                InnerNode new_root = new InnerNode(rootNode.order);
                new_root.keys[0] = n2.getSmallestKey();
                new_root.references[0] = n1;
                new_root.references[1] = n2;
                this.rootNode = new_root;
                return value;
            }

            /*
             N ist Blatt
            - parent = path.pop
            parent ist nicht voll
            - parent.keys[pos] = smallest_N2;
            - parent.references[pos] = left;
            - parent.references[pos+1] = right;
            - alles nach rechts verschieben
            - n1.nextSibling = n2
            parent ist voll
            - kopiere parent in neuen Knoten mit höherer Order
            - füge wie oben N1 und N2 ein
            - setze für nächsten Schleifendurchlauf N = parent
             */

            BPlusTreeNode parent = path.pop();
            Boolean parentWasFull = false;
            BPlusTreeNode tmpParent = parent;
            if (parent.isFull()){
                // Todo: this doesnt work
                parentWasFull = true;
                // kopiere Parent in Parent mit einer größeren Order
                parent = copyInnerNode((InnerNode) parent, rootNode.order + 1);
            }
            Integer pos = getNodePosition(parent, originalNode);
            if (pos == null){
                throw new Error("node isn't in parent");
            }
            if (currentNode instanceof LeafNode){
                parent.keys[pos] = n2.getSmallestKey();
            } else {
                parent.keys[pos] = getBiggestKey(n1);
            }
            parent.references[pos] = n1;
            // Add keys
            addToPosition(pos + 1, parent, n2);

            if (currentNode instanceof LeafNode){
                for(int i = 0; i < parent.references.length - 1; i++){
                    if(parent.references[i] == null){
                        continue;
                    }
                    ((LeafNode) parent.references[i]).nextSibling = (LeafNode) parent.references[i+1];
                }
            }

            if (!parentWasFull){
                return value;
            }
            currentNode = parent;
            originalNode = tmpParent;

        }
        /*

        ---- Schleife ----
        split N in zwei Knoten mit leaf-order


        N Wurzel? (path leer)
        - neuer Knoten (Innernode)
        - N2.getsmallestKey als key setzen
        - wurzel value auf N1 und N2 setzen

        N ist Blatt
        - parent = path.pop
        parent ist nicht voll
        - parent.keys[pos] = smallest_N2;
        - parent.references[pos] = left;
        - parent.references[pos+1] = right;
        - alles nach rechts verschieben
        - n1.nextSibling = n2
        parent ist voll
        - kopiere parent in neuen Knoten mit höherer Order
        - füge wie oben N1 und N2 ein
        - setze für nächsten Schleifendurchlauf N = parent

        N ist kein Blatt
        - parent = path.pop
        - pos = Position wo altes N war
        - parent.keys[pos] = biggestN1;
        - aus N1 biggest key entfernen
        - parent.references[pos] = left;
        - parent.references[pos+1] = right;
        - alles nach rechts verschieben
          */



        //Todo Initital Root Node
//        BPlusTreeNode currentNode = leaf;
//        Integer overflowKey = key;
//        Object overflowValue = value;
//        while (true){
//            if (!currentNode.isFull()){
//                addToNode(currentNode, overflowKey, overflowValue);
//                return value;
//            }
//            Boolean insertNode1 = insertionPosition(currentNode, overflowKey) < currentNode.n /2;
//            BPlusTreeNode[] split = splitNode(currentNode);
//            BPlusTreeNode node1 = split[0];
//            BPlusTreeNode node2 = split[1];
//            if (insertNode1){
//                addToNode(node1, overflowKey, overflowValue);
//            } else {
//                addToNode(node2, overflowKey, overflowValue);
//            }
//            if (path.isEmpty()){
//                // Wurzelbehandlung
//                break;
//            }
//            BPlusTreeNode parent = path.pop();
//            //Blatt?
//            if (node1 instanceof LeafNode){
//                // wenn einfügbar, dann tu das
//                if (!parent.isFull()){
//                    //replace old key with both new keys
//                    addToNode(parent, node1.)
//                    addToNode(parent, node2.getSmallestKey(), node2);
//                    return value;
//                }
//                overflowKey = node2.getSmallestKey();
//                overflowValue = node2;
//            } else {
//                Integer highestKey = node1.keys[node1.keys.length - 1];
//                if (!parent.isFull()){
//                    addToNode(parent, highestKey, node1);
//                    return value;
//                }
//                overflowKey = highestKey;
//                overflowValue = node1;
//            }
//            currentNode = parent;
//        }
//        // behandlung der Wurzel
        
    }

    // todo thats bs
    private BPlusTreeNode copyInnerNode(InnerNode node, int order){
        return new InnerNode(order, node.references);
    }

    private Integer getBiggestKey(BPlusTreeNode node){
        Integer max = node.keys[0];
        for(Integer key : node.keys){
            if (key == null){
                break;
            }
            if (key > max){
                max = key;
            }
        }
        return max;
    }

    private Integer getNodePosition(BPlusTreeNode originalNode, BPlusTreeNode replaceNode){
        Integer pos = IntStream.range(0, originalNode.references.length)
                .filter(i -> originalNode.references[i] == replaceNode)
                .findFirst()
                .orElse(-1);

        if (pos != -1){
            return pos;
        }
        return null;
    }

    private BPlusTreeNode[] splitNode(BPlusTreeNode node, int order){
        BPlusTreeNode leftNode;
        BPlusTreeNode rightNode;
        if (node instanceof InnerNode){
            leftNode = new InnerNode(order);
            rightNode = new InnerNode(order);
        } else {
            leftNode = new LeafNode(order);
            rightNode = new LeafNode(order);
        }
        for (int i = 0; i < node.order; i++) {
            if (i < (int) Math.ceil(node.n/2)){
                leftNode.keys[i] = node.keys[i];
                leftNode.references[i] = node.references[i];
            } else if (i < node.n){
                int index = i - (int) Math.ceil(node.n/2);
                rightNode.keys[index] = node.keys[i];
                rightNode.references[index] = node.references[i];
            } else {
                if (node instanceof LeafNode){
                    // then n == order
                    break;
                }
                int index = i - (int) Math.ceil(node.n/2);
                rightNode.references[index] = node.references[i];
            }
        }
        BPlusTreeNode[] result = {leftNode, rightNode};
        return result;
    }

    private Integer addToPosition(Integer pos, BPlusTreeNode node, BPlusTreeNode value){
        if (pos > 0 && node.references[pos] != null){
            Integer key = ((BPlusTreeNode) node.references[pos]).getSmallestKey();
            return addToPosition(pos, node, key, value);
        }
        return addToPosition(pos, node, null, value);
    }

    private Integer addToPosition(Integer pos, BPlusTreeNode node, Integer key, Object value){
        if (node.isFull()){
            return -1;
        }

        if (key != null){
            Integer iKey = key;
            Integer keyCache;
            for (int i = pos; i < node.keys.length; i++) {
                keyCache = node.keys[i];
                node.keys[i] = iKey;
                iKey = keyCache;
            }
            node.keys[pos] = key;
        }

        Object iValue = value;
        Object valueCache;

        for (int i = pos; i < node.references.length; i++) {
            valueCache = node.references[i];
            node.references[i] = iValue;
            iValue = valueCache;
        }

        if (value instanceof LeafNode){
            node.references[pos] = (LeafNode) value;
        } else if (value instanceof InnerNode){
            node.references[pos] = (InnerNode) value;
        } else {
            node.references[pos] = (ValueReference) value;
        }
        return 0;
    }

    private Integer insertionPosition(BPlusTreeNode node, Integer key){
        return  IntStream.range(0, node.keys.length)
                .filter(i -> node.keys[i] != null)
                .filter(i -> node.keys[i] > key)
                .findFirst()
                .orElse(node.getNodeSize());
    }
    private Integer addToNode(BPlusTreeNode node, Integer key, Object value){
        if (node.isFull()){
            return -1;
        }

        int pos = insertionPosition(node, key);
        return addToPosition(pos, node, key, value);
    }

    private Stack<BPlusTreeNode> getPathToKey(int key){
        Stack<BPlusTreeNode> path = new Stack<>();
        InnerNode currentParentNode;

        if (rootNode instanceof InitialRootNode){
            path.push(rootNode);
            return path;
        }

        currentParentNode = (InnerNode) rootNode;
        path.push(currentParentNode);
        while (true){
            BPlusTreeNode childNode = currentParentNode.selectChild(key);
            path.push(childNode);
            if (childNode instanceof LeafNode){
                return path;
            }
            currentParentNode = (InnerNode) childNode;
        }
    }
}
