package com.kamesuta.physxmc.utils;

import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 変換用のユーティリティクラス
 */
public final class ConversionUtility {
    
    private ConversionUtility(){
        throw new AssertionError();
    }

    /**
     * クォータニオンをオイラー角に変換
     *
     * @param quaternion クォータニオン
     * @return オイラー角(- 1 ~ 1)
     */
    public static Vector3f convertToEulerAngles(Quaternionf quaternion) {
        Vector3f euler = new Vector3f();
        quaternion.getEulerAnglesXYZ(euler);
        return euler;
    }

    /**
     * オイラー角(-1~1)をクォータニオンに変換
     *
     * @return クォータニオン
     */
    public static Quaternionf convertToQuaternion(double eulerX, double eulerY, double eulerZ) {
        Quaternionf quaternion = new Quaternionf();
        quaternion.rotationXYZ((float) eulerX, (float) eulerY, (float) eulerZ);

        return quaternion;
    }

    /**
     * オイラー角をyaw/pitchに変換
     *
     * @return yaw/pitch
     */
    public static float[] convertToYawPitch(double eulerX, double eulerY, double eulerZ) {
        double yaw = eulerY;
        double pitch = -eulerX;

        yaw = normalizeAngle(yaw);
        pitch = normalizeAngle(pitch);

        return new float[]{(float) yaw, (float) pitch};
    }

    /**
     * yaw/pitchをオイラー角に変換
     *
     * @return オイラー角
     */
    public static Vector3f convertToEulerAngles(double yaw, double pitch) {
        double eulerX = -pitch;
        double eulerY = yaw;
        double eulerZ = 0.0;

        eulerX = normalizeAngle(eulerX);
        eulerY = normalizeAngle(eulerY);

        return new Vector3f((float) eulerX, (float) eulerY, (float) eulerZ);
    }

    /**
     * 角度を正規化
     *
     * @return 正規化された角度
     */
    public static double normalizeAngle(double angle) {
        angle %= 360.0;

        if (angle < -180.0) {
            angle += 360.0;
        } else if (angle > 180.0) {
            angle -= 360.0;
        }

        return angle;
    }

    /**
     * 変換行列を取得する
     *
     * @param transformation 変換
     * @return 変換行列
     */
    public static Matrix4f getTransformationMatrix(Transformation transformation) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.translation(transformation.getTranslation());
        matrix4f.rotate(transformation.getLeftRotation());
        matrix4f.scale(transformation.getScale());
        matrix4f.rotate(transformation.getRightRotation());
        return matrix4f;
    }

    /**
     * チャンクのshortLocを通常のLocationに変換する
     */
    public static Location convertShortLocation(World world, BlockPosition sectionPosition, short shortLoc) {
        int y = (sectionPosition.getY() * 16) + (shortLoc & 0xF);
        int z = (sectionPosition.getZ() * 16) + ((shortLoc >> 4) & 0xF);
        int x = (sectionPosition.getX() * 16) + ((shortLoc >> 8) & 0xF);
        return new Location(world, x, y, z);
    }
}
