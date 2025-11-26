package com.theroboz.rescomp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sgdk.rescomp.Resource;
import sgdk.rescomp.resource.Bin;
import sgdk.rescomp.resource.Palette;

import sgdk.rescomp.tool.Util;
import sgdk.rescomp.type.Basics.CollisionType;
import sgdk.rescomp.type.Basics.Compression;
import sgdk.tool.ArrayMath;
import sgdk.tool.ImageUtil;
import sgdk.tool.ImageUtil.BasicImageInfo;

public class SpriteFile extends Resource
{
    public final int wf; // width of frame cell in tile
    public final int hf; // height of frame cell in tile
    public final List<SpriteFileAnimation> animations;
    public int maxNumTile;
    public int maxNumSprite;

    final int hc;

    public final Palette palette;
    public SpriteFile(String id, String imgFile, String spritesDefFile, int wf, int hf, Compression compression, int[][] time, CollisionType collision, boolean optDuplicate) throws Exception
    {
        super(id);

        maxNumTile = 0;
        maxNumSprite = 0;
        animations = new ArrayList<>();

        if ((wf >= 32) || (hf >= 32))
            throw new IllegalArgumentException("SPRITE_FILE '" + id + "' has frame width or height >= 32 tiles (not supported)");

        // set frame size
        this.wf = wf;
        this.hf = hf;

        // get 8bpp pixels and also check image dimension is aligned to tile
        final byte[] image = ImageUtil.getImageAs8bpp(imgFile, true, true);

        // happen when we couldn't retrieve palette data from RGB image
        if (image == null)
            throw new IllegalArgumentException(
                    "RGB image '" + imgFile + "' does not contains palette data (see 'Important note about image format' in the rescomp.txt file");

        // find max color index
        final int maxIndex = ArrayMath.max(image, false);
        if (maxIndex >= 64)
            throw new IllegalArgumentException("'" + imgFile
                    + "' uses color index >= 64, SPRITE_FILE resource requires image with a maximum of 64 colors, use 4bpp indexed colors image instead if you are unsure.");

        // retrieve basic infos about the image
        final BasicImageInfo imgInfo = ImageUtil.getBasicInfo(imgFile);
        final int w = imgInfo.w;
        // we determine 'h' from data length and 'w' as we can crop image vertically to remove palette data
        final int h = image.length / w;

        final int palIndex;
        try
        {
            // get palette index used (only 1 palette allowed for sprite)
            palIndex = ImageUtil.getSpritePaletteIndex(image, w, h);
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException(
                    "'" + imgFile + "' SPRITE_FILE resource use more than 1 palette (16 colors), use 4bpp indexed colors image instead if you are unsure.", e);
        }
        // get size in tile
        final int wt = w / 8;
        final int ht = h / 8;

        // check image size is correct
        if ((wt % wf) != 0)
            throw new IllegalArgumentException("Error: '" + imgFile + "' width (" + w + ") is not a multiple of cell width (" + (wf * 8) + ").");
        if ((ht % hf) != 0)
            throw new IllegalArgumentException("Error: '" + imgFile + "' height (" + h + ") is not a multiple of cell height (" + (hf * 8) + ").");

        // build PALETTE
        palette = (Palette) addInternalResource(new Palette(id + "_palette", imgFile, palIndex * 16, 16, true));

        // Read sprite definitions from file
        final SpriteFileReader spriteDefReader = new SpriteFileReader(spritesDefFile);
        final int numAnim = ht / hf;


        for (int i = 0; i < numAnim; i++)
        {
            // build sprite animation
            SpriteFileAnimation animation = new SpriteFileAnimation(id + "_animation" + i, image, wt, ht, i, wf, hf, time[Math.min(time.length - 1, i)], collision, compression, spriteDefReader.getAnimationFrameDefinitions(i), optDuplicate);

            // check if empty
            if (!animation.isEmpty())
            {
                // add as internal resource (get duplicate if exist)
                animation = (SpriteFileAnimation) addInternalResource(animation);

                // update maximum number of tile and sprite
                maxNumTile = Math.max(maxNumTile, animation.getMaxNumTile());
                maxNumSprite = Math.max(maxNumSprite, animation.getMaxNumSprite());

                // add animation
                animations.add(animation);
            }
        }

        if (animations.isEmpty())
            throw new IllegalArgumentException("SPRITE_FILE '" + id + "' has no valid animations");

        // compute hash code
        hc = (wf << 0) ^ (hf << 8) ^ (maxNumTile << 16) ^ (maxNumSprite << 24) ^ animations.hashCode() ^ palette.hashCode();
    }

    @Override
    public int internalHashCode()
    {
        return hc;
    }

    @Override
    public boolean internalEquals(Object obj)
    {
        if (obj instanceof SpriteFile)
        {
            final SpriteFile sprite = (SpriteFile) obj;
            return (wf == sprite.wf) && (hf == sprite.hf) && (maxNumTile == sprite.maxNumTile) && (maxNumSprite == sprite.maxNumSprite)
                    && animations.equals(sprite.animations) && palette.equals(sprite.palette);
        }

        return false;
    }

    @Override
    public List<Bin> getInternalBinResources()
    {
        return new ArrayList<>();
    }

    @Override
    public String toString()
    {
        return id + ": wf=" + wf + " hf=" + hf + " numAnim=" + animations.size() + " maxNumTile=" + maxNumTile + " maxNumSprite=" + maxNumSprite;
    }

    @Override
    public int shallowSize()
    {
        return (animations.size() * 4) + 2 + 2 + 4 + 2 + 4 + 2 + 2;
    }

    @Override
    public int totalSize()
    {
        int result = 0;

        for (SpriteFileAnimation animation : animations)
            result += animation.totalSize();

        return result + palette.totalSize() + shallowSize();
    }

    @Override
    public void out(ByteArrayOutputStream outB, StringBuilder outS, StringBuilder outH) throws IOException
    {
        // can't store pointer so we just reset binary stream here (used for compression only)
        outB.reset();

        // animations pointer table
        Util.decl(outS, outH, null, id + "_animations", 2, false);
        for (SpriteFileAnimation animation : animations)
            outS.append("    dc.l    " + animation.id + "\n");

        outS.append("\n");

        // SpriteDefinition structure
        Util.decl(outS, outH, "SpriteDefinition", id, 2, global);
        // set frame cell size
        outS.append("    dc.w    " + (wf * 8) + "\n");
        outS.append("    dc.w    " + (hf * 8) + "\n");
        // set palette pointer
        outS.append("    dc.l    " + palette.id + "\n");
        // set number of animation
        outS.append("    dc.w    " + animations.size() + "\n");
        // set animations pointer
        outS.append("    dc.l    " + id + "_animations" + "\n");
        // set maximum number of tile used by a single animation frame (used for VRAM tile space
        // allocation)
        outS.append("    dc.w    " + maxNumTile + "\n");
        // set maximum number of VDP sprite used by a single animation frame (used for VDP sprite
        // allocation)
        outS.append("    dc.w    " + maxNumSprite + "\n");

        outS.append("\n");
    }
}