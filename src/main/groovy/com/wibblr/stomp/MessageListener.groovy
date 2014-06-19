package com.wibblr.stomp

interface MessageListener {
    public void messageReceived(Message message)

    public void exceptionRaised(Throwable throwable)
}
