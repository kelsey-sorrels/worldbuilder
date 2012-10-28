package aaronsantos.worldbuilder.drawing;

import aaronsantos.worldbuilder.WorldSnapShot;

/**
 * Interface for drawing world snapshots.
 */
public interface Drawer
{
    /**
     * Draws the world snap shot.
     * @param snapShot The snapshot to draw.
     */
    void draw(WorldSnapShot snapShot);
}
