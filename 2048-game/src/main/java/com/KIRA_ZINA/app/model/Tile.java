package com.KIRA_ZINA.app.model;

/**
 * Represents a single tile in the 2048 game.
 * Contains the tile's value, grid position, animated position for smooth rendering,
 * and animation state for merge effects.
 */
public class Tile {
    private int value;
    private int row;
    private int col;
    
    // Animated position for smooth movement (in pixels)
    private double animatedX;
    private double animatedY;
    
    // Target position for animation (in pixels)
    private double targetX;
    private double targetY;
    
    // Scale for merge pop animation (1.0 = normal size)
    private double scale;
    
    // Flag to indicate if this tile is being merged (will disappear)
    private boolean merging;
    
    // Flag to indicate if this tile just appeared (new tile)
    private boolean isNew;
    
    // Rotation angle for animation (in degrees)
    private double rotation;
    
    // Opacity for fade effects (0.0 = transparent, 1.0 = opaque)
    private float opacity;
    
    /**
     * Creates a new tile with the specified value and position.
     */
    public Tile(int value, int row, int col) {
        this.value = value;
        this.row = row;
        this.col = col;
        this.scale = 1.0;
        this.merging = false;
        this.isNew = true;
        this.rotation = 0.0;
        this.opacity = 1.0f;
    }
    
    /**
     * Updates the animated position towards the target position.
     * @param progress Animation progress from 0.0 to 1.0
     */
    public void updateAnimation(double progress) {
        animatedX = animatedX + (targetX - animatedX) * progress;
        animatedY = animatedY + (targetY - animatedY) * progress;
    }
    
    /**
     * Sets the target position for animation.
     */
    public void setTargetPosition(double x, double y) {
        this.targetX = x;
        this.targetY = y;
    }
    
    /**
     * Sets the current animated position (used for initialization).
     */
    public void setAnimatedPosition(double x, double y) {
        this.animatedX = x;
        this.animatedY = y;
        this.targetX = x;
        this.targetY = y;
    }
    
    // Getters and setters
    public int getValue() {
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    public int getRow() {
        return row;
    }
    
    public void setRow(int row) {
        this.row = row;
    }
    
    public int getCol() {
        return col;
    }
    
    public void setCol(int col) {
        this.col = col;
    }
    
    public double getAnimatedX() {
        return animatedX;
    }
    
    public double getAnimatedY() {
        return animatedY;
    }
    
    public double getScale() {
        return scale;
    }
    
    public void setScale(double scale) {
        this.scale = scale;
    }
    
    public boolean isMerging() {
        return merging;
    }
    
    public void setMerging(boolean merging) {
        this.merging = merging;
    }
    
    public boolean isNew() {
        return isNew;
    }
    
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
    
    public double getRotation() {
        return rotation;
    }
    
    public void setRotation(double rotation) {
        this.rotation = rotation;
    }
    
    public float getOpacity() {
        return opacity;
    }
    
    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }
}
