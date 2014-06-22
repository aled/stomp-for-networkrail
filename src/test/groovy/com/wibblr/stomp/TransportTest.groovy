package com.wibblr.stomp

import groovy.transform.CompileStatic

@CompileStatic
class TransportTest extends GroovyTestCase {

    /**
     * Show that we can send and receive messages over a TCP connection
     */
    void testStart() {

        // start a local TCP server which echoes the first 2 received messages
        // (but with an additional header "reply:n"), and then stops
        def server = new ServerSocket(0)
        int port = server.getLocalPort();

        Thread.start {
            server.accept() { Socket socket ->
                socket.withStreams { InputStream input, OutputStream output ->
                    Parser parser = new Parser(input)
                    (1..2).each {
                        def message = parser.readMessage()
                        output.write(new Message(
                                command: message.command,
                                headers: (message.headers + [reply:it.toString()]),
                                body: message.body
                        ).toBytes())
                        output.flush()
                    }
                }
            }
        }

        def receivedMessages = new ArrayList<Message>()
        def transport = new Transport("127.0.0.1", port, new MessageListener() {
            @Override
            void messageReceived(Message message) {
                receivedMessages.add(message)
            }

            @Override
            void exceptionRaised(Throwable t) { }
        })

        transport.start();

        transport.sendMessage(new Message(command: "cmd1", headers: [a:"b"])) // no body
        transport.sendMessage(new Message(command: "cmd2", headers: [a:"b"], body: "asdf"))

        // The transport replies to 2 messages then stops. Wait up to 10 seconds for the transport to stop.
        for (int i = 0; i < 1000 && !transport.isStopped(); i++) {
            try { Thread.sleep(10) } catch (InterruptedException) { }
        }

        assertEquals 2, receivedMessages.size()
        assertEquals new Message(command: "cmd1", headers: [a:"b", reply:"1"]).toString(), receivedMessages[0].toString()
        assertEquals(new Message(command: "cmd2", headers: [a:"b", reply:"2"], body: "asdf").toString(), receivedMessages[1].toString())
        assertTrue transport.isStopped()
    }
}