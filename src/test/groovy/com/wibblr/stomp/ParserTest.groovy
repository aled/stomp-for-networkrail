package com.wibblr.stomp

import groovy.transform.CompileStatic
import org.junit.Assert

@CompileStatic
class ParserTest extends GroovyTestCase {

    // test that when cancel is called, the parser stops immediately (even if blocked on a read),
    // and does not send part-completed messages
    void testCancel() {
        def is = new PipedInputStream(65535)
        // big enough to hold everything we write to the output stream without blocking
        def os = new PipedOutputStream()

        is.connect(os)

        // write a whole message and a partial message
        os.write(generateMessage(100).toString().getBytes("UTF-8"))
        os.write(generateMessage(100).toString().substring(0, 50).getBytes("UTF-8"))

        def mainThread = Thread.currentThread()
        def receivedMessages = new ArrayList<Message>()
        def messageListener = new MessageListener() {
            @Override
            void messageReceived(Message message) {
                receivedMessages.add(message)
                mainThread.interrupt()
            }

            @Override
            void exceptionRaised(Throwable t) {
                Assert.assertTrue(t.toString(), t instanceof EOFException)
                mainThread.interrupt()
            }
        }

        def parser = new Parser(is, messageListener)
        parser.start()
        try {
            Thread.sleep(60000)
        } catch (InterruptedException) {
        }
        assertEquals(1, receivedMessages.size())

        // Sleep for a little while to ensure that the parser is blocked on a read
        try {
            Thread.sleep(50)
        } catch (InterruptedException) {
        }

        // Cancel method waits for the parser's worker thread to complete
        parser.cancel()

        assertTrue(parser.getCompleted())
        assertEquals(1, receivedMessages.size())
    }

    private String generateBody(int length) {
        def sb = new StringBuffer(length)
        (1..length).each { sb.append("a") }
        sb.toString()
    }

    private Message generateMessage(int bodyLength) {
        new Message(command: "command1", headers: [a: "b", c: "d"], body: generateBody(bodyLength))
    }

    void testRun() {
        def mainThread = Thread.currentThread()
        def messages = new ArrayList<Message>()
        int numMessages = 100
        (1..numMessages).each { messages.add(generateMessage(4000)) }

        def stream = new ByteArrayInputStream(messages.collect { it.toString() }.join("").getBytes("UTF-8"))

        def receivedMessages = new ArrayList<Message>(numMessages)
        def messageListener = new MessageListener() {
            @Override
            void messageReceived(Message message) {
                receivedMessages.add(message)
            }

            @Override
            void exceptionRaised(Throwable t) {
                Assert.assertTrue(t instanceof EOFException)
                mainThread.interrupt()
            }
        }

        new Parser(stream, messageListener).start()
        try {
            Thread.sleep(60000)
        } catch (InterruptedException) {
        }

        assertEquals(messages.size(), receivedMessages.size())
        for (int i = 0; i < messages.size(); i++) {
            assertEquals(messages[i].toString(), receivedMessages[i].toString())
        }
    }
}
