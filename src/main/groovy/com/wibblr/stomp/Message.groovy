package com.wibblr.stomp

import groovy.transform.CompileStatic

/**
 * Represents a STOMP message.
 * Contains a command, headers and body.
 */
@CompileStatic
class Message {
    String command
    Map<String, String> headers = new HashMap<String, String>()
    String body

    def String toString() {
        def sb = new StringBuffer()
        sb.append(command)
        sb.append("\n")
        headers.each {
            sb.append(it.key)
            sb.append(":")
            sb.append(it.value)
            sb.append("\n")
        }
        sb.append("\n")
        sb.append(body)
        sb.append("\0")
        sb.toString()
    }

    def byte[] toBytes() {
        toString().getBytes("UTF-8")
    }
}
