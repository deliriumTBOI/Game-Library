package com.gamelib.gamelib.service.impl;

import com.gamelib.gamelib.service.LogService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.Getter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Getter
public class LogServiceImpl implements LogService {
    private static final String LOG_FILE_PATH = "logs/gamelib.log";
    private static final String LOGS_DIR = "logs/";

    private final Map<String, String> logFiles = new ConcurrentHashMap<>();
    private final Map<String, String> taskStatus = new ConcurrentHashMap<>();

    @Override
    @Async
    public CompletableFuture<String> generateLogFileForDateAsync(String date) {
        String taskId = UUID.randomUUID().toString();
        taskStatus.put(taskId, "PROCESSING");

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(20000);

                Path sourcePath = Paths.get(LOG_FILE_PATH);
                if (!Files.exists(sourcePath)) {
                    throw new IllegalStateException("Source log file not found");
                }

                List<String> filteredLines;
                try (Stream<String> lines = Files.lines(sourcePath)) {
                    filteredLines = lines
                            .filter(line -> line.startsWith(date))
                            .toList();
                }

                if (filteredLines.isEmpty()) {
                    throw new IllegalStateException("No logs found for date");
                }

                Files.createDirectories(Paths.get(LOGS_DIR));
                String filename = String.format("%slogs-%s-%s.log", LOGS_DIR, date, taskId);
                Files.write(Paths.get(filename), filteredLines);

                logFiles.put(taskId, filename);
                taskStatus.put(taskId, "COMPLETED");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // ✅ правильный способ обработки
                taskStatus.put(taskId, "FAILED: Task was interrupted");

            } catch (Exception e) {
                String errorMsg = e.getMessage();
                taskStatus.put(taskId, "FAILED: " + errorMsg);
            }
        });

        return CompletableFuture.completedFuture(taskId);
    }


    @Override
    public String getLogFilePath(String taskId) {
        return logFiles.get(taskId);
    }

    @Override
    public String getTaskStatus(String taskId) {
        return taskStatus.getOrDefault(taskId, "NOT_FOUND");
    }
}