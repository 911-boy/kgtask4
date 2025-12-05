package com.cgvsu;

import com.cgvsu.render_engine.RenderEngine;
import javafx.fxml.FXML;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Vector3f;

import com.cgvsu.model.Model;
import com.cgvsu.objreader.ObjReader;
import com.cgvsu.objreader.ObjReaderException;
import com.cgvsu.objreader.ObjWriter;
import com.cgvsu.render_engine.Camera;

/**
 * ========================================================================
 * ПУНКТ 2: СЦЕНА - РАБОТА С НЕСКОЛЬКИМИ МОДЕЛЯМИ
 * ========================================================================
 * Храним список всех загруженных моделей и индекс активной модели.
 * Активная модель - та, над которой выполняются трансформации и сохранение.
 */
public class GuiController {

    final private float TRANSLATION = 0.5F;

    @FXML
    AnchorPane anchorPane;

    @FXML
    private Canvas canvas;

    // ===== ПУНКТ 4: ИНТЕРФЕЙС - ИНФОРМАТИВНАЯ СТРОКА СТАТУСА =====
    // Строка статуса показывает текущее состояние программы, активную модель и статистику
    @FXML
    private Label statusLabel;

    @FXML
    private Label modelsInfoLabel;

    @FXML
    private Label activeModelLabel;

    @FXML
    private Label modelStatsLabel;

    // ===== ПУНКТ 2: СЦЕНА - СПИСОК МОДЕЛЕЙ И АКТИВНАЯ МОДЕЛЬ =====
    // Список всех загруженных моделей (может быть несколько)
    private final List<Model> models = new ArrayList<>();
    // Индекс активной модели в списке (для трансформаций и сохранения)
    private int activeModelIndex = -1;

    private Camera camera = new Camera(
            new Vector3f(0, 00, 100),
            new Vector3f(0, 0, 0),
            1.0F, 1, 0.01F, 100);

    private Timeline timeline;

    // Вращение модели мышкой
    private float modelRotationX = 0.0f;
    private float modelRotationY = 0.0f;
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean isDragging = false;

    // Выделение полигона
    private int selectedPolygonIndex = -1;

    @FXML
    private void initialize() {
        anchorPane.prefWidthProperty().addListener((ov, oldValue, newValue) -> canvas.setWidth(newValue.doubleValue()));
        anchorPane.prefHeightProperty().addListener((ov, oldValue, newValue) -> canvas.setHeight(newValue.doubleValue()));

        timeline = new Timeline();
        timeline.setCycleCount(Animation.INDEFINITE);

        KeyFrame frame = new KeyFrame(Duration.millis(15), event -> {
            double width = canvas.getWidth();
            double height = canvas.getHeight();

            canvas.getGraphicsContext2D().clearRect(0, 0, width, height);
            camera.setAspectRatio((float) (width / height));

            // Устанавливаем цвет линий в зависимости от темы
            // Проверяем наличие класса dark-theme для определения текущей темы
            javafx.scene.paint.Color strokeColor = anchorPane.getStyleClass().contains("dark-theme") 
                    ? javafx.scene.paint.Color.LIGHTGRAY 
                    : javafx.scene.paint.Color.BLACK;
            canvas.getGraphicsContext2D().setStroke(strokeColor);

            Model activeModel = getActiveModel();
            if (activeModel != null) {
                RenderEngine.render(canvas.getGraphicsContext2D(), camera, activeModel, (int) width, (int) height,
                        modelRotationX, modelRotationY, selectedPolygonIndex, strokeColor);
            }
        });

        timeline.getKeyFrames().add(frame);
        timeline.play();

        setupMouseHandlers();
        setupKeyboardHandlers();
        // По умолчанию устанавливаем темную тему для комфортной работы
        setDarkTheme();
        updateStatusBar();
    }

