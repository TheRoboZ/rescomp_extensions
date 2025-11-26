package com.theroboz.rescomp;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import sgdk.rescomp.Resource;
import sgdk.rescomp.resource.Bin;
import sgdk.rescomp.resource.Tileset;
import sgdk.rescomp.resource.internal.Collision;

import sgdk.rescomp.tool.SpriteCutter;
import sgdk.rescomp.tool.Util;
import sgdk.rescomp.type.Basics;
import sgdk.rescomp.type.SpriteCell;
import sgdk.rescomp.type.Basics.CollisionType;
import sgdk.rescomp.type.SpriteCell.OptimizationLevel;
import sgdk.rescomp.type.SpriteCell.OptimizationType;
import sgdk.tool.ImageUtil;

public class SpriteFileFrame extends Resource {
   public final List<VDPSpriteFile> vdpSprites;
   public final Collision collision;
   public final Tileset tileset;
   public final int timer;
   final int hc;
   final byte[] frameImage;
   final Dimension frameDim;
   final Basics.CollisionType collisionType;
   final Basics.Compression compression;
   final int fhc;

   public SpriteFileFrame(String id, byte[] frameImage8bpp, int wf, int hf, int timer, Basics.CollisionType collisionType, Basics.Compression compression, List<SpriteCell> sprites) {
      super(id);
      this.vdpSprites = new ArrayList();
      this.timer = timer;
      this.collisionType = collisionType;
      this.compression = compression;
      this.frameImage = frameImage8bpp;
      this.frameDim = new Dimension(wf * 8, hf * 8);
      this.fhc = computeFastHashcode(frameImage8bpp, this.frameDim, timer, collisionType, compression);
      if (sprites.isEmpty()) {
         System.out.println("Sprite frame '" + id + "' is empty");
         this.tileset = (Tileset)addInternalResource(new Tileset(id + "_tileset", false));
      } else {
         int optNumTile = 0;

         SpriteCell spr;
         for(Iterator var11 = sprites.iterator(); var11.hasNext(); optNumTile += spr.numTile) {
            spr = (SpriteCell)var11.next();
         }

         System.out.println("Sprite frame '" + id + "' - " + sprites.size() + " VDP sprites and " + optNumTile + " tiles");
         this.tileset = (Tileset)addInternalResource(new Tileset(id + "_tileset", this.frameImage, wf * 8, hf * 8, sprites, compression, false));
      }

      Collision coll;
      if (collisionType == CollisionType.NONE) {
         coll = null;
      } else {
         Basics.CollisionBase c = null;
         switch (collisionType) {
            case CIRCLE:
               c = new Basics.Circle(wf * 8 / 2, hf * 8 / 2, wf * 8 * 3 / 8);
               break;
            case BOX:
               c = new Basics.Box(wf * 8 * 1 / 4, hf * 8 * 1 / 4, wf * 8 * 3 / 4, hf * 8 * 3 / 4);
         }

         coll = new Collision(id + "_collision", (Basics.CollisionBase)c);
      }

      if (coll != null) {
         this.collision = (Collision)addInternalResource(coll);
      } else {
         this.collision = null;
      }

      int ind = 0;
      Iterator var12 = sprites.iterator();

      while(var12.hasNext()) {
         SpriteCell sprite = (SpriteCell)var12.next();
         this.vdpSprites.add(new VDPSpriteFile(id + "_sprite" + ind++, sprite, wf, hf));
      }

      this.hc = timer << 16 ^ (this.tileset != null ? this.tileset.hashCode() : 0) ^ this.vdpSprites.hashCode() ^ (this.collision != null ? this.collision.hashCode() : 0);
   }

   public SpriteFileFrame(String id, byte[] frameImage8bpp, int wf, int hf, int timer, Basics.CollisionType collisionType, Basics.Compression compression, SpriteCell.OptimizationType optType, SpriteCell.OptimizationLevel optLevel) {
      this(id, frameImage8bpp, wf, hf, timer, collisionType, compression, computeSpriteCutting(id, frameImage8bpp, wf, hf, optType, optLevel));
   }

   public SpriteFileFrame(String id, byte[] image8bpp, int w, int h, int frameIndex, int animIndex, int wf, int hf, int timer, Basics.CollisionType collisionType, Basics.Compression compression, SpriteCell.OptimizationType optType, SpriteCell.OptimizationLevel optLevel) {
      this(id, ImageUtil.getSubImage(image8bpp, new Dimension(w * 8, h * 8), new Rectangle(frameIndex * wf * 8, animIndex * hf * 8, wf * 8, hf * 8)), wf, hf, timer, collisionType, compression, optType, optLevel);
   }

