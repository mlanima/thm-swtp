package de.thm.swtp.api.tag.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class BlocklistService {

    private final Set<String> blockedWords;

    public BlocklistService(final ResourcePatternResolver resourceLoader) {
        var words = new HashSet<String>();
        try {
            var resources = resourceLoader.getResources("classpath:bad-words/*");
            for (var resource : resources) {
                loadFile(resource, words);
            }
            log.info("Loaded {} blocked words from {} language files", words.size(), resources.length);
        } catch (IOException e) {
            log.warn("Could not load bad-words blocklist — blocklist is empty", e);
        }
        this.blockedWords = Set.copyOf(words);
    }

    private static void loadFile(final Resource resource, final Set<String> words) {
        var filename = resource.getFilename();
        if (filename == null) {
            return;
        }
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            var count = 0;
            while ((line = reader.readLine()) != null) {
                var trimmed = line.trim().toLowerCase();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    words.add(trimmed);
                    count++;
                }
            }
            log.debug("Loaded {} words from bad-words/{}", count, filename);
        } catch (IOException e) {
            log.warn("Failed to read bad-words/{}", filename, e);
        }
    }

    public boolean contains(final String word) {
        return blockedWords.contains(word.toLowerCase().trim());
    }
}
