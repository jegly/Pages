// Build-tool transitives (AGP / Kotlin / Hilt plugin classpath) that Dependabot flags. They never
// ship in the APK; constraints lift them to patched releases.
buildscript {
    val nettyVersion = "4.2.19.Final"
    val bouncyCastleVersion = "1.86"
    dependencies {
        constraints {
            classpath("io.netty:netty-buffer:$nettyVersion")
            classpath("io.netty:netty-codec:$nettyVersion")
            classpath("io.netty:netty-codec-http:$nettyVersion")
            classpath("io.netty:netty-codec-http2:$nettyVersion")
            classpath("io.netty:netty-common:$nettyVersion")
            classpath("io.netty:netty-handler:$nettyVersion")
            classpath("io.netty:netty-handler-proxy:$nettyVersion")
            classpath("io.netty:netty-resolver:$nettyVersion")
            classpath("io.netty:netty-transport:$nettyVersion")
            classpath("io.netty:netty-transport-native-epoll:$nettyVersion")
            classpath("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")
            classpath("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")
            classpath("org.jdom:jdom2:2.0.6.1")
            classpath("org.bitbucket.b_c:jose4j:0.9.7")
            classpath("com.google.guava:guava:33.7.2-jre")
            classpath("org.apache.commons:commons-lang3:3.21.0")
        }
    }
}

/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * By listing all the plugins used throughout all subprojects in the root project build script, it
 * ensures that the build script classpath remains the same for all projects. This avoids potential
 * problems with mismatching versions of transitive plugin dependencies. A subproject that applies
 * an unlisted plugin will have that plugin and its dependencies _appended_ to the classpath, not
 * replacing pre-existing dependencies.
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.pages.root)
}
