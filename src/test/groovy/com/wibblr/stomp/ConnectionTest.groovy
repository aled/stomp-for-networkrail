package com.wibblr.stomp

import groovy.transform.CompileStatic

@CompileStatic
class ConnectionTest extends GroovyTestCase {
    void testConnection() {
        // Topics available:
        //  /topic/RTPPM_ALL - real time performance
        //  /topic/TRAIN_MVT_ALL_TOC - train movements
        //  /topic/TD_ALL_SIG_AREA - train describer
        //  /topic/VSTP_ALL - late notice schedules
        //  /topic/TSR_ALL_ROUTE - temporary speed restrictions

        def authProperties = new Properties()
        authProperties.load(new FileInputStream(System.getProperty("user.home") + "/.wibblr/auth.properties"))

        String username = authProperties["username"]
        String password = authProperties["password"]
        String clientId = authProperties["clientId"]

        def connection = new Connection(username, password, clientId,
                ["/topic/TRAIN_MVT_ALL_TOC", "/topic/TD_ALL_SIG_AREA", "/topic/RTPPM_ALL", "/topic/VSTP_ALL", "/topic/TSR_ALL_ROUTE"], null)
        connection.start()
        Thread.sleep(99999999)
    }
}