   static List<SpriteCell> computeSpriteCutting(String id, byte[] frameImage8bpp, int wf, int hf, SpriteCell.OptimizationType optType, SpriteCell.OptimizationLevel optLevel) throws UnsupportedOperationException {
      Dimension frameDim = new Dimension(wf * 8, hf * 8);
      List sprites;
      boolean empty;
      if (optType == OptimizationType.NONE) {
         sprites = SpriteCutter.getFastOptimizedSpriteList(frameImage8bpp, frameDim, OptimizationType.NONE, false);
      } else if (optLevel != OptimizationLevel.SLOW && optLevel != OptimizationLevel.MAX) {
         empty = optLevel == OptimizationLevel.MEDIUM;
         sprites = SpriteCutter.getFastOptimizedSpriteList(frameImage8bpp, frameDim, optType, empty);
         if (sprites.size() > 16 && optType != OptimizationType.MIN_SPRITE) {
            sprites = SpriteCutter.getFastOptimizedSpriteList(frameImage8bpp, frameDim, OptimizationType.MIN_SPRITE, empty);
         }

         if (sprites.size() > 16 && !empty) {
            sprites = SpriteCutter.getFastOptimizedSpriteList(frameImage8bpp, frameDim, OptimizationType.MIN_SPRITE, true);
         }

         if (sprites.size() > 16) {
            sprites = SpriteCutter.getSlowOptimizedSpriteList(frameImage8bpp, frameDim, 100000L, OptimizationType.MIN_SPRITE);
         }
      } else {
         int iteration = optLevel == OptimizationLevel.SLOW ? 500000 : 5000000;
         sprites = SpriteCutter.getSlowOptimizedSpriteList(frameImage8bpp, frameDim, (long)iteration, optType);
         if (sprites.size() > 16 && optType != OptimizationType.MIN_SPRITE) {
            sprites = SpriteCutter.getSlowOptimizedSpriteList(frameImage8bpp, frameDim, (long)iteration, OptimizationType.MIN_SPRITE);
         }
      }

      if (sprites.size() > 16) {
         throw new UnsupportedOperationException("Sprite frame '" + id + "' uses " + sprites.size() + " internal sprites, that is above the limit (16), try to reduce the sprite size or split it.");
      } else {
         if (!sprites.isEmpty() && optType == OptimizationType.NONE) {
            empty = true;
            byte[] var12 = frameImage8bpp;
            int var11 = frameImage8bpp.length;

            for(int var10 = 0; var10 < var11; ++var10) {
               byte b = var12[var10];
               if ((b & 15) != 0) {
                  empty = false;
                  break;
               }
            }

            if (empty) {
               sprites.clear();
            }
         }

         return sprites;
      }
   }

   static int computeFastHashcode(byte[] frameImage8bpp, Dimension frameDim, int timer, Basics.CollisionType collision, Basics.Compression compression) {
      return timer << 16 ^ (collision != null ? collision.hashCode() : 0) ^ Arrays.hashCode(frameImage8bpp) ^ frameDim.hashCode() ^ compression.hashCode();
   }

   public List<SpriteCell> getSprites() {
      List<SpriteCell> result = new ArrayList();
      Iterator var3 = this.vdpSprites.iterator();

      while(var3.hasNext()) {
         VDPSpriteFile sprite = (VDPSpriteFile)var3.next();
         result.add(new SpriteCell(sprite.offsetX, sprite.offsetY, sprite.wt * 8, sprite.ht * 8, OptimizationType.BALANCED));
      }

      return result;
   }

   public int getNumSprite() {
      return this.isEmpty() ? 0 : this.vdpSprites.size();
   }

   public boolean isEmpty() {
      return this.tileset.isEmpty();
   }

   public boolean isOptimisable() {
      if (this.vdpSprites.size() == 1) {
         VDPSpriteFile vdpSprite = (VDPSpriteFile)this.vdpSprites.get(0);
         return vdpSprite.wt * 8 == this.frameDim.width && vdpSprite.ht * 8 == this.frameDim.height && vdpSprite.offsetX == 0 && vdpSprite.offsetY == 0;
      } else {
         return false;
      }
   }

   public int getNumTile() {
      return this.isEmpty() ? 0 : this.tileset.getNumTile();
   }

   public int internalHashCode() {
      return this.hc;
   }

   public boolean internalEquals(Object obj) {
      if (!(obj instanceof SpriteFileFrame)) {
         return false;
      } else {
         SpriteFileFrame spriteFrame = (SpriteFileFrame)obj;
         return this.timer == spriteFrame.timer && this.tileset.equals(spriteFrame.tileset) && this.vdpSprites.equals(spriteFrame.vdpSprites) && (this.collision == spriteFrame.collision || this.collision != null && this.collision.equals(spriteFrame.collision));
      }
   }

   public List<Bin> getInternalBinResources() {
      return new ArrayList();
   }

   public String toString() {
      return this.id + ": numTile=" + this.getNumTile() + " numSprite=" + this.getNumSprite();
   }

   public int shallowSize() {
      return this.vdpSprites.size() * 6 + 1 + 1 + 4 + 4;
   }

   public int totalSize() {
      return this.isEmpty() ? this.shallowSize() : this.tileset.totalSize() + (this.collision != null ? this.collision.totalSize() : 0) + this.shallowSize();
   }

   public void out(ByteArrayOutputStream outB, StringBuilder outS, StringBuilder outH) throws IOException {
      outB.reset();
      Util.decl(outS, outH, "AnimationFrame", this.id, 2, this.global);
      int numSprite = this.isOptimisable() ? 129 : this.getNumSprite();
      outS.append("    dc.w    " + (numSprite << 8 & '\uff00' | this.timer << 0 & 255) + "\n");
      outS.append("    dc.l    " + this.tileset.id + "\n");
      if (this.collision == null) {
         outS.append("    dc.l    0\n");
      } else {
         outS.append("    dc.l    " + this.collision.id + "\n");
      }

      Iterator var6 = this.vdpSprites.iterator();

      while(var6.hasNext()) {
         VDPSpriteFile sprite = (VDPSpriteFile)var6.next();
         sprite.internalOutS(outS);
      }

      outS.append("\n");
   }
}
