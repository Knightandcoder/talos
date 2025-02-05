package com.talosvfx.talos.editor.widgets.ui.common;

import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;

public class SquareButton extends Button {

    private Image icon;
    private Cell iconCell;

    public SquareButton(Skin skin, Drawable drawable) {
        build(skin, drawable, false);
    }

    public SquareButton(Skin skin, Label label) {
        setSkin(skin);
        setStyle(skin.get("square", ButtonStyle.class));

        label.setAlignment(Align.center);

        iconCell = add(label).center().pad(5).padLeft(10).padRight(10);

        getStyle().checked = null;
    }

    public SquareButton(Skin skin, Drawable drawable, boolean toggle) {
        build(skin, drawable, toggle);
    }

    private void build(Skin skin, Drawable drawable, boolean toggle) {
        setSkin(skin);
        setStyle(skin.get("square", ButtonStyle.class));
        if(!toggle) {
           setDisabled(true);
        }
        setSize(24, 24);

        icon = new Image(drawable);
        icon.setOrigin(Align.center);

        iconCell = add(icon).center().padBottom(2);
    }

    public void flipHorizontal() {
        icon.setRotation(icon.getRotation() + 180);
    }

    public void flipVertical() {
        icon.setRotation(icon.getRotation() + 90);
    }

    public Cell getIconCell() {
        return iconCell;
    }
}
