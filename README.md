## 项目build
初始化项目如果checksum不通过，请将build/checksum/checksum.xml复制覆盖项目根目录的checksum.xml

gradle src:build -PskipCheckstyle -PchecksumIgnore -Prat -PskipSpotless -x test

gradle src:dist:createDist

## 添加jar流程
    比如这里我需要添加influxdb-java-2.15.jar， 使用maven或gradle下载该jar包，进行sha256加密得出字符串
    1. 修改项目根目录下gradle.properties, 新增一行influxdb.version=2.15

    2. 修改项目根目录下checksum.xml，新增一行，如下：
      <!-- group为groupId， module为artifactId， version为版本号，内容之值为进行sha256的字符串 -->
      <dependency group='org.influxdb' module='influxdb-java' version='2.15'>
          <sha512>CD2D7826E70805D24214A2FD78AB403FA0EF5A019EC6E6A23C4DD94A381439106E6E236342E86CDB7F4F5B8886524E5F28648AE24B5E3EF85469012BCC4625C6</sha512>
      </dependency>

    3. 将build目录下的checksum.xml复制到根目录checksum.xml，进行覆盖。

    4. 修改项目根目录下settings.tradle.kts，在val expectedSha512 = mapOf 下新增一行，如下：
      //长字符串为进行sha256加密得出字符串， 后面为jar文件名称（这里名称要和maven或gradle下载的名称一致）
      "CD2D7826E70805D24214A2FD78AB403FA0EF5A019EC6E6A23C4DD94A381439106E6E236342E86CDB7F4F5B8886524E5F28648AE24B5E3EF85469012BCC4625C6"
            to "influxdb-java-2.15.jar"

    5. 修改jmeter/src/bom下build.gradle.kts， 新增一行，如下：
    //这里可以理解为定义了一个获取influxdb的接口
    //第一个参数是groupId加artifactId
    //第二个参数是第一步在gradle.properties的.version前的字符串，即influxdb
    apiv("org.influxdb:influxdb-java", "influxdb")

    6. 修改你需要使用的模块下的build.gradle.kts，我这里改的是components下，所以路径是jmeter/src/components下的
    //在该文件dependencies {}中添加如下：
    //可以理解为获取influxdb接口添加了实现，是在这个模块下实现的，所以这个模块可以使用
    implementation("org.influxdb:influxdb-java");

    7. 上面完成后，刷新gradle就会下载influxdb，然后就可以在对应模块使用了。
