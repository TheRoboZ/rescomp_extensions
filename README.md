# SLICED_SPRITE — rescomp extension

A processor that reads .png sprite sheets and a complementary "sprites slices definition" to produce SGDK-compatible sprite resources with automatic cutting and optional optimization. This Lets you manually control how a big sprite is composed by smaller VDP sprites (8, 16, 24, 32 sizes)

## Features
- Read textual sprite definitions (.txt) or detect outlined regions in indexed, transparent PNGs (.png).
- Text file can be exported from Aseprite using the script sgdk_slices.lua in root, jsut use slices to define the rectangles and provide the frame width and height when the script requests it
- Map detected rectangles into animations and frames using the sheet grid (W x H).
- Produce the same frame/animation output using both definitions type.
- Drop-in replacement for SGDK's SPRRITE resource with added cutting and optimization. No need to modify your .c code, just change the resource definition
- Flexible sprites_def parameter: recognized by extension (.txt or .png) anywhere after the required height parameter — no need to place it strictly last or to declare the full optional paramters list.

## sprites_def formats
### Text (.txt)
```
  - Format:
    [ANIMATION n]
    FRAME m
    x y width height
    ...

  - Example:
    [ANIMATION 0]
    FRAME 0
    0 0 32 32
    32 0 64 32
    FRAME 1
    0 0 31 31
    32 32 63 63
    # FRAME 2 - missing! BUT it will be processed automatically with DEFAULT SGDK SETTINGS

    # Animation 1 - missing! BUT it will be processed automatically with DEFAULT SGDK SETTINGS
```

### Indexed PNG (.png)
  - The processor uses IndexedOutlineDetector.detect(file) to find outlined rectangles in an indexed-color PNG.
  - Rectangles are grouped into animations and frames by grid:
    - animIndex = rect.y / H
    - frameIndex = rect.x / W
  - Rectangles must not cross frame/animation boundaries.

## Constraints & notes
- Use .png defs only with transparent, indexed-color PNGs (the outline detector expects IndexColorModel).
- Frame tile size (width/height) must be > 0 and < 32 (tile counts).
- When using .txt rectangles, allowed region sizes are multiples of 8 (8, 16, 24, 32 in pixels as enforced by the parser).
#### - If there are invalid rectangles, the processor will cut based on full frames using sgdk default SPRITE cutting.
#### - If sprites_def is omitted, the processor will cut based on full frames using sgdk default SPRITE cutting.

### Quick usage:

- Place rescomp_ext.jar in your project's res root to enable the processor.

- in your .res file:

```
# minimal parameters

SLICED_SPRITE name "file" width height [compression [time [collision [opt_type [opt_level [opt_duplicate ["sprites_def"]]]]]]]

# or up to full parameters:

SLICED_SPRITE name "file" width height ["sprites_def"]

```

### Key behavior: flexible sprites_def placement
- The processor recognizes the sprites definition file by its extension (.txt or .png) anywhere after the height (fields[4]).
- You can provide the same optional parameters as SPRITE before or after it; the processor will detect which field is the sprites_def and ignore it when parsing the other optional parameters.

```
name          Sprite variable name

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

sprites_def:    .txt or .png file with per-animation/frame definitions

```

### Example valid invocations:
  - Minimal (no definitions): SLICED_SPRITE mySprite "sheet.png" 2 3
  - With external definition (last): SLICED_SPRITE mySprite "sheet.png" 2 3 NONE [[3]] BOX BALANCED FAST FALSE "cuts.txt"
  - With definition in the middle (still accepted): SLICED_SPRITE mySprite "sheet.png" 2 3 "cuts.txt" NONE [[3]] BOX
  - With PNG outlines as defs: SLICED_SPRITE mySprite "sheet.png" 2 3 FAST "outlines_indexed.png"

### Drop-in replacement
- To replace SGDK default sprite resource, use SLICED_SPRITE with the same image/width/height parameters. Existing build flows can adopt this processor with no other changes while gaining the automatic cutting and optimization features.

Examples
- Basic:
  SLICED_SPRITE hero "hero_sheet.png" 2 3
- With manual cuts:
  SLICED_SPRITE hero "hero_sheet.png" 2 3 NONE [[5]] BOX BALANCED FAST FALSE "hero_cuts.txt"
- Using outlines detection:
  SLICED_SPRITE enemy "enemy_indexed.png" 4 2 FAST "enemy_outlines.png"

## License / Notes
- Place rescomp_ext.jar in your project's res root to enable the processor.
- See the project's source for implementation details (IndexedOutlineDetector and SpriteCutReader).









