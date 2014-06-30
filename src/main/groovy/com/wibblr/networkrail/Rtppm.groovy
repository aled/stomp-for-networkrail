package com.wibblr.networkrail

import com.wibblr.stomp.Message
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * RTPPM (Real Time Public Performance Measures)
 */
@CompileStatic
class Rtppm {

    private static List<Integer> parsePpm(Map m) {
        def ppm = []
        try {
            ppm.add(Integer.parseInt(m["OnTime"].toString()))
            ppm.add(Integer.parseInt(m["Late"].toString()))
            ppm.add(Integer.parseInt(m["CancelVeryLate"].toString()))
        }
        catch (Exception e) {
            ppm = [0, 0, 0]
        }
        return ppm
    }
    // Process RTPPM Stomp messages into the format used by the web page.
    // The three numbers represent the number of trains on time, late and very late.
    //
    //                "LastUpdated": "2014-04-01T14:12:11+01:00",
    //                "PpmNational": {
    //                    "Name": "National",
    //                    "Ppm": [254, 1, 4]
    //                },
    //                "PpmByOperator": [
    //                        {
    //                            "Name": "East Coast",
    //                            "Ppm": [20, 1, 2]
    //                        },
    //                        {
    //                            "Name": "Heathrow Express",
    //                            "Ppm": [10, 2, 3]
    //                        },
    //                        ...
    //                ]
    public static String parseToWebJson(Message message) {

        if (message.headers.containsKey("destination")
                && message.headers["destination"] == "/topic/RTPPM_ALL") {

            DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
            iso8601.setTimeZone(TimeZone.getTimeZone("UTC"))

            Map m = (Map)new JsonSlurper().parseText(message.body)

            Map RTPPMDataMsgV1 = (Map)m["RTPPMDataMsgV1"]

            long timestamp = Long.parseLong(RTPPMDataMsgV1["timestamp"].toString())
            Map RTPPMData = (Map)RTPPMDataMsgV1["RTPPMData"]
            Map NationalPPM = (Map)RTPPMData["NationalPage"]["NationalPPM"]

            def json = new JsonBuilder()
            json (
                LastUpdated: iso8601.format(new Date(timestamp)),
                PpmNational: [Name: "National", Ppm: parsePpm(NationalPPM)],
                PpmByOperator: ((List)RTPPMData["OperatorPage"])
                    .sort { operator ->
                        ((Map)(operator))["Operator"]["name"].toString().toLowerCase()
                    }
                    .collect { operator ->
                        Map mo = (Map) operator
                        [Name: mo["Operator"]["name"], Ppm: parsePpm((Map)mo["Operator"])]
                    }
            )

            return json.toString()
        }
    }
}
