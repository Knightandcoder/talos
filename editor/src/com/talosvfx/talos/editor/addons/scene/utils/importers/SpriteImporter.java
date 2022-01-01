package com.talosvfx.talos.editor.addons.scene.utils.importers;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector2;
import com.talosvfx.talos.editor.addons.scene.SceneEditorAddon;
import com.talosvfx.talos.editor.addons.scene.SceneEditorWorkspace;
import com.talosvfx.talos.editor.addons.scene.logic.GameObject;
import com.talosvfx.talos.editor.addons.scene.logic.components.SpriteRendererComponent;
import com.talosvfx.talos.editor.addons.scene.utils.AssetImporter;
import com.talosvfx.talos.editor.addons.scene.utils.ImportUtils;
import com.talosvfx.talos.editor.addons.scene.utils.metadata.SpriteMetadata;

public class SpriteImporter {

    public static FileHandle run(FileHandle fileHandle) {
        FileHandle importedAsset = AssetImporter.importAssetFile(fileHandle);
        // this is now copied to our assets folder, and metadata created

        boolean was9slice = false;

        if(fileHandle.nameWithoutExtension().endsWith(".9")) {
            was9slice = true;
            // it's a nine slice, and needs metadata created accordingly
            FileHandle metadataHandle = AssetImporter.getMetadataHandleFor(importedAsset);
            metadataHandle = AssetImporter.renameAsset(metadataHandle, metadataHandle.nameWithoutExtension().replace(".9", "") + ".meta");
            importedAsset = AssetImporter.renameAsset(importedAsset, importedAsset.nameWithoutExtension().replace(".9", "") + ".png");
            SpriteMetadata metadata = AssetImporter.readMetadata(metadataHandle, SpriteMetadata.class);

            Pixmap pixmap = new Pixmap(importedAsset);
            int[] splits = ImportUtils.getSplits(pixmap);
            metadata.borderData = splits;

            AssetImporter.saveMetadata(metadataHandle, metadata);

            Pixmap newPixmap = ImportUtils.cropImage(pixmap, 1, 1, pixmap.getWidth() - 1, pixmap.getHeight() - 1);
            PixmapIO.writePNG(importedAsset, newPixmap);

            pixmap.dispose();
            newPixmap.dispose();
        }

        SceneEditorWorkspace workspace = SceneEditorAddon.get().workspace;
        Vector2 sceneCords = workspace.getMouseCordsOnScene();
        GameObject gameObject = workspace.createSpriteObject(importedAsset, sceneCords);
        if(was9slice) {
            SpriteRendererComponent component = gameObject.getComponent(SpriteRendererComponent.class);
            component.renderMode = SpriteRendererComponent.RenderMode.sliced;
        }

        return importedAsset;
    }

}
