package com.theroboz.rescomp;

import sgdk.rescomp.Compiler;
import sgdk.rescomp.Processor;
import sgdk.rescomp.Resource;
import sgdk.rescomp.resource.Sprite;
import sgdk.rescomp.tool.Util;
import sgdk.rescomp.type.Basics.CollisionType;
import sgdk.rescomp.type.Basics.Compression;
import sgdk.rescomp.type.SpriteCell.OptimizationLevel;
import sgdk.rescomp.type.SpriteCell.OptimizationType;
import sgdk.tool.FileUtil;
import sgdk.tool.StringUtil;

public class SpriteCutProcessor implements Processor
{
    @Override
    public String getId()
    {
        return "SLICED_SPRITE";
    }

    @Override
    public Resource execute(String[] fields) throws Exception
    {
        if (fields.length < 5)
        {
            System.out.println("Wrong SLICED_SPRITE definition");
            System.out.println("  SLICED_SPRITE name \"file\" width height [compression [time [collision [opt_type [opt_level [opt_duplicate [\"sprites_def\"]]]]]]]");
            System.out.println("  name          Sprite variable name");
            System.out.println("  file          the image file to convert to SpriteDefinition structure (BMP or PNG image)");
            System.out.println("  width         width of a single sprite frame in tile");
            System.out.println("  height        height of a single sprite frame in tile");
            System.out.println("  compression   compression type, accepted values:");
            System.out.println("                   -1 / BEST / AUTO = use best compression");
            System.out.println("                    0 / NONE        = no compression (default)");
            System.out.println("                    1 / APLIB       = aplib library (good compression ratio but slow)");
            System.out.println("                    2 / FAST / LZ4W = custom lz4 compression (average compression ratio but fast)");
            System.out.println("  time          display frame time in 1/60 of second (time between each animation frame)");
            System.out.println("                    If this value is set to 0 (default) then auto animation is disabled");
            System.out.println("                    It can be set globally (single value) or independently for each frame of each animation");
            System.out.println("                    Example for a sprite sheet of 3 animations x 5 frames:");
            System.out.println("                    [[3,3,3,4,4][4,5,5][2,3,3,4]]");
            System.out.println("                    As you can see you can have empty value for empty frame");
            System.out.println("  collision     collision type: CIRCLE, BOX or NONE (NONE by default)");
            System.out.println("  opt_type      sprite cutting optimization strategy, accepted values:");
            System.out.println("                    0 / BALANCED  = balance between used tiles and hardware sprites (default)");
            System.out.println("                    1 / SPRITE    = reduce the number of hardware sprite (using bigger sprite) at the expense of more used tiles");
            System.out.println("                    2 / TILE      = reduce the number of tiles at the expense of more hardware sprite (using smaller sprite)");
            System.out.println("                    3 / NONE      = no optimization (cover the whole sprite frame)");
            System.out.println("  opt_level     optimization level for the sprite cutting operation:");
            System.out.println("                    FAST      = fast optimisation, good enough in general (default)");
            System.out.println("                    MEDIUM    = intermediate optimisation level, provide better results than FAST but ~5 time slower");
            System.out.println("                    SLOW      = advanced optimisation level using a genetic algorithm (80000 iterations), ~20 time slower than FAST");
            System.out.println("                    MAX       = maximum optimisation level, genetic algorithm (500000 iterations), ~100 time slower than FAST");
            System.out.println("  opt_duplicate enabled optimization of consecutive duplicated frames by removing them and increasing animation time to compensante.");
            System.out.println("                    FALSE     = no optimization (default)");
            System.out.println("                                Note that duplicated frames pixel data are still removed by rescomp binary blob optimizer");
            System.out.println("                    TRUE      = only the first instance of consecutive duplicated frames is kept and 'timer' value is increased to compensate the removed frames time.");
            System.out.println("                                Note that it *does* change the 'animation.numFrame' information so beware of that when enabling this optimization.");
            System.out.println("  sprites_def   file containing sprite definitions per animation and frame");
            return null;
        }

          // get resource id
        final String id = fields[1];
        // get input file
        final String fileIn = FileUtil.adjustPath(Compiler.resDir, fields[2]);
        // get frame size (in tile)
        final int wf = StringUtil.parseInt(fields[3], 0);
        final int hf = StringUtil.parseInt(fields[4], 0);

        if ((wf < 1) || (hf < 1))
        {
            System.out.println("Wrong SLICED_SPRITE definition");
            System.out.println("  SLICED_SPRITE name \"file\" width height [compression [time [collision [opt_type [opt_level [opt_duplicate [\"sprites_def\"]]]]]]]");
            System.out.println("  width and height (size of sprite frame) should be > 0");

            return null;
        }

        // frame size over limit (we need VDP sprite offset to fit into u8 type)
        if ((wf >= 32) || (hf >= 32))
        {
            System.out.println("Wrong SLICED_SPRITE definition");
            System.out.println("  SLICED_SPRITE name \"file\" width height [compression [time [collision [opt_type [opt_level [opt_duplicate [\"sprites_def\"]]]]]]]");
            System.out.println("  width and height (size of sprite frame) should be < 32");

            return null;
        }

// Search for spritesDefFile (recognized by .txt or .png extension) starting from field 5 onwards
        String spritesDefFile = null;
        int spritesDefFileIndex = -1;
        for (int i = 5; i < fields.length; i++)
        {
            String extension = FileUtil.getFileExtension(fields[i], true);
            if (StringUtil.equals(extension, ".txt") || StringUtil.equals(extension, ".png"))
            {
                spritesDefFile = fields[i];
                spritesDefFileIndex = i;
                break;
            }
        }

        // get packed value
        Compression compression = Compression.NONE;
        if (fields.length >= 6 && spritesDefFileIndex != 5)
            compression = Util.getCompression(fields[5]);
        // get frame time
        int[][] time = new int[][] {{ 0 }};
        if (fields.length >= 7 && spritesDefFileIndex != 6)
            time = StringUtil.parseIntArray2D(fields[6], new int[][] {{ 0 }});
        // get collision value
        CollisionType collision = CollisionType.NONE;
        if (fields.length >= 8 && spritesDefFileIndex != 7)
            collision = Util.getCollision(fields[7]);
        // get optimization value
        OptimizationType opt = OptimizationType.BALANCED;
        if (fields.length >= 9 && spritesDefFileIndex != 8)
            opt = Util.getSpriteOptType(fields[8]);
        // get max number of iteration
        OptimizationLevel optLevel = OptimizationLevel.FAST;
        boolean showCut = false;
        if (fields.length >= 10 && spritesDefFileIndex != 9)
        {
            optLevel = Util.getSpriteOptLevel(fields[9]);
            showCut = true;
        }
        boolean optDuplicate = false;
        if (fields.length >= 11 && spritesDefFileIndex != 10)
            optDuplicate = Boolean.parseBoolean(fields[10]);

        Compiler.addResourceFile(fileIn);

        if (spritesDefFile != null)
        {
            final String adjustedSpritesDefFile = FileUtil.adjustPath(Compiler.resDir, spritesDefFile);
            // add resource file (used for deps generation)
            String extension = FileUtil.getFileExtension(adjustedSpritesDefFile, true);
            if (StringUtil.equals(extension, ".txt") || StringUtil.equals(extension, ".png"))
            {
                Compiler.addResourceFile(adjustedSpritesDefFile);
                return new SpriteCut(id, fileIn, wf, hf, compression, time, collision, opt, optLevel, showCut, optDuplicate, adjustedSpritesDefFile);
            }
            else
            {
                System.out.println("Wrong SLICED_SPRITE definition");
                System.out.println("CUTS definition file '" + adjustedSpritesDefFile + "' must be a text or PNG file! but is of type " + extension);
                return null;
            }
        }
        else
        {
            // add resource file (used for deps generation)
            return new Sprite(id, fileIn, wf, hf, compression, time, collision, opt, optLevel, showCut, optDuplicate);
        }
    }
}
