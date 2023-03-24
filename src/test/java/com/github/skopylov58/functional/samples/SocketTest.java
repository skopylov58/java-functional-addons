package com.github.skopylov58.functional.samples;

import java.io.OutputStream;
import java.net.Socket;

import org.junit.Test;

import com.github.skopylov58.functional.Try;

public class SocketTest {
    
    
    @Test
    public void testTraditional() {
        try (Socket s = new Socket("host", 8888)) {
            OutputStream outputStream = s.getOutputStream();
            outputStream.write(new byte[] {1,2,3});
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Test
    public void testWithTry() {
        try (var s = Try.of(() -> new Socket("host", 8888))) {
            s.map(Socket::getOutputStream)
            .onSuccess(out -> out.write(new byte[] {1,2,3}))
            .onFailure(e -> System.out.println(e));
        }
    }


}
