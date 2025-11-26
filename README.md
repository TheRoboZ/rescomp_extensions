# sprites_def example with manually typed cutting regions

```
# Animation 0 - first xeno
[ANIMATION 0]
FRAME 0
0 0 31 31
32 32 63 63
FRAME 1
0 0 31 31
32 32 63 63
# FRAME 2 - missing! BUT it will be processed automatically with DEFAULT SGDK SETTINGS

# Animation 1 - missing! BUT it will be processed automatically with DEFAULT SGDK SETTINGS
```

# end of sprites_def


# RESCOMP EXTENSION USAGE:
copy rescomp_ext.jar in your \res root folder


    SPRITE_CUT xeno_spr  "xeno.png" "sprite_cuts.txt" 8 8 NONE  4 NONE

    SPRITE_FILE name \"file\" \"sprites_def\" width height [compression [collision]]

    name          Sprite variable name

    file          the image file to convert to SpriteDefinition structure (BMP or PNG image)

    sprites_def   file containing sprite definitions per animation and frame

    width         width of a single sprite frame in tile

    height        height of a single sprite frame in tile

    compression   compression type, accepted values:
                    -1 / BEST / AUTO = use best compression
                    0 / NONE        = no compression (default)
                    1 / APLIB       = aplib library (good compression ratio but slow)
                    2 / FAST / LZ4W = custom lz4 compression (average compression ratio but fast)

    time          display frame time in 1/60 of second (time between each animation frame)
                    If this value is set to 0 (default) then auto animation is disabled
                    It can be set globally (single value) or independently for each frame of each animation
                    Example for a sprite sheet of 3 animations x 5 frames:
                    [[3,3,3,4,4][4,5,5][2,3,3,4]]
                    As you can see you can have empty value for empty frame

    collision     collision type: CIRCLE, BOX or NONE (NONE by default)

    opt_duplicate enabled optimization of consecutive duplicated frames by removing them and increasing animation time to compensate.
                    FALSE     = no optimization (default)
                                Note that duplicated frames pixel data are still removed by rescomp binary blob optimizer
                    TRUE      = only the first instance of consecutive duplicated frames is kept and 'timer' value is increased to compensate the removed frames time.
                                Note that it *does* change the 'animation.numFrame' information so beware of that when enabling this optimization.