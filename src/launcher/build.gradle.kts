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
dependencies {
    api("org.apache.commons:commons-collections4")
    api("org.apache.commons:commons-lang3")
    api("com.alibaba:fastjson")
    api("io.shulie.flpt:jmeter-redis-tool")
//    implementation("org.slf4j:slf4j-api")
}
val lastEditYear: String by rootProject.extra

tasks.withType<ProcessResources>().configureEach {
    val version = rootProject.version.toString()
    inputs.property("@VERSION@", version)
    inputs.property("@YEAR@", lastEditYear)
    filter { x: String ->
        x.replace("@VERSION@", version)
            .replace("@YEAR@", lastEditYear)
    }
}

tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME) {
    manifest {
        attributes["Main-Class"] = "org.apache.jmeter.NewDriver"
//        attributes["Class-Path"] = "../lib/jmeter-redis-tool-1.3.2.jar"
    }
}

