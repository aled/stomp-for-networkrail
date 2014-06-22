package com.wibblr.stomp

import groovy.transform.CompileStatic
import org.junit.Assert

@CompileStatic
class ParserTest extends GroovyTestCase {

    private String generateBody(int length) {
        def sb = new StringBuffer(length)
        (1..length).each { sb.append("a") }
        sb.toString()
    }

    private Message generateMessage(int bodyLength) {
        new Message(command: "command1", headers: [a: "b", c: "d"], body: generateBody(bodyLength))
    }

    void testPartialMessage() {
        // write a whole message and a partial message
        def message = generateMessage(100).toString();
        def stream = new ByteArrayInputStream((message + message.substring(0, 50)).getBytes("UTF-8"))
        def parser = new Parser(new BufferedInputStream(stream))
        assertEquals(message.toString(), parser.readMessage().toString())
        // next read should give EOF exception
        Exception receivedException
        try {
            parser.readMessage()
        }
        catch (Exception e) {
            receivedException = e
        }
        assertTrue(receivedException instanceof EOFException)
    }

    void testReadMessage() {
        def messages = new ArrayList<Message>()
        int numMessages = 100
        (0..numMessages).each { messages.add(generateMessage(4000)) }

        def stream = new ByteArrayInputStream(messages.collect { it.toString() }.join("").getBytes("UTF-8"))

        def parser = new Parser(new BufferedInputStream(stream))
        (0..numMessages).each {
            assertEquals(messages[it].toString(), parser.readMessage().toString())
        }

        // next read should give EOF exception
        Exception receivedException
        try {
            parser.readMessage()
        }
        catch (Exception e) {
            receivedException = e
        }

        assertTrue(receivedException instanceof EOFException)
    }
}