    private void setupKeyboardHandlers() {
        canvas.setFocusTraversable(true);
        
        // Обработка на канвасе
        canvas.setOnKeyPressed(event -> {
            handleKeyboardZoom(event);
        });
        
        // Обработка на сцене для работы даже когда канвас не в фокусе
        canvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (canvas.isFocused() || event.getTarget() == canvas) {
                        handleKeyboardZoom(event);
                    }
                });
            }
        });
    }

    private void handleKeyboardZoom(KeyEvent event) {
        Vector3f position = camera.getPosition();
        Vector3f target = camera.getTarget();
        
        // Вычисляем направление от камеры к цели
        Vector3f direction = new Vector3f();
        direction.sub(target, position);
        float distance = direction.length();
        
        if (distance < 0.0001f) return;
        
        direction.normalize();
        float zoomStep = TRANSLATION * 2.0f; // Шаг зума
        float rotationStep = 0.05f; // Шаг вращения модели
        
        switch (event.getCode()) {
            case UP:
            case PLUS:
            case EQUALS:
                // Приближение - двигаем камеру к цели
                direction.scale(zoomStep);
                Vector3f newPosForward = new Vector3f(position);
                newPosForward.add(direction);
                camera.setPosition(newPosForward);
                break;
            case DOWN:
            case MINUS:
                // Отдаление - двигаем камеру от цели
                direction.scale(-zoomStep);
                Vector3f newPosBackward = new Vector3f(position);
                newPosBackward.add(direction);
                camera.setPosition(newPosBackward);
                break;
            case LEFT:
                // Вращение модели влево
                modelRotationY -= rotationStep;
                break;
            case RIGHT:
                // Вращение модели вправо
                modelRotationY += rotationStep;
                break;
            case W:
                // Вращение модели вверх
                modelRotationX -= rotationStep;
                // Ограничение углов
                if (modelRotationX < -Math.PI / 2) modelRotationX = (float) (-Math.PI / 2);
                break;
            case S:
                // Вращение модели вниз
                modelRotationX += rotationStep;
                // Ограничение углов
                if (modelRotationX > Math.PI / 2) modelRotationX = (float) (Math.PI / 2);
                break;
        }
    }

    private void setupMouseHandlers() {
        // Установка фокуса на канвас при клике
        canvas.setOnMouseClicked(event -> {
            canvas.requestFocus();
        });
        
        // Вращение модели перетаскиванием мыши
        canvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                isDragging = true;
                lastMouseX = event.getX();
                lastMouseY = event.getY();
            }
            canvas.requestFocus();
        });

        canvas.setOnMouseDragged(event -> {
            if (isDragging && event.getButton() == MouseButton.PRIMARY) {
                double deltaX = event.getX() - lastMouseX;
                double deltaY = event.getY() - lastMouseY;

                // Вращение модели
                modelRotationY += (float) (deltaX * 0.01);
                modelRotationX += (float) (deltaY * 0.01);

                // Ограничение углов
                if (modelRotationX > Math.PI / 2) modelRotationX = (float) (Math.PI / 2);
                if (modelRotationX < -Math.PI / 2) modelRotationX = (float) (-Math.PI / 2);

                lastMouseX = event.getX();
                lastMouseY = event.getY();
            }
        });

        canvas.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                isDragging = false;
            }
        });

        // Выделение полигона кликом правой кнопкой мыши
        canvas.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                Model activeModel = getActiveModel();
                if (activeModel != null) {
                    int polygonIndex = RenderEngine.findPolygonAtPoint(
                            camera, activeModel, (int) canvas.getWidth(), (int) canvas.getHeight(),
                            modelRotationX, modelRotationY, event.getX(), event.getY());

                    if (polygonIndex >= 0) {
                        selectedPolygonIndex = polygonIndex;
                        updateStatusBar();
                        statusLabel.setText("✓ Polygon #" + polygonIndex + " selected (Click 'Delete Selected' to remove)");
                    } else {
                        selectedPolygonIndex = -1;
                        updateStatusBar();
                        statusLabel.setText("Ready");
                    }
                }
            }
        });

        // Зум колесиком мыши
        canvas.setOnScroll((ScrollEvent event) -> {
            double deltaY = event.getDeltaY();
            Vector3f position = camera.getPosition();
            float zoomFactor = (float) (deltaY > 0 ? 0.9 : 1.1);
            
            Vector3f direction = new Vector3f();
            direction.sub(camera.getTarget(), position);
            direction.normalize();
            direction.scale((float) (position.length() * (1 - zoomFactor)));
            
            Vector3f newPosition = new Vector3f(position);
            newPosition.add(direction);
            camera.setPosition(newPosition);
        });
    }

    private void updateStatusBar() {
        Model activeModel = getActiveModel();
        
        // Обновление информации о моделях
        modelsInfoLabel.setText("Models: " + models.size());
        
        // Обновление информации об активной модели
        if (activeModel != null && activeModelIndex >= 0) {
            activeModelLabel.setText("Active: Model #" + (activeModelIndex + 1));
            modelStatsLabel.setText("Vertices: " + activeModel.vertices.size() + 
                                   " | Polygons: " + activeModel.polygons.size());
            statusLabel.setText("✓ Ready");
        } else {
            activeModelLabel.setText("Active: None");
            modelStatsLabel.setText("Vertices: 0 | Polygons: 0");
            if (models.isEmpty()) {
                statusLabel.setText("Ready - Load a model to start");
            } else {
                statusLabel.setText("Ready");
            }
        }
     }

    /**
     * ========================================================================
     * ПУНКТ 1: ЗАГРУЗКА И ЧТТЕНИЕ МОДЕЛЕЙ
     * ========================================================================
     * Загрузка OBJ-файла через диалог выбора файла.
     * Используется качественный ObjReader для чтения модели.
     * Загруженная модель добавляется в список моделей и становится активной.
     * 
     * ПУНКТ 5: ОБРАБОТКА ОШИБОК
     * При ошибках чтения (ObjReaderException, IOException) показывается
     * окно с ошибкой, чтобы пользователь мог обдумать свои действия.
     */
    @FXML
    private void onOpenModelMenuItemClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Model (*.obj)", "*.obj"));
        fileChooser.setTitle("Load Model");

        File file = fileChooser.showOpenDialog((Stage) canvas.getScene().getWindow());
        if (file == null) {
            return;
        }

        Path fileName = Path.of(file.getAbsolutePath());

        try {
            // ПУНКТ 1: Чтение файла и парсинг через ObjReader
            String fileContent = Files.readString(fileName);
            Model loadedModel = ObjReader.read(fileContent);
            
            // ПУНКТ 2: Добавление модели в список и установка как активной
            models.add(loadedModel);
            activeModelIndex = models.size() - 1;
            updateStatusBar();
            statusLabel.setText("✓ Model loaded: " + file.getName());
        } catch (ObjReaderException exception) {
            // ПУНКТ 5: Обработка ошибок парсинга OBJ-файла
            showError("Ошибка при чтении OBJ-файла", exception.getMessage());
            statusLabel.setText("✗ Error loading model");
        } catch (IOException exception) {
            // ПУНКТ 5: Обработка ошибок чтения файла
            showError("Ошибка при чтении файла", exception.getMessage());
            statusLabel.setText("✗ Error reading file");
        }
    }

    /**
     * ========================================================================
     * ПУНКТ 1: ЗАГРУЗКА И ЧТЕНИЕ МОДЕЛЕЙ - СОХРАНЕНИЕ МОДЕЛИ
     * ========================================================================
     * Сохранение активной модели в OBJ-файл с помощью ObjWriter.
     * Сохранение происходит через кнопку в меню (File -> Save Active Model).
     * 
     * ПУНКТ 5: ОБРАБОТКА ОШИБОК
     * При ошибке сохранения показывается окно с ошибкой.
     */
    @FXML
    private void onSaveModelMenuItemClick() {
        // ПУНКТ 2: Получаем активную модель для сохранения
        Model activeModel = getActiveModel();
        if (activeModel == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Model (*.obj)", "*.obj"));
        fileChooser.setTitle("Save Model");

        File file = fileChooser.showSaveDialog((Stage) canvas.getScene().getWindow());
        if (file == null) {
            return;
        }

        // ПУНКТ 1: Используем ObjWriter для сохранения модели
        String content = ObjWriter.write(activeModel);
        try {
            Files.writeString(file.toPath(), content);
            statusLabel.setText("✓ Model saved: " + file.getName());
        } catch (IOException exception) {
            // ПУНКТ 5: Обработка ошибок сохранения
            showError("Ошибка при сохранении файла", exception.getMessage());
            statusLabel.setText("✗ Error saving model");
        }
    }

    /**
     * ========================================================================
     * ПУНКТ 2: СЦЕНА - ПЕРЕКЛЮЧЕНИЕ МЕЖДУ МОДЕЛЯМИ
     * ========================================================================
     * Переход к следующей модели как к активной.
     * Предусмотрено для дальнейших трансформаций только над одной моделью.
     * Пользователь может выбирать, какая модель активна для трансформаций и сохранения.
     */
    @FXML
    private void onNextModelMenuItemClick() {
        if (models.isEmpty()) {
            activeModelIndex = -1;
            selectedPolygonIndex = -1;
            updateStatusBar();
            return;
        }
        // Циклическое переключение: после последней модели переходим к первой
        activeModelIndex = (activeModelIndex + 1) % models.size();
        selectedPolygonIndex = -1; // Сбрасываем выделение при смене модели
        updateStatusBar();
    }

    /**
     * ПУНКТ 2: СЦЕНА - ПЕРЕКЛЮЧЕНИЕ К ПРЕДЫДУЩЕЙ МОДЕЛИ
     */
    @FXML
    private void onPreviousModelMenuItemClick() {
        if (models.isEmpty()) {
            activeModelIndex = -1;
            selectedPolygonIndex = -1;
            updateStatusBar();
            return;
        }
        // Циклическое переключение: перед первой моделью переходим к последней
        activeModelIndex = (activeModelIndex - 1 + models.size()) % models.size();
        selectedPolygonIndex = -1; // Сбрасываем выделение при смене модели
        updateStatusBar();
    }

    /**
     * ПУНКТ 2: СЦЕНА - ПОЛУЧЕНИЕ АКТИВНОЙ МОДЕЛИ
     * Доступ к активной модели для других частей программы
     * (например, для трансформаций, реализуемых другими студентами).
     * Только активная модель может быть трансформирована и сохранена.
     */
    public Model getActiveModel() {
        if (activeModelIndex < 0 || activeModelIndex >= models.size()) {
            return null;
        }
        return models.get(activeModelIndex);
    }

    /**
     * Удаление активной модели из сцены.
     * После удаления активной становится предыдущая модель (если есть),
     * или индекс сбрасывается, если это была последняя модель.
     */
    @FXML
    private void onDeleteActiveModelClick() {
        if (models.isEmpty()) {
            showInfo("Нет загруженных моделей для удаления.");
            return;
        }

        if (activeModelIndex < 0 || activeModelIndex >= models.size()) {
            showInfo("Нет активной модели для удаления.");
            return;
        }

        // Удаляем активную модель
        models.remove(activeModelIndex);
        selectedPolygonIndex = -1; // Сбрасываем выделение

        // Обновляем индекс активной модели
        if (models.isEmpty()) {
            // Если это была последняя модель
            activeModelIndex = -1;
        } else if (activeModelIndex >= models.size()) {
            // Если удалили последнюю модель в списке, переходим к предыдущей
            activeModelIndex = models.size() - 1;
        }
        // Если удалили не последнюю, индекс остается корректным (смещается автоматически)

        updateStatusBar();
        statusLabel.setText("✓ Active model deleted");
    }

    // ========================================================================
    // ПУНКТ 3: УДАЛЕНИЕ ЧАСТИ МОДЕЛИ
    // ========================================================================
    // Возможность удалять вершины и полигоны внутри программы.
    // Интерфейс продуман самостоятельно: диалоги ввода индекса и выделение мышью.

    /**
     * ПУНКТ 3: Удаление полигона по индексу через диалог ввода.
     */
    @FXML
    private void onDeletePolygonMenuItemClick() {
        Model model = getActiveModel();
        if (model == null || model.polygons.isEmpty()) {
            showInfo("Нет активной модели или в модели нет полигонов.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Delete polygon");
        dialog.setHeaderText("Удаление полигона");
        dialog.setContentText("Введите индекс полигона (0.." + (model.polygons.size() - 1) + "):");

        dialog.showAndWait().ifPresent(text -> {
            try {
                int index = Integer.parseInt(text);
                if (index < 0 || index >= model.polygons.size()) {
                    showInfo("Индекс должен быть в диапазоне 0.." + (model.polygons.size() - 1));
                    return;
                }
                model.deletePolygon(index);
                if (selectedPolygonIndex == index) {
                    selectedPolygonIndex = -1;
                } else if (selectedPolygonIndex > index) {
                    selectedPolygonIndex--;
                }
                updateStatusBar();
                statusLabel.setText("✓ Polygon #" + index + " deleted");
            } catch (NumberFormatException e) {
                showInfo("Индекс должен быть целым числом.");
            }
        });
    }

    /**
     * ПУНКТ 3: Удаление выделенного полигона (выделение через правый клик мыши).
     * Более удобный способ удаления - выделил мышью и удалил кнопкой.
     */
    @FXML
    private void onDeleteSelectedPolygonClick() {
        Model model = getActiveModel();
        if (model == null) {
            showInfo("Нет активной модели.");
            return;
        }

        if (selectedPolygonIndex < 0 || selectedPolygonIndex >= model.polygons.size()) {
            showInfo("Полигон не выбран. Кликните правой кнопкой мыши по полигону для выделения.");
            return;
        }

        int index = selectedPolygonIndex;
        model.deletePolygon(index);
        selectedPolygonIndex = -1;
        updateStatusBar();
        statusLabel.setText("✓ Selected polygon deleted");
    }

    /**
     * ПУНКТ 3: Удаление вершины по индексу через диалог ввода.
     */
    @FXML
    private void onDeleteVertexMenuItemClick() {
        Model model = getActiveModel();
        if (model == null || model.vertices.isEmpty()) {
            showInfo("Нет активной модели или в модели нет вершин.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Delete vertex");
        dialog.setHeaderText("Удаление вершины");
        dialog.setContentText("Введите индекс вершины (0.." + (model.vertices.size() - 1) + "):");

        dialog.showAndWait().ifPresent(text -> {
            try {
                int index = Integer.parseInt(text);
                if (index < 0 || index >= model.vertices.size()) {
                    showInfo("Индекс должен быть в диапазоне 0.." + (model.vertices.size() - 1));
                    return;
                }
                model.deleteVertex(index);
                updateStatusBar();
                statusLabel.setText("✓ Vertex #" + index + " deleted");
            } catch (NumberFormatException e) {
                showInfo("Индекс должен быть целым числом.");
            }
        });
    }

    /**
     * ========================================================================
     * ПУНКТ 5: ОБРАБОТКА ОШИБОК - ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
     * ========================================================================
     * Методы для показа информационных сообщений и ошибок пользователю.
     * Используются для предотвращения зависания или падения программы.
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * ПУНКТ 5: Показ окна с ошибкой, чтобы пользователь мог обдумать свои действия
     * и нажать "OK", а не получить зависание или падение программы.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========================================================================
    // ПУНКТ 4: ИНТЕРФЕЙС - ДИНАМИЧЕСКОЕ ПЕРЕКЛЮЧЕНИЕ ТЕМ
    // ========================================================================
    // Реализовано переключение тем: "светлая/темная".
    // Пользователь может выбрать удобную для работы тему оформления.

    @FXML
    private void onSetLightThemeMenuItemClick() {
        setLightTheme();
    }

    @FXML
    private void onSetDarkThemeMenuItemClick() {
        setDarkTheme();
    }

    /**
     * ПУНКТ 4: Установка светлой темы оформления.
     */
    private void setLightTheme() {
        anchorPane.setStyle("-fx-background-color: #f4f4f4;");
        anchorPane.getStyleClass().remove("dark-theme");
        canvas.getGraphicsContext2D().setStroke(javafx.scene.paint.Color.BLACK);
        
        // Применяем светлую тему ко всей сцене и всем дочерним элементам
        if (canvas.getScene() != null) {
            javafx.scene.Node root = canvas.getScene().getRoot();
            root.getStyleClass().remove("dark-theme");
            // Убираем темную тему со всех дочерних элементов
            root.getStyleClass().remove("dark-theme");
        }
    }

    /**
     * ПУНКТ 4: Установка темной темы оформления.
     * Полностью темная тема для комфортной работы в темное время суток.
     * Все элементы интерфейса становятся темными: меню, панели, диалоги, статус-бар.
     */
    private void setDarkTheme() {
        anchorPane.setStyle("-fx-background-color: #1a1a1a;");
        if (!anchorPane.getStyleClass().contains("dark-theme")) {
            anchorPane.getStyleClass().add("dark-theme");
        }
        canvas.getGraphicsContext2D().setStroke(javafx.scene.paint.Color.LIGHTGRAY);
        
        // Применяем темную тему ко всей сцене и всем дочерним элементам
        if (canvas.getScene() != null) {
            javafx.scene.Node root = canvas.getScene().getRoot();
            if (!root.getStyleClass().contains("dark-theme")) {
                root.getStyleClass().add("dark-theme");
            }
        }
    }

    @FXML
    public void handleCameraForward(ActionEvent actionEvent) {
        // Зум - приближение
        Vector3f position = camera.getPosition();
        Vector3f target = camera.getTarget();
        Vector3f direction = new Vector3f();
        direction.sub(target, position);
        float distance = direction.length();
        if (distance > 0.0001f) {
            direction.normalize();
            direction.scale(TRANSLATION * 2.0f);
            Vector3f newPos = new Vector3f(position);
            newPos.add(direction);
            camera.setPosition(newPos);
        }
    }

    @FXML
    public void handleCameraBackward(ActionEvent actionEvent) {
        // Зум - отдаление
        Vector3f position = camera.getPosition();
        Vector3f target = camera.getTarget();
        Vector3f direction = new Vector3f();
        direction.sub(target, position);
        float distance = direction.length();
        if (distance > 0.0001f) {
            direction.normalize();
            direction.scale(-TRANSLATION * 2.0f);
            Vector3f newPos = new Vector3f(position);
            newPos.add(direction);
            camera.setPosition(newPos);
        }
    }

    @FXML
    public void handleCameraLeft(ActionEvent actionEvent) {
        // Вращение модели влево
        modelRotationY -= 0.05f;
    }

    @FXML
    public void handleCameraRight(ActionEvent actionEvent) {
        // Вращение модели вправо
        modelRotationY += 0.05f;
    }

    @FXML
    public void handleCameraUp(ActionEvent actionEvent) {
        // Вращение модели вверх
        modelRotationX -= 0.05f;
        // Ограничение углов
        if (modelRotationX < -Math.PI / 2) modelRotationX = (float) (-Math.PI / 2);
    }

    @FXML
    public void handleCameraDown(ActionEvent actionEvent) {
        // Вращение модели вниз
        modelRotationX += 0.05f;
        // Ограничение углов
        if (modelRotationX > Math.PI / 2) modelRotationX = (float) (Math.PI / 2);
    }

    @FXML
    private void onShowHelpClick() {
        String helpText = "📖 Simple3DViewer - Инструкция\n\n" +
                "🖱 УПРАВЛЕНИЕ МЫШЬЮ:\n" +
                "• Левый клик + перетаскивание - Вращение модели\n" +
                "• Колесико мыши - Приближение/отдаление (зум)\n" +
                "• Правый клик по полигону - Выделение полигона (красным цветом)\n\n" +
                "⌨ УПРАВЛЕНИЕ КЛАВИАТУРОЙ:\n" +
                "• Стрелки ↑↓ - Приближение/отдаление (зум)\n" +
                "• Стрелки ←→ - Вращение модели влево/вправо\n" +
                "• +/- - Приближение/отдаление (зум)\n" +
                "• W/S - Вращение модели вверх/вниз\n\n" +
                "📂 ФАЙЛЫ:\n" +
                "• Load - Загрузить OBJ модель\n" +
                "• Save - Сохранить активную модель\n\n" +
                "🎨 СЦЕНА:\n" +
                "• Prev/Next Model - Переключение между загруженными моделями\n" +
                "• Del Model - Удаление активной модели из сцены\n" +
                "• В строке статуса отображается активная модель и статистика\n\n" +
                "✂ РЕДАКТИРОВАНИЕ:\n" +
                "• Del Polygon - Удалить полигон по индексу\n" +
                "• Del Selected - Удалить выделенный полигон (правый клик)\n" +
                "• Del Vertex - Удалить вершину по индексу\n\n" +
                "🎭 ТЕМЫ:\n" +
                "• Light/Dark - Переключение светлой/темной темы\n\n" +
                "💡 СОВЕТЫ:\n" +
                "• Выделите полигон правым кликом, затем нажмите 'Del Selected'\n" +
                "• Используйте колесико мыши для удобного просмотра деталей\n" +
                "• Вращайте модель для лучшего обзора со всех сторон";

        Alert helpAlert = new Alert(Alert.AlertType.INFORMATION);
        helpAlert.setTitle("Инструкция");
        helpAlert.setHeaderText("Simple3DViewer - Руководство пользователя");
        helpAlert.setContentText(helpText);
        helpAlert.setResizable(true);
        helpAlert.getDialogPane().setPrefWidth(600);
        helpAlert.showAndWait();
    }
}