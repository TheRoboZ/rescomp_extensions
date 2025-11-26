package com.theroboz.rescomp;

import java.util.List;
import sgdk.rescomp.type.SpriteCell;

public class SpriteFrameDefinition
{
    public final List<SpriteCell> cells;

    public SpriteFrameDefinition(List<SpriteCell> cells)
    {
        this.cells = cells;
    }
}