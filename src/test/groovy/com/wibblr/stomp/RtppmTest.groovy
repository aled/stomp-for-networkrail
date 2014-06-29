package com.wibblr.stomp

import com.wibblr.networkrail.Rtppm
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
class RtppmTest extends GroovyTestCase {
    void testParseForWeb() {

        Message message = new Parser(getClass().getClassLoader().getResourceAsStream("Rtppm.stomp")).readMessage()

        String expectedJson = JsonOutput.prettyPrint("{" +
                "")

        String actualJson = JsonOutput.prettyPrint(Rtppm.parseToWebJson(message))

        assertEquals(expectedJson, actualJson)
    }
}
