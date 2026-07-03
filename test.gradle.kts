repositories {
    mavenCentral()
}

tasks.register("fetch") {
    doLast {
        val url = java.net.URL("https://jitpack.io/api/builds/com.github.junkfood02/youtubedl-android")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        if (conn.responseCode == 200) {
            println(conn.inputStream.bufferedReader().readText())
        } else {
            println("Error: ${conn.responseCode}")
        }
    }
}
