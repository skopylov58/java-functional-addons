package com.github.skopylov58.retry.samples;

import static org.junit.Assert.assertFalse;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.skopylov58.functional.Try;
import com.github.skopylov58.retry.Retry;

/**
 * Example to try and retry exceptional runnables.
 * <p>
 * Given: {@link #sendMail(String, String)} void procedure to send mail to SMTP server
 * <p>
 * Required: send mail to the first available SMTP server in the given server list 
 * and retry some times this operation in case of failures.
 * 
 * @author sergey.kopylov@hpe.com
 *
 */
public class SendMailTest {

    @Test
    public void test() throws Exception {
        boolean result = sendWithRetry(new String [] {"smpt1", "smtp2"}, "Some message");
        assertFalse(result);
    }

    /**
     * Sends mail to the SMTP server
     * @param smtpServer
     * @param email
     * @throws Exception in case of any errors
     */
    public void sendMail(String smtpServer, String email) throws Exception{
        throw new RuntimeException("Can't send message '" + email + "' to " + smtpServer);
    }

    /**
     * Sends mail to the first alive SMTP server.
     * @param smtpServers list of SMTP sdervers
     * @param msg message to send
     * @throws Exception if all servers are failed
     */
    public void sendFirstAvailable(String [] smtpServers, String msg) throws Exception {
        Stream.of(smtpServers)
        .map(s -> Try.of(() -> sendMail(s, msg)))
        //.peek(Try::logException)
        .flatMap(Try::stream)
        .findFirst()
        .get();
    }
    
    public boolean sendWithRetry(String [] smtpServers, String msg) {
        
        var handler = Retry.Handler.simple(10, Duration.ofMillis(10))
        .also((i, th) -> this.onError(i, 10, th));
        CompletableFuture<Void> future = 
                Retry.of(() -> sendFirstAvailable(smtpServers, msg))
                .withHandler(handler)
                .retry();
        return Try.of(() -> future.get()).isSuccess();
    }
    
    void onError(long cur, long max, Throwable th) {
        System.out.println(String.format("%d of %d %s", cur, max, th.getMessage()));
    }


}
