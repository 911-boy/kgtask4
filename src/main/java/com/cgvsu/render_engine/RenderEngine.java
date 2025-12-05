package com.cgvsu.render_engine;

import java.util.ArrayList;

import com.cgvsu.math.Vector3f;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javax.vecmath.*;
import com.cgvsu.model.Model;
import static com.cgvsu.render_engine.GraphicConveyor.*;

public class RenderEngine {

    public static void render(
            final GraphicsContext graphicsContext,
            final Camera camera,
            final Model mesh,
            final int width,
            final int height,
            final float rotationX,
            final float rotationY,
            final int selectedPolygonIndex,
            final javafx.scene.paint.Color defaultColor)
    {
        Matrix4f modelMatrix = createModelMatrix(rotationX, rotationY);
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = camera.getProjectionMatrix();

        Matrix4f modelViewProjectionMatrix = new Matrix4f(modelMatrix);
        modelViewProjectionMatrix.mul(viewMatrix);
        modelViewProjectionMatrix.mul(projectionMatrix);

        final int nPolygons = mesh.polygons.size();
        for (int polygonInd = 0; polygonInd < nPolygons; ++polygonInd) {
            final int nVerticesInPolygon = mesh.polygons.get(polygonInd).getVertexIndices().size();

            ArrayList<Point2f> resultPoints = new ArrayList<>();
            for (int vertexInPolygonInd = 0; vertexInPolygonInd < nVerticesInPolygon; ++vertexInPolygonInd) {
                Vector3f vertex = mesh.vertices.get(mesh.polygons.get(polygonInd).getVertexIndices().get(vertexInPolygonInd));

                javax.vecmath.Vector3f vertexVecmath = new javax.vecmath.Vector3f(vertex.x, vertex.y, vertex.z);

                Point2f resultPoint = vertexToPoint(multiplyMatrix4ByVector3(modelViewProjectionMatrix, vertexVecmath), width, height);
                resultPoints.add(resultPoint);
            }

            // Выделение выбранного полигона другим цветом
            if (polygonInd == selectedPolygonIndex) {
                graphicsContext.setStroke(Color.RED);
                graphicsContext.setLineWidth(3.0);
            } else {
                graphicsContext.setStroke(defaultColor);
                graphicsContext.setLineWidth(1.0);
            }

            for (int vertexInPolygonInd = 1; vertexInPolygonInd < nVerticesInPolygon; ++vertexInPolygonInd) {
                graphicsContext.strokeLine(
                        resultPoints.get(vertexInPolygonInd - 1).x,
                        resultPoints.get(vertexInPolygonInd - 1).y,
                        resultPoints.get(vertexInPolygonInd).x,
                        resultPoints.get(vertexInPolygonInd).y);
            }

            if (nVerticesInPolygon > 0)
                graphicsContext.strokeLine(
                        resultPoints.get(nVerticesInPolygon - 1).x,
                        resultPoints.get(nVerticesInPolygon - 1).y,
                        resultPoints.get(0).x,
                        resultPoints.get(0).y);
        }
    }

    private static Matrix4f createModelMatrix(float rotationX, float rotationY) {
        // Матрица вращения вокруг оси X
        Matrix4f rotX = new Matrix4f();
        rotX.setIdentity();
        float cosX = (float) Math.cos(rotationX);
        float sinX = (float) Math.sin(rotationX);
        rotX.m11 = cosX;
        rotX.m12 = -sinX;
        rotX.m21 = sinX;
        rotX.m22 = cosX;

        // Матрица вращения вокруг оси Y
        Matrix4f rotY = new Matrix4f();
        rotY.setIdentity();
        float cosY = (float) Math.cos(rotationY);
        float sinY = (float) Math.sin(rotationY);
        rotY.m00 = cosY;
        rotY.m02 = sinY;
        rotY.m20 = -sinY;
        rotY.m22 = cosY;

        // Комбинируем вращения
        Matrix4f result = new Matrix4f();
        result.mul(rotY, rotX);
        return result;
    }

    /**
     * Находит полигон под указанной точкой на экране
     */
    public static int findPolygonAtPoint(
            final Camera camera,
            final Model mesh,
            final int width,
            final int height,
            final float rotationX,
            final float rotationY,
            final double screenX,
            final double screenY)
    {
        Matrix4f modelMatrix = createModelMatrix(rotationX, rotationY);
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = camera.getProjectionMatrix();

        Matrix4f modelViewProjectionMatrix = new Matrix4f(modelMatrix);
        modelViewProjectionMatrix.mul(viewMatrix);
        modelViewProjectionMatrix.mul(projectionMatrix);

        final double threshold = 10.0; // Радиус поиска в пикселях

        for (int polygonInd = 0; polygonInd < mesh.polygons.size(); ++polygonInd) {
            final int nVerticesInPolygon = mesh.polygons.get(polygonInd).getVertexIndices().size();
            
            ArrayList<Point2f> resultPoints = new ArrayList<>();
            for (int vertexInPolygonInd = 0; vertexInPolygonInd < nVerticesInPolygon; ++vertexInPolygonInd) {
                Vector3f vertex = mesh.vertices.get(mesh.polygons.get(polygonInd).getVertexIndices().get(vertexInPolygonInd));
                javax.vecmath.Vector3f vertexVecmath = new javax.vecmath.Vector3f(vertex.x, vertex.y, vertex.z);
                Point2f resultPoint = vertexToPoint(multiplyMatrix4ByVector3(modelViewProjectionMatrix, vertexVecmath), width, height);
                resultPoints.add(resultPoint);
            }

            // Проверяем, находится ли точка внутри полигона или рядом с его границами
            if (isPointNearPolygon(screenX, screenY, resultPoints, threshold)) {
                return polygonInd;
            }
        }

        return -1;
    }

    private static boolean isPointNearPolygon(double x, double y, ArrayList<Point2f> polygonPoints, double threshold) {
        // Проверка близости к границам полигона
        for (int i = 0; i < polygonPoints.size(); ++i) {
            Point2f p1 = polygonPoints.get(i);
            Point2f p2 = polygonPoints.get((i + 1) % polygonPoints.size());
            
            double dist = pointToLineDistance(x, y, p1.x, p1.y, p2.x, p2.y);
            if (dist < threshold) {
                return true;
            }
        }
        return false;
    }

    private static double pointToLineDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double A = px - x1;
        double B = py - y1;
        double C = x2 - x1;
        double D = y2 - y1;

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = lenSq != 0 ? dot / lenSq : -1;

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        double dx = px - xx;
        double dy = py - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }
}