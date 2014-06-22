package com.wibblr.stomp;

import groovy.transform.CompileStatic;

/**
 * This class implements a persistent STOMP connection. The underlying
 * transport will be transparently restarted as required.
 *
 * All subscription topics are specified in the constructor. It is not
 * possible to add/remove subscriptions after this (except by starting
 * a new Connection)
 */
@CompileStatic
public class Connection implements MessageListener {
    private String username
    private String password
    private List<String> topics
    private String connectionId = System.currentTimeMillis().toString()
    private Transport transport
    private int backoffSeconds = 1

    public Connection(String username, String password, List<String> topics) {
        this.username = username
        this.password = password
        this.topics = topics
    }

    public void start() {
        transport = new Transport("localhost", 61618, this)
        transport.start()
        System.out.println("Connection: logging in")
        transport.sendMessage(new Message(command:'CONNECT', headers:['client-id':'client-' + username, 'login':username, 'passcode':password]))
    }

    public void restart() {
        try {Thread.sleep(1000 * backoffSeconds)} catch (InterruptedException) { }
        backoffSeconds *= 2
        transport.cancel()
        start()
    }

    @Override
    void messageReceived(Message message) {
        // upon connection, subscribe to required topics
        if (message.command == 'CONNECTED') {
            backoffSeconds = 1
            topics.each {
                transport.sendMessage(new Message(command:'SUBSCRIBE', headers:['destination':it, 'ack':'client', 'activemq.subscriptionName':username + '-' + connectionId + '-' + it]))
            }
        }
        else if (message.command == 'MESSAGE') {
            System.out.println(message.body)

            // for now, just acknowledge every message immediately
            transport.sendMessage(new Message(command:'ACK', headers: ['message-id':message.headers['message-id']]))
        }
        else if (message.command == 'ERROR') {
            System.out.println(message.body)
            restart()
        }
    }

    @Override
    void exceptionRaised(Throwable t) {
        restart()
    }
}
