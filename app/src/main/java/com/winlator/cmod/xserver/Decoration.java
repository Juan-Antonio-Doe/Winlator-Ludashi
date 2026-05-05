/*
 * Decoration.java from Brunodev85's Winlator codebase.
 * Licensed under LGPL-2.1.
 */

package com.winlator.cmod.xserver;

public enum Decoration {
    ALL, BORDER, RESIZEH, TITLE, MENU, MINIMIZE, MAXIMIZE;

    public int flag() {
        return 1 << ordinal();
    }
}
