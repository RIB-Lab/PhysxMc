package com.kamesuta.physxmc

import java.io.File
import java.net.URL
import java.nio.file.Files

object PhysxLoader {
    /** プラグインのクラスローダー */
    private val pluginClassLoader = PhysxLoader::class.java.classLoader

    /** アプリケーションクラスローダー */
    private val appClassLoader = pluginClassLoader.parent

    /** ライブラリのバージョン */
    private const val physxJniVersion = "1.1.0"

    /**
     * PhysXライブラリをアプリケーションクラスローダーにロードします。
     * プラグインローダーでロードするとプラグインのリロードに失敗するため、アプリケーションクラスローダーに直接ロードします。
     */
    fun loadPhysxOnAppClassloader() {
        // 全体のクラスローダーに登録
        val forceCopy = "true".equals(System.getProperty("physx.forceCopyLibs", "false"), ignoreCase = true)
        val libFile = copyLibsFromResources(forceCopy)
        val ucp = appClassLoader.javaClass.getDeclaredField("ucp").apply { isAccessible = true }
        val addURL = ucp.type.getMethod("addURL", URL::class.java).apply { isAccessible = true }
        addURL(ucp[appClassLoader], libFile.toURI().toURL())

        // 全体のクラスローダーで読み込む
        val loaderClass = appClassLoader.loadClass("de.fabmax.physxjni.Loader")
        val loadMethod = loaderClass.getMethod("load")
        loadMethod(null)
    }

    /**
     * リソースからライブラリをコピーします。
     * 展開先は `${java.io.tmpdir}/de.fabmax.physx-jni/${physxJniVersion}/physxmc-libs.jar` になります。
     */
    private fun copyLibsFromResources(forceCopy: Boolean): File {
        val tempLibDir =
            File(System.getProperty("java.io.tmpdir"), "de.fabmax.physx-jni${File.separator}${physxJniVersion}")
        check(!(tempLibDir.exists() && !tempLibDir.isDirectory || !tempLibDir.exists() && !tempLibDir.mkdirs())) { "Failed creating native lib dir $tempLibDir" }

        // 1st: make sure all libs are available in system temp dir
        val libIn = pluginClassLoader.getResourceAsStream("libs.zip")
            ?: throw IllegalStateException("Failed loading libs.zip from resources")
        val libTmpFile = File(tempLibDir, "physxmc-libs.jar")
        if (forceCopy && libTmpFile.exists()) {
            check(libTmpFile.delete()) { "Failed deleting existing native lib file $libTmpFile" }
        }
        if (!libTmpFile.exists()) {
            Files.copy(libIn, libTmpFile.toPath())
        }
        return libTmpFile
    }
}