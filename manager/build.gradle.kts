import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random
import groovy.json.JsonSlurper

val defaultManagerPackageName: String by rootProject.extra
val apiCode: Int by rootProject.extra
val verCode: Int by rootProject.extra
val verName: String by rootProject.extra
val coreVerCode: Int by rootProject.extra
val coreVerName: String by rootProject.extra

val randomGitHubUsername: String = run {
    val token = System.getenv("GITHUB_TOKEN").orEmpty()
    var failCount = 0
    while (true) {
        try {
            val id = Random.nextInt(1, 190000001)
            val conn = (URL("https://api.github.com/user/$id")
            .openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "token $token")
                }
            }

            if (conn.responseCode == 200) {
                val data = conn.inputStream.bufferedReader().use { it.readText() }
                val login = (JsonSlurper().parseText(data) as Map<*, *>)["login"] as? String
                if (!login.isNullOrBlank()) {
                    return@run login
                }
            }
        } catch (_: Exception) {
        }
        if (++failCount >= 3) {
            break
        }
        Thread.sleep(1_000)
    }
    "LSPosed"
}

val randomValidated = Random.nextInt(100000000, 1000000000)
val randomUpdate = Random.nextLong(8000000000000000000, 9000000000000000000)

plugins {
    alias(libs.plugins.agp.app)
    alias(lspatch.plugins.compose.compiler)
    alias(lspatch.plugins.google.devtools.ksp)
    alias(lspatch.plugins.rikka.tools.refine)
    alias(lspatch.plugins.kotlin.android)
    id("kotlin-parcelize")
}

android {
    defaultConfig {
        applicationId = defaultManagerPackageName
        resValue(
            "string",
            "randomghname",
            "LSPatch IT (GitHub@${randomGitHubUsername})"
        )
        resValue("string", "randomvid", randomValidated.toString())
        resValue(
            "string",
            "randomuurl",
            "https://bot.lsposed.org/update/${randomUpdate}/zygisk.json"
        )
    }

    androidResources {
        noCompress.add(".so")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                          "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                          "proguard-rules.pro"
            )
        }
        all {
            sourceSets[name].assets.srcDirs(rootProject.projectDir.resolve("out/assets/$name"))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }

    namespace = "org.lsposed.lspatch"

    applicationVariants.all {
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
}

afterEvaluate {
    android.applicationVariants.forEach { variant ->
        val variantLowered = variant.name.lowercase()
        val variantLoweredOutName = if (variant.name.lowercase() == "debug") {
            "release-log"
        } else {
            "release"
        }
        val variantCapped = variant.name.replaceFirstChar { it.uppercase() }

        task<Copy>("copy${variantCapped}Assets") {
            dependsOn(":meta-loader:copy$variantCapped")
            dependsOn(":patch-loader:copy$variantCapped")
            tasks["merge${variantCapped}Assets"].dependsOn(this)

            into("$buildDir/intermediates/assets/$variantLowered/merge${variantCapped}Assets")
            from("${rootProject.projectDir}/out/assets/${variant.name}")
        }

        task<Copy>("build$variantCapped") {
            dependsOn(tasks["assemble$variantCapped"])
            from(variant.outputs.map { it.outputFile })
            into("${rootProject.projectDir}/out/$variantLowered")
            rename(".*.apk", "LSPatch-v$verName-ed-$verCode-$variantLoweredOutName.apk")
        }
    }
}

dependencies {
    implementation(projects.patch)
    implementation(projects.services.daemonService)
    implementation(projects.share.android)
    implementation(projects.share.java)
    implementation(platform(lspatch.androidx.compose.bom))

    annotationProcessor(lspatch.androidx.room.compiler)
    compileOnly(lspatch.rikka.hidden.stub)
    debugImplementation(lspatch.androidx.compose.ui.tooling)
    debugImplementation(lspatch.androidx.customview)
    debugImplementation(lspatch.androidx.customview.poolingcontainer)
    implementation(lspatch.androidx.activity.compose)
    implementation(lspatch.androidx.compose.material.icons.extended)
    implementation(lspatch.androidx.compose.material3)
    implementation(lspatch.androidx.compose.ui)
    implementation(lspatch.androidx.compose.ui.tooling.preview)
    implementation(lspatch.androidx.core.ktx)
    implementation(lspatch.androidx.lifecycle.viewmodel.compose)
    implementation(lspatch.androidx.navigation.compose)
    implementation(libs.androidx.preference)
    implementation(lspatch.androidx.room.ktx)
    implementation(lspatch.androidx.room.runtime)
    implementation(lspatch.google.accompanist.navigation.animation)
    implementation(lspatch.google.accompanist.pager)
    implementation(lspatch.google.accompanist.swiperefresh)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(lspatch.rikka.shizuku.api)
    implementation(lspatch.rikka.shizuku.provider)
    implementation(lspatch.rikka.refine)
    implementation(lspatch.raamcosta.compose.destinations)
    implementation(libs.appiconloader)
    implementation(libs.hiddenapibypass)
    ksp(lspatch.androidx.room.compiler)
    ksp(lspatch.raamcosta.compose.destinations.ksp)
}
