/*******************************************************************************
 * Copyright 2019 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.talosvfx.talos;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.kotcrab.vis.ui.VisUI;
import com.talosvfx.talos.editor.NodeStage;
import com.talosvfx.talos.editor.UIStage;
import com.talosvfx.talos.editor.WorkplaceStage;
import com.talosvfx.talos.editor.addons.AddonController;
import com.talosvfx.talos.editor.dialogs.ErrorReporting;
import com.talosvfx.talos.editor.project.FileTracker;
import com.talosvfx.talos.editor.project.IProject;
import com.talosvfx.talos.editor.project.TalosProject;
import com.talosvfx.talos.editor.project.ProjectController;
import com.talosvfx.talos.editor.utils.CameraController;
import com.talosvfx.talos.editor.utils.ScreenshotService;
import com.talosvfx.talos.runtime.ScopePayload;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;

import static org.lwjgl.glfw.GLFW.glfwSetDropCallback;
import static org.lwjgl.system.MemoryUtil.*;

public class TalosMain extends ApplicationAdapter {

	private static boolean focused = true;

	private UIStage uiStage;

	private NodeStage nodeStage;

	private WorkplaceStage currentWorkplaceStage;

	private ProjectController projectController;

	private Skin skin;

	private static TalosMain instance;

	private AddonController addonController;

	public ObjectMap<Class, String> moduleNames = new ObjectMap<>();

	public static TalosMain Instance () {
		return instance;
	}

	public ScopePayload globalScope = new ScopePayload();

	public UIStage UIStage () {
		return uiStage;
	}

	public ErrorReporting errorReporting;

	public NodeStage NodeStage () {
		return nodeStage;
	}

	private Preferences preferences;

	private FileTracker fileTracker = new FileTracker();

	private InputMultiplexer inputMultiplexer;

	public TalosProject TalosProject() {
		return ProjectController.TLS;
	}

	public IProject Project() {
		return projectController.getProject();
	}

	public ProjectController ProjectController () {
		return projectController;
	}

	public Preferences Prefs() {
		return preferences;
	}

	public ScreenshotService Screeshot() {
		return screenshotService;
	}

	private ScreenshotService screenshotService;

	public Cursor pickerCursor;
	private Cursor currentCursor;

	public TalosMain () {

	}

	public boolean isOsX() {
		String osName = System.getProperty("os.name").toLowerCase();
		boolean isMacOs = osName.startsWith("mac os x");
		return isMacOs;
	}

	@Override
	public void create () {
		final Lwjgl3Graphics graphics = (Lwjgl3Graphics)Gdx.graphics;
		glfwSetDropCallback(graphics.getWindow().getWindowHandle(), new GLFWDropCallback() {
			@Override
			public void invoke (long window, int count, long names) {

				PointerBuffer namebuffer = memPointerBuffer(names, count);
				final String[] filesPaths = new String[count];
				for (int i = 0; i < count; i++) {
					String pathToObject = memUTF8(memByteBufferNT1(namebuffer.get(i)));
					filesPaths[i] = pathToObject;
				}

				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run () {
						final int x = Gdx.input.getX();
						final int y = Gdx.input.getY();

						try {
							nodeStage.fileDrop(filesPaths, x, y);
							uiStage.fileDrop(filesPaths, x, y);
						}  catch (Exception e) {
							TalosMain.Instance().reportException(e);
						}
					}
				});
			}
		});


		TalosMain.instance = this;

		screenshotService = new ScreenshotService();

		addonController = new AddonController();

		preferences = Gdx.app.getPreferences("talos-preferences");

		TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("skin/uiskin.atlas"));
		skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
		skin.addRegions(atlas);

		VisUI.load(skin);

		pickerCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("cursors/picker.png")), 0, 0);

		uiStage = new UIStage(skin);

		errorReporting = new ErrorReporting();

		nodeStage = new NodeStage(skin);
		currentWorkplaceStage = nodeStage;

		projectController = new ProjectController();

		uiStage.init();
		nodeStage.init();

		addonController.initAll();

		inputMultiplexer = new InputMultiplexer(uiStage.getStage(), currentWorkplaceStage.getStage());

		Gdx.input.setInputProcessor(inputMultiplexer);

		// final init after all is done
		TalosMain.Instance().ProjectController().newProject(ProjectController.TLS);


		GLFWWindowFocusCallback glfwWindowFocusCallback = GLFWWindowFocusCallback.create(new GLFWWindowFocusCallback() {
			@Override
			public void invoke (long window, boolean focused) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run () {
						TalosMain.focused = focused;
					}
				});
			}
		});
		GLFW.glfwSetWindowFocusCallback(((Lwjgl3Graphics)Gdx.graphics).getWindow().getWindowHandle(), glfwWindowFocusCallback);

	}

	public void setCursor(Cursor cursor) {
		if(currentCursor != cursor) {
			if(cursor != null) {
				Gdx.graphics.setCursor(cursor);
			} else {
				Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
			}
			currentCursor = cursor;
		}
	}

	public void disableNodeStage() {
		currentWorkplaceStage = null;
		inputMultiplexer.removeProcessor(nodeStage.getStage());
	}

	public void setThirdPartyStage(WorkplaceStage stage) {
		currentWorkplaceStage = stage;
		inputMultiplexer.setProcessors(uiStage.getStage(), currentWorkplaceStage.getStage());
	}

	public void enableNodeStage() {
		currentWorkplaceStage = nodeStage;
		inputMultiplexer.setProcessors(uiStage.getStage(), currentWorkplaceStage.getStage());
	}

	@Override
	public void render () {
		if (!focused) {
			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (currentWorkplaceStage != null) {
			Gdx.gl.glClearColor(currentWorkplaceStage.getBgColor().r, currentWorkplaceStage.getBgColor().g, currentWorkplaceStage.getBgColor().b, 1);
		} else {
			Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1);
		}
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		if (currentWorkplaceStage != null) {
			currentWorkplaceStage.act();
			currentWorkplaceStage.getStage().act();
			currentWorkplaceStage.getStage().draw();
		}

		fileTracker.update();

		uiStage.getStage().act();
		uiStage.getStage().draw();

		screenshotService.postRender();
	}

	public void reportException(Throwable e) {
		errorReporting.reportException(e);
	}

	public void resize (int width, int height) {
		if(currentWorkplaceStage != null) {
			currentWorkplaceStage.resize(width, height);
		}
		uiStage.resize(width, height);
	}

	@Override
	public void dispose () {
		addonController.dispose();
		if(currentWorkplaceStage != null && currentWorkplaceStage.getStage() != null) {
			currentWorkplaceStage.getStage().dispose();
		}
		pickerCursor.dispose();
		uiStage.getStage().dispose();
	}

	public Skin getSkin() {
		return skin;
	}

	public CameraController getCameraController() {
		return currentWorkplaceStage.getCameraController();
	}

	public AddonController Addons() {
		return addonController;
	}

	public FileTracker FileTracker() {
		return fileTracker;
	}

	public WorkplaceStage getNodeStage () {
		return currentWorkplaceStage;
	}
}
