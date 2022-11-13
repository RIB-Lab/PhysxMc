import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.ajoberstar.grgit.Grgit

plugins {
    // Javaプラグインを適用
    java
    // Kotlinを使用するためのプラグイン
    kotlin("jvm") version "1.7.10"
    // Kdoc(Kotlin用Javadoc)を生成するためのプラグイン
    id("org.jetbrains.dokka") version "1.7.10"
    // ShadowJar(依存関係埋め込み)を使用するためのプラグイン
    id("com.github.johnrengelman.shadow") version "6.0.0"
    // Gitに応じた自動バージョニングを行うためのプラグイン
    id("org.ajoberstar.grgit") version "4.1.1"
}

// グループ定義
group = "com.kamesuta"
// バージョン定義
version = run {
    // Gitに応じた自動バージョニングを行うための設定
    val grgit = runCatching { Grgit.open(mapOf("currentDir" to project.rootDir)) }.getOrNull()
        ?: return@run "unknown" // .gitがない
    // HEADがバージョンを示すタグを指している場合はそのタグをバージョンとする
    val versionStr = grgit.describe {
        longDescr = false
        tags = true
        match = listOf("v[0-9]*")
    } ?: "0.0.0" // バージョンを示すタグがない
    // GitHub Actionsでビルドする場合は環境変数からビルド番号を取得する
    val buildNumber = System.getenv("GITHUB_RUN_NUMBER") ?: "git"
    // コミットされていない変更がある場合は+dirtyを付与する
    val dirty = if (grgit.status().isClean) "" else "+dirty"
    // リリースバージョン以外は-SNAPSHOTを付与する
    val snapshot = if (versionStr.matches(Regex(".*-[0-9]+-g[0-9a-f]{7}"))) "-SNAPSHOT" else ""
    // バージョンを組み立てる
    "${versionStr}.${buildNumber}${snapshot}${dirty}"
}

repositories {
    mavenCentral()
    // Paperの依存リポジトリ
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

configurations {
    // PhysX関係を別の設定にする
    val includeLib by creating
    compileOnly.get().extendsFrom(includeLib)
}

dependencies {
    // PaperAPI
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")

    // IncludeLib
    val includeLib by configurations

    // java bindings
    includeLib("de.fabmax:physx-jni:1.1.0")

    // native libraries, you can add the one matching your system or all
    includeLib("de.fabmax:physx-jni:1.1.0:natives-windows")
    includeLib("de.fabmax:physx-jni:1.1.0:natives-linux")
    includeLib("de.fabmax:physx-jni:1.1.0:natives-macos")

    // JOML
    implementation("org.joml:joml:1.10.5")
}

tasks {
    // Javadocを出力する
    val javadocJar by registering(Jar::class) {
        archiveClassifier.set("javadoc")
        from(dokkaHtml)
        dependsOn(dokkaHtml)
    }

    jar {
        // -bukkitを除く
        archiveAppendix.set("")
        // 依存関係を埋め込んでいないjarは末尾に-originalを付与する
        archiveClassifier.set("original")
    }

    // ソースjarを生成する
    val includeLibZip by registering(ShadowJar::class) {
        archiveBaseName.set("libs")
        archiveVersion.set("")
        archiveClassifier.set("")
        archiveExtension.set("zip")
        destinationDirectory.set(layout.buildDirectory.dir("distributions"))
        val includeLib by project.configurations
        from(includeLib)
    }

    // リソースパックを生成する
    val resourcepack by registering(Zip::class) {
        archiveClassifier.set("resourcepack")
        destinationDirectory.set(layout.buildDirectory.dir("libs"))
        from("resourcepack")
    }

    // fatJarを生成する
    shadowJar {
        // 依存関係を埋め込んだjarは末尾なし
        archiveClassifier.set("")
        // IncludeLibを埋め込む
        from(includeLibZip)
    }

    // ソースjarを生成する
    val sourcesJar by registering(Jar::class) {
        archiveClassifier.set("sources")
        val main by kotlin.sourceSets.getting
        from(main.kotlin.srcDirs)
    }

    // アーティファクトを登録する
    artifacts {
        // 依存関係を埋め込んだjarをビルドする
        add("archives", shadowJar)
        // Javadocを出力する (-javadoc)
        add("archives", javadocJar)
        // ソースjarを生成する (-sources)
        add("archives", sourcesJar)
        // リソースパックを生成する
        add("archives", resourcepack)
    }

    // plugin.ymlの中にバージョンを埋め込む
    @Suppress("UnstableApiUsage")
    withType<ProcessResources> {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
