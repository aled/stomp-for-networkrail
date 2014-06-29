package com.wibblr.networkrail

import com.wibblr.stomp.Connection
import com.wibblr.stomp.Message
import com.wibblr.stomp.MessageListener

import groovy.transform.CompileStatic
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.zip.GZIPOutputStream

@CompileStatic
public class DataFeed {

    public static void main(String[] args) {
        // read username and password from ~/.wibblr/auth.properties
        def authProperties = new Properties()
        authProperties.load(new FileInputStream(System.getProperty("user.home") + "/.wibblr/auth.properties"))

        String username = authProperties["username"]
        String password = authProperties["password"]
        String clientId = authProperties["clientId"]
        String rtppmForWebFilename = authProperties["ppm.filename"]

        OutputStream currentOutputStream
        String currentFilename
        String currentDir

        // save message to disk. Use directory structure yyyy/yyyymmdd/yyyymmdd-1400.gz
        MessageListener listener = new MessageListener() {

            @Override
            void messageReceived(Message message) {

                // write raw messages to disk
                try {
                    DateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmm'Z'")
                    df.setTimeZone(TimeZone.getTimeZone("UTC"))
                    String filename = df.format(new Date(System.currentTimeMillis())) + ".gz"

                    // directory has format yyyy/yyyyMMdd
                    String dir = filename.substring(0, 4) + File.separator + filename.substring(0, 8)
                    if (dir != currentDir) {
                        currentDir = dir
                        new File(dir).mkdirs()
                    }

                    if (filename != currentFilename) {
                        currentFilename = filename
                        if (currentOutputStream != null) currentOutputStream.close()
                        currentOutputStream = new GZIPOutputStream(new FileOutputStream(new File(dir, filename)))
                    }

                    currentOutputStream.write(message.toBytes())
                    currentOutputStream.write((byte) '\n' as char)
                }
                catch (Exception e) {
                    System.out.println(e.toString())
                }

                if (message.headers.containsKey("destination")
                        && message.headers["destination"] == "/topic/RTPPM_ALL") {
                    try {
                        String rtppmForWeb = Rtppm.parseToWebJson(message)
                        File rtppmFile = new File(rtppmForWebFilename)
                        File tempFile = new File(rtppmFile.getCanonicalFile().parent, "." + rtppmFile.name + "." + System.currentTimeMillis())
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)))
                        bw.write(rtppmForWeb)
                        bw.flush()
                        bw.close()
                        //System.out.println("renaming " + tempFile.canonicalPath + " to " + rtppmFile.canonicalPath)
                        tempFile.renameTo(rtppmFile)
                    }
                    catch (Exception e) {
                        e.printStackTrace(System.err)
                    }
                }

            }

            @Override
            void exceptionRaised(Throwable throwable) {
                throw new NotImplementedException()
            }
        }

        // Topics available:
        //  /topic/RTPPM_ALL - real time performance
        //  /topic/TRAIN_MVT_ALL_TOC - train movements
        //  /topic/TD_ALL_SIG_AREA - train describer
        //  /topic/VSTP_ALL - late notice schedules
        //  /topic/TSR_ALL_ROUTE - temporary speed restrictions
        def connection = new Connection(username, password, clientId,
                ["/topic/TRAIN_MVT_ALL_TOC", "/topic/TD_ALL_SIG_AREA", "/topic/RTPPM_ALL", "/topic/VSTP_ALL", "/topic/TSR_ALL_ROUTE"],
                //["/topic/RTPPM_ALL"],
                listener)
        connection.start()

        System.out.println("Press any key to stop")
        System.in.read()

        connection.cancel()
        System.out.println("Closing file")
        if (currentOutputStream != null) currentOutputStream.close()
    }
}