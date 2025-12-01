package com.theroboz.sliced_sprite;

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

public class SpriteCutReader
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

    public SpriteCutReader(String filePath, int W, int H) throws IOException
    {
        this.file = new File(filePath);
        this.animationDefinitions = new HashMap<>();

        // Only parse if the source file has a .txt extension (case-insensitive)
        final String name = this.file.getName();
        if (name != null)
        {
            if(name.toLowerCase().endsWith(".txt"))
                parseTextFile();
            else if (name.toLowerCase().endsWith(".png"))
                parsePNGFile(W, H);
        }
    }

    private void parseTextFile() throws IOException
    {
        if (!file.exists())
            throw new IOException("CUTS definition file not found: " + file.getAbsolutePath());

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
                        final SpriteCell cell = parseRectangle(line, currentAnimation, currentFrame);
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

    private void parsePNGFile(int regionW, int regionH) throws IOException
    {
        if (!file.exists())
            throw new IOException("CUTS definition file not found: " + file.getAbsolutePath());

        final IndexedOutlineDetector.Rect[] rects = IndexedOutlineDetector.detect(file);

        if (rects == null || rects.length == 0)
            return;

        // Map<animIndex, Map<frameIndex, List<SpriteCell>>>
        final Map<Integer, Map<Integer, List<SpriteCell>>> grouped = new HashMap<>();

        for (IndexedOutlineDetector.Rect r : rects)
        {
            // Determine animation by vertical region, frame by horizontal region
            final int animIndex = (r.y) / regionH;
            final int frameIndex = (r.x) / regionW;

            grouped.computeIfAbsent(animIndex, k -> new HashMap<>())
                   .computeIfAbsent(frameIndex, k -> new ArrayList<>())
                   .add(new SpriteCell(r.x - frameIndex*regionH, r.y - animIndex*regionH, r.width, r.height, OptimizationType.BALANCED));
                   System.out.println("Saved anim "+animIndex+" frame "+frameIndex+" rectangle " + (r.x - frameIndex*regionH) +" "+  (r.y - animIndex*regionH) +" " + r.width +" " + r.height);

        }

        // Save definitions for each animation/frame
        for (Map.Entry<Integer, Map<Integer, List<SpriteCell>>> animEntry : grouped.entrySet())
        {
            final int animIndex = animEntry.getKey();
            final Map<Integer, List<SpriteCell>> frames = animEntry.getValue();

            // Ensure consistent ordering of frames when saving (not strictly required by saveFrameDefinition,
            // but keeps output deterministic)
            final List<Integer> frameIndices = new ArrayList<>(frames.keySet());
            frameIndices.sort(Integer::compareTo);

            for (Integer fIdx : frameIndices)
            {
                final List<SpriteCell> cells = frames.get(fIdx);
                saveFrameDefinition(animIndex, fIdx, new SpriteFrameDefinition(cells));
            }
        }

        System.out.println("\n\n");
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

    private SpriteCell parseRectangle(String line, int currentAnimation, int currentFrame)
    {
        line = line.replaceAll("[,\\s]+", " ").trim();
        final String[] parts = line.split(" ");

        if (parts.length < 4)
            return null;

        try
        {
            final int x = Integer.parseInt(parts[0]);
            final int y = Integer.parseInt(parts[1]);
            final int width = Integer.parseInt(parts[2]);
            final int height = Integer.parseInt(parts[3]);

            if ((width==8 || width==16 || width==24 || width==32)
                && (height==8 || height==16 || height==24 || height==32))
            {
                return new SpriteCell(x, y, width, height, OptimizationType.BALANCED);
            }
            else
            {
                System.out.println("\n ERROR: ANIM "+currentAnimation+" FRAME "+currentFrame+" WIDTH "+width+" / HEIGHT "+height+"must be 8, 16, 24 or 32. FAME Processed with default values \n");
                return null;
            }
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