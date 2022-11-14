package exercise1;

import de.hpi.dbs2.ChosenImplementation;
import de.hpi.dbs2.dbms.*;
import de.hpi.dbs2.exercise1.SortOperation;
import org.jetbrains.annotations.NotNull;
import de.hpi.dbs2.dbms.utils.BlockSorter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ChosenImplementation(true)
public class TPMMSJava extends SortOperation {
    public TPMMSJava(@NotNull BlockManager manager, int sortColumnIndex) {
        super(manager, sortColumnIndex);
    }

    @Override
    public int estimatedIOCost(@NotNull Relation relation) {
        int blockMgrCapacity = getBlockManager().getFreeBlocks() + getBlockManager().getUsedBlocks();
        if (blockMgrCapacity * blockMgrCapacity < relation.getEstimatedSize()){
            throw new RelationSizeExceedsCapacityException();
        }
        // Idee: 1. Phase relation ein und auslesen (*2), 2. Phase relation einlesen (wird auf output nicht disk geschrieben)
        return relation.getEstimatedSize() * 3;
    }


    @Override
    public void sort(@NotNull Relation relation, @NotNull BlockOutput output) {
        estimatedIOCost(relation);
        /*
        Step one: sort all block-parts

         */
        List<Block> tmpBlockList = new ArrayList<Block>();
        List<List<Block>> blockList = new ArrayList<>(); // contains all loaded Blocks divided into it's sublists
        int blockMgrCapacity = getBlockManager().getFreeBlocks() + getBlockManager().getUsedBlocks();
        for (Iterator<Block> it = relation.iterator(); it.hasNext(); ) {
            Block block = it.next();
            tmpBlockList.add(block);
            // write in Blockmanager
            getBlockManager().load(block);
            if (getBlockManager().getFreeBlocks() == 0){ //was am ende
                // sort blocks
                BlockSorter.INSTANCE.sort(relation, tmpBlockList, relation.getColumns().getColumnComparator(getSortColumnIndex()));
                System.out.println(tmpBlockList);
                //release blocks to main memory
                for (Block b : tmpBlockList){
                    getBlockManager().release(b, true);
                }
                blockList.add(List.copyOf(tmpBlockList));
                tmpBlockList.clear();
            }
        }

        /*
        Step two: sort partial lists

        Teilliste von 0-2 und 3-5, alles in der blockList gelagert.
        Teillisten erkennen wir an der blockManager Größe
        nehme je die ersten Blöcke heraus, vergleiche sie und schreibe den kleinsten Block in den Output
         */

        for (int i = 0; i < blockMgrCapacity; i++){
            for (List<Block> blocks : blockList){
                tmpBlockList.add(blocks.get(i)); //out of bound!
                getBlockManager().load(blocks.get(i));
            }
            BlockSorter.INSTANCE.sort(relation, tmpBlockList, relation.getColumns().getColumnComparator(getSortColumnIndex()));
            for (Block block : tmpBlockList){
                output.output(block);
                getBlockManager().release(block, false);
            }
            tmpBlockList.clear();
        }
    }
}
