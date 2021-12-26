package com.talosvfx.talos.editor.addons.scene.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.*;
import com.talosvfx.talos.editor.addons.scene.logic.components.IComponent;
import com.talosvfx.talos.editor.widgets.propertyWidgets.IPropertyProvider;

import java.io.StringWriter;

public class Scene implements GameObjectContainer, Json.Serializable, IPropertyHolder {

    public String path;

    public GameObject root;

    public Scene() {

    }

    public Scene(String path) {
        this.path = path;

        root = new GameObject();
    }

    @Override
    public Array<GameObject> getGameObjects () {
        return root.getGameObjects();
    }

    @Override
    public Iterable<IComponent> getComponents () {
        return null;
    }

    @Override
    public void addGameObject (GameObject gameObject) {
        root.addGameObject(gameObject);
    }

    @Override
    public void addComponent (IComponent component) {

    }

    @Override
    public String getName () {
        FileHandle fileHandle = Gdx.files.absolute(path);
        return fileHandle.nameWithoutExtension();
    }

    @Override
    public void write (Json json) {
        json.writeValue("path", path);
    }

    @Override
    public void read (Json json, JsonValue jsonData) {
        path = jsonData.getString("path");
    }

    public void save () {
        try {
            FileHandle file = Gdx.files.absolute(path);

            StringWriter stringWriter = new StringWriter();
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);
            json.setWriter(stringWriter);
            json.getWriter().object();

            save(json);

            String finalString = stringWriter.toString() + "}";

            file.writeString(finalString, false);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public void save(Json json) {
        FileHandle file = Gdx.files.absolute(path);
        String name = file.nameWithoutExtension();

        json.writeValue("name", name);
        json.writeArrayStart("gameObjects");
        Array<GameObject> gameObjects = getGameObjects();
        if(gameObjects != null) {
            for (GameObject gameObject : gameObjects) {
                json.writeValue(gameObject);
            }
        }
        json.writeArrayEnd();
    }

    public void loadFromPath() {
        Json json = new Json();
        FileHandle dataFile = Gdx.files.absolute(path);
        JsonValue jsonValue = new JsonReader().parse(dataFile.readString());

        JsonValue gameObjectsJson = jsonValue.get("gameObjects");
        root = new GameObject();
        for(JsonValue gameObjectJson: gameObjectsJson) {
            GameObject gameObject = json.readValue(GameObject.class, gameObjectJson);
            root.addGameObject(gameObject);
        }
    }

    @Override
    public Iterable<IPropertyProvider> getPropertyProviders () {
        return new Array<>();
    }
}
