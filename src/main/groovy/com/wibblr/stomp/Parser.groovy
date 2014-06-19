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
class Parser extends Thread {
    volatile boolean completed = false

    private InputStream stream
    private MessageListener listener
    private volatile boolean cancel = false

    public Parser(InputStream stream, MessageListener listener) {
        this.stream = stream
        this.listener = listener
    }

    def cancel() {
        cancel = true
        interrupt()
        join(1000)
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
        while (!cancel && (b = buffer.read()) != terminator) {
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

    public void run() {
        def buffer = new BufferedInputStream(stream)
        try {
            while (!cancel) {
                def message = new Message()

                // first line is the message command
                message.command = readLine(buffer)

                // subsequent lines are the headers
                while (addHeader(message.headers, readLine(buffer)));

                // blank line indicates end of headers; addHeader will return false
                // body is up until the next \0 character (strictly if there is a content-length header
                // we should use that; however Network Rail doesn't seem to use it.)
                message.body = readBody(buffer)

                if (!cancel) {
                    listener.messageReceived(message)
                }
            }
        }
        catch (Throwable t) {
            if (!cancel) {
                listener.exceptionRaised(t)
            }
        }
        completed = true
    }
}
