package com.theroboz.rescomp;

import sgdk.rescomp.Compiler;
import sgdk.rescomp.Processor;
import sgdk.rescomp.Resource;
import sgdk.rescomp.tool.Util;
import sgdk.rescomp.type.Basics.CollisionType;
import sgdk.rescomp.type.Basics.Compression;
import sgdk.tool.FileUtil;
import sgdk.tool.StringUtil;

public class SpriteCutProcessor implements Processor
{
    @Override
    public String getId()
    {
        return "SPRITE_CUT";
    }

    @Override
    public Resource execute(String[] fields) throws Exception
    {
        if (fields.length < 5)
        {
            System.out.println("Wrong SPRITE_FILE definition");
            System.out.println("SPRITE_FILE name \"file\" \"sprites_def\" width height [compression [collision]]");
            System.out.println("  name          Sprite variable name");
            System.out.println("  file          the image file to convert to SpriteDefinition structure (BMP or PNG image)");
            System.out.println("  sprites_def   file containing sprite definitions per animation and frame");
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
            System.out.println("  opt_duplicate enabled optimization of consecutive duplicated frames by removing them and increasing animation time to compensante.");
            System.out.println("                    FALSE     = no optimization (default)");
            System.out.println("                                Note that duplicated frames pixel data are still removed by rescomp binary blob optimizer");
            System.out.println("                    TRUE      = only the first instance of consecutive duplicated frames is kept and 'timer' value is increased to compensate the removed frames time.");
            System.out.println("                                Note that it *does* change the 'animation.numFrame' information so beware of that when enabling this optimization.");
            return null;
        }

        // get resource id
        final String id = fields[1];
        // get input file
        final String fileIn = FileUtil.adjustPath(Compiler.resDir, fields[2]);
        final String spritesDefFile = FileUtil.adjustPath(Compiler.resDir, fields[3]);
        // get frame size (in tile)
        final int wf = StringUtil.parseInt(fields[4], 0);
        final int hf = StringUtil.parseInt(fields[5], 0);

        if ((wf < 1) || (hf < 1))
        {
            System.out.println("Wrong SPRITE definition");
            System.out.println("SPRITE_FILE name \"file\" \"sprites_def\" width height [compression [collision]]");
            System.out.println("  width and height (size of sprite frame) should be > 0");
            return null;
        }

        if ((wf >= 32) || (hf >= 32))
        {
            System.out.println("Wrong SPRITE definition");
            System.out.println("SPRITE_FILE name \"file\" \"sprites_def\" width height [compression [collision]]");

            System.out.println("Error: width and height should be < 32");
            return null;
        }
        // get packed value
        Compression compression = Compression.NONE;
        if (fields.length >= 7)
            compression = Util.getCompression(fields[6]);
         // get frame time
        int[][] time = new int[][] {{ 0 }};
        if (fields.length >= 8)
            time = StringUtil.parseIntArray2D(fields[7], new int[][] {{ 0 }});
        // get collision value
        CollisionType collision = CollisionType.NONE;
        if (fields.length >= 9)
            collision = Util.getCollision(fields[8]);

        boolean optDuplicate = false;
        if (fields.length >= 10)
            optDuplicate = Boolean.parseBoolean(fields[9]);

        Compiler.addResourceFile(fileIn);
        Compiler.addResourceFile(spritesDefFile);

        return new SpriteCut(id, fileIn, spritesDefFile, wf, hf, compression, time, collision, optDuplicate);

    }
}
