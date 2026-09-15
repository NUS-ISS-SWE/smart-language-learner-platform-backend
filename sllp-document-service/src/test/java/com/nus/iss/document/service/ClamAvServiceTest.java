package com.nus.iss.document.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.nus.iss.document.exception.MalwareDetectedException;
import com.nus.iss.document.exception.MalwareScanUnavailableException;

class ClamAvServiceTest {

    @Test
    void streamsFileBytesUsingClamAvProtocol() throws Exception {
        byte[] expected = new byte[9_000];
        for (int index = 0; index < expected.length; index++) {
            expected[index] = (byte) (index % 251);
        }

        try (ServerSocket server = new ServerSocket(0)) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<byte[]> received = executor.submit(() -> receiveScanAndReplyClean(server));
                ClamAvService service = new ClamAvService(
                        "127.0.0.1", server.getLocalPort(), 1_000, 1_000);

                service.scan(expected);

                assertArrayEquals(expected, received.get());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void acceptsCleanResponse() {
        assertDoesNotThrow(() -> ClamAvService.interpretResponse("stream: OK"));
    }

    @Test
    void rejectsDetectedMalware() {
        assertThrows(MalwareDetectedException.class,
                () -> ClamAvService.interpretResponse("stream: Win.Test.EICAR_HDB-1 FOUND"));
    }

    @Test
    void failsClosedForScannerError() {
        assertThrows(MalwareScanUnavailableException.class,
                () -> ClamAvService.interpretResponse("stream: INSTREAM size limit exceeded. ERROR"));
    }

    @Test
    void failsClosedForUnexpectedResponse() {
        assertThrows(MalwareScanUnavailableException.class,
                () -> ClamAvService.interpretResponse("unexpected"));
    }

    private static byte[] receiveScanAndReplyClean(ServerSocket server) throws Exception {
        try (Socket socket = server.accept();
                DataInputStream input = new DataInputStream(socket.getInputStream())) {
            byte[] command = input.readNBytes(10);
            assertArrayEquals("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII), command);

            ByteArrayOutputStream received = new ByteArrayOutputStream();
            int length;
            while ((length = input.readInt()) != 0) {
                received.write(input.readNBytes(length));
            }

            socket.getOutputStream().write("stream: OK\0".getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return received.toByteArray();
        }
    }
}
