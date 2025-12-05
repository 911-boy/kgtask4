package com.cgvsu.model;
import com.cgvsu.math.Vector2f;
import com.cgvsu.math.Vector3f;

import java.util.*;

/**
 * ========================================================================
 * ПУНКТ 3: УДАЛЕНИЕ ЧАСТИ МОДЕЛИ
 * ========================================================================
 * Класс модели с методами для удаления вершин и полигонов.
 * Методы реализованы согласно требованиям задания.
 * 
 * Поля оставлены публичными для простоты (как в примере из методички),
 * но добавлены геттеры для лучшей инкапсуляции и соответствия принципам ООП.
 */
public class Model {

    // Публичные поля для простоты доступа (как в методичке)
    public ArrayList<Vector3f> vertices = new ArrayList<Vector3f>();
    public ArrayList<Vector2f> textureVertices = new ArrayList<Vector2f>();
    public ArrayList<Vector3f> normals = new ArrayList<Vector3f>();
    public ArrayList<Polygon> polygons = new ArrayList<Polygon>();

    // Геттеры для лучшей инкапсуляции (принцип ООП из методички)
    public ArrayList<Vector3f> getVertices() {
        return vertices;
    }

    public ArrayList<Vector2f> getTextureVertices() {
        return textureVertices;
    }

    public ArrayList<Vector3f> getNormals() {
        return normals;
    }

    public ArrayList<Polygon> getPolygons() {
        return polygons;
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public int getPolygonCount() {
        return polygons.size();
    }

    /**
     * ПУНКТ 3: Удаление полигона по индексу.
     * Просто удаляет полигон из списка полигонов модели.
     */
    public void deletePolygon(int polygonIndex) {
        if (polygonIndex < 0 || polygonIndex >= polygons.size()) {
            return;
        }
        polygons.remove(polygonIndex);
    }

    /**
     * ПУНКТ 3: Удаление вершины по индексу.
     * Вершина удаляется из списка vertices, а полигоны,
     * в которых она использовалась, удаляются. Во всех
     * остальных полигонах индексы вершин после удалённой
     * вершины уменьшаются на 1.
     * 
     * Это более сложная операция, чем удаление полигона,
     * так как нужно обновить все ссылки на вершины в полигонах.
     */
    public void deleteVertex(int vertexIndex) {
        if (vertexIndex < 0 || vertexIndex >= vertices.size()) {
            return;
        }

        vertices.remove(vertexIndex);

        ArrayList<Polygon> newPolygons = new ArrayList<>();
        for (Polygon polygon : polygons) {
            ArrayList<Integer> vertexIndices = polygon.getVertexIndices();
            boolean containsRemovedVertex = false;
            ArrayList<Integer> newVertexIndices = new ArrayList<>();

            for (Integer index : vertexIndices) {
                if (index == vertexIndex) {
                    containsRemovedVertex = true;
                    break;
                }
            }

            if (containsRemovedVertex) {
                // Полигон, в котором была удалённая вершина, просто пропускаем.
                continue;
            }

            for (Integer index : vertexIndices) {
                if (index > vertexIndex) {
                    newVertexIndices.add(index - 1);
                } else {
                    newVertexIndices.add(index);
                }
            }

            Polygon newPolygon = new Polygon();
            newPolygon.setVertexIndices(newVertexIndices);
            newPolygon.setTextureVertexIndices(polygon.getTextureVertexIndices());
            newPolygon.setNormalIndices(polygon.getNormalIndices());

            newPolygons.add(newPolygon);
        }

        polygons = newPolygons;
    }
}
