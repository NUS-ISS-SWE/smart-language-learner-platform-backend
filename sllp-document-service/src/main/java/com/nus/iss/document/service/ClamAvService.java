package com.nus.iss.document.service;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nus.iss.document.exception.MalwareDetectedException;
import com.nus.iss.document.exception.MalwareScanUnavailableException;

import lombok.extern.slf4j.Slf4j;

/** Streams uploaded file bytes to a ClamAV daemon before they are stored or parsed. */
@Service
@Slf4j
public class ClamAvService {

    private static final byte[] INSTREAM_COMMAND = "zINSTREAM\0".getBytes(StandardCharsets.US_ASCII);
    private static final int CHUNK_SIZE = 8 * 1024;
    private static final int MAX_RESPONSE_SIZE = 4 * 1024;

    private final String host;
    private final int port;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public ClamAvService(
            @Value("${application.malware-scan.host:localhost}") String host,
            @Value("${application.malware-scan.port:3310}") int port,
            @Value("${application.malware-scan.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${application.malware-scan.read-timeout-ms:30000}") int readTimeoutMs) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * Scan a complete upload using ClamAV's INSTREAM protocol.
     *
     * @param fileBytes upload bytes to scan
     * @throws MalwareDetectedException if ClamAV detects malware
     * @throws MalwareScanUnavailableException if no conclusive clean result is available
     */
    public void scan(byte[] fileBytes) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);

            try (DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
                output.write(INSTREAM_COMMAND);
                for (int offset = 0; offset < fileBytes.length; offset += CHUNK_SIZE) {
                    int length = Math.min(CHUNK_SIZE, fileBytes.length - offset);
                    output.writeInt(length);
                    output.write(fileBytes, offset, length);
                }
                output.writeInt(0);
                output.flush();

                String response = readResponse(socket.getInputStream());
                interpretResponse(response);
            }
        } catch (MalwareDetectedException | MalwareScanUnavailableException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("ClamAV scan failed because the scanner is unavailable");
            throw new MalwareScanUnavailableException(
                    "File could not be scanned for malware; please try again later", ex);
        }
    }

    private static String readResponse(InputStream input) throws IOException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        int current;
        while ((current = input.read()) != -1 && current != 0) {
            if (response.size() >= MAX_RESPONSE_SIZE) {
                throw new IOException("ClamAV response exceeded the allowed size");
            }
            response.write(current);
        }
        if (response.size() == 0 || current == -1) {
            throw new IOException("ClamAV returned an incomplete response");
        }
        return response.toString(StandardCharsets.US_ASCII);
    }

    static void interpretResponse(String response) {
        if (response.endsWith(": OK")) {
            return;
        }
        if (response.endsWith(" FOUND")) {
            log.warn("ClamAV rejected an uploaded file: {}", response);
            throw new MalwareDetectedException();
        }

        log.error("ClamAV returned an inconclusive scan result: {}", response);
        throw new MalwareScanUnavailableException(
                "File could not be scanned for malware; please try again later");
    }
}
