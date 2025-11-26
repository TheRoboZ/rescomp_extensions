package com.theroboz.rescomp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sgdk.rescomp.type.SpriteCell;
import sgdk.rescomp.type.SpriteCell.OptimizationType;

public class SpriteFileReader
{
    private final File file;
    private final Map<Integer, List<SpriteFrameDefinition>> animationDefinitions;


    /**
     * Creates a reader for sprite definitions from file.
     * File format:
     * [ANIMATION x]
     * FRAME y
     * x y width height
     * x y width height
     * ...
     * FRAME z
     * ...
     *
     * Example:
     * [ANIMATION 0]
     * FRAME 0
     * 0 0 32 32
     * 32 0 32 32
     * FRAME 1
     * 0 32 32 32
     * 32 32 32 32
     */
    public SpriteFileReader(String filePath) throws IOException
    {
        this.file = new File(filePath);
        this.animationDefinitions = new HashMap<>();

        parseFile();
    }

    private void parseFile() throws IOException
    {
        if (!file.exists())
            throw new IOException("Sprite definition file not found: " + file.getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            String line;
            int lineNumber = 0;
            int currentAnimation = -1;
            int currentFrame = -1;

            List<SpriteCell> currentCells = new ArrayList<>();

            while ((line = reader.readLine()) != null)
            {
                lineNumber++;
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                try
                {
                    if (line.startsWith("[ANIMATION"))
                    {
                        // Save previous frame if exists
                        if (currentAnimation >= 0 && currentFrame >= 0)
                            saveFrameDefinition(currentAnimation, currentFrame, new SpriteFrameDefinition(currentCells));

                        // Parse animation index
                        currentAnimation = parseAnimationHeader(line);
                        currentFrame = -1;
                        currentCells = new ArrayList<>();
                    }
                    else if (line.startsWith("FRAME"))
                    {
                        // Save previous frame if exists
                        if (currentFrame >= 0)
                            saveFrameDefinition(currentAnimation, currentFrame, new SpriteFrameDefinition(currentCells));

                        // Parse frame header
                        final String[] parts = line.split("\\s+");
                        if (parts.length < 2)
                            throw new IOException("Invalid FRAME format at line " + lineNumber + ": " + line);

                        currentFrame = Integer.parseInt(parts[1]);
                        currentCells = new ArrayList<>();
                    }
                    else if (currentAnimation >= 0 && currentFrame >= 0)
                    {
                        // Parse rectangle
                        final SpriteCell cell = parseRectangle(line);
                        if (cell != null)
                            currentCells.add(cell);
                    }
                }
                catch (NumberFormatException e)
                {
                    throw new IOException("Error parsing line " + lineNumber + ": " + line, e);
                }
            }

            // Save last frame
            if (currentAnimation >= 0 && currentFrame >= 0)
                saveFrameDefinition(currentAnimation, currentFrame, new SpriteFrameDefinition(currentCells));
        }
    }

    private int parseAnimationHeader(String line) throws IOException
    {
        try
        {
            final int start = line.indexOf("[ANIMATION") + 10;
            final int end = line.indexOf("]", start);
            return Integer.parseInt(line.substring(start, end).trim());
        }
        catch (Exception e)
        {
            throw new IOException("Invalid animation header format: " + line, e);
        }
    }

    private SpriteCell parseRectangle(String line)
    {
        line = line.replaceAll("[,\\s]+", " ").trim();
        final String[] parts = line.split(" ");

        if (parts.length < 4)
            return null;

        try
        {
            final int x = Integer.parseInt(parts[0]);
            final int y = Integer.parseInt(parts[1]);
            final int width = Integer.parseInt(parts[2])-x+1;
            final int height = Integer.parseInt(parts[3])-y+1;

            return new SpriteCell(x, y, width, height, OptimizationType.BALANCED);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private void saveFrameDefinition(int animIndex, int frameIndex, SpriteFrameDefinition definition)
    {
        if (!animationDefinitions.containsKey(animIndex))
            animationDefinitions.put(animIndex, new ArrayList<>());

        final List<SpriteFrameDefinition> frames = animationDefinitions.get(animIndex);
        // Ensure list is large enough
        while (frames.size() <= frameIndex)
            frames.add(null);

        frames.set(frameIndex, definition);
    }

    /**
     * Gets all frame definitions for a specific animation.
     *
     * @param animIndex
     *        Animation index
     * @return List of SpriteFrameDefinition for the animation
     */
    public List<SpriteFrameDefinition> getAnimationFrameDefinitions(int animIndex)
    {
        final List<SpriteFrameDefinition> definitions = animationDefinitions.get(animIndex);
        return (definitions != null) ? definitions : new ArrayList<>();
    }

    /**
     * Gets sprite cells for a specific animation frame.
     *
     * @param animIndex
     *        Animation index
     * @param frameIndex
     *        Frame index
     * @return List of SpriteCell for the frame
     */
    public List<SpriteCell> getFrameSprites(int animIndex, int frameIndex)
    {
        final List<SpriteFrameDefinition> definitions = animationDefinitions.get(animIndex);
        if (definitions != null && frameIndex < definitions.size() && definitions.get(frameIndex) != null)
            return definitions.get(frameIndex).cells;

        return new ArrayList<>();
    }
}