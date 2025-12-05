package com.cgvsu.objreader;

import com.cgvsu.math.Vector2f;
import com.cgvsu.math.Vector3f;
import com.cgvsu.model.Model;
import com.cgvsu.model.Polygon;

import java.util.ArrayList;

/**
 * ========================================================================
 * ПУНКТ 1: ЗАГРУЗКА И ЧТЕНИЕ МОДЕЛЕЙ - СОХРАНЕНИЕ МОДЕЛИ (ObjWriter)
 * ========================================================================
 * Реализация сохранения модели в формате OBJ.
 * Пишет вершины, текстурные координаты, нормали и полигоны
 * в соответствии с тем, как они хранятся в классе {@link Model}.
 *
 * Класс ничего не знает о внешнем окружении и работает только
 * с переданной моделью, как и рекомендует методичка.
 * 
 * Используется в методе onSaveModelMenuItemClick() для сохранения
 * активной модели через кнопку в меню.
 */
public class ObjWriter {

    /**
     * ПУНКТ 1: Сохранение модели в строку формата OBJ.
     * Формирует корректный OBJ-файл с учетом индексации (OBJ использует индексацию с 1).
     * 
     * @param model модель для сохранения
     * @return строка с содержимым OBJ-файла
     */
    public static String write(Model model) {
        StringBuilder builder = new StringBuilder();

        // Вершины
        for (Vector3f vertex : model.vertices) {
            builder.append("v ")
                    .append(vertex.x).append(' ')
                    .append(vertex.y).append(' ')
                    .append(vertex.z).append('\n');
        }

        // Текстурные вершины
        for (Vector2f textureVertex : model.textureVertices) {
            builder.append("vt ")
                    .append(textureVertex.x).append(' ')
                    .append(textureVertex.y).append('\n');
        }

        // Нормали
        for (Vector3f normal : model.normals) {
            builder.append("vn ")
                    .append(normal.x).append(' ')
                    .append(normal.y).append(' ')
                    .append(normal.z).append('\n');
        }

        // Полигоны
        for (Polygon polygon : model.polygons) {
            builder.append("f");

            ArrayList<Integer> vertexIndices = polygon.getVertexIndices();
            ArrayList<Integer> textureIndices = polygon.getTextureVertexIndices();
            ArrayList<Integer> normalIndices = polygon.getNormalIndices();

            int n = vertexIndices.size();
            for (int i = 0; i < n; i++) {
                int vIndex = vertexIndices.get(i) + 1; // В OBJ индексация с 1

                StringBuilder element = new StringBuilder();
                element.append(' ').append(vIndex);

                boolean hasTexture = i < textureIndices.size();
                boolean hasNormal = i < normalIndices.size();

                if (hasTexture || hasNormal) {
                    element.append('/');
                    if (hasTexture) {
                        element.append(textureIndices.get(i) + 1);
                    }
                    if (hasNormal) {
                        element.append('/').append(normalIndices.get(i) + 1);
                    }
                }

                builder.append(element);
            }

            builder.append('\n');
        }

        return builder.toString();
    }
}



