package com.rockbite.tools.talos.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.widget.Menu;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;
import com.rockbite.tools.talos.TalosMain;
import com.rockbite.tools.talos.editor.widgets.ui.PreviewWidget;
import com.rockbite.tools.talos.editor.widgets.ui.TimelineWidget;

import java.io.File;
import java.io.FileFilter;

public class UIStage {

	private final Stage stage;
	private final Skin skin;

	Table fullScreenTable;

	private TimelineWidget timelineWidget;
	private PreviewWidget previewWidget;

	FileChooser fileChooser;


	public UIStage (Skin skin) {
		stage = new Stage(new ScreenViewport());
		this.skin = skin;
	}

	public void init () {
		fullScreenTable = new Table();
		fullScreenTable.setFillParent(true);

		stage.addActor(fullScreenTable);

		defaults();
		constructMenu();
		constructSplitPanes();

		initFileChoosers();
	}

	public Stage getStage () {
		return stage;
	}

	private void defaults () {
		fullScreenTable.top().left();
	}

	private void constructMenu () {
		Table topTable = new Table();
		topTable.setBackground(skin.getDrawable("button-main-menu"));

		MenuBar menuBar = new MenuBar();
		Menu projectMenu = new Menu("File");
		menuBar.addMenu(projectMenu);
		Menu helpMenu = new Menu("Help");
		MenuItem about = new MenuItem("About");
		helpMenu.addItem(about);
		menuBar.addMenu(helpMenu);

		about.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				VisDialog dialog = Dialogs.showOKDialog(stage, "About Talos 1.0.2", "Talos Particle Editor 1.0.2");
			}
		});

		MenuItem newProject = new MenuItem("New Project");
		MenuItem openProject = new MenuItem("Open Project");
		MenuItem saveProject = new MenuItem("Save Project");
		MenuItem examples = new MenuItem("Examples");
		MenuItem importItem = new MenuItem("Legacy Import");
		PopupMenu examplesPopup = new PopupMenu();
		examples.setSubMenu(examplesPopup);
		initExampleList(examplesPopup);
		MenuItem saveAsProject = new MenuItem("Save As Project");
		MenuItem exitApp = new MenuItem("Exit");

		projectMenu.addItem(newProject);
		projectMenu.addItem(openProject);
		projectMenu.addItem(saveProject);
		projectMenu.addItem(saveAsProject);
		projectMenu.addSeparator();
		projectMenu.addItem(examples);
		projectMenu.addItem(importItem);
		projectMenu.addSeparator();
		projectMenu.addItem(exitApp);

		newProject.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				newProjectAction();
			}
		});

		openProject.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				openProjectAction();
			}
		});

		saveProject.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				saveProjectAction();
			}
		});

		saveAsProject.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				saveAsProjectAction();
			}
		});

		importItem.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				legacyImportAction();
			}
		});

		exitApp.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				System.exit(0);
			}
		});

		topTable.add(menuBar.getTable()).left().grow();

		fullScreenTable.add(topTable).growX();

		// adding key listeners for menu items
		stage.addListener(new InputListener() {
			@Override
			public boolean keyDown(InputEvent event, int keycode) {
				if(keycode == Input.Keys.N && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
					TalosMain.Instance().Project().newProject();
				}
				if(keycode == Input.Keys.O && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
					openProjectAction();
				}
				if(keycode == Input.Keys.S && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
					saveProjectAction();
				}

				return super.keyDown(event, keycode);
			}

		});
	}

	private void initFileChoosers() {
		fileChooser = new FileChooser(FileChooser.Mode.SAVE);
		fileChooser.setBackground(skin.getDrawable("window-noborder"));
	}


	private void newProjectAction() {
		TalosMain.Instance().Project().newProject();
	}


	private void openProjectAction() {
		fileChooser.setMode(FileChooser.Mode.OPEN);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory() || pathname.getAbsolutePath().endsWith(".tls");
			}
		});
		fileChooser.setSelectionMode(FileChooser.SelectionMode.FILES);

		fileChooser.setListener(new FileChooserAdapter() {
			@Override
			public void selected (Array<FileHandle> file) {
				TalosMain.Instance().Project().loadProject(Gdx.files.absolute(file.first().file().getAbsolutePath()));
			}
		});

		stage.addActor(fileChooser.fadeIn());
	}

	private void saveProjectAction() {
		if(!TalosMain.Instance().Project().isBoundToFile()) {
			saveAsProjectAction();
		} else {
			TalosMain.Instance().Project().saveProject();
		}
	}

	private void saveAsProjectAction() {
		fileChooser.setMode(FileChooser.Mode.SAVE);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory() || pathname.getAbsolutePath().endsWith(".tls");
			}
		});
		fileChooser.setSelectionMode(FileChooser.SelectionMode.FILES);

		fileChooser.setListener(new FileChooserAdapter() {
			@Override
			public void selected(Array<FileHandle> file) {
				String path = file.first().file().getAbsolutePath();
				if(!path.endsWith(".tls")) {
					if(path.indexOf(".") > 0) {
						path = path.substring(0, path.indexOf("."));
					}
					path += ".tls";
				}
				FileHandle handle = Gdx.files.absolute(path);
				TalosMain.Instance().Project().saveProject(handle);
			}
		});

		stage.addActor(fileChooser.fadeIn());
	}


	public void legacyImportAction() {
		fileChooser.setMode(FileChooser.Mode.OPEN);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileFilter(new FileChooser.DefaultFileFilter(fileChooser));
		fileChooser.setSelectionMode(FileChooser.SelectionMode.FILES);

		fileChooser.setListener(new FileChooserAdapter() {
			@Override
			public void selected (Array<FileHandle> file) {
				TalosMain.Instance().Project().importFromLegacyFormat(file.get(0));
			}
		});

		stage.addActor(fileChooser.fadeIn());
	}


	private void constructSplitPanes () {
		previewWidget = new PreviewWidget();

		timelineWidget = new TimelineWidget(skin);

		Table midTable = new Table();
		Table bottomTable = new Table();
		bottomTable.setSkin(skin);
		bottomTable.setBackground(skin.getDrawable("button-main-menu"));

		Table timelineContainer = new Table();
		Table libraryContainer = new Table();

		libraryContainer.addListener(new ClickListener(0) { //Quick hack for library container intercepting touch as its an empty table currently
			@Override
			public void clicked (InputEvent event, float x, float y) {
			}
		});
		libraryContainer.addListener(new ClickListener(1) { //Quick hack for library container intercepting touch as its an empty table currently
			@Override
			public void clicked (InputEvent event, float x, float y) {
			}
		});
		libraryContainer.setTouchable(Touchable.enabled);
		VisSplitPane bottomPane = new VisSplitPane(timelineContainer, libraryContainer, false);

		timelineContainer.add(timelineWidget).grow().expand().fill();
		bottomTable.add(bottomPane).expand().grow();

		VisSplitPane verticalPane = new VisSplitPane(midTable, bottomTable, true);
		verticalPane.setMaxSplitAmount(0.8f);
		verticalPane.setMinSplitAmount(0.2f);
		verticalPane.setSplitAmount(0.7f);

		Table leftTable = new Table(); leftTable.setSkin(skin);
		leftTable.add(previewWidget).grow();
		Table rightTable = new Table(); rightTable.setSkin(skin);
		rightTable.add().grow();
		VisSplitPane horizontalPane = new VisSplitPane(leftTable, rightTable, false);
		midTable.add(horizontalPane).expand().grow().fill();
		horizontalPane.setMaxSplitAmount(0.8f);
		horizontalPane.setMinSplitAmount(0.2f);
		horizontalPane.setSplitAmount(0.3f);

		fullScreenTable.row();
		fullScreenTable.add(verticalPane).grow();
	}


	private void initExampleList (PopupMenu examples) {
		FileHandle dir = Gdx.files.internal("samples");
		for (final FileHandle file : dir.list()) {
			if (file.extension().equals("tls")) {
				MenuItem item = new MenuItem(file.name());
				examples.addItem(item);

				item.addListener(new ClickListener() {
					@Override
					public void clicked (InputEvent event, float x, float y) {
						super.clicked(event, x, y);
						//openProject(file);
						//currentProjectPath = null;
					}
				});
			}
		}
	}

	public TimelineWidget Timeline() {
		return timelineWidget;
	}

	public void resize (int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	public void setEmitters (Array<ParticleEmitterWrapper> emitterWrappers) {
		timelineWidget.setEmitters(emitterWrappers);
	}
}
