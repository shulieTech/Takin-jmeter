/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pluginManagement {
    plugins {
        fun String.v() = extra["$this.version"].toString()
        fun PluginDependenciesSpec.idv(id: String, key: String = id) = id(id) version key.v()

        idv("com.github.autostyle")
        idv("com.github.spotbugs")
        idv("com.github.vlsi.crlf", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.gradle-extensions", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.ide", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.stage-vote-release", "com.github.vlsi.vlsi-release-plugins")
        idv("org.jetbrains.gradle.plugin.idea-ext")
        idv("org.nosphere.apache.rat")
        idv("org.sonarqube")
    }
}

plugins {
    `gradle-enterprise`
}

// This is the name of a current project
// Note: it cannot be inferred from the directory name as developer might clone JMeter to jmeter_tmp folder
rootProject.name = "jmeter"

include(
        "src:bom",
        "src:bshclient",
        "src:launcher",
        "src:components",
        "src:config",
        "src:core",
        "src:examples",
        "src:functions",
        "src:generator",
        "src:jorphan",
        "src:licenses",
        "src:protocol:bolt",
        "src:protocol:ftp",
        "src:protocol:http",
        "src:protocol:java",
        "src:protocol:jdbc",
        "src:protocol:jms",
        "src:protocol:junit",
        "src:protocol:junit-sample",
        "src:protocol:ldap",
        "src:protocol:mail",
        "src:protocol:mongodb",
        "src:protocol:native",
        "src:protocol:tcp",
        "src:release",
        "src:testkit",
        "src:testkit-wiremock",
        "src:dist",
        "src:dist-check")

// See https://github.com/gradle/gradle/issues/1348#issuecomment-284758705 and
// https://github.com/gradle/gradle/issues/5321#issuecomment-387561204
// Gradle inherits Ant "default excludes", however we do want to archive those files
org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitattributes")
org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitignore")

fun String?.toBool(nullAs: Boolean, blankAs: Boolean, default: Boolean) =
    when {
        this == null -> nullAs
        isBlank() -> blankAs
        default -> !equals("false", ignoreCase = true)
        else -> equals("true", ignoreCase = true)
    }

fun property(name: String) =
        when (extra.has(name)) {
                true -> extra.get(name) as? String
                else -> null
        }

if (property("localReleasePlugins").toBool(nullAs = false, blankAs = true, default = false)) {
        // This enables to use local clone of vlsi-release-plugins for debugging purposes
        includeBuild("../vlsi-release-plugins")
}

val isCiServer = System.getenv().containsKey("CI")

if (isCiServer) {
    gradleEnterprise {
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
            tag("CI")
        }
    }
}

// Checksum plugin sources can be validated at https://github.com/vlsi/vlsi-release-plugins
buildscript {
    dependencies {
        classpath("com.github.vlsi.gradle:checksum-dependency-plugin:${settings.extra["com.github.vlsi.checksum-dependency.version"]}") {
            // Gradle ships kotlin-stdlib which is good enough
            exclude("org.jetbrains.kotlin", "kotlin-stdlib")
        }
    }
    repositories {
        gradlePluginPortal()
    }
}

// Note: we need to verify the checksum for checksum-dependency-plugin itself
val expectedSha512 = mapOf(
    "F7040C571C2A2727F2EED4EA772F5A7C5D9CB393828B7A2331F7167E467429486F5F3E9423883FE9A6D652FFB0484EAE722CDFB46D97180209BCBEEBF9C25DE3"
            to "gradle-enterprise-gradle-plugin-3.4.jar",
    "43BC9061DFDECA0C421EDF4A76E380413920E788EF01751C81BDC004BD28761FBD4A3F23EA9146ECEDF10C0F85B7BE9A857E9D489A95476525565152E0314B5B"
            to "bcpg-jdk15on-1.62.jar",
    "2BA6A5DEC9C8DAC2EB427A65815EB3A9ADAF4D42D476B136F37CD57E6D013BF4E9140394ABEEA81E42FBDB8FC59228C7B85C549ED294123BF898A7D048B3BD95"
            to "bcprov-jdk15on-1.62.jar",
    "17DAAF511BE98F99007D7C6B3762C9F73ADD99EAB1D222985018B0258EFBE12841BBFB8F213A78AA5300F7A3618ACF252F2EEAD196DF3F8115B9F5ED888FE827"
            to "okhttp-4.1.0.jar",
    "93E7A41BE44CC17FB500EA5CD84D515204C180AEC934491D11FC6A71DAEA761FB0EECEF865D6FD5C3D88AAF55DCE3C2C424BE5BA5D43BEBF48D05F1FA63FA8A7"
            to "okio-2.2.2.jar",
    "46F188DD310C7C9D77EAEF3E85A6FE5A47FD3CD809467C5B35D7D9A79DC8B99E9B7F43C631A96DE0E98C08F644EA64B37168724C5FB1FECCABAD388DADC79626"
            to "jedis-3.6.3.jar",
    "98D10F2AAC83CC3D97B9E7EAB82825892E62CCD9457F8B60D574775F5EEB3AFA2EEE06539995692F33C811C5D35E754F0E79DEC2529F58900427CEC595496D7B"
            to "fastjson-1.2.72.jar",
    "06E291ACB6D9DB884701A84C756D1F22E2A8D0468FB33246C9F6065335B99F3E21A4C434046C3D458A7520E5EE14B02397B66205453AB7B76D2DB9AB6A18164E"
            to "jmeter-redis-tool-1.3.2.jar",
    "C7B05935FA8E5839AAD4902123BE4DC82B0C789956AFC4635B9A394DEEBC385D64FB530AF52E9C51C1E2AC6FAAA4B40B56B9274DB47F28BDF2525160481B894A"
            to "jmeter-amdb-tool-1.3.1.jar",
    "AD897EAFEC91200553827FE9F20C3477351E8625B1C54E6DD6B2070732B15139E3BFDBA044C41C3C794D8F2D38CF2527799AF1487B2256EF9F6B217387DDF374"
            to "jmeter-executor-tool-1.3.1.jar",
    settings.extra["com.github.vlsi.checksum-dependency.sha512"].toString()
    to "checksum-dependency-plugin.jar"
)

fun File.sha512(): String {
    val md = java.security.MessageDigest.getInstance("SHA-512")
    forEachBlock { buffer, bytesRead ->
        md.update(buffer, 0, bytesRead)
    }
    return BigInteger(1, md.digest()).toString(16).toUpperCase()
}

val violations =
    buildscript.configurations["classpath"]
        .resolve()
        .sortedBy { it.name }
        .associateWith { it.sha512() }
        .filterNot { (_, sha512) -> expectedSha512.contains(sha512) }
        .entries
        .joinToString("\n  ") { (file, sha512) -> "SHA-512(${file.name}) = $sha512 ($file)" }

if (violations.isNotBlank()) {
    throw GradleException("Buildscript classpath has files that were not explicitly permitted:\n  $violations")
}

apply(plugin = "com.github.vlsi.checksum-dependency")

// This enables to try local Autostyle
property("localAutostyle")?.ifBlank { "../autostyle" }?.let {
    println("Importing project '$it'")
    includeBuild(it)
}

property("localDarklaf")?.ifBlank { "../darklaf" }?.let {
    println("Importing project '$it'")
    includeBuild(it)
}
