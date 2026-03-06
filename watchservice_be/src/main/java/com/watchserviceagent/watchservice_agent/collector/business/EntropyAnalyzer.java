package com.watchserviceagent.watchservice_agent.collector.business;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 클래스 이름 : EntropyAnalyzer
 * 기능 : 파일의 Shannon 엔트로피를 계산하는 유틸리티 컴포넌트. 암호화된 파일 탐지에 사용된다.
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
@Component
public class EntropyAnalyzer {

    /**
     * 함수 이름 : computeSampleEntropy
     * 기능 : 파일에서 최대 maxBytes 만큼 읽어 Shannon 엔트로피를 계산한다. 바이트 값 0~255의 출현 빈도를 세어 계산한다.
     * 매개변수 : path - 대상 파일 경로, maxBytes - 최대 샘플 바이트 수 (예: 4096)
     * 반환값 : double - 엔트로피 값 (0 이상, 대략 최대 8 근처)
     * 예외 : IOException - 파일 읽기 실패 시
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    public double computeSampleEntropy(Path path, int maxBytes) throws IOException {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }

        int[] freq = new int[256];
        int total = 0;

        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[1024];
            while (total < maxBytes) {
                int remaining = maxBytes - total;
                int toRead = Math.min(buffer.length, remaining);
                int read = in.read(buffer, 0, toRead);
                if (read == -1) {
                    break; // EOF
                }
                for (int i = 0; i < read; i++) {
                    int b = buffer[i] & 0xFF;
                    freq[b]++;
                }
                total += read;
            }
        }

        if (total == 0) {
            // 빈 파일 등의 경우 엔트로피 0
            return 0.0;
        }

        double entropy = 0.0;
        for (int count : freq) {
            if (count == 0) continue;
            double p = (double) count / (double) total;
            entropy -= p * (log2(p));
        }
        return entropy;
    }

    /**
     * 함수 이름 : computeSampleEntropy
     * 기능 : 기본 샘플 크기(4096 bytes)로 엔트로피를 계산한다.
     * 매개변수 : path - 대상 파일 경로
     * 반환값 : double - 엔트로피 값
     * 예외 : IOException - 파일 읽기 실패 시
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    public double computeSampleEntropy(Path path) throws IOException {
        return computeSampleEntropy(path, 4096);
    }

    /**
     * 함수 이름 : computeMultiSectionEntropy
     * 기능 : 파일을 앞(40%)/중간(40%)/끝(20%) 3구간에서 샘플링하여 Shannon 엔트로피를 계산한다.
     *        파일 크기가 totalSampleBytes 이하이면 기존 순차 읽기 방식을 사용한다.
     *        중간·끝부터 암호화하는 랜섬웨어나 PDF/ZIP 헤더 파일에서 더 정확한 엔트로피를 반환한다.
     * 매개변수 : path - 대상 파일 경로, totalSampleBytes - 전체 샘플 바이트 수 (예: 4096)
     * 반환값 : double - 엔트로피 값 (0 이상, 최대 약 8)
     * 예외 : IOException - 파일 읽기 실패 시
     * 작성 날짜 : 2026/03/06
     * 작성자 : 시스템
     */
    public double computeMultiSectionEntropy(Path path, int totalSampleBytes) throws IOException {
        if (totalSampleBytes <= 0) {
            throw new IllegalArgumentException("totalSampleBytes must be positive");
        }

        long fileSize = Files.size(path);

        if (fileSize <= totalSampleBytes) {
            return computeSampleEntropy(path, totalSampleBytes);
        }

        // 3구간 샘플 크기: 앞 40%, 중간 40%, 끝 20%
        int frontBytes  = (int) (totalSampleBytes * 0.4);
        int middleBytes = (int) (totalSampleBytes * 0.4);
        int endBytes    = totalSampleBytes - frontBytes - middleBytes; // 나머지 (≈20%)

        int[] freq = new int[256];
        int total = 0;

        try (SeekableByteChannel ch = Files.newByteChannel(path)) {
            // 구간 1: 파일 앞
            total += readSection(ch, 0L, frontBytes, freq);

            // 구간 2: 파일 중간 (파일 중앙 - middleBytes/2 위치)
            long middleOffset = (fileSize / 2) - (middleBytes / 2);
            if (middleOffset < frontBytes) middleOffset = frontBytes;
            total += readSection(ch, middleOffset, middleBytes, freq);

            // 구간 3: 파일 끝
            long endOffset = fileSize - endBytes;
            if (endOffset < middleOffset + middleBytes) endOffset = middleOffset + middleBytes;
            total += readSection(ch, endOffset, endBytes, freq);
        }

        if (total == 0) return 0.0;

        double entropy = 0.0;
        for (int count : freq) {
            if (count == 0) continue;
            double p = (double) count / (double) total;
            entropy -= p * log2(p);
        }
        return entropy;
    }

    private int readSection(SeekableByteChannel ch, long offset, int length, int[] freq) throws IOException {
        if (length <= 0) return 0;
        ch.position(offset);
        ByteBuffer buf = ByteBuffer.allocate(Math.min(length, 4096));
        int remaining = length;
        int read = 0;
        while (remaining > 0) {
            buf.clear();
            if (buf.capacity() > remaining) buf.limit(remaining);
            int n = ch.read(buf);
            if (n <= 0) break;
            buf.flip();
            for (int i = 0; i < n; i++) {
                freq[buf.get() & 0xFF]++;
            }
            read += n;
            remaining -= n;
        }
        return read;
    }

    /**
     * 함수 이름 : computeEntropy
     * 기능 : 메모리 상의 바이트 배열 전체에 대해 엔트로피를 계산한다.
     * 매개변수 : data - 바이트 배열
     * 반환값 : double - 엔트로피 값
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    public double computeEntropy(byte[] data) {
        if (data == null || data.length == 0) {
            return 0.0;
        }

        int[] freq = new int[256];
        for (byte b : data) {
            freq[b & 0xFF]++;
        }

        int total = data.length;
        double entropy = 0.0;

        for (int count : freq) {
            if (count == 0) continue;
            double p = (double) count / (double) total;
            entropy -= p * (log2(p));
        }
        return entropy;
    }

    /**
     * 함수 이름 : log2
     * 기능 : 밑이 2인 로그를 계산한다.
     * 매개변수 : x - 입력 값
     * 반환값 : double - log2(x)
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    private double log2(double x) {
        return Math.log(x) / Math.log(2.0);
    }
}
