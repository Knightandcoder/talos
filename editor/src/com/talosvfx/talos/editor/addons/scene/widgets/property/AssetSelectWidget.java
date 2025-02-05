package com.talosvfx.talos.editor.addons.scene.widgets.property;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.talosvfx.talos.TalosMain;
import com.talosvfx.talos.editor.addons.scene.SceneEditorAddon;
import com.talosvfx.talos.editor.addons.scene.utils.importers.AssetImporter;
import com.talosvfx.talos.editor.addons.scene.widgets.AssetListPopup;
import com.talosvfx.talos.editor.widgets.propertyWidgets.PropertyWidget;
import com.talosvfx.talos.editor.widgets.ui.EditableLabel;
import com.talosvfx.talos.editor.widgets.ui.FilteredTree;
import com.talosvfx.talos.editor.widgets.ui.common.SquareButton;

import java.util.function.Supplier;

public class AssetSelectWidget extends PropertyWidget<String> {

    private Label nameLabel;
    private String path;

    private String filter;

    public AssetSelectWidget() {
        super();
    }

    public AssetSelectWidget (String name, String extension, Supplier<String> supplier, ValueChanged<String> valueChanged) {
        super(name, supplier, valueChanged);
        this.filter = extension;
    }

    @Override
    public PropertyWidget clone() {
        AssetSelectWidget clone = (AssetSelectWidget) super.clone();
        clone.filter = filter;
        return clone;
    }

    @Override
    public String getValue () {
        return path;
    }

    @Override
    public Actor getSubWidget () {
        Table table = new Table();
        Skin skin = TalosMain.Instance().getSkin();
        final SquareButton button = new SquareButton(skin, skin.getDrawable("ic-file-edit"));

        nameLabel = new Label("", skin);
        nameLabel.setEllipsis(true);
        nameLabel.setAlignment(Align.right);

        table.add(nameLabel).growX().maxWidth(130).padRight(2);
        table.add(button).right().padRight(-2);

        button.addListener(new ClickListener() {
            @Override
            public void clicked (InputEvent event, float x, float y) {
                Vector2 pos = new Vector2(button.getWidth()/2f, button.getHeight()/2f);
                button.localToStageCoordinates(pos);

                AssetListPopup assetListPopup = SceneEditorAddon.get().workspace.getAssetListPopup();
                assetListPopup.showPopup(getStage(), pos, filter, new FilteredTree.ItemListener() {
                    @Override
                    public void chosen (FilteredTree.Node node) {
                        String path = (String) node.getObject();
                        if(Gdx.files.absolute(path).isDirectory()) return;

                        String relative = AssetImporter.relative(Gdx.files.absolute(path));

                        updateWidget(relative);
                        callValueChanged(relative);
                        assetListPopup.remove();
                    }
                });
            }
        });

        return table;
    }

    @Override
    public void updateWidget (String value) {
        if(value == null || value.isEmpty()) {
            nameLabel.setText("-");
            path = null;
            return;
        }
        FileHandle handle = AssetImporter.get(value);
        if(handle.exists()) {
            nameLabel.setText(handle.name());
        } else {
            nameLabel.setText("n/a");
        }
        path = value;
    }
}
