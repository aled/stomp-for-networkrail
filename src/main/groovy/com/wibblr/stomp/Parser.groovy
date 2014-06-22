package com.wibblr.stomp

import groovy.transform.CompileStatic

/**
 * Takes an input stream of bytes, and splits it into message objects.
 * Not profiled or optimized!
 *
 * Use the following simple rules:
 *  o first line is message command
 *  o subsequent lines are the headers
 *  o blank line indicates end of headers
 *  o body extends to the next \0
 *  o repeat
 */

@CompileStatic
class Parser {

    private BufferedInputStream buffer

    public Parser(InputStream stream) {
        buffer = new BufferedInputStream(stream)
    }

    private String readLine(BufferedInputStream buffer) {
        readUntil(buffer, (byte) '\n' as char)
    }

    private String readBody(BufferedInputStream buffer) {
        readUntil(buffer, (byte) '\0' as char)
    }

    private String readUntil(BufferedInputStream buffer, byte terminator) {
        def baos = new ByteArrayOutputStream()
        int b = 0
        while ((b = buffer.read()) != terminator) {
            if (b < 0) throw new EOFException()
            baos.write(b)
        };
        baos.toString("UTF-8")
    }

    private boolean addHeader(Map headers, String line) {
        if (line == "")
            return false

        int pos = line.indexOf(":")
        def key = line.substring(0, pos)
        def value = line.substring(pos + 1, line.length())
        headers[key] = value
        return true
    }

    public Message readMessage() {
        def message = new Message()

        // first line is the message command
        message.command = readLine(buffer)

        // ...but there seems to be an additional newline
        // in the second and subsequent messages
        if (message.command == "")
            message.command = readLine(buffer)

        // subsequent lines are the headers
        while (addHeader(message.headers, readLine(buffer)));

        // blank line indicates end of headers; addHeader will return false
        // body is up until the next \0 character (strictly if there is a content-length header
        // we should use that; however Network Rail doesn't seem to use it.)
        message.body = readBody(buffer)

        return message
    }
}
