package com.wibblr.stomp

import groovy.transform.CompileStatic

/**
 * Transport class is responsible for sending and receiving messages over
 * a TCP connection.
 */
@CompileStatic
class Transport {

    volatile boolean stopped

    private volatile boolean cancel
    private String host
    private int port
    private MessageListener listener
    private Socket socket
    private OutputStream outputStream
    private Parser parser

    public Transport(String host, int port, MessageListener listener) {
        this.host = host
        this.port = port
        this.listener = listener
    }

    public void cancel() {
        cancel = true;
        if (socket != null) socket.close()
    }

    public void start() {
        socket = new Socket(host, port)
        socket.setKeepAlive(true)
        outputStream = socket.getOutputStream()
        parser = new Parser(socket.getInputStream())
        Thread.start {
            try {
                while (!cancel) {
                    listener.messageReceived(parser.readMessage())
                }
            } catch (Throwable t) {
                cancel()
                listener.exceptionRaised(t)
            }
            stopped = true
        }
    }

    def sendMessage(Message message) {
        try {
            outputStream.write(message.toBytes())
        }
        catch (Throwable t) {
            cancel()
            listener.exceptionRaised(t)
        }
    }
}
